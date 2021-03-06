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

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.cache.CacheMode;
import org.apache.ignite.cache.CacheWriteSynchronizationMode;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.internal.IgniteKernal;
import org.apache.ignite.testframework.junits.common.GridCommonAbstractTest;
import org.apache.ignite.transactions.Transaction;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.apache.ignite.cache.CacheAtomicityMode.TRANSACTIONAL;
import static org.apache.ignite.cache.CacheMode.LOCAL;
import static org.apache.ignite.cache.CacheMode.PARTITIONED;
import static org.apache.ignite.cache.CacheMode.REPLICATED;

/**
 * Tests for {@link GridCacheMvccManager}.
 */
@RunWith(JUnit4.class)
public class GridCacheMvccManagerSelfTest extends GridCommonAbstractTest {
    /** Cache mode. */
    private CacheMode mode;

    /** {@inheritDoc} */
    @Override protected IgniteConfiguration getConfiguration(String igniteInstanceName) throws Exception {
        IgniteConfiguration cfg = super.getConfiguration(igniteInstanceName);

        cfg.setCacheConfiguration(cacheConfiguration());

        return cfg;
    }

    /** @return Cache configuration. */
    protected CacheConfiguration cacheConfiguration() {
        CacheConfiguration cfg = defaultCacheConfiguration();

        cfg.setCacheMode(mode);
        cfg.setWriteSynchronizationMode(CacheWriteSynchronizationMode.FULL_SYNC);
        cfg.setAtomicityMode(TRANSACTIONAL);

        return cfg;
    }

    /** @throws Exception If failed. */
    @Test
    public void testLocalCache() throws Exception {
        mode = LOCAL;

        testCandidates(1);
    }

    /** @throws Exception If failed. */
    @Test
    public void testReplicatedCache() throws Exception {
        mode = REPLICATED;

        testCandidates(3);
    }

    /** @throws Exception If failed. */
    @Test
    public void testPartitionedCache() throws Exception {
        mode = PARTITIONED;

        testCandidates(3);
    }

    /**
     * @param gridCnt Grid count.
     * @throws Exception If failed.
     */
    private void testCandidates(int gridCnt) throws Exception {
        try {
            Ignite ignite = startGridsMultiThreaded(gridCnt);

            IgniteCache<Integer, Integer> cache = ignite.cache(DEFAULT_CACHE_NAME);

            Transaction tx = ignite.transactions().txStart();

            cache.put(1, 1);

            tx.commit();

            for (int i = 0; i < gridCnt; i++) {
                assert ((IgniteKernal)grid(i)).internalCache(DEFAULT_CACHE_NAME).context().mvcc().localCandidates().isEmpty();
                assert ((IgniteKernal)grid(i)).internalCache(DEFAULT_CACHE_NAME).context().mvcc().remoteCandidates().isEmpty();
            }
        }
        finally {
            stopAllGrids();
        }
    }
}
