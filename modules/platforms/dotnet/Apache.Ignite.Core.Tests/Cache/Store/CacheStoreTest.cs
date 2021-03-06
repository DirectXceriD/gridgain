/*
 *                   GridGain Community Edition Licensing
 *                   Copyright 2019 GridGain Systems, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License") modified with Commons Clause
 * Restriction; you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 *
 * Commons Clause Restriction
 *
 * The Software is provided to you by the Licensor under the License, as defined below, subject to
 * the following condition.
 *
 * Without limiting other conditions in the License, the grant of rights under the License will not
 * include, and the License does not grant to you, the right to Sell the Software.
 * For purposes of the foregoing, “Sell” means practicing any or all of the rights granted to you
 * under the License to provide to third parties, for a fee or other consideration (including without
 * limitation fees for hosting or consulting/ support services related to the Software), a product or
 * service whose value derives, entirely or substantially, from the functionality of the Software.
 * Any license notice or attribution required by the License must also include this Commons Clause
 * License Condition notice.
 *
 * For purposes of the clause above, the “Licensor” is Copyright 2019 GridGain Systems, Inc.,
 * the “License” is the Apache License, Version 2.0, and the Software is the GridGain Community
 * Edition software provided with this notice.
 */

namespace Apache.Ignite.Core.Tests.Cache.Store
{
    using System;
    using System.Collections;
    using System.Collections.Generic;
    using System.Linq;
    using Apache.Ignite.Core.Binary;
    using Apache.Ignite.Core.Cache;
    using Apache.Ignite.Core.Cache.Configuration;
    using Apache.Ignite.Core.Cache.Store;
    using Apache.Ignite.Core.Common;
    using Apache.Ignite.Core.Impl;
    using NUnit.Framework;

    /// <summary>
    /// Tests cache store functionality.
    /// </summary>
    public class CacheStoreTest
    {
        /** */
        private const string BinaryStoreCacheName = "binary_store";

        /** */
        private const string ObjectStoreCacheName = "object_store";

        /** */
        private const string CustomStoreCacheName = "custom_store";

        /** */
        private const string TemplateStoreCacheName = "template_store*";

        /// <summary>
        /// Fixture set up.
        /// </summary>
        [TestFixtureSetUp]
        public virtual void BeforeTests()
        {
            Restart();
        }

        /// <summary>
        /// Fixture tear down.
        /// </summary>
        [TestFixtureTearDown]
        public void AfterTests()
        {
            try
            {
                // 3 stores are expected in HandleRegistry.
                TestUtils.AssertHandleRegistryHasItems(Ignition.GetIgnite(), 3, 1000);
            }
            finally
            {
                Ignition.StopAll(true);
            }
        }

        
        /// <summary>
        /// Stop all instances, set and start a test instance.
        /// </summary>
        private void Restart()
        {
            Ignition.StopAll(true);
            
            Ignition.Start(new IgniteConfiguration(TestUtils.GetTestConfiguration())
            {
                IgniteInstanceName = GridName,
                SpringConfigUrl = "config\\native-client-test-cache-store.xml",
            });
        }

        /// <summary>
        /// Test tear down.
        /// </summary>
        [TearDown]
        public void AfterTest()
        {
            CacheTestStore.Reset();

            var cache = GetCache();

            cache.Clear();

            Assert.IsTrue(cache.IsEmpty(),
                "Cache is not empty: " +
                string.Join(", ", cache.Select(x => string.Format("[{0}:{1}]", x.Key, x.Value))));

            TestUtils.AssertHandleRegistryHasItems(300, 3, Ignition.GetIgnite(GridName));
        }

        /// <summary>
        /// Tests that simple cache loading works and exceptions are propagated properly.
        /// </summary>
        [Test]
        public void TestLoadCache()
        {
            var cache = GetCache();

            Assert.AreEqual(0, cache.GetSize());

            cache.LoadCache(new CacheEntryFilter(), 100, 10);

            Assert.AreEqual(5, cache.GetSize());

            for (int i = 105; i < 110; i++)
                Assert.AreEqual("val_" + i, cache.Get(i));

            // Test exception in filter
            Assert.Throws<CacheStoreException>(() => cache.LoadCache(new ExceptionalEntryFilter(), 100, 10));

            // Test exception in store
            CacheTestStore.ThrowError = true;
            CheckCustomStoreError(Assert.Throws<CacheStoreException>(() =>
                cache.LoadCache(new CacheEntryFilter(), 100, 10)).InnerException);
        }

        /// <summary>
        /// Tests cache loading in local mode.
        /// </summary>
        [Test]
        public void TestLocalLoadCache()
        {
            var cache = GetCache();

            Assert.AreEqual(0, cache.GetSize());

            cache.LocalLoadCache(new CacheEntryFilter(), 100, 10);

            Assert.AreEqual(5, cache.GetSize());

            for (int i = 105; i < 110; i++)
                Assert.AreEqual("val_" + i, cache.Get(i));
        }

        /// <summary>
        /// Tests that object metadata propagates properly during cache loading.
        /// </summary>
        [Test]
        public void TestLoadCacheMetadata()
        {
            CacheTestStore.LoadObjects = true;

            var cache = GetCache();

            Assert.AreEqual(0, cache.GetSize());

            cache.LocalLoadCache(null, 0, 3);

            Assert.AreEqual(3, cache.GetSize());

            var meta = cache.WithKeepBinary<Key, IBinaryObject>().Get(new Key(0)).GetBinaryType();

            Assert.NotNull(meta);

            Assert.AreEqual("Apache.Ignite.Core.Tests.Cache.Store.Value", meta.TypeName);
        }

        /// <summary>
        /// Tests asynchronous cache load.
        /// </summary>
        [Test]
        public void TestLoadCacheAsync()
        {
            var cache = GetCache();

            Assert.AreEqual(0, cache.GetSize());

            cache.LocalLoadCacheAsync(new CacheEntryFilter(), 100, 10).Wait();

            Assert.AreEqual(5, cache.GetSizeAsync().Result);

            for (int i = 105; i < 110; i++)
            {
                Assert.AreEqual("val_" + i, cache.GetAsync(i).Result);
            }

            // Test errors
            CacheTestStore.ThrowError = true;
            CheckCustomStoreError(
                Assert.Throws<AggregateException>(
                    () => cache.LocalLoadCacheAsync(new CacheEntryFilter(), 100, 10).Wait())
                    .InnerException);
        }

        /// <summary>
        /// Tests write-through and read-through behavior.
        /// </summary>
        [Test]
        public void TestPutLoad()
        {
            var cache = GetCache();

            cache.Put(1, "val");

            IDictionary map = GetStoreMap();

            Assert.AreEqual(1, map.Count);

            // TODO: IGNITE-4535
            //cache.LocalEvict(new[] { 1 });

            //Assert.AreEqual(0, cache.GetSize(CachePeekMode.All));

            Assert.AreEqual("val", cache.Get(1));

            Assert.AreEqual(1, cache.GetSize());
        }

        /// <summary>
        /// Tests that exceptions from user code are propagated properly.
        /// </summary>
        [Test]
        public void TestExceptions()
        {
            var cache = GetCache();

            cache.Put(1, "val");

            CacheTestStore.ThrowError = true;
            
            var ex = Assert.Throws<CacheStoreException>(() => cache.Put(-2, "fail"));

            Assert.IsTrue(ex.ToString().Contains(
                "at Apache.Ignite.Core.Tests.Cache.Store.CacheTestStore.ThrowIfNeeded"));  // Check proper stack trace.

            Assert.IsNotNull(ex.InnerException);  // RollbackException.

            var javaEx = ex.InnerException.InnerException as JavaException;
            Assert.IsNotNull(javaEx);

            CheckCustomStoreError(javaEx.InnerException);

            // TODO: IGNITE-4535
            //cache.LocalEvict(new[] {1});
            //CheckCustomStoreError(Assert.Throws<CacheStoreException>(() => cache.Get(1)).InnerException);

            CacheTestStore.ThrowError = false;

            cache.Remove(1);
        }
        
        /// <summary>
        /// Tests that exceptions from CacheStoreFactory are propagated properly.
        /// </summary>
        [Test]
        public void TestFailedCacheStoreException()
        {
            try
            {
                var ccfg = new CacheConfiguration("CacheWithFailedStore")
                {
                    CacheStoreFactory = new FailedCacheStoreFactory(),
                    ReadThrough = true
                };

                Assert.Throws<CacheException>(() => Ignition.GetIgnite(GridName).GetOrCreateCache<int, int>(ccfg));
            }
            finally
            {
                // After failed cache grid is in ivalid state. Should be restarted.
                Restart();
            }
          
        }

        [Test]
        [Ignore("IGNITE-4657")]
        public void TestExceptionsNoRemove()
        {
            var cache = GetCache();

            cache.Put(1, "val");

            CacheTestStore.ThrowError = true;
            CheckCustomStoreError(Assert.Throws<CacheStoreException>(() => cache.Put(-2, "fail")).InnerException);

            cache.LocalEvict(new[] {1});
            CheckCustomStoreError(Assert.Throws<CacheStoreException>(() => cache.Get(1)).InnerException);
        }

        /// <summary>
        /// Tests write-through and read-through behavior with binarizable values.
        /// </summary>
        [Test]
        public void TestPutLoadBinarizable()
        {
            var cache = GetBinaryStoreCache<int, Value>();

            cache.Put(1, new Value(1));

            IDictionary map = GetStoreMap();

            Assert.AreEqual(1, map.Count);

            IBinaryObject v = (IBinaryObject)map[1];

            Assert.AreEqual(1, v.GetField<int>("_idx"));

            // TODO: IGNITE-4535
            //cache.LocalEvict(new[] { 1 });

            //Assert.AreEqual(0, cache.GetSize());

            Assert.AreEqual(1, cache.Get(1).Index);

            Assert.AreEqual(1, cache.GetSize());
        }

        /// <summary>
        /// Tests write-through and read-through behavior with storeKeepBinary=false.
        /// </summary>
        [Test]
        public void TestPutLoadObjects()
        {
            var cache = GetObjectStoreCache<int, Value>();

            cache.Put(1, new Value(1));

            IDictionary map = GetStoreMap();

            Assert.AreEqual(1, map.Count);

            Value v = (Value)map[1];

            Assert.AreEqual(1, v.Index);

            // TODO: IGNITE-4535
            //cache.LocalEvict(new[] { 1 });

            //Assert.AreEqual(0, cache.GetSize());

            Assert.AreEqual(1, cache.Get(1).Index);

            Assert.AreEqual(1, cache.GetSize());
        }

        /// <summary>
        /// Tests cache store LoadAll functionality.
        /// </summary>
        [Test]
        public void TestPutLoadAll()
        {
            var putMap = new Dictionary<int, string>();

            for (int i = 0; i < 10; i++)
                putMap.Add(i, "val_" + i);

            var cache = GetCache();

            cache.PutAll(putMap);

            IDictionary map = GetStoreMap();

            Assert.AreEqual(10, map.Count);

            for (int i = 0; i < 10; i++)
                Assert.AreEqual("val_" + i, map[i]);

            cache.Clear();

            Assert.AreEqual(0, cache.GetSize());

            ICollection<int> keys = new List<int>();

            for (int i = 0; i < 10; i++)
                keys.Add(i);

            IDictionary<int, string> loaded = cache.GetAll(keys).ToDictionary(x => x.Key, x => x.Value);

            Assert.AreEqual(10, loaded.Count);

            for (int i = 0; i < 10; i++)
                Assert.AreEqual("val_" + i, loaded[i]);

            Assert.AreEqual(10, cache.GetSize());
        }

        /// <summary>
        /// Tests cache store removal.
        /// </summary>
        [Test]
        public void TestRemove()
        {
            var cache = GetCache();

            for (int i = 0; i < 10; i++)
                cache.Put(i, "val_" + i);

            IDictionary map = GetStoreMap();

            Assert.AreEqual(10, map.Count);

            for (int i = 0; i < 5; i++)
                cache.Remove(i);

            Assert.AreEqual(5, map.Count);

            for (int i = 5; i < 10; i++)
                Assert.AreEqual("val_" + i, map[i]);
        }

        /// <summary>
        /// Tests cache store removal.
        /// </summary>
        [Test]
        public void TestRemoveAll()
        {
            var cache = GetCache();

            for (int i = 0; i < 10; i++)
                cache.Put(i, "val_" + i);

            IDictionary map = GetStoreMap();

            Assert.AreEqual(10, map.Count);

            cache.RemoveAll(new List<int> { 0, 1, 2, 3, 4 });

            Assert.AreEqual(5, map.Count);

            for (int i = 5; i < 10; i++)
                Assert.AreEqual("val_" + i, map[i]);
        }

        /// <summary>
        /// Tests cache store with transactions.
        /// </summary>
        [Test]
        public void TestTx()
        {
            var cache = GetCache();

            using (var tx = cache.Ignite.GetTransactions().TxStart())
            {
                tx.AddMeta("meta", 100);

                cache.Put(1, "val");

                tx.Commit();
            }

            IDictionary map = GetStoreMap();

            Assert.AreEqual(1, map.Count);

            Assert.AreEqual("val", map[1]);
        }

        /// <summary>
        /// Tests multithreaded cache loading.
        /// </summary>
        [Test]
        public void TestLoadCacheMultithreaded()
        {
            CacheTestStore.LoadMultithreaded = true;

            var cache = GetCache();

            Assert.AreEqual(0, cache.GetSize());

            cache.LocalLoadCache(null, 0, null);

            Assert.AreEqual(1000, cache.GetSize());

            for (int i = 0; i < 1000; i++)
                Assert.AreEqual("val_" + i, cache.Get(i));
        }

        /// <summary>
        /// Tests that cache store property values are propagated from Spring XML.
        /// </summary>
        [Test]
        public void TestCustomStoreProperties()
        {
            var cache = GetCustomStoreCache();
            Assert.IsNotNull(cache);

            Assert.AreEqual(42, CacheTestStore.intProperty);
            Assert.AreEqual("String value", CacheTestStore.stringProperty);
        }

        /// <summary>
        /// Tests cache store with dynamically started cache.
        /// </summary>
        [Test]
        public void TestDynamicStoreStart()
        {
            var grid = Ignition.GetIgnite(GridName);
            var reg = ((Ignite) grid).HandleRegistry;
            var handleCount = reg.Count;

            var cache = GetTemplateStoreCache();
            Assert.IsNotNull(cache);

            cache.Put(1, cache.Name);
            Assert.AreEqual(cache.Name, CacheTestStore.Map[1]);

            Assert.AreEqual(handleCount + 1, reg.Count);
            grid.DestroyCache(cache.Name);
            Assert.AreEqual(handleCount, reg.Count);
        }

        /// <summary>
        /// Tests the load all.
        /// </summary>
        [Test]
        public void TestLoadAll([Values(true, false)] bool isAsync)
        {
            var cache = GetCache();

            var loadAll = isAsync
                ? (Action<IEnumerable<int>, bool>) ((x, y) => { cache.LoadAllAsync(x, y).Wait(); })
                : cache.LoadAll;

            Assert.AreEqual(0, cache.GetSize());

            loadAll(Enumerable.Range(105, 5), false);

            Assert.AreEqual(5, cache.GetSize());

            for (int i = 105; i < 110; i++)
                Assert.AreEqual("val_" + i, cache[i]);

            // Test overwrite
            cache[105] = "42";

            cache.LocalEvict(new[] { 105 });
            loadAll(new[] {105}, false);
            Assert.AreEqual("42", cache[105]);

            loadAll(new[] {105, 106}, true);
            Assert.AreEqual("val_105", cache[105]);
            Assert.AreEqual("val_106", cache[106]);
        }

        /// <summary>
        /// Tests the argument passing to LoadCache method.
        /// </summary>
        [Test]
        public void TestArgumentPassing()
        {
            var cache = GetBinaryStoreCache<object, object>();

            Action<object> checkValue = o =>
            {
                cache.Clear();
                Assert.AreEqual(0, cache.GetSize());
                cache.LoadCache(null, null, 1, o);
                Assert.AreEqual(o, cache[1]);
            };

            // Null.
            cache.LoadCache(null, null);
            Assert.AreEqual(0, cache.GetSize());

            // Empty args array.
            cache.LoadCache(null);
            Assert.AreEqual(0, cache.GetSize());

            // Simple types.
            checkValue(1);
            checkValue(new[] {1, 2, 3});

            checkValue("1");
            checkValue(new[] {"1", "2"});

            checkValue(Guid.NewGuid());
            checkValue(new[] {Guid.NewGuid(), Guid.NewGuid()});

            checkValue(DateTime.Now);
            checkValue(new[] {DateTime.Now, DateTime.UtcNow});

            // Collections.
            checkValue(new ArrayList {1, "2", 3.3});
            checkValue(new List<int> {1, 2});
            checkValue(new Dictionary<int, string> {{1, "foo"}});
        }

        /// <summary>
        /// Get's grid name for this test.
        /// </summary>
        /// <value>Grid name.</value>
        protected virtual string GridName
        {
            get { return null; }
        }

        /// <summary>
        /// Gets the store map.
        /// </summary>
        private static IDictionary GetStoreMap()
        {
            return CacheTestStore.Map;
        }

        /// <summary>
        /// Gets the cache.
        /// </summary>
        private ICache<int, string> GetCache()
        {
            return GetBinaryStoreCache<int, string>();
        }

        /// <summary>
        /// Gets the binary store cache.
        /// </summary>
        private ICache<TK, TV> GetBinaryStoreCache<TK, TV>()
        {
            return Ignition.GetIgnite(GridName).GetCache<TK, TV>(BinaryStoreCacheName);
        }

        /// <summary>
        /// Gets the object store cache.
        /// </summary>
        private ICache<TK, TV> GetObjectStoreCache<TK, TV>()
        {
            return Ignition.GetIgnite(GridName).GetCache<TK, TV>(ObjectStoreCacheName);
        }

        /// <summary>
        /// Gets the custom store cache.
        /// </summary>
        private ICache<int, string> GetCustomStoreCache()
        {
            return Ignition.GetIgnite(GridName).GetCache<int, string>(CustomStoreCacheName);
        }

        /// <summary>
        /// Gets the template store cache.
        /// </summary>
        private ICache<int, string> GetTemplateStoreCache()
        {
            var cacheName = TemplateStoreCacheName.Replace("*", Guid.NewGuid().ToString());
            
            return Ignition.GetIgnite(GridName).GetOrCreateCache<int, string>(cacheName);
        }

        /// <summary>
        /// Checks the custom store error.
        /// </summary>
        private static void CheckCustomStoreError(Exception err)
        {
            var customErr = err.GetBaseException() as CacheTestStore.CustomStoreException;

            Assert.IsNotNull(customErr);
            Assert.AreEqual(customErr.Message, customErr.Details);
        }
    }

    /// <summary>
    /// Cache key.
    /// </summary>
    internal class Key
    {
        /** */
        private readonly int _idx;

        /// <summary>
        /// Initializes a new instance of the <see cref="Key"/> class.
        /// </summary>
        public Key(int idx)
        {
            _idx = idx;
        }

        /** <inheritdoc /> */
        public override bool Equals(object obj)
        {
            if (obj == null || obj.GetType() != GetType())
                return false;

            return ((Key)obj)._idx == _idx;
        }

        /** <inheritdoc /> */
        public override int GetHashCode()
        {
            return _idx;
        }
    }

    /// <summary>
    /// Cache value.
    /// </summary>
    internal class Value
    {
        /** */
        private readonly int _idx;

        /// <summary>
        /// Initializes a new instance of the <see cref="Value"/> class.
        /// </summary>
        public Value(int idx)
        {
            _idx = idx;
        }

        /// <summary>
        /// Gets the index.
        /// </summary>
        public int Index
        {
            get { return _idx; }
        }
    }

    /// <summary>
    /// Cache entry predicate.
    /// </summary>
    [Serializable]
    public class CacheEntryFilter : ICacheEntryFilter<int, string>
    {
        /** <inheritdoc /> */
        public bool Invoke(ICacheEntry<int, string> entry)
        {
            return entry.Key >= 105;
        }
    }

    /// <summary>
    /// Cache entry predicate that throws an exception.
    /// </summary>
    [Serializable]
    public class ExceptionalEntryFilter : ICacheEntryFilter<int, string>
    {
        /** <inheritdoc /> */
        public bool Invoke(ICacheEntry<int, string> entry)
        {
            throw new Exception("Expected exception in ExceptionalEntryFilter");
        }
    }

    /// <summary>
    /// Test factory.
    /// </summary>
    [Serializable]
    public class FailedCacheStoreFactory : IFactory<ICacheStore>
    {
        /// <summary>
        /// Creates an instance of the cache store. Throws an exception during creation.
        /// </summary>
        public ICacheStore CreateInstance()
        {
            throw new Exception("FailedCacheStoreFactory.CreateInstance exception");
        }
    }
}
