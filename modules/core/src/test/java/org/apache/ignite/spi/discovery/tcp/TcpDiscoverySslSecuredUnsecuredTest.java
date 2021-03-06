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

package org.apache.ignite.spi.discovery.tcp;

import java.io.IOException;
import java.io.InputStream;
import java.io.StreamCorruptedException;
import java.net.Socket;
import java.util.concurrent.Callable;
import javax.net.ssl.SSLException;
import org.apache.ignite.IgniteCheckedException;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.spi.discovery.tcp.ipfinder.TcpDiscoveryIpFinder;
import org.apache.ignite.testframework.GridTestUtils;
import org.apache.ignite.testframework.junits.common.GridCommonAbstractTest;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests cases when node connects to cluster with different SSL configuration.
 * Exception with meaningful message should be thrown.
 */
@RunWith(JUnit4.class)
public class TcpDiscoverySslSecuredUnsecuredTest extends GridCommonAbstractTest {
    /** */
    private volatile TcpDiscoverySpi spi;

    /** {@inheritDoc} */
    @Override protected IgniteConfiguration getConfiguration(final String gridName) throws Exception {
        final IgniteConfiguration cfg = super.getConfiguration(gridName);

        cfg.setClientMode(gridName.contains("client"));

        if (gridName.contains("ssl"))
            cfg.setSslContextFactory(GridTestUtils.sslFactory());

        if (spi != null) {
            final TcpDiscoveryIpFinder finder = ((TcpDiscoverySpi)cfg.getDiscoverySpi()).getIpFinder();

            spi.setIpFinder(finder);

            cfg.setDiscoverySpi(spi);
        }

        return cfg;
    }

    /** {@inheritDoc} */
    @Override protected void afterTest() throws Exception {
        stopAllGrids();
    }

    /**
     * @throws Exception If failed.
     */
    @Test
    public void testSecuredUnsecuredServerConnection() throws Exception {
        checkConnection("plain-server", "ssl-server");
    }

    /**
     * @throws Exception If failed.
     */
    @Test
    public void testUnsecuredSecuredServerConnection() throws Exception {
        checkConnection("ssl-server", "plain-server");
    }

    /**
     * @throws Exception If failed.
     */
    @Test
    public void testSecuredClientUnsecuredServerConnection() throws Exception {
        checkConnection("plain-server", "ssl-client");
    }

    /**
     * @throws Exception If failed.
     */
    @Test
    public void testUnsecuredClientSecuredServerConnection() throws Exception {
        checkConnection("ssl-server", "plain-client");
    }

    /**
     * @throws Exception If failed.
     */
    @Test
    public void testPlainServerNodesRestart() throws Exception {
        checkNodesRestart("plain-server-1", "plain-server-2");
    }

    /**
     * @throws Exception If failed.
     */
    @Test
    public void testSslServerNodesRestart() throws Exception {
        checkNodesRestart("ssl-server-1", "ssl-server-2");
    }

    /**
     * @throws Exception If failed.
     */
    @Test
    public void testPlainClientNodesRestart() throws Exception {
        checkNodesRestart("plain-server", "plain-client");
    }

    /**
     * @throws Exception If failed.
     */
    @Test
    public void testSslClientNodesRestart() throws Exception {
        checkNodesRestart("ssl-server", "ssl-client");
    }

    /**
     * @param name1 First grid name.
     * @param name2 Second grid name.
     * @throws Exception If failed.
     */
    private void checkNodesRestart(String name1, String name2) throws Exception {
        startGrid(name1);

        spi = new FailDiscoverySpi(!name1.contains("ssl"));

        startGrid(name2);
    }

    /**
     * @param name1 First grid name.
     * @param name2 Second grid name.
     * @throws Exception If failed.
     */
    @SuppressWarnings("ThrowableNotThrown")
    private void checkConnection(final String name1, final String name2) throws Exception {
        startGrid(name1);

        GridTestUtils.assertThrows(null, new Callable<Object>() {
            @Override public Object call() throws Exception {
                startGrid(name2);

                return null;
            }
        }, IgniteCheckedException.class, null);
    }

    /**
     *
     */
    private class FailDiscoverySpi extends TcpDiscoverySpi {
        /** */
        private int cnt = 1;

        /** */
        private final boolean plain;

        /**
         * @param plain Plain conection flag.
         */
        private FailDiscoverySpi(final boolean plain) {
            this.plain = plain;
        }

        /** {@inheritDoc} */
        @Override protected <T> T readMessage(final Socket sock, @Nullable final InputStream in,
            final long timeout) throws IOException, IgniteCheckedException {
            if (cnt-- > 0) {
                if (plain)
                    throw new StreamCorruptedException("Test exception");
                else
                    throw new SSLException("Test SSL exception");
            }

            return super.readMessage(sock, in, timeout);
        }
    }


}
