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

package org.apache.ignite.internal;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.ignite.Ignite;
import org.apache.ignite.Ignition;
import org.apache.ignite.cluster.ClusterGroup;
import org.apache.ignite.cluster.ClusterNode;
import org.apache.ignite.internal.util.typedef.F;
import org.apache.ignite.internal.util.typedef.internal.U;
import org.apache.ignite.lang.IgnitePredicate;
import org.apache.ignite.marshaller.Marshaller;
import org.apache.ignite.testframework.GridTestUtils;
import org.apache.ignite.testframework.junits.common.GridCommonTest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Test for {@link ClusterGroup}.
 */
@GridCommonTest(group = "Kernal Self")
@RunWith(JUnit4.class)
public class ClusterGroupSelfTest extends ClusterGroupAbstractTest {
    /** Nodes count. */
    private static final int NODES_CNT = 4;

    /** Projection node IDs. */
    private static Collection<UUID> ids;

    /** */
    private static Ignite ignite;

    /** {@inheritDoc} */
    @SuppressWarnings({"ConstantConditions"})
    @Override protected void beforeTestsStarted() throws Exception {
        assert NODES_CNT > 2;

        ids = new LinkedList<>();

        try {
            for (int i = 0; i < NODES_CNT; i++) {
                Ignition.setClientMode(i > 1);

                Ignite g = startGrid(i);

                ids.add(g.cluster().localNode().id());

                if (i == 0)
                    ignite = g;
            }

            waitForTopology(NODES_CNT);
        }
        finally {
            Ignition.setClientMode(false);
        }
    }

    /** {@inheritDoc} */
    @Override protected ClusterGroup projection() {
        return grid(0).cluster().forPredicate(F.nodeForNodeIds(ids));
    }

    /** {@inheritDoc} */
    @Override protected UUID localNodeId() {
        return grid(0).localNode().id();
    }

    /**
     * @throws Exception If failed.
     */
    @Test
    public void testRandom() throws Exception {
        assertTrue(ignite.cluster().nodes().contains(ignite.cluster().forRandom().node()));
    }

    /**
     * @throws Exception If failed.
     */
    @Test
    public void testOldest() throws Exception {
        ClusterGroup oldest = ignite.cluster().forOldest();

        ClusterNode node = null;

        long minOrder = Long.MAX_VALUE;

        for (ClusterNode n : ignite.cluster().nodes()) {
            if (n.order() < minOrder) {
                node = n;

                minOrder = n.order();
            }
        }

        assertEquals(oldest.node(), ignite.cluster().forNode(node).node());

        ClusterGroup emptyGrp = ignite.cluster().forAttribute("nonExistent", "val");

        assertEquals(0, emptyGrp.forOldest().nodes().size());
    }

    /**
     * @throws Exception If failed.
     */
    @Test
    public void testYoungest() throws Exception {
        ClusterGroup youngest = ignite.cluster().forYoungest();

        ClusterNode node = null;

        long maxOrder = Long.MIN_VALUE;

        for (ClusterNode n : ignite.cluster().nodes()) {
            if (n.order() > maxOrder) {
                node = n;

                maxOrder = n.order();
            }
        }

        assertEquals(youngest.node(), ignite.cluster().forNode(node).node());

        ClusterGroup emptyGrp = ignite.cluster().forAttribute("nonExistent", "val");

        assertEquals(0, emptyGrp.forYoungest().nodes().size());
    }

    /**
     * @throws Exception If failed.
     */
    @Test
    public void testForDaemons() throws Exception {
        assertEquals(4, ignite.cluster().nodes().size());

        ClusterGroup daemons = ignite.cluster().forDaemons();
        ClusterGroup srvs = ignite.cluster().forServers();

        assertEquals(0, daemons.nodes().size());
        assertEquals(2, srvs.nodes().size());

        Ignition.setDaemon(true);

        try (Ignite g = startGrid(NODES_CNT)) {
            Ignition.setDaemon(false);

            try (Ignite g1 = startGrid(NODES_CNT + 1)) {
                assertEquals(1, ignite.cluster().forDaemons().nodes().size());
                assertEquals(3, srvs.nodes().size());
                assertEquals(1, daemons.nodes().size());
            }
        }
    }

    /**
     * @throws Exception If failed.
     */
    @Test
    public void testNewNodes() throws Exception {
        ClusterGroup youngest = ignite.cluster().forYoungest();
        ClusterGroup oldest = ignite.cluster().forOldest();

        ClusterNode old = oldest.node();
        ClusterNode last = youngest.node();

        assertNotNull(last);

        try (Ignite g = startGrid(NODES_CNT)) {
            ClusterNode n = g.cluster().localNode();

            ClusterNode latest = youngest.node();

            assertNotNull(latest);
            assertEquals(latest.id(), n.id());
            assertEquals(oldest.node(), old);
        }
    }

    /**
     * @throws Exception If failed.
     */
    @Test
    public void testForPredicate() throws Exception {
        IgnitePredicate<ClusterNode> evenP = new IgnitePredicate<ClusterNode>() {
            @Override public boolean apply(ClusterNode node) {
                return node.order() % 2 == 0;
            }
        };

        IgnitePredicate<ClusterNode> oddP = new IgnitePredicate<ClusterNode>() {
            @Override public boolean apply(ClusterNode node) {
                return node.order() % 2 == 1;
            }
        };

        ClusterGroup remotes = ignite.cluster().forRemotes();

        ClusterGroup evenYoungest = remotes.forPredicate(evenP).forYoungest();
        ClusterGroup evenOldest = remotes.forPredicate(evenP).forOldest();

        ClusterGroup oddYoungest = remotes.forPredicate(oddP).forYoungest();
        ClusterGroup oddOldest = remotes.forPredicate(oddP).forOldest();

        int clusterSize = ignite.cluster().nodes().size();

        assertEquals(grid(gridMaxOrder(clusterSize, true)).localNode().id(), evenYoungest.node().id());
        assertEquals(grid(1).localNode().id(), evenOldest.node().id());

        assertEquals(grid(gridMaxOrder(clusterSize, false)).localNode().id(), oddYoungest.node().id());
        assertEquals(grid(2).localNode().id(), oddOldest.node().id());

        try (Ignite g4 = startGrid(NODES_CNT); Ignite g5 = startGrid(NODES_CNT + 1)) {
            clusterSize = g4.cluster().nodes().size();

            assertEquals(grid(gridMaxOrder(clusterSize, true)).localNode().id(), evenYoungest.node().id());
            assertEquals(grid(1).localNode().id(), evenOldest.node().id());

            assertEquals(grid(gridMaxOrder(clusterSize, false)).localNode().id(), oddYoungest.node().id());
            assertEquals(grid(2).localNode().id(), oddOldest.node().id());
        }
    }

    /**
     * @throws Exception If failed.
     */
    @Test
    public void testAgeClusterGroupSerialization() throws Exception {
        Marshaller marshaller = ignite.configuration().getMarshaller();

        ClusterGroup grp = ignite.cluster().forYoungest();
        ClusterNode node = grp.node();

        byte[] arr = marshaller.marshal(grp);

        ClusterGroup obj = marshaller.unmarshal(arr, null);

        assertEquals(node.id(), obj.node().id());

        try (Ignite ignore = startGrid()) {
            obj = marshaller.unmarshal(arr, null);

            assertEquals(grp.node().id(), obj.node().id());
            assertFalse(node.id().equals(obj.node().id()));
        }
    }

    /**
     * @throws Exception If failed.
     */
    @Test
    public void testClientServer() throws Exception {
        ClusterGroup srv = ignite.cluster().forServers();

        assertEquals(2, srv.nodes().size());
        assertTrue(srv.nodes().contains(ignite(0).cluster().localNode()));
        assertTrue(srv.nodes().contains(ignite(1).cluster().localNode()));

        ClusterGroup cli = ignite.cluster().forClients();

        assertEquals(2, srv.nodes().size());
        assertTrue(cli.nodes().contains(ignite(2).cluster().localNode()));
        assertTrue(cli.nodes().contains(ignite(3).cluster().localNode()));
    }

    /**
     * @throws Exception If failed.
     */
    @Test
    public void testForCacheNodesOnDynamicCacheCreateDestroy() throws Exception {
        Random rnd = ThreadLocalRandom.current();

        final AtomicReference<Exception> ex = new AtomicReference<>();

        IgniteInternalFuture fut = runCacheCreateDestroyTask(ex);

        while (!fut.isDone())
            ignite.cluster().forCacheNodes("cache" + rnd.nextInt(16)).nodes();

        if (ex.get() != null)
            throw ex.get();
    }

    /**
     * @throws Exception If failed.
     */
    @Test
    public void testForClientNodesOnDynamicCacheCreateDestroy() throws Exception {
        Random rnd = ThreadLocalRandom.current();

        final AtomicReference<Exception> ex = new AtomicReference<>();

        IgniteInternalFuture fut = runCacheCreateDestroyTask(ex);

        while (!fut.isDone())
            ignite.cluster().forClientNodes("cache" + rnd.nextInt(16)).nodes();

        if (ex.get() != null)
            throw ex.get();
    }

    /**
     * @param exHldr Exception holder.
     * @return Task future.
     */
    private IgniteInternalFuture runCacheCreateDestroyTask(final AtomicReference<Exception> exHldr) {
        final long deadline = System.currentTimeMillis() + 5000;

        final AtomicInteger cntr = new AtomicInteger();

        return GridTestUtils.runMultiThreadedAsync(new Runnable() {
            @Override public void run() {
                int startIdx = cntr.getAndAdd(4);
                int idx = 0;
                boolean start = true;

                Set<String> caches = U.newHashSet(4);

                while (System.currentTimeMillis() < deadline) {
                    try {
                        if (start) {
                            caches.add("cache" + (startIdx + idx));
                            ignite.createCache("cache" + (startIdx + idx));
                        }
                        else {
                            ignite.destroyCache("cache" + (startIdx + idx));
                            caches.remove("cache" + (startIdx + idx));
                        }

                        if ((idx = (idx + 1) % 4) == 0)
                            start = !start;
                    }
                    catch (Exception e) {
                        addException(exHldr, e);

                        break;
                    }
                }

                for (String cache : caches) {
                    try {
                        ignite.destroyCache(cache);
                    }
                    catch (Exception e) {
                        addException(exHldr, e);
                    }
                }
            }
        }, 4, "cache-start-destroy");
    }

    /**
     * @param exHldr Exception holder.
     * @param ex Exception.
     */
    private void addException(AtomicReference<Exception> exHldr, Exception ex) {
        if (exHldr.get() != null || !exHldr.compareAndSet(null, ex))
            exHldr.get().addSuppressed(ex);
    }

    /**
     * @throws Exception If failed.
     */
    @Test
    public void testEmptyGroup() throws Exception {
        ClusterGroup emptyGrp = ignite.cluster().forAttribute("nonExistent", "val");

        assertEquals(0, emptyGrp.forOldest().nodes().size());
        assertEquals(0, emptyGrp.forYoungest().nodes().size());
        assertEquals(0, emptyGrp.forAttribute("nonExistent2", "val").nodes().size());
        assertEquals(0, emptyGrp.forCacheNodes("cacheName").nodes().size());
        assertEquals(0, emptyGrp.forClientNodes("cacheName").nodes().size());
        assertEquals(0, emptyGrp.forClients().nodes().size());
        assertEquals(0, emptyGrp.forDaemons().nodes().size());
        assertEquals(0, emptyGrp.forDataNodes("cacheName").nodes().size());
        assertEquals(0, emptyGrp.forRandom().nodes().size());
        assertEquals(0, emptyGrp.forRemotes().nodes().size());
        assertEquals(0, emptyGrp.forServers().nodes().size());
        assertEquals(0, emptyGrp.forHost(ignite.cluster().localNode()).nodes().size());
        assertEquals(0, emptyGrp.forHost("127.0.0.1").nodes().size());
    }

    /**
     * @param cnt Count.
     * @param even Even.
     */
    private static int gridMaxOrder(int cnt, boolean even) {
        assert cnt > 2;

        cnt = cnt - (cnt % 2);

        return even ? cnt - 1 : cnt - 2;
    }
}
