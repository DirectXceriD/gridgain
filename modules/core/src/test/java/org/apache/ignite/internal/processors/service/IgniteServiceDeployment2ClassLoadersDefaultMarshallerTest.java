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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.apache.ignite.cluster.ClusterNode;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.lang.IgnitePredicate;
import org.apache.ignite.marshaller.Marshaller;
import org.apache.ignite.services.Service;
import org.apache.ignite.services.ServiceConfiguration;
import org.apache.ignite.testframework.GridTestExternalClassLoader;
import org.apache.ignite.testframework.config.GridTestProperties;
import org.apache.ignite.testframework.junits.common.GridCommonAbstractTest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests that not all nodes in cluster need user's service definition (only nodes according to filter).
 */
@RunWith(JUnit4.class)
public class IgniteServiceDeployment2ClassLoadersDefaultMarshallerTest extends GridCommonAbstractTest {
    /** */
    private static final String NOOP_SERVICE_CLS_NAME = "org.apache.ignite.tests.p2p.NoopService";

    /** */
    private static final String NOOP_SERVICE_2_CLS_NAME = "org.apache.ignite.tests.p2p.NoopService2";

    /** */
    private static final int GRID_CNT = 6;

    /** */
    private static final String NODE_NAME_ATTR = "NODE_NAME";

    /** */
    private static final URL[] URLS;

    /** */
    private static ClassLoader extClsLdr1;

    /** */
    private static ClassLoader extClsLdr2;

    /** */
    private Set<String> grp1 = new HashSet<>();

    /** */
    private Set<String> grp2 = new HashSet<>();

    /** */
    private boolean client;

    /**
     * Initialize URLs.
     */
    static {
        try {
            URLS = new URL[] {new URL(GridTestProperties.getProperty("p2p.uri.cls"))};
        }
        catch (MalformedURLException e) {
            throw new RuntimeException("Define property p2p.uri.cls", e);
        }
    }


    /** {@inheritDoc} */
    @Override protected IgniteConfiguration getConfiguration(String igniteInstanceName) throws Exception {
        IgniteConfiguration cfg = super.getConfiguration(igniteInstanceName);

        cfg.setPeerClassLoadingEnabled(false);

        cfg.setMarshaller(marshaller());

        cfg.setUserAttributes(Collections.singletonMap(NODE_NAME_ATTR, igniteInstanceName));

        if (grp1.contains(igniteInstanceName))
            cfg.setClassLoader(extClsLdr1);

        if (grp2.contains(igniteInstanceName))
            cfg.setClassLoader(extClsLdr2);

        cfg.setClientMode(client);

        return cfg;
    }

    /**
     * @return Marshaller.
     */
    protected Marshaller marshaller() {
        return null;
    }

    /** {@inheritDoc} */
    @Override protected void beforeTest() throws Exception {
        super.beforeTest();

        for (int i = 0; i < GRID_CNT; i += 2)
            grp1.add(getTestIgniteInstanceName(i));

        for (int i = 1; i < GRID_CNT; i += 2)
            grp2.add(getTestIgniteInstanceName(i));
    }

    /** {@inheritDoc} */
    @Override protected void beforeTestsStarted() throws Exception {
        extClsLdr1 = new GridTestExternalClassLoader(URLS, NOOP_SERVICE_2_CLS_NAME);
        extClsLdr2 = new GridTestExternalClassLoader(URLS, NOOP_SERVICE_CLS_NAME);
    }

    /** {@inheritDoc} */
    @Override protected void afterTestsStopped() throws Exception {
        extClsLdr1 = null;
        extClsLdr2 = null;
    }

    /** {@inheritDoc} */
    @Override protected void afterTest() throws Exception {
        stopAllGrids();

        super.afterTest();
    }

    /**
     * @throws Exception If failed.
     */
    @Test
    public void testServiceDeployment1() throws Exception {
        startGrid(0).services().deploy(serviceConfig(true));

        startGrid(1).services().deploy(serviceConfig(false));

        client = true;

        startGrid(2).services().deploy(serviceConfig(true));

        startGrid(3).services().deploy(serviceConfig(false));

        for (int i = 0; i < 4; i++)
            ignite(i).services().serviceDescriptors();

        ignite(0).services().cancel("TestDeploymentService1");

        ignite(1).services().cancel("TestDeploymentService2");
    }

    /**
     * @throws Exception If failed.
     */
    @Test
    public void testServiceDeployment2() throws Exception {
        for (int i = 0 ; i < 4; i++)
            startGrid(i);

        client = true;

        for (int i = 4 ; i < 6; i++)
            startGrid(i);

        ignite(4).services().deploy(serviceConfig(true));

        ignite(5).services().deploy(serviceConfig(false));
    }

    /**
     * @throws Exception If failed.
     */
    @Test
    public void testServiceDeployment3() throws Exception {
        startGrid(0).services().deploy(serviceConfig(true));

        startGrid(1);

        startGrid(2);

        stopGrid(0);

        awaitPartitionMapExchange();

        ignite(1).services().deploy(serviceConfig(false));

        startGrid(0);
    }

    /**
     * @param firstGrp First group flag.
     * @return Service configuration.
     * @throws Exception If failed.
     */
    private ServiceConfiguration serviceConfig(final boolean firstGrp) throws Exception {
        ServiceConfiguration srvCfg = new ServiceConfiguration();

        srvCfg.setNodeFilter(new TestNodeFilter(firstGrp ? grp1 : grp2));

        Class<Service> srvcCls;

        if (firstGrp)
            srvcCls = (Class<Service>)extClsLdr1.loadClass(NOOP_SERVICE_CLS_NAME);
        else
            srvcCls = (Class<Service>)extClsLdr2.loadClass(NOOP_SERVICE_2_CLS_NAME);

        Service srvc = srvcCls.newInstance();

        srvCfg.setService(srvc);

        srvCfg.setName("TestDeploymentService" + (firstGrp ? 1 : 2));

        srvCfg.setMaxPerNodeCount(1);

        return srvCfg;
    }

    /**
     *
     */
    private static class TestNodeFilter implements IgnitePredicate<ClusterNode> {
        /** */
        private Set<String> grp;

        /**
         * @param grp Group.
         */
        private TestNodeFilter(Set<String> grp) {
            this.grp = grp;
        }

        /** {@inheritDoc} */
        @SuppressWarnings("SuspiciousMethodCalls")
        @Override public boolean apply(ClusterNode node) {
            Object igniteInstanceName = node.attribute(NODE_NAME_ATTR);

            return grp.contains(igniteInstanceName);
        }
    }
}
