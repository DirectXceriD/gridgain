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

package org.apache.ignite.internal.processors.cache;

import java.io.Serializable;
import javax.cache.processor.EntryProcessor;
import javax.cache.processor.MutableEntry;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.configuration.CacheConfiguration;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 *
 */
@RunWith(JUnit4.class)
public abstract class IgniteCacheCopyOnReadDisabledAbstractTest extends GridCacheAbstractSelfTest {
    /** {@inheritDoc} */
    @Override protected int gridCount() {
        return 1;
    }

    /** {@inheritDoc} */
    @Override protected CacheConfiguration cacheConfiguration(String igniteInstanceName) throws Exception {
        CacheConfiguration ccfg = super.cacheConfiguration(igniteInstanceName);

        assertTrue(ccfg.isCopyOnRead());

        ccfg.setCopyOnRead(false);

        return ccfg;
    }

    /** {@inheritDoc} */
    @Override protected boolean onheapCacheEnabled() {
        return true;
    }

    /**
     * @throws Exception If failed.
     */
    @Test
    public void testCopyOnReadDisabled() throws Exception {
        IgniteCache<TestKey, TestValue> cache = ignite(0).cache(DEFAULT_CACHE_NAME);

        for (int i = 0; i < 100; i++) {
            TestKey key = new TestKey(i);
            TestValue val = new TestValue(i);

            cache.put(key, val);

            TestValue val0 = cache.get(key);

            assertSame(val0, cache.get(key));

            assertNotSame(val, val0); // Original user value is always copied.

            assertSame(val0, cache.localPeek(key));
        }

        /* Does not seem to work anymore since main storage mechanism is always off-heap.
        TestKey key = new TestKey(0);

        TestValue val0 = cache.get(key);

        TestValue invokeVal = cache.invoke(key, new EntryProcessor<TestKey, TestValue, TestValue>() {
            @Override public TestValue process(MutableEntry<TestKey, TestValue> entry, Object... args) {
                return entry.getValue();
            }
        });

        assertSame(val0, invokeVal);*/
    }

    /**
     *
     */
    public static class TestKey implements Serializable {
        /** */
        private int key;

        /**
         * @param key Key.
         */
        public TestKey(int key) {
            this.key = key;
        }

        /** {@inheritDoc} */
        @Override public boolean equals(Object o) {
            if (!(o instanceof TestKey))
                return false;

            TestKey testKey = (TestKey)o;

            return key == testKey.key;

        }

        /** {@inheritDoc} */
        @Override public int hashCode() {
            return key;
        }
    }

    /**
     *
     */
    static class TestValue implements Serializable {
        /** */
        private int val;

        /**
         * @param val Value.
         */
        public TestValue(int val) {
            this.val = val;
        }
    }
}
