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

import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.internal.IgniteInternalFuture;
import org.apache.ignite.internal.IgniteKernal;
import org.apache.ignite.internal.cluster.ClusterTopologyCheckedException;
import org.apache.ignite.internal.processors.affinity.AffinityTopologyVersion;
import org.apache.ignite.internal.util.typedef.X;
import org.apache.ignite.internal.util.typedef.internal.U;
import org.apache.ignite.lang.IgniteCallable;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.apache.ignite.testframework.GridTestUtils;
import org.apache.ignite.testframework.MvccFeatureChecker;
import org.apache.ignite.testframework.junits.common.GridCommonAbstractTest;
import org.apache.ignite.transactions.Transaction;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.apache.ignite.cache.CacheAtomicityMode.TRANSACTIONAL;
import static org.apache.ignite.cache.CacheMode.PARTITIONED;
import static org.apache.ignite.transactions.TransactionConcurrency.PESSIMISTIC;
import static org.apache.ignite.transactions.TransactionIsolation.REPEATABLE_READ;

/**
 * Test case checks partition exchange when non-cache node joins topology (partition
 * exchange should be skipped in this case).
 */
@SuppressWarnings("unchecked")
@RunWith(JUnit4.class)
public class GridCacheMixedPartitionExchangeSelfTest extends GridCommonAbstractTest {
    /** Flag indicating whether to include cache to the node configuration. */
    private boolean cache;

    /** {@inheritDoc} */
    @Override protected void beforeTestsStarted() throws Exception {
        if (MvccFeatureChecker.forcedMvcc())
            fail("https://issues.apache.org/jira/browse/IGNITE-9470");

        super.beforeTestsStarted();
    }

    /** {@inheritDoc} */
    @Override protected IgniteConfiguration getConfiguration(String igniteInstanceName) throws Exception {
        IgniteConfiguration cfg = super.getConfiguration(igniteInstanceName);

        ((TcpDiscoverySpi)cfg.getDiscoverySpi()).setForceServerMode(true);

        if (cache)
            cfg.setCacheConfiguration(cacheConfiguration());
        else
            cfg.setClientMode(true);

        return cfg;
    }

    /**
     * @return Cache configuration.
     */
    private CacheConfiguration cacheConfiguration() {
        CacheConfiguration ccfg = defaultCacheConfiguration();

        ccfg.setCacheMode(PARTITIONED);
        ccfg.setAtomicityMode(TRANSACTIONAL);
        ccfg.setNearConfiguration(null);
        ccfg.setBackups(1);

        return ccfg;
    }

    /**
     * @throws Exception If failed.
     */
    @Test
    public void testNodeJoinLeave() throws Exception {
        try {
            cache = true;

            startGrids(4);

            awaitPartitionMapExchange();

            final AtomicBoolean finished = new AtomicBoolean();

            IgniteInternalFuture<Long> fut = GridTestUtils.runMultiThreadedAsync(new IgniteCallable<Object>() {
                @Override public Object call() throws Exception {
                    Random rnd = new Random();

                    int keys = 100;

                    while (!finished.get()) {
                        int g = rnd.nextInt(4);

                        int key = rnd.nextInt(keys);

                        IgniteCache<Integer, Integer> prj = grid(g).cache(DEFAULT_CACHE_NAME);

                        try {
                            try (Transaction tx = grid(g).transactions().txStart(PESSIMISTIC, REPEATABLE_READ)) {
                                Integer val = prj.get(key);

                                val = val == null ? 1 : val + 1;

                                prj.put(key, val);

                                tx.commit();
                            }
                        }
                        catch (Exception e) {
                            if (!X.hasCause(e, ClusterTopologyCheckedException.class))
                                throw e;
                        }
                    }

                    return null;
                }
            }, 4, "async-runner");

            cache = false;

            for (int r = 0; r < 3; r++) {
                for (int i = 4; i < 8; i++)
                    startGrid(i);

                for (int i = 4; i < 8; i++)
                    stopGrid(i);
            }

            // Check we can start more cache nodes after non-cache ones.
            cache = true;

            startGrid(4);

            U.sleep(500);

            finished.set(true);

            fut.get();

            AffinityTopologyVersion topVer = new AffinityTopologyVersion(grid(0).cluster().topologyVersion());

            assertEquals(29, topVer.topologyVersion());

            // Check all grids have all exchange futures completed.
            for (int i = 0; i < 4; i++) {
                IgniteKernal grid = (IgniteKernal)grid(i);

                GridCacheContext<Object, Object> cctx = grid.internalCache(DEFAULT_CACHE_NAME).context();

                IgniteInternalFuture<AffinityTopologyVersion> verFut = cctx.affinity().affinityReadyFuture(topVer);

                assertEquals(topVer, verFut.get());
                assertEquals(topVer, cctx.topologyVersionFuture().get());
            }
        }
        finally {
            stopAllGrids();
        }
    }
}
