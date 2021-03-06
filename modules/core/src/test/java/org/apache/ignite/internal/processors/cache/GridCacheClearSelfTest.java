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

import java.util.Collections;
import java.util.Set;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
import org.apache.ignite.cache.CacheMode;
import org.apache.ignite.cache.CachePeekMode;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.NearCacheConfiguration;
import org.apache.ignite.internal.util.lang.GridAbsPredicate;
import org.apache.ignite.internal.util.typedef.F;
import org.apache.ignite.testframework.GridTestUtils;
import org.apache.ignite.testframework.junits.common.GridCommonAbstractTest;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests for cache clear.
 */
@RunWith(JUnit4.class)
public class GridCacheClearSelfTest extends GridCommonAbstractTest {
    /** {@inheritDoc} */
    @Override protected void beforeTestsStarted() throws Exception {
        startGridsMultiThreaded(3);

        Ignition.setClientMode(true);

        try {
            startGrid("client1");
            startGrid("client2");
        }
        finally {
            Ignition.setClientMode(false);
        }
    }

    /**
     * @throws Exception If failed.
     */
    @Test
    public void testClearPartitioned() throws Exception {
        testClear(CacheMode.PARTITIONED, false, null);
    }

    /**
     * @throws Exception If failed.
     */
    @Test
    public void testClearPartitionedNear() throws Exception {
        testClear(CacheMode.PARTITIONED, true, null);
    }

    /**
     * @throws Exception If failed.
     */
    @Test
    public void testClearReplicated() throws Exception {
        testClear(CacheMode.REPLICATED, false, null);
    }

    /**
     * @throws Exception If failed.
     */
    @Test
    public void testClearReplicatedNear() throws Exception {
        testClear(CacheMode.REPLICATED, true, null);
    }

    /**
     * @throws Exception If failed.
     */
    @Test
    public void testClearKeyPartitioned() throws Exception {
        testClear(CacheMode.PARTITIONED, false, Collections.singleton(3));
    }

    /**
     * @throws Exception If failed.
     */
    @Test
    public void testClearKeyPartitionedNear() throws Exception {
        testClear(CacheMode.PARTITIONED, true, Collections.singleton(3));
    }

    /**
     * @throws Exception If failed.
     */
    @Test
    public void testClearKeyReplicated() throws Exception {
        testClear(CacheMode.REPLICATED, false, Collections.singleton(3));
    }

    /**
     * @throws Exception If failed.
     */
    @Test
    public void testClearKeyReplicatedNear() throws Exception {
        testClear(CacheMode.REPLICATED, true, Collections.singleton(3));
    }

    /**
     * @throws Exception If failed.
     */
    @Test
    public void testClearKeysPartitioned() throws Exception {
        testClear(CacheMode.PARTITIONED, false, F.asSet(2, 6, 9));
    }

    /**
     * @throws Exception If failed.
     */
    @Test
    public void testClearKeysPartitionedNear() throws Exception {
        testClear(CacheMode.PARTITIONED, true, F.asSet(2, 6, 9));
    }

    /**
     * @throws Exception If failed.
     */
    @Test
    public void testClearKeysReplicated() throws Exception {
        testClear(CacheMode.REPLICATED, false, F.asSet(2, 6, 9));
    }

    /**
     * @throws Exception If failed.
     */
    @Test
    public void testClearKeysReplicatedNear() throws Exception {
        testClear(CacheMode.REPLICATED, true, F.asSet(2, 6, 9));
    }

    /**
     * @param cacheMode Cache mode.
     * @param near Near cache flag.
     * @param keys Keys to clear.
     * @throws Exception If failed.
     */
    private void testClear(CacheMode cacheMode, boolean near, @Nullable Set<Integer> keys) throws Exception {
        Ignite client1 = client1();
        Ignite client2 = client2();

        final String cacheName = DEFAULT_CACHE_NAME;

        try {
            CacheConfiguration<Integer, Integer> cfg = new CacheConfiguration<>(cacheName);

            cfg.setCacheMode(cacheMode);

            IgniteCache<Integer, Integer> cache1 = near ?
                client1.createCache(cfg, new NearCacheConfiguration<Integer, Integer>()) :
                client1.createCache(cfg);

            IgniteCache<Integer, Integer> cache2 = near ?
                client2.createNearCache(cacheName, new NearCacheConfiguration<Integer, Integer>()) :
                client2.<Integer, Integer>cache(cacheName);

            GridTestUtils.waitForCondition(new GridAbsPredicate() {
                @Override public boolean apply() {
                    return ignite(0).cluster().forCacheNodes(cacheName).nodes().size() == 5;
                }
            }, 5000);

            for (int i = 0; i < 10; i++)
                cache1.put(i, i);

            for (int i = 0; i < 10; i++)
                cache2.get(i);

            assertEquals(10, cache1.size(CachePeekMode.PRIMARY));
            assertEquals(10, cache2.size(CachePeekMode.PRIMARY));
            assertEquals(near ? 10 : 0, cache1.localSize(CachePeekMode.NEAR));
            assertEquals(near ? 10 : 0, cache2.localSize(CachePeekMode.NEAR));

            if (F.isEmpty(keys))
                cache1.clear();
            else if (keys.size() == 1)
                cache1.clear(F.first(keys));
            else
                cache1.clearAll(keys);

            int expSize = F.isEmpty(keys) ? 0 : 10 - keys.size();

            assertEquals(expSize, cache1.size(CachePeekMode.PRIMARY));
            assertEquals(expSize, cache2.size(CachePeekMode.PRIMARY));
            assertEquals(near ? expSize : 0, cache1.localSize(CachePeekMode.NEAR));
            assertEquals(near ? expSize : 0, cache2.localSize(CachePeekMode.NEAR));
        }
        finally {
            client1.destroyCache(cacheName);
        }
    }

    /**
     * @return Client 1.
     */
    private Ignite client1() {
        return grid("client1");
    }

    /**
     * @return Client 2.
     */
    private Ignite client2() {
        return grid("client2");
    }
}
