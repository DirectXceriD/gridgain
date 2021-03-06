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

import java.io.Serializable;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import javax.swing.JOptionPane;
import org.apache.ignite.cluster.ClusterNode;
import org.apache.ignite.events.Event;
import org.apache.ignite.internal.managers.eventstorage.GridLocalEventListener;
import org.apache.ignite.testframework.junits.spi.GridSpiAbstractTest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Base discovery test class.
 * @param <T> SPI implementation class.
 */
@RunWith(JUnit4.class)
public abstract class AbstractDiscoveryTest<T extends DiscoverySpi> extends GridSpiAbstractTest<T> {
    /** */
    @SuppressWarnings({"ClassExplicitlyExtendsThread"})
    private class Pinger extends Thread {
        /** */
        private final Object mux = new Object();

        /** */
        @SuppressWarnings({"FieldAccessedSynchronizedAndUnsynchronized"})
        private boolean isCanceled;

        /** {@inheritDoc} */
        @Override public void run() {
            Random rnd = new Random();

            while (isCanceled) {
                try {
                    Collection<ClusterNode> nodes = getSpi().getRemoteNodes();

                    pingNode(UUID.randomUUID(), false);

                    for (ClusterNode item : nodes) {
                        pingNode(item.id(), true);
                    }

                    pingNode(UUID.randomUUID(), false);
                }
                catch (Exception e) {
                    error("Can't get SPI.", e);
                }

                synchronized (mux) {
                    if (isCanceled) {
                        try {
                            mux.wait(getPingFrequency() * (1 + rnd.nextInt(10)));
                        }
                        catch (InterruptedException e) {
                            //No-op.
                        }
                    }
                }
            }
        }

        /**
         * @param nodeId Node UUID.
         * @param exists Exists flag.
         * @throws Exception If failed.
         */
        private void pingNode(UUID nodeId, boolean exists) throws Exception {
            boolean flag = getSpi().pingNode(nodeId);

            info((flag != exists ? "***Error*** " : "") + "Ping " + (exists ? "exist" : "random") +
                " node [nodeId=" + nodeId + ", pingResult=" + flag + ']');
        }

        /** {@inheritDoc} */
        @Override public void interrupt() {
            synchronized (mux) {
                isCanceled = true;

                mux.notifyAll();
            }

            super.interrupt();
        }
    }

    /**
     * @return Ping frequency.
     */
    public abstract long getPingFrequency();

    /**
     * @return Pinger start flag.
     */
    public boolean isPingerStart() {
        return true;
    }

    /** */
    private class DiscoveryListener implements GridLocalEventListener {
        /** {@inheritDoc} */
        @Override public void onEvent(Event evt) {
            info("Discovery event [event=" + evt + ']');
        }
    }

    /**
     * @throws Exception If failed.
     */
    @Test
    public void testDiscovery() throws Exception {
        GridLocalEventListener discoLsnr = new DiscoveryListener();

        getSpiContext().addLocalEventListener(discoLsnr);

        Pinger pinger = null;

        if (isPingerStart()) {
            pinger = new Pinger();

            pinger.start();
        }

        JOptionPane.showMessageDialog(null, "Press OK to end test.");

        if (pinger != null)
            pinger.interrupt();
    }

    /** {@inheritDoc} */
    @Override protected Map<String, Serializable> getNodeAttributes() {
        Map<String, Serializable> attrs = new HashMap<>(1);

        attrs.put("testDiscoveryAttribute", new Date());

        return attrs;
    }
}
