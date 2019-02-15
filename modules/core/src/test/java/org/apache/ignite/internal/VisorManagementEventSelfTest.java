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

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.ignite.cluster.ClusterNode;
import org.apache.ignite.compute.ComputeTask;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.events.EventType;
import org.apache.ignite.events.TaskEvent;
import org.apache.ignite.internal.visor.VisorTaskArgument;
import org.apache.ignite.lang.IgnitePredicate;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.apache.ignite.spi.discovery.tcp.ipfinder.vm.TcpDiscoveryVmIpFinder;
import org.apache.ignite.testframework.junits.common.GridCommonAbstractTest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.apache.ignite.events.EventType.EVT_MANAGEMENT_TASK_STARTED;;

/**
 *
 */
@RunWith(JUnit4.class)
public class VisorManagementEventSelfTest extends GridCommonAbstractTest {
    /** {@inheritDoc} */
    @Override protected IgniteConfiguration getConfiguration(String gridName) throws Exception {
        IgniteConfiguration cfg = new IgniteConfiguration();

        // Enable visor management events.
        cfg.setIncludeEventTypes(
            EVT_MANAGEMENT_TASK_STARTED
        );

        cfg.setCacheConfiguration(
            new CacheConfiguration<Integer, Integer>()
                .setName("TEST")
                .setIndexedTypes(Integer.class, Integer.class)
                .setStatisticsEnabled(true)
        );

        TcpDiscoveryVmIpFinder ipFinder = new TcpDiscoveryVmIpFinder();

        List<String> addrs = Arrays.asList("127.0.0.1:47500..47502");

        ipFinder.setAddresses(addrs);

        TcpDiscoverySpi discoSpi = new TcpDiscoverySpi();

        discoSpi.setIpFinder(ipFinder);

        cfg.setDiscoverySpi(discoSpi);

        return cfg;
    }

    /** {@inheritDoc} */
    @Override protected void afterTest() throws Exception {
        stopAllGrids();

        cleanPersistenceDir();

        super.afterTest();
    }

    /**
     * Current test case start valid one node visor task that has GridVisorManagementTask annotation.
     * No exceptions are expected.
     *
     * @throws Exception If failed.
     */
    @Test
    public void testManagementOneNodeVisorTask() throws Exception {
        IgniteEx ignite = startGrid(0);

        doTestVisorTask(TestManagementVisorOneNodeTask.class, new VisorTaskArgument(), ignite);
    }

    /**
     * Current test case start valid multi node visor task that has GridVisorManagementTask annotation.
     * No exceptions are expected.
     *
     * @throws Exception If failed.
     */
    @Test
    public void testManagementMultiNodeVisorTask() throws Exception {
        IgniteEx ignite = startGrid(0);

        doTestVisorTask(TestManagementVisorMultiNodeTask.class, new VisorTaskArgument(), ignite);
    }

    /**
     * Current test case start one node visor task that has not GridVisorManagementTask annotation.
     * No exceptions are expected.
     *
     * @throws Exception If failed.
     */
    @Test
    public void testNotManagementOneNodeVisorTask() throws Exception {
        IgniteEx ignite = startGrid(0);

        doTestNotManagementVisorTask(TestNotManagementVisorOneNodeTask.class, new VisorTaskArgument(), ignite);
    }

    /**
     * Current test case start multi node visor task that has not GridVisorManagementTask annotation.
     * No exceptions are expected.
     *
     * @throws Exception If failed.
     */
    @Test
    public void testNotManagementMultiNodeVisorTask() throws Exception {
        IgniteEx ignite = startGrid(0);

        doTestNotManagementVisorTask(TestNotManagementVisorMultiNodeTask.class, new VisorTaskArgument(), ignite);
    }

    /**
     * @param cls class of the task.
     * @param arg argument.
     * @param ignite instance of Ignite.
     *
     * @throws Exception If failed.
     */
    private <T, R>  void doTestVisorTask(
        Class<? extends ComputeTask<VisorTaskArgument<T>, R>> cls, T arg, IgniteEx ignite) throws Exception
    {
        final AtomicReference<TaskEvent> evt = new AtomicReference<>();

        final CountDownLatch evtLatch = new CountDownLatch(1);

        ignite.events().localListen(new IgnitePredicate<TaskEvent>() {
            @Override public boolean apply(TaskEvent e) {
                evt.set(e);

                evtLatch.countDown();

                return false;
            }
        }, EventType.EVT_MANAGEMENT_TASK_STARTED);

        for (ClusterNode node : ignite.cluster().forServers().nodes())
            ignite.compute().executeAsync(cls, new VisorTaskArgument<>(node.id(), arg, true));

        assertTrue(evtLatch.await(10000, TimeUnit.MILLISECONDS));

        assertNotNull(evt.get());
    }

    /**
     * @param cls class of the task.
     * @param arg argument.
     * @param ignite instance of Ignite.
     *
     * @throws Exception If failed.
     */
    private <T, R>  void doTestNotManagementVisorTask(
        Class<? extends ComputeTask<VisorTaskArgument<T>, R>> cls, T arg, IgniteEx ignite) throws Exception
    {
        final AtomicReference<TaskEvent> evt = new AtomicReference<>();

        final CountDownLatch evtLatch = new CountDownLatch(1);

        ignite.events().localListen(new IgnitePredicate<TaskEvent>() {
            @Override public boolean apply(TaskEvent e) {
                evt.set(e);

                evtLatch.countDown();

                return false;
            }
        }, EventType.EVT_MANAGEMENT_TASK_STARTED);

        for (ClusterNode node : ignite.cluster().forServers().nodes())
            ignite.compute().executeAsync(cls, new VisorTaskArgument<>(node.id(), arg, true));

        assertFalse(evtLatch.await(10000, TimeUnit.MILLISECONDS));
    }
}
