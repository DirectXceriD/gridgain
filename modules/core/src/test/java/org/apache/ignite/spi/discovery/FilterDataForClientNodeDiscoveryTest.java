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

package org.apache.ignite.spi.discovery;

import java.util.Arrays;
import org.apache.ignite.cluster.ClusterNode;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.internal.managers.discovery.CustomEventListener;
import org.apache.ignite.internal.managers.discovery.DiscoCache;
import org.apache.ignite.internal.managers.discovery.DiscoveryCustomMessage;
import org.apache.ignite.internal.managers.discovery.DiscoveryServerOnlyCustomMessage;
import org.apache.ignite.internal.managers.discovery.GridDiscoveryManager;
import org.apache.ignite.internal.processors.affinity.AffinityTopologyVersion;
import org.apache.ignite.lang.IgniteUuid;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.apache.ignite.testframework.junits.common.GridCommonAbstractTest;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 *
 */
@RunWith(JUnit4.class)
public class FilterDataForClientNodeDiscoveryTest extends GridCommonAbstractTest {
    /** Join servers count. */
    private int joinSrvCnt;

    /** Join clients count. */
    private int joinCliCnt;

    /** {@inheritDoc} */
    @Override protected void afterTest() throws Exception {
        stopAllGrids();

        super.afterTest();
    }

    /**
     * @throws Exception If failed.
     */
    @Test
    public void testDataBag() throws Exception {
        startGrid(configuration(0, false));
        startGrid(configuration(1, false));

        assertEquals(3, joinSrvCnt);
        assertEquals(0, joinCliCnt);

        startGrid(configuration(2, true));
        startGrid(configuration(3, true));

        assertEquals(5, joinSrvCnt);
        assertEquals(4, joinCliCnt);
    }

    /**
     * @throws Exception If failed.
     */
    @Test
    public void testDiscoveryServerOnlyCustomMessage() throws Exception {
        startGrid(configuration(0, false));
        startGrid(configuration(1, false));
        startGrid(configuration(2, true));
        startGrid(configuration(3, true));

        final boolean [] recvMsg = new boolean[4];

        for (int i = 0; i < 4; ++i) {
            final int idx0 = i;

            grid(i).context().discovery().setCustomEventListener(
                MessageForServer.class, new CustomEventListener<MessageForServer>() {
                @Override public void onCustomEvent(AffinityTopologyVersion topVer, ClusterNode snd,
                    MessageForServer msg) {

                    recvMsg[idx0] = true;
                }
            });
        }

        for (int i = 0; i < 4; ++i) {
            Arrays.fill(recvMsg, false);

            grid(i).context().discovery().sendCustomEvent(new MessageForServer());

            Thread.sleep(500);

            assertEquals(true, recvMsg[0]);
            assertEquals(true, recvMsg[1]);
            assertEquals(false, recvMsg[2]);
            assertEquals(false, recvMsg[3]);
        }
    }


    /**
     * @param nodeIdx Node index.
     * @param client Client flag.
     * @return Ignite configuration.
     * @throws Exception On error.
     */
    private IgniteConfiguration configuration(int nodeIdx, boolean client) throws Exception {
        IgniteConfiguration cfg = getConfiguration(getTestIgniteInstanceName(nodeIdx));

        TcpDiscoverySpi testSpi = new TestDiscoverySpi();

        testSpi.setIpFinder(sharedStaticIpFinder);

        cfg.setDiscoverySpi(testSpi);

        cfg.setClientMode(client);

        return cfg;
    }

    /**
     *
     */
    private class TestDiscoverySpi extends TcpDiscoverySpi {
        /** Test exchange. */
        private TestDiscoveryDataExchange testEx = new TestDiscoveryDataExchange();

        /**
         *
         */
        public TestDiscoverySpi() {
            exchange = testEx;
        }


        /** {@inheritDoc} */
        @Override public void setDataExchange(DiscoverySpiDataExchange exchange) {
            testEx.setExchange(exchange);
        }
    }

    /**
     *
     */
    private class TestDiscoveryDataExchange implements DiscoverySpiDataExchange {
        /** Real exchange. */
        private DiscoverySpiDataExchange ex;

        /** {@inheritDoc} */
        @Override public DiscoveryDataBag collect(DiscoveryDataBag dataBag) {
            if (dataBag.isJoiningNodeClient())
                joinCliCnt++;
            else
                joinSrvCnt++;

            return ex.collect(dataBag);
        }

        /** {@inheritDoc} */
        @Override public void onExchange(DiscoveryDataBag dataBag) {
            ex.onExchange(dataBag);
        }

        /**
         * @param ex Exchange.
         */
        public void setExchange(DiscoverySpiDataExchange ex) {
            this.ex = ex;
        }
    }

    /**
     *
     */
    private static class MessageForServer implements DiscoveryServerOnlyCustomMessage {
        /** */
        private static final long serialVersionUID = 0L;

        /** */
        private final IgniteUuid id = IgniteUuid.randomUuid();

        /** {@inheritDoc} */
        @Override public IgniteUuid id() {
            return id;
        }

        /** {@inheritDoc} */
        @Nullable @Override public DiscoveryCustomMessage ackMessage() {
            return null;
        }

        /** {@inheritDoc} */
        @Override public boolean isMutable() {
            return false;
        }

        /** {@inheritDoc} */
        @Override public boolean stopProcess() {
            return false;
        }

        /** {@inheritDoc} */
        @Override public DiscoCache createDiscoCache(GridDiscoveryManager mgr, AffinityTopologyVersion topVer,
            DiscoCache discoCache) {
            return null;
        }
    }
}
