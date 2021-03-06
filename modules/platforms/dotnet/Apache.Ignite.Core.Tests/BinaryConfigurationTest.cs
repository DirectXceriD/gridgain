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

namespace Apache.Ignite.Core.Tests
{
    using System;
    using System.Collections;
    using System.Collections.Generic;
    using System.Linq;
    using Apache.Ignite.Core.Binary;
    using Apache.Ignite.Core.Cache;
    using Apache.Ignite.Core.Common;
    using NUnit.Framework;

    /// <summary>
    /// Binary configuration tests.
    /// </summary>
    public class BinaryConfigurationTest
    {
        /** Cache. */
        private ICache<int, TestGenericBinarizableBase> _cache;

        /** Random generator. */
        private static readonly Random Rnd = new Random();

        /** Test types for code config */
        private static readonly Type[] TestTypes = {
            typeof (TestGenericBinarizable<int>),
            typeof (TestGenericBinarizable<string>),
            typeof (TestGenericBinarizable<TestGenericBinarizable<int>>),
            typeof (TestGenericBinarizable<List<Tuple<int, string>>>),
            typeof (TestGenericBinarizable<int, string>),
            typeof (TestGenericBinarizable<int, TestGenericBinarizable<string>>),
            typeof (TestGenericBinarizable<int, string, Type>),
            typeof (TestGenericBinarizable<int, string, TestGenericBinarizable<int, string, Type>>)
        };

        /** Test types for xml config */
        private static readonly Type[] TestTypesXml = {
            typeof (TestGenericBinarizable<long>),
            typeof (TestGenericBinarizable<Type>),
            typeof (TestGenericBinarizable<TestGenericBinarizable<long>>),
            typeof (TestGenericBinarizable<List<Tuple<long, string>>>),
            typeof (TestGenericBinarizable<long, string>),
            typeof (TestGenericBinarizable<long, TestGenericBinarizable<string>>),
            typeof (TestGenericBinarizable<long, string, Type>),
            typeof (TestGenericBinarizable<long, string, TestGenericBinarizable<long, string, Type>>)
        };

        /// <summary>
        /// Starts the grid with provided config.
        /// </summary>
        /// <param name="binaryConfiguration">The binary configuration.</param>
        private void StartGrid(BinaryConfiguration binaryConfiguration)
        {
            Ignition.StopAll(true);

            var grid = Ignition.Start(new IgniteConfiguration(TestUtils.GetTestConfiguration())
            {
                SpringConfigUrl = "Config\\cache-binarizables.xml",
                BinaryConfiguration = binaryConfiguration
            });

            _cache = grid.GetCache<int, TestGenericBinarizableBase>("default");
        }

        /// <summary>
        /// Test fixture tear-down routine.
        /// </summary>
        [TestFixtureTearDown]
        public void TestFixtureTearDown()
        {
            Ignition.StopAll(true);
        }

        /// <summary>
        /// Tests the configuration set in code.
        /// </summary>
        [Test]
        public void TestCodeConfiguration()
        {
            var cfg = new BinaryConfiguration
            {
                TypeConfigurations = TestTypes.Select(x => new BinaryTypeConfiguration(x)).ToList()
            };

            StartGrid(new BinaryConfiguration(cfg));

            CheckBinarizableTypes(TestTypes);
        }

        /// <summary>
        /// Tests the configuration set in xml.
        /// </summary>
        [Test]
        public void TestXmlConfiguration()
        {
            StartGrid(null);

            CheckBinarizableTypes(TestTypesXml);
        }

        /// <summary>
        /// Tests that invalid configuration produces meaningful error message.
        /// </summary>
        [Test]
        public void TestInvalidConfiguration()
        {
            // Pass open generic type.
            var cfg = new IgniteConfiguration(TestUtils.GetTestConfiguration())
            {
                // Open generics are not allowed
                BinaryConfiguration = new BinaryConfiguration(typeof(List<>))
            };

            var ex = Assert.Throws<IgniteException>(() => Ignition.Start(cfg));
            Assert.AreEqual("Failed to start Ignite.NET, check inner exception for details", ex.Message);
            Assert.IsNotNull(ex.InnerException);
            Assert.IsTrue(ex.InnerException.Message.StartsWith(
                "Open generic types (Type.IsGenericTypeDefinition == true) are not allowed in BinaryConfiguration: " +
                "System.Collections.Generic.List`1"));

            // Pass open generic type name.
            cfg.BinaryConfiguration = new BinaryConfiguration {Types = new[] {typeof(IList<>).AssemblyQualifiedName}};
            
            ex = Assert.Throws<IgniteException>(() => Ignition.Start(cfg));
            Assert.AreEqual("Failed to start Ignite.NET, check inner exception for details", ex.Message);
            Assert.IsNotNull(ex.InnerException);
            Assert.IsTrue(ex.InnerException.Message.StartsWith(
                "Open generic types (Type.IsGenericTypeDefinition == true) are not allowed in BinaryConfiguration: " +
                "System.Collections.Generic.IList`1"));

            // Pass interface.
            cfg.BinaryConfiguration = new BinaryConfiguration(typeof(ICollection));
            
            ex = Assert.Throws<IgniteException>(() => Ignition.Start(cfg));
            Assert.AreEqual("Failed to start Ignite.NET, check inner exception for details", ex.Message);
            Assert.IsNotNull(ex.InnerException);
            Assert.IsTrue(ex.InnerException.Message.StartsWith(
                "Abstract types and interfaces are not allowed in BinaryConfiguration: " +
                "System.Collections.ICollection"));
        }

        /// <summary>
        /// Checks that specified types are binarizable and can be successfully used in cache.
        /// </summary>
        private void CheckBinarizableTypes(IEnumerable<Type> testTypes)
        {
            int key = 0;

            foreach (var typ in testTypes)
            {
                key += 1;

                var inst = CreateInstance(typ);

                _cache.Put(key, inst);

                var result = _cache.Get(key);

                Assert.AreEqual(inst.Prop, result.Prop);

                Assert.AreEqual(typ, result.GetType());
            }
        }

        /// <summary>
        /// Creates the instance of specified test binarizable type and sets a value on it.
        /// </summary>
        private static TestGenericBinarizableBase CreateInstance(Type type)
        {
            var inst = (TestGenericBinarizableBase)Activator.CreateInstance(type);

            inst.Prop = Rnd.Next(int.MaxValue);

            return inst;
        }
    }

    public abstract class TestGenericBinarizableBase
    {
        public object Prop { get; set; }
    }

    public class TestGenericBinarizable<T> : TestGenericBinarizableBase
    {
        public T Prop1 { get; set; }
    }

    public class TestGenericBinarizable<T1, T2> : TestGenericBinarizableBase
    {
        public T1 Prop1 { get; set; }
        public T2 Prop2 { get; set; }
    }

    public class TestGenericBinarizable<T1, T2, T3> : TestGenericBinarizableBase
    {
        public T1 Prop1 { get; set; }
        public T2 Prop2 { get; set; }
        public T3 Prop3 { get; set; }
    }
}