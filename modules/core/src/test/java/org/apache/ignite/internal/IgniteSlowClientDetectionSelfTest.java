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

import java.util.concurrent.CountDownLatch;
import javax.cache.event.CacheEntryListenerException;
import javax.cache.event.CacheEntryUpdatedListener;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.IgniteState;
import org.apache.ignite.Ignition;
import org.apache.ignite.cache.query.ContinuousQuery;
import org.apache.ignite.cluster.ClusterNode;
import org.apache.ignite.configuration.DataStorageConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.events.DiscoveryEvent;
import org.apache.ignite.events.Event;
import org.apache.ignite.events.EventType;
import org.apache.ignite.internal.managers.communication.GridIoManager;
import org.apache.ignite.internal.util.lang.GridAbsPredicate;
import org.apache.ignite.internal.util.nio.GridNioServer;
import org.apache.ignite.internal.util.typedef.internal.U;
import org.apache.ignite.lang.IgnitePredicate;
import org.apache.ignite.spi.communication.tcp.TcpCommunicationSpi;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.apache.ignite.testframework.GridTestUtils;
import org.apache.ignite.testframework.junits.common.GridCommonAbstractTest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 *
 */
@RunWith(JUnit4.class)
public class IgniteSlowClientDetectionSelfTest extends GridCommonAbstractTest {
    /** */
    public static final String PARTITIONED = "partitioned";

    /**
     * @return Node count.
     */
    private int nodeCount() {
        return 5;
    }

    /** {@inheritDoc} */
    @Override protected IgniteConfiguration getConfiguration(String igniteInstanceName) throws Exception {
        IgniteConfiguration cfg = super.getConfiguration(igniteInstanceName);

        ((TcpDiscoverySpi)cfg.getDiscoverySpi()).setClientReconnectDisabled(true);

        if (getTestIgniteInstanceName(nodeCount() - 1).equals(igniteInstanceName) ||
            getTestIgniteInstanceName(nodeCount() - 2).equals(igniteInstanceName))
            cfg.setClientMode(true);

        TcpCommunicationSpi commSpi = new TcpCommunicationSpi();

        commSpi.setSlowClientQueueLimit(50);
        commSpi.setSharedMemoryPort(-1);
        commSpi.setIdleConnectionTimeout(300_000);
        commSpi.setConnectionsPerNode(1);

        cfg.setCommunicationSpi(commSpi);

        DataStorageConfiguration dbCfg = new DataStorageConfiguration();
        dbCfg.setPageSize(16 * 1024);

        cfg.setDataStorageConfiguration(dbCfg);

        return cfg;
    }

    /** {@inheritDoc} */
    @Override protected void beforeTestsStarted() throws Exception {
        super.beforeTestsStarted();

        startGrids(nodeCount());
    }

    /**
     * @throws Exception If failed.
     */
    @Test
    public void testSlowClient() throws Exception {
        final IgniteEx slowClient = grid(nodeCount() - 1);

        final ClusterNode slowClientNode = slowClient.localNode();

        final CountDownLatch evtSegmentedLatch = new CountDownLatch(1);

        slowClient.events().localListen(new IgnitePredicate<Event>() {
            @Override public boolean apply(Event evt) {
                assertEquals("Unexpected event: " + evt, evt.type(), EventType.EVT_NODE_SEGMENTED);

                DiscoveryEvent evt0 = (DiscoveryEvent)evt;

                assertEquals(slowClientNode, evt0.eventNode());
                assertEquals(5L, evt0.topologyVersion());

                evtSegmentedLatch.countDown();

                return false;
            }
        }, EventType.EVT_NODE_SEGMENTED);

        final CountDownLatch evtFailedLatch = new CountDownLatch(nodeCount() - 1);

        for (int i = 0; i < nodeCount() - 1; i++) {
            grid(i).events().localListen(new IgnitePredicate<Event>() {
                @Override public boolean apply(Event evt) {
                    assertEquals("Unexpected event: " + evt, evt.type(), EventType.EVT_NODE_FAILED);

                    DiscoveryEvent evt0 = (DiscoveryEvent) evt;

                    assertEquals(slowClientNode, evt0.eventNode());
                    assertEquals(6L, evt0.topologyVersion());
                    assertEquals(4, evt0.topologyNodes().size());

                    evtFailedLatch.countDown();

                    return false;
                }
            }, EventType.EVT_NODE_FAILED);
        }

        assertTrue(slowClient.cluster().localNode().isClient());

        IgniteCache<Object, Object> cache = slowClient.getOrCreateCache(PARTITIONED);

        IgniteEx client0 = grid(nodeCount() - 2);

        assertTrue(client0.cluster().localNode().isClient());

        IgniteCache<Object, Object> cache0 = client0.getOrCreateCache(PARTITIONED);

        cache.query(new ContinuousQuery<>().setLocalListener(new Listener()));

        for (int i = 0; i < 100; i++)
            cache0.put(0, i);

        GridIoManager ioMgr = slowClient.context().io();

        TcpCommunicationSpi commSpi = (TcpCommunicationSpi)((Object[])U.field(ioMgr, "spis"))[0];

        GridNioServer nioSrvr = U.field(commSpi, "nioSrvr");

        GridTestUtils.setFieldValue(nioSrvr, "skipRead", true);

        // Initiate messages for client.
        for (int i = 0; i < 100; i++)
            cache0.put(0, new byte[10 * 1024]);

        boolean wait = GridTestUtils.waitForCondition(new GridAbsPredicate() {
            @Override public boolean apply() {
                return Ignition.state(slowClient.name()) == IgniteState.STOPPED_ON_SEGMENTATION;
            }
        }, getTestTimeout());

        assertTrue(wait);

        assertTrue("Failed to wait for client failed event", evtFailedLatch.await(5000, MILLISECONDS));
        assertTrue("Failed to wait for client segmented event", evtSegmentedLatch.await(5000, MILLISECONDS));
    }

    /**
     *
     */
    private static class Listener implements CacheEntryUpdatedListener<Object, Object> {
        /** {@inheritDoc} */
        @Override public void onUpdated(Iterable iterable) throws CacheEntryListenerException {
            System.out.println(">>>> Received update: " + iterable);
        }
    }
}
