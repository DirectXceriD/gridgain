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

import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.internal.IgniteEx;
import org.apache.ignite.internal.util.typedef.internal.U;
import org.apache.ignite.spi.discovery.tcp.ipfinder.vm.TcpDiscoveryVmIpFinder;
import org.apache.ignite.testframework.GridStringLogger;
import org.apache.ignite.testframework.junits.common.GridCommonAbstractTest;
import javax.management.JMX;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests TcpDiscoverySpiMBean.
 */
@RunWith(JUnit4.class)
public class TcpDiscoverySpiMBeanTest extends GridCommonAbstractTest {
    /** */
    private GridStringLogger strLog = new GridStringLogger();

    /** */
    private static final TcpDiscoveryVmIpFinder IP_FINDER = new TcpDiscoveryVmIpFinder(true);

    /** {@inheritDoc} */
    @Override protected IgniteConfiguration getConfiguration(final String igniteInstanceName) throws Exception {
        IgniteConfiguration cfg = super.getConfiguration(igniteInstanceName);
        TcpDiscoverySpi tcpSpi = new TcpDiscoverySpi();
        tcpSpi.setIpFinder(IP_FINDER);
        cfg.setDiscoverySpi(tcpSpi);
        cfg.setGridLogger(strLog);

        return cfg;
    }

    /**
     * Tests TcpDiscoverySpiMBean#getCurrentTopologyVersion() and TcpDiscoverySpiMBean#dumpRingStructure().
     *
     * @throws Exception if fails.
     */
    @Test
    public void testMBean() throws Exception {
        startGrids(3);

        MBeanServer srv = ManagementFactory.getPlatformMBeanServer();

        try {
            for (int i = 0; i < 3; i++) {
                IgniteEx grid = grid(i);

                ObjectName spiName = U.makeMBeanName(grid.context().igniteInstanceName(), "SPIs",
                        TcpDiscoverySpi.class.getSimpleName());

                TcpDiscoverySpiMBean bean = JMX.newMBeanProxy(srv, spiName, TcpDiscoverySpiMBean.class);

                assertNotNull(bean);
                assertEquals(grid.cluster().topologyVersion(), bean.getCurrentTopologyVersion());

                bean.dumpRingStructure();
                assertTrue(strLog.toString().contains("TcpDiscoveryNodesRing"));
            }
        }
        finally {
            stopAllGrids();
        }
    }
}
