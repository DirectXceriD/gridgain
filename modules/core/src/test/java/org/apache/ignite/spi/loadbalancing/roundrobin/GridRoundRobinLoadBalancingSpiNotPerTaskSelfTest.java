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

package org.apache.ignite.spi.loadbalancing.roundrobin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.apache.ignite.GridTestJob;
import org.apache.ignite.GridTestTaskSession;
import org.apache.ignite.IgniteException;
import org.apache.ignite.cluster.ClusterNode;
import org.apache.ignite.compute.ComputeTaskSession;
import org.apache.ignite.events.TaskEvent;
import org.apache.ignite.lang.IgniteUuid;
import org.apache.ignite.testframework.GridSpiTestContext;
import org.apache.ignite.testframework.GridTestNode;
import org.apache.ignite.testframework.junits.spi.GridSpiAbstractTest;
import org.apache.ignite.testframework.junits.spi.GridSpiTest;
import org.apache.ignite.testframework.junits.spi.GridSpiTestConfig;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.apache.ignite.events.EventType.EVT_TASK_FAILED;
import static org.apache.ignite.events.EventType.EVT_TASK_FINISHED;
import static org.apache.ignite.spi.loadbalancing.roundrobin.GridRoundRobinTestUtils.checkCyclicBalancing;

/**
 * Tests round robin load balancing.
 */
@GridSpiTest(spi = RoundRobinLoadBalancingSpi.class, group = "Load Balancing SPI")
@RunWith(JUnit4.class)
public class GridRoundRobinLoadBalancingSpiNotPerTaskSelfTest
    extends GridSpiAbstractTest<RoundRobinLoadBalancingSpi> {
    /**
     * @return Per-task configuration parameter.
     */
    @GridSpiTestConfig
    public boolean getPerTask() {
        return false;
    }

    /** {@inheritDoc} */
    @Override protected GridSpiTestContext initSpiContext() throws Exception {
        GridSpiTestContext spiCtx = super.initSpiContext();

        spiCtx.createLocalNode();
        spiCtx.createRemoteNodes(10);

        return spiCtx;
    }

    /** {@inheritDoc} */
    @Override protected void beforeTest() throws Exception {
        assert !getSpi().isPerTask() : "Invalid SPI configuration.";
    }

    /**
     * @throws Exception If test failed.
     */
    @Test
    public void testMultipleNodes() throws Exception {
        List<ClusterNode> allNodes = (List<ClusterNode>)getSpiContext().nodes();

        ComputeTaskSession ses = new GridTestTaskSession();

        List<UUID> orderedNodes = new ArrayList<>(getSpi().getNodeIds(ses));

        assertEquals("Balancer doesn't use all available nodes", orderedNodes.size(), allNodes.size());

        checkCyclicBalancing(getSpi(), allNodes, orderedNodes, ses);
    }

    /**
     * @throws Exception If test failed.
     */
    @Test
    public void testMultipleTaskSessions() throws Exception {
        ComputeTaskSession ses1 = new GridTestTaskSession(IgniteUuid.randomUuid());
        ComputeTaskSession ses2 = new GridTestTaskSession(IgniteUuid.randomUuid());

        List<ClusterNode> allNodes = (List<ClusterNode>)getSpiContext().nodes();

        List<UUID> orderedNodes = getSpi().getNodeIds(ses1);

        assertEquals("Balancer doesn't use all available nodes", orderedNodes.size(), allNodes.size());

        checkCyclicBalancing(getSpi(), allNodes, orderedNodes, ses1, ses2);

        getSpiContext().triggerEvent(new TaskEvent(
            null, null, EVT_TASK_FINISHED, ses1.getId(), null, null, false, null));
        getSpiContext().triggerEvent(new TaskEvent(
            null, null, EVT_TASK_FAILED, ses2.getId(), null, null, false, null));
    }

    /**
     * @throws Exception If test failed.
     */
    @Test
    public void testBalancingOneNode() throws Exception {
        ComputeTaskSession ses = new GridTestTaskSession();

        List<ClusterNode> allNodes = (List<ClusterNode>)getSpiContext().nodes();

        List<ClusterNode> balancedNode = Arrays.asList(allNodes.get(0));

        ClusterNode firstNode = getSpi().getBalancedNode(ses, balancedNode, new GridTestJob());
        ClusterNode secondNode = getSpi().getBalancedNode(ses, balancedNode, new GridTestJob());

        assertEquals(firstNode, secondNode);
    }

    /** */
    @Test
    public void testNodeNotInTopology() throws Exception {
        ComputeTaskSession ses = new GridTestTaskSession();

        ClusterNode node = new GridTestNode(UUID.randomUUID());

        List<ClusterNode> notInTop = Arrays.asList(node);

        try {
            getSpi().getBalancedNode(ses, notInTop, new GridTestJob());
        }
        catch (IgniteException e) {
            assertTrue(e.getMessage().contains("Task topology does not have alive nodes"));
        }
    }
}
