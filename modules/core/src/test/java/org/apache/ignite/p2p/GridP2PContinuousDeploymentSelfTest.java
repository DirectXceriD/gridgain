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

package org.apache.ignite.p2p;

import java.util.UUID;
import org.apache.ignite.Ignite;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.events.Event;
import org.apache.ignite.events.EventType;
import org.apache.ignite.lang.IgniteBiPredicate;
import org.apache.ignite.lang.IgnitePredicate;
import org.apache.ignite.testframework.junits.common.GridCommonAbstractTest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.apache.ignite.cache.CacheMode.PARTITIONED;
import static org.apache.ignite.cache.CacheRebalanceMode.SYNC;
import static org.apache.ignite.cache.CacheWriteSynchronizationMode.FULL_SYNC;
import static org.apache.ignite.configuration.DeploymentMode.CONTINUOUS;

/**
 * Tests for continuous deployment with cache and changing topology.
 */
@RunWith(JUnit4.class)
public class GridP2PContinuousDeploymentSelfTest extends GridCommonAbstractTest {
    /** Number of grids cache. */
    private static final int GRID_CNT = 2;

    /** Name for Ignite instance without cache. */
    private static final String IGNITE_INSTANCE_NAME = "grid-no-cache";

    /** First test task name. */
    private static final String TEST_TASK_1 = "org.apache.ignite.tests.p2p.GridP2PContinuousDeploymentTask1";

    /** Second test task name. */
    private static final String TEST_TASK_2 = "org.apache.ignite.tests.p2p.GridP2PContinuousDeploymentTask2";

    /** Test predicate. */
    private static final String TEST_PREDICATE = "org.apache.ignite.tests.p2p.GridEventConsumeFilter";

    /** Client mode. */
    private boolean clientMode;

    /** {@inheritDoc} */
    @Override protected IgniteConfiguration getConfiguration(String igniteInstanceName) throws Exception {
        IgniteConfiguration cfg = super.getConfiguration(igniteInstanceName);

        cfg.setDeploymentMode(CONTINUOUS);

        if (IGNITE_INSTANCE_NAME.equals(igniteInstanceName))
            cfg.setCacheConfiguration();
        else
            cfg.setCacheConfiguration(cacheConfiguration());

        cfg.setPeerClassLoadingEnabled(true);

        if (clientMode)
            cfg.setClientMode(true);

        return cfg;
    }

    /**
     * @return Cache configuration.
     * @throws Exception In case of error.
     */
    protected CacheConfiguration cacheConfiguration() throws Exception {
        CacheConfiguration cfg = defaultCacheConfiguration();

        cfg.setCacheMode(PARTITIONED);
        cfg.setBackups(1);
        cfg.setWriteSynchronizationMode(FULL_SYNC);
        cfg.setRebalanceMode(SYNC);

        return cfg;
    }

    /** {@inheritDoc} */
    @Override protected void beforeTest() throws Exception {
        stopAllGrids();
    }

    /** {@inheritDoc} */
    @Override protected void afterTest() throws Exception {
        stopAllGrids();
    }

    /**
     * @throws Exception If failed.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testDeployment() throws Exception {
        startGridsMultiThreaded(GRID_CNT);

        Ignite ignite = startGrid(IGNITE_INSTANCE_NAME);

        Class cls = getExternalClassLoader().loadClass(TEST_TASK_1);

        compute(ignite.cluster().forRemotes()).execute(cls, null);

        stopGrid(IGNITE_INSTANCE_NAME);

        ignite = startGrid(IGNITE_INSTANCE_NAME);

        cls = getExternalClassLoader().loadClass(TEST_TASK_2);

        compute(ignite.cluster().forRemotes()).execute(cls, null);

        stopGrid(IGNITE_INSTANCE_NAME);
    }

    /**
     * Tests that server node joins correctly to existing cluster if it has deployed user class with enabled P2P.
     *
     * @throws Exception If failed.
     */
    @Test
    public void testServerJoinWithP2PClassDeployedInCluster() throws Exception {
        startGrids(GRID_CNT);

        ClassLoader extLdr = getExternalClassLoader();

        clientMode = true;

        Ignite client = startGrid(2);

        Class<?> cls = extLdr.loadClass(TEST_PREDICATE);

        client.events().remoteListen(
            new IgniteBiPredicate<UUID, Event>() {
                @Override public boolean apply(UUID uuid, Event event) {
                    return true;
                }
            },
            (IgnitePredicate<Event>) cls.newInstance(),
            EventType.EVT_CACHE_OBJECT_PUT
        );

        clientMode = false;

        Ignite srv = startGrid(3);

        srv.events().remoteListen(
            new IgniteBiPredicate<UUID, Event>() {
                @Override public boolean apply(UUID uuid, Event event) {
                    return true;
                }
            },
            (IgnitePredicate<Event>) cls.newInstance(),
            EventType.EVT_CACHE_OBJECT_PUT
        );

        awaitPartitionMapExchange();
    }
}
