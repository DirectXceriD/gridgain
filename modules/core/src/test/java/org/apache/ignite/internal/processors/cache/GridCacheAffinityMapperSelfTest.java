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

import java.util.ArrayList;
import java.util.List;
import org.apache.ignite.cache.affinity.AffinityKey;
import org.apache.ignite.cache.affinity.AffinityKeyMapped;
import org.apache.ignite.cache.affinity.AffinityKeyMapper;
import org.apache.ignite.testframework.GridTestUtils;
import org.apache.ignite.testframework.junits.common.GridCommonAbstractTest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Test affinity mapper.
 */
@RunWith(JUnit4.class)
public class GridCacheAffinityMapperSelfTest extends GridCommonAbstractTest {
    /** {@inheritDoc} */
    @Override protected void beforeTestsStarted() throws Exception {
        startGrid();
    }

    /**
     *
     */
    @Test
    public void testMethodAffinityMapper() {
        AffinityKeyMapper mapper =
            new GridCacheDefaultAffinityKeyMapper();

        GridTestUtils.setFieldValue(mapper, "ignite", grid());

        List<AffinityKey<Integer>> keys = new ArrayList<>();

        for (int i = 1; i <= 10; i++)
            keys.add(new AffinityKey<>(i, Integer.toString(i)));

        for (int i = 1; i <= 10; i++) {
            AffinityKey<Integer> key = keys.get(i - 1);

            Object mapped = mapper.affinityKey(key);

            info("Mapped key: " + mapped);

            assertNotNull(mapped);
            assertSame(key.affinityKey(), mapped);
        }
    }

    /**
     *
     */
    @Test
    public void testFieldAffinityMapper() {
        AffinityKeyMapper mapper =
            new GridCacheDefaultAffinityKeyMapper();

        GridTestUtils.setFieldValue(mapper, "ignite", grid());

        List<FieldAffinityKey<Integer>> keys = new ArrayList<>();

        for (int i = 1; i <= 10; i++)
            keys.add(new FieldAffinityKey<>(i, Integer.toString(i)));

        for (int i = 1; i <= 10; i++) {
            FieldAffinityKey<Integer> key = keys.get(i - 1);

            Object mapped = mapper.affinityKey(key);

            info("Mapped key: " + mapped);

            assertNotNull(mapped);
            assertSame(key.affinityKey(), mapped);
        }
    }

    /**
     *
     */
    @Test
    public void testFieldAffinityMapperWithWrongClass() {
        AffinityKeyMapper mapper =
            new GridCacheDefaultAffinityKeyMapper();

        GridTestUtils.setFieldValue(mapper, "ignite", grid());

        FieldNoAffinityKey key = new FieldNoAffinityKey();
        Object mapped = mapper.affinityKey(key);
        assertEquals(key, mapped);
    }

    /**
     * Test key for field annotation.
     */
    private static class FieldNoAffinityKey {
        // No-op.
    }

    /**
     * Test key for field annotation.
     */
    private static class FieldAffinityKey<K> {
        /** Key. */
        private K key;

        /** Affinity key. */
        @AffinityKeyMapped
        private Object affKey;

        /**
         * Initializes key together with its affinity key counter-part.
         *
         * @param key Key.
         * @param affKey Affinity key.
         */
        FieldAffinityKey(K key, Object affKey) {
            this.key = key;
            this.affKey = affKey;
        }

        /**
         * @return Key.
         */
        public K key() {
            return key;
        }

        /**
         * @return Affinity key.
         */
        public Object affinityKey() {
            return affKey;
        }
    }
}
