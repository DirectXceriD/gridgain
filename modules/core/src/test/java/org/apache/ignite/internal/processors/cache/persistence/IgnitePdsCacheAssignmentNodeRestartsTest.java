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

package org.apache.ignite.internal.processors.cache.persistence;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.ignite.Ignite;
import org.apache.ignite.cache.CacheAtomicityMode;
import org.apache.ignite.cache.CacheMode;
import org.apache.ignite.cache.affinity.rendezvous.RendezvousAffinityFunction;
import org.apache.ignite.cluster.ClusterNode;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.DataRegionConfiguration;
import org.apache.ignite.configuration.DataStorageConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.configuration.WALMode;
import org.apache.ignite.internal.IgniteEx;
import org.apache.ignite.internal.IgniteInternalFuture;
import org.apache.ignite.internal.IgniteKernal;
import org.apache.ignite.internal.processors.affinity.AffinityTopologyVersion;
import org.apache.ignite.internal.processors.cache.CacheGroupDescriptor;
import org.apache.ignite.internal.processors.cache.IgniteInternalCache;
import org.apache.ignite.internal.util.typedef.G;
import org.apache.ignite.lang.IgniteUuid;
import org.apache.ignite.testframework.GridTestUtils;
import org.apache.ignite.testframework.MvccFeatureChecker;
import org.apache.ignite.testframework.junits.common.GridCommonAbstractTest;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.apache.ignite.cache.CacheAtomicityMode.TRANSACTIONAL;
import static org.apache.ignite.cache.CacheMode.PARTITIONED;
import static org.apache.ignite.cache.CacheWriteSynchronizationMode.FULL_SYNC;
import static org.apache.ignite.internal.processors.cache.persistence.GridCacheDatabaseSharedManager.IGNITE_PDS_CHECKPOINT_TEST_SKIP_SYNC;

/**
 * The test validates assignment after nodes restart with enabled persistence.
 */
@RunWith(JUnit4.class)
public class IgnitePdsCacheAssignmentNodeRestartsTest extends GridCommonAbstractTest {
    /** {@inheritDoc} */
    @Override protected IgniteConfiguration getConfiguration(String igniteInstanceName) throws Exception {
        IgniteConfiguration cfg = super.getConfiguration(igniteInstanceName);

        cfg.setConsistentId(igniteInstanceName);

        cfg.setDataStorageConfiguration(new DataStorageConfiguration()
            .setPageSize(1024)
            .setDefaultDataRegionConfiguration(new DataRegionConfiguration()
                .setPersistenceEnabled(true)
                .setInitialSize(50L * 1024 * 1024)
                .setMaxSize(50L * 1024 * 1024)
            )
            .setWalMode(WALMode.LOG_ONLY)
        );

        return cfg;
    }

    /** {@inheritDoc} */
    @Override protected void beforeTest() throws Exception {
        super.beforeTest();

        cleanPersistenceDir();
    }

    /** {@inheritDoc} */
    @Override protected void afterTest() throws Exception {
        stopAllGrids();

        cleanPersistenceDir();

        super.afterTest();
    }

    /**
     * @param name          Name.
     * @param atomicityMode Atomicity mode.
     * @param cacheMode     Cache mode.
     * @param backups       Backups.
     * @param grp           Group.
     * @return Cache configuration.
     */
    private CacheConfiguration cacheConfiguration(String name,
        CacheAtomicityMode atomicityMode,
        CacheMode cacheMode,
        int backups,
        String grp) {
        CacheConfiguration ccfg = new CacheConfiguration(name);

        ccfg.setAtomicityMode(atomicityMode);
        ccfg.setWriteSynchronizationMode(FULL_SYNC);
        ccfg.setCacheMode(cacheMode);
        ccfg.setGroupName(grp);

        ccfg.setAffinity(new RendezvousAffinityFunction(false, 128));

        if (cacheMode == PARTITIONED)
            ccfg.setBackups(backups);

        return ccfg;
    }

    /**
     * @throws Exception If failed.
     */
    @Test
    public void testAssignmentAfterRestarts() throws Exception {
        try {
            Assume.assumeFalse("https://issues.apache.org/jira/browse/IGNITE-10582", MvccFeatureChecker.forcedMvcc());

            System.setProperty(IGNITE_PDS_CHECKPOINT_TEST_SKIP_SYNC, "true");

            final int gridsCnt = 5;

            final int groupsCnt = 2;

            final IgniteEx node = (IgniteEx) startGridsMultiThreaded(gridsCnt);

            final List<CacheConfiguration> cfgs = Arrays.asList(
                cacheConfiguration("g1c1", TRANSACTIONAL, PARTITIONED, gridsCnt, "testGrp1"),
                cacheConfiguration("g1c2", TRANSACTIONAL, PARTITIONED, gridsCnt, "testGrp1"),
                cacheConfiguration("g2c1", TRANSACTIONAL, PARTITIONED, gridsCnt, "testGrp2"),
                cacheConfiguration("g2c2", TRANSACTIONAL, PARTITIONED, gridsCnt, "testGrp2"));

            node.getOrCreateCaches(cfgs);

            validateDepIds(groupsCnt);

            stopAllGrids();

            IgniteEx node2 = (IgniteEx) startGridsMultiThreaded(gridsCnt);

            validateDepIds(groupsCnt); // Deployment ids must be the same on all nodes.

            final int restartIdxFrom = 2;

            final AtomicInteger idx = new AtomicInteger(restartIdxFrom);

            IgniteInternalFuture fut = GridTestUtils.runMultiThreadedAsync(new Callable<Void>() {
                @Override public Void call() throws Exception {
                    int nodeIdx = idx.getAndIncrement();

                    stopGrid(nodeIdx);

                    return null;
                }
            }, gridsCnt - restartIdxFrom, "stop-node");

            fut.get();

            awaitPartitionMapExchange();

            checkAffinity();

            idx.set(restartIdxFrom);

            fut = GridTestUtils.runMultiThreadedAsync(new Callable<Void>() {
                @Override public Void call() throws Exception {
                    int nodeIdx = idx.getAndIncrement();

                    startGrid(nodeIdx);

                    return null;
                }
            }, gridsCnt - restartIdxFrom, "start-node");

            fut.get();

            awaitPartitionMapExchange();

            AffinityTopologyVersion topVer = node2.context().cache().context().exchange().readyAffinityVersion();

            log.info("Using version: " + topVer);

            checkAffinity();
        }
        finally {
            System.clearProperty(IGNITE_PDS_CHECKPOINT_TEST_SKIP_SYNC);
        }
    }

    /**
     * @param grpCnt Group count.
     */
    private void validateDepIds(int grpCnt) {
        Map<Integer, IgniteUuid> depIds = new HashMap<>();

        for (Ignite ignite : G.allGrids()) {
            final Map<Integer, CacheGroupDescriptor> descMap = ((IgniteEx) ignite).context().cache().cacheGroupDescriptors();

            for (Map.Entry<Integer, CacheGroupDescriptor> entry : descMap.entrySet()) {
                final IgniteUuid u = entry.getValue().deploymentId();

                final IgniteUuid u0 = depIds.get(entry.getKey());

                if (u0 == null)
                    depIds.put(entry.getKey(), u);
                else
                    assertEquals("Descriptors do not match", u0, u);
            }
        }

        assertEquals(grpCnt + 1, depIds.size());
    }

    /**
     * @throws Exception If failed.
     */
    private void checkAffinity() throws Exception {
        List<Ignite> nodes = G.allGrids();

        ClusterNode crdNode = null;

        for (Ignite node : nodes) {
            ClusterNode locNode = node.cluster().localNode();

            if (crdNode == null || locNode.order() < crdNode.order())
                crdNode = locNode;
        }

        AffinityTopologyVersion topVer = ((IgniteKernal) grid(crdNode)).
            context().cache().context().exchange().readyAffinityVersion();

        Map<String, List<List<ClusterNode>>> affMap = new HashMap<>();

        for (Ignite node : nodes) {
            IgniteKernal node0 = (IgniteKernal) node;

            for (IgniteInternalCache cache : node0.context().cache().caches()) {
                List<List<ClusterNode>> aff = affMap.get(cache.name());
                List<List<ClusterNode>> aff0 = cache.context().affinity().assignments(topVer);

                if (aff != null)
                    assertEquals(aff, aff0);
                else
                    affMap.put(cache.name(), aff0);
            }
        }
    }
}
