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

package org.apache.ignite.internal.processors.service;

import java.util.concurrent.CountDownLatch;
import junit.framework.TestCase;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteServices;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.lang.IgniteFuture;
import org.apache.ignite.services.Service;
import org.apache.ignite.services.ServiceConfiguration;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Single node services test.
 */
@RunWith(JUnit4.class)
public class GridServiceProcessorMultiNodeSelfTest extends GridServiceProcessorAbstractSelfTest {
    /** {@inheritDoc} */
    @Override protected int nodeCount() {
        return 4;
    }

    /**
     * @throws Exception If failed.
     */
    @Test
    public void testSingletonUpdateTopology() throws Exception {
        String name = "serviceSingletonUpdateTopology";

        Ignite g = randomGrid();

        CountDownLatch latch = new CountDownLatch(1);

        DummyService.exeLatch(name, latch);

        IgniteServices svcs = g.services();

        IgniteFuture<?> fut = svcs.deployClusterSingletonAsync(name, new DummyService());

        info("Deployed service: " + name);

        fut.get();

        info("Finished waiting for service future: " + name);

        latch.await();

        TestCase.assertEquals(name, 1, DummyService.started(name));
        TestCase.assertEquals(name, 0, DummyService.cancelled(name));

        int nodeCnt = 2;

        startExtraNodes(nodeCnt);

        try {
            TestCase.assertEquals(name, 1, DummyService.started(name));
            TestCase.assertEquals(name, 0, DummyService.cancelled(name));

            info(">>> Passed checks.");

            checkCount(name, g.services().serviceDescriptors(), 1);
        }
        finally {
            stopExtraNodes(nodeCnt);
        }
    }

    /**
     * @throws Exception If failed.
     */
    @Test
    public void testAffinityDeployUpdateTopology() throws Exception {
        Ignite g = randomGrid();

        final Integer affKey = 1;

        // Store a cache key.
        g.cache(CACHE_NAME).put(affKey, affKey.toString());

        final String name = "serviceAffinityUpdateTopology";

        IgniteServices svcs = g.services();

        IgniteFuture<?> fut = svcs.deployKeyAffinitySingletonAsync(name, new AffinityService(affKey),
            CACHE_NAME, affKey);

        info("Deployed service: " + name);

        fut.get();

        info("Finished waiting for service future: " + name);

        checkCount(name, g.services().serviceDescriptors(), 1);

        int nodeCnt = 2;

        startExtraNodes(nodeCnt);

        try {
            checkCount(name, g.services().serviceDescriptors(), 1);
        }
        finally {
            stopExtraNodes(nodeCnt);
        }
    }

    /**
     * @throws Exception If failed.
     */
    @Test
    public void testDeployOnEachNodeButClientUpdateTopology() throws Exception {
        // Prestart client node.
        Ignite client = startGrid("client", getConfiguration("client").setClientMode(true));

        try {
            final String name = "serviceOnEachNodeButClientUpdateTopology";

            Ignite g = randomGrid();

            CountDownLatch latch = new CountDownLatch(nodeCount());

            DummyService.exeLatch(name, latch);

            IgniteServices svcs = g.services();

            IgniteFuture<?> fut = svcs.deployNodeSingletonAsync(name, new DummyService());

            info("Deployed service: " + name);

            fut.get();

            info("Finished waiting for service future: " + name);

            latch.await();

            // Ensure service is deployed
            assertNotNull(client.services().serviceProxy(name, Service.class, false, 2000));

            assertEquals(name, nodeCount(), DummyService.started(name));
            assertEquals(name, 0, DummyService.cancelled(name));

            int servers = 2;

            latch = new CountDownLatch(servers);

            DummyService.exeLatch(name, latch);

            int clients = 2;

            startExtraNodes(servers, clients);

            try {
                latch.await();

                waitForDeployment(name, servers);

                // Since we start extra nodes, there may be extra start and cancel events,
                // so we check only the difference between start and cancel and
                // not start and cancel events individually.
                assertEquals(name, nodeCount() + servers, DummyService.started(name) - DummyService.cancelled(name));

                checkCount(name, g.services().serviceDescriptors(), nodeCount() + servers);
            }
            finally {
                stopExtraNodes(servers + clients);
            }
        }
        finally {
            stopGrid("client");
        }
    }

    /**
     * @throws Exception If failed.
     */
    @Test
    public void testDeployOnEachProjectionNodeUpdateTopology() throws Exception {
        // Prestart client node.
        Ignite client = startGrid("client", getConfiguration("client").setClientMode(true));

        try {
            final String name = "serviceOnEachProjectionNodeUpdateTopology";

            Ignite g = randomGrid();

            int prestartedSrvcs = 1;

            CountDownLatch latch = new CountDownLatch(prestartedSrvcs);

            DummyService.exeLatch(name, latch);

            IgniteServices svcs = g.services(g.cluster().forClients());

            IgniteFuture<?> fut = svcs.deployNodeSingletonAsync(name, new DummyService());

            info("Deployed service: " + name);

            fut.get();

            info("Finished waiting for service future: " + name);

            latch.await();

            // Ensure service is deployed
            assertNotNull(client.services().serviceProxy(name, Service.class, false, 2000));

            assertEquals(name, prestartedSrvcs, DummyService.started(name));
            assertEquals(name, 0, DummyService.cancelled(name));

            int servers = 2;

            int clients = 2;

            latch = new CountDownLatch(clients);

            DummyService.exeLatch(name, latch);

            startExtraNodes(servers, clients);

            try {
                latch.await();

                waitForDeployment(name, clients);

                // Since we start extra nodes, there may be extra start and cancel events,
                // so we check only the difference between start and cancel and
                // not start and cancel events individually.
                assertEquals(name, clients + prestartedSrvcs, DummyService.started(name) - DummyService.cancelled(name));

                checkCount(name, g.services().serviceDescriptors(), clients + prestartedSrvcs);
            }
            finally {
                stopExtraNodes(servers + clients);
            }
        }
        finally {
            stopGrid("client");
        }
    }

    /**
     * @throws Exception If failed.
     */
    @Test
    public void testDeployOnEachNodeUpdateTopology() throws Exception {
        // Prestart client node.
        Ignite client = startGrid("client", getConfiguration("client").setClientMode(true));

        try {
            final String name = "serviceOnEachNodeUpdateTopology";

            Ignite g = randomGrid();

            final int prestartedNodes = nodeCount() + 1;

            CountDownLatch latch = new CountDownLatch(prestartedNodes);

            DummyService.exeLatch(name, latch);

            ServiceConfiguration srvcCfg = new ServiceConfiguration();

            srvcCfg.setNodeFilter(new CacheConfiguration.IgniteAllNodesPredicate());
            srvcCfg.setName(name);
            srvcCfg.setMaxPerNodeCount(1);
            srvcCfg.setService(new DummyService());

            IgniteServices svcs = g.services();

            IgniteFuture<?> fut = svcs.deployAsync(srvcCfg);

            info("Deployed service: " + name);

            fut.get();

            info("Finished waiting for service future: " + name);

            latch.await();

            // Ensure service is deployed
            assertNotNull(client.services().serviceProxy(name, Service.class, false, 2000));

            assertEquals(name, prestartedNodes, DummyService.started(name));
            assertEquals(name, 0, DummyService.cancelled(name));

            int servers = 2;
            int clients = 2;

            int extraNodes = servers + clients;

            latch = new CountDownLatch(extraNodes);

            DummyService.exeLatch(name, latch);

            startExtraNodes(servers, clients);

            try {
                latch.await();

                waitForDeployment(name, prestartedNodes + extraNodes);

                // Since we start extra nodes, there may be extra start and cancel events,
                // so we check only the difference between start and cancel and
                // not start and cancel events individually.
                assertEquals(name, prestartedNodes + extraNodes,
                    DummyService.started(name) - DummyService.cancelled(name));

                checkCount(name, g.services().serviceDescriptors(), prestartedNodes + extraNodes);
            }
            finally {
                stopExtraNodes(extraNodes);
            }
        }
        finally {
            stopGrid("client");
        }
    }

    /**
     * @throws Exception If failed.
     */
    @SuppressWarnings("deprecation")
    @Test
    public void testDeployLimits() throws Exception {
        final String name = "serviceWithLimitsUpdateTopology";

        Ignite g = randomGrid();

        final int totalInstances = nodeCount() + 1;

        CountDownLatch latch = new CountDownLatch(nodeCount());

        DummyService.exeLatch(name, latch);

        ServiceConfiguration srvcCfg = new ServiceConfiguration();

        srvcCfg.setName(name);
        srvcCfg.setMaxPerNodeCount(1);
        srvcCfg.setTotalCount(totalInstances);
        srvcCfg.setService(new DummyService());

        IgniteServices svcs = g.services().withAsync();

        svcs.deploy(srvcCfg);

        IgniteFuture<?> fut = svcs.future();

        info("Deployed service: " + name);

        fut.get();

        info("Finished waiting for service future: " + name);

        latch.await();

        assertEquals(name, nodeCount(), DummyService.started(name));
        assertEquals(name, 0, DummyService.cancelled(name));

        checkCount(name, g.services().serviceDescriptors(), nodeCount());

        latch = new CountDownLatch(1);

        DummyService.exeLatch(name, latch);

        int extraNodes = 2;

        startExtraNodes(extraNodes);

        try {
            latch.await();

            waitForDeployment(name, totalInstances);

            // Since we start extra nodes, there may be extra start and cancel events,
            // so we check only the difference between start and cancel and
            // not start and cancel events individually.
            assertEquals(name, totalInstances, DummyService.started(name) - DummyService.cancelled(name));

            checkCount(name, g.services().serviceDescriptors(), totalInstances);
        }
        finally {
            stopExtraNodes(extraNodes);
        }
    }
}
