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

package org.apache.ignite.internal.managers.deployment;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.ignite.IgniteException;
import org.apache.ignite.cluster.ClusterNode;
import org.apache.ignite.compute.ComputeTaskFuture;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.internal.managers.communication.GridIoMessage;
import org.apache.ignite.lang.IgniteInClosure;
import org.apache.ignite.plugin.extensions.communication.Message;
import org.apache.ignite.spi.IgniteSpiException;
import org.apache.ignite.spi.communication.tcp.TcpCommunicationSpi;
import org.apache.ignite.testframework.junits.common.GridCommonAbstractTest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.apache.ignite.cache.CacheMode.REPLICATED;
import static org.apache.ignite.cache.CacheWriteSynchronizationMode.FULL_SYNC;

/**
 * Tests message count for different deployment scenarios.
 */
@RunWith(JUnit4.class)
public class GridDeploymentMessageCountSelfTest extends GridCommonAbstractTest {
    /** Test p2p task. */
    private static final String TEST_TASK = "org.apache.ignite.tests.p2p.SingleSplitTestTask";

    /** SPIs. */
    private Map<String, MessageCountingCommunicationSpi> commSpis = new ConcurrentHashMap<>();

    /** {@inheritDoc} */
    @Override protected IgniteConfiguration getConfiguration(String igniteInstanceName) throws Exception {
        IgniteConfiguration cfg = super.getConfiguration(igniteInstanceName);

        cfg.setPeerClassLoadingEnabled(true);

        CacheConfiguration cacheCfg = defaultCacheConfiguration();

        cacheCfg.setCacheMode(REPLICATED);
        cacheCfg.setWriteSynchronizationMode(FULL_SYNC);

        cfg.setCacheConfiguration(cacheCfg);

        MessageCountingCommunicationSpi commSpi = new MessageCountingCommunicationSpi();

        commSpis.put(igniteInstanceName, commSpi);

        cfg.setCommunicationSpi(commSpi);

        return cfg;
    }

    /** {@inheritDoc} */
    @Test
    public void testTaskDeployment() throws Exception {
        ClassLoader ldr = getExternalClassLoader();

        Class taskCls = ldr.loadClass(TEST_TASK);

        try {
            startGrids(2);

            ComputeTaskFuture<Object> taskFut = executeAsync(grid(0).compute(), taskCls, 2);

            Integer res = (Integer)taskFut.get();

            assertEquals(Integer.valueOf(2), res);

            for (MessageCountingCommunicationSpi spi : commSpis.values()) {
                assertTrue(spi.deploymentMessageCount() > 0);

                spi.resetCount();
            }

            for (int i = 0; i < 10; i++) {
                taskFut = executeAsync(grid(0).compute(), taskCls, 2);

                res = (Integer)taskFut.get();

                assertEquals(Integer.valueOf(2), res);
            }

            for (MessageCountingCommunicationSpi spi : commSpis.values())
                assertEquals(0, spi.deploymentMessageCount());
        }
        finally {
            stopAllGrids();
        }
    }

    /**
     *
     */
    private class MessageCountingCommunicationSpi extends TcpCommunicationSpi {
        /** */
        private AtomicInteger msgCnt = new AtomicInteger();

        /** {@inheritDoc} */
        @Override public void sendMessage(ClusterNode node, Message msg, IgniteInClosure<IgniteException> ackC)
            throws IgniteSpiException {
            if (isDeploymentMessage((GridIoMessage)msg))
                msgCnt.incrementAndGet();

            super.sendMessage(node, msg, ackC);
        }

        /**
         * @return Number of deployment messages.
         */
        public int deploymentMessageCount() {
            return msgCnt.get();
        }

        /**
         * Resets counter to zero.
         */
        public void resetCount() {
            msgCnt.set(0);
        }

        /**
         * Checks if it is a p2p deployment message.
         *
         * @param msg Message to check.
         * @return {@code True} if this is a p2p message.
         */
        private boolean isDeploymentMessage(GridIoMessage msg) {
            Object origMsg = msg.message();

            boolean dep = (origMsg instanceof GridDeploymentRequest) || (origMsg instanceof GridDeploymentResponse);

            if (dep)
                info(">>> Got deployment message: " + origMsg);

            return dep;
        }
    }
}
