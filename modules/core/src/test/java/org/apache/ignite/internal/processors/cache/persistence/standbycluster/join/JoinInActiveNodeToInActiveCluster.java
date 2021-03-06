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

package org.apache.ignite.internal.processors.cache.persistence.standbycluster.join;

import org.apache.ignite.internal.processors.cache.persistence.standbycluster.AbstractNodeJoinTemplate;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 *
 */
@RunWith(JUnit4.class)
public class JoinInActiveNodeToInActiveCluster extends AbstractNodeJoinTemplate {
    /** {@inheritDoc} */
    @Override public JoinNodeTestPlanBuilder withOutConfigurationTemplate() throws Exception {
        JoinNodeTestPlanBuilder b = builder();

        b.clusterConfiguration(
            cfg(name(0)).setActiveOnStart(false),
            cfg(name(1)).setActiveOnStart(false),
            cfg(name(2)).setActiveOnStart(false)
        ).afterClusterStarted(
            b.checkCacheEmpty()
        ).nodeConfiguration(
            cfg(name(3)).setActiveOnStart(false)
        ).afterNodeJoin(
            b.checkCacheEmpty()
        ).stateAfterJoin(
            false
        ).afterActivate(
            b.checkCacheOnlySystem()
        );

        return b;
    }

    /** {@inheritDoc} */
    @Override public JoinNodeTestPlanBuilder staticCacheConfigurationOnJoinTemplate() throws Exception {
        JoinNodeTestPlanBuilder b = builder();

        b.clusterConfiguration(
            cfg(name(0)).setActiveOnStart(false),
            cfg(name(1)).setActiveOnStart(false),
            cfg(name(2)).setActiveOnStart(false)
        ).afterClusterStarted(
            b.checkCacheEmpty()
        ).nodeConfiguration(
            cfg(name(3))
                .setActiveOnStart(false)
                .setCacheConfiguration(allCacheConfigurations())
        ).afterNodeJoin(
            b.checkCacheEmpty()
        ).stateAfterJoin(
            false
        ).afterActivate(
            b.checkCacheNotEmpty()
        );

        return b;
    }

    /** {@inheritDoc} */
    @Override public JoinNodeTestPlanBuilder staticCacheConfigurationInClusterTemplate() throws Exception {
        JoinNodeTestPlanBuilder b = builder();

        b.clusterConfiguration(
            cfg(name(0))
                .setActiveOnStart(false)
                .setCacheConfiguration(allCacheConfigurations()),
            cfg(name(1)).setActiveOnStart(false),
            cfg(name(2)).setActiveOnStart(false)
        ).afterClusterStarted(
            b.checkCacheEmpty()
        ).nodeConfiguration(
            cfg(name(3)).setActiveOnStart(false)
        ).afterNodeJoin(
            b.checkCacheEmpty()
        ).stateAfterJoin(
            false
        ).afterActivate(
            b.checkCacheNotEmpty()
        );

        return b;
    }

    /** {@inheritDoc} */
    @Override public JoinNodeTestPlanBuilder staticCacheConfigurationSameOnBothTemplate() throws Exception {
        JoinNodeTestPlanBuilder b = builder();

        b.clusterConfiguration(
            cfg(name(0))
                .setActiveOnStart(false)
                .setCacheConfiguration(allCacheConfigurations()),
            cfg(name(1)).setActiveOnStart(false),
            cfg(name(2)).setActiveOnStart(false)
        ).afterClusterStarted(
            b.checkCacheEmpty()
        ).nodeConfiguration(
            cfg(name(3))
                .setActiveOnStart(false)
                .setCacheConfiguration(allCacheConfigurations())
        ).afterNodeJoin(
            b.checkCacheEmpty()
        ).stateAfterJoin(
            false
        ).afterActivate(
            b.checkCacheNotEmpty()
        );

        return b;
    }

    /** {@inheritDoc} */
    @Override public JoinNodeTestPlanBuilder staticCacheConfigurationDifferentOnBothTemplate() throws Exception {
        JoinNodeTestPlanBuilder b = builder();

        b.clusterConfiguration(
            cfg(name(0))
                .setActiveOnStart(false)
                .setCacheConfiguration(transactionCfg()),
            cfg(name(1)).setActiveOnStart(false),
            cfg(name(2)).setActiveOnStart(false)
        ).afterClusterStarted(
            b.checkCacheEmpty()
        ).nodeConfiguration(
            cfg(name(3))
                .setActiveOnStart(false)
                .setCacheConfiguration(atomicCfg())
        ).afterNodeJoin(
            b.checkCacheEmpty()
        ).stateAfterJoin(
            false
        ).afterActivate(
            b.checkCacheNotEmpty()
        );

        return b;
    }

    // Server node join.

    /** {@inheritDoc} */
    @Test
    @Override public void testJoinWithOutConfiguration() throws Exception {
        withOutConfigurationTemplate().execute();
    }

    /** {@inheritDoc} */
    @Test
    @Override public void testStaticCacheConfigurationOnJoin() throws Exception {
        staticCacheConfigurationOnJoinTemplate().execute();
    }

    /** {@inheritDoc} */
    @Test
    @Override public void testStaticCacheConfigurationInCluster() throws Exception {
        staticCacheConfigurationInClusterTemplate().execute();
    }

    /** {@inheritDoc} */
    @Test
    @Override public void testStaticCacheConfigurationSameOnBoth() throws Exception {
        staticCacheConfigurationSameOnBothTemplate().execute();
    }

    /** {@inheritDoc} */
    @Test
    @Override public void testStaticCacheConfigurationDifferentOnBoth() throws Exception {
        staticCacheConfigurationDifferentOnBothTemplate().execute();
    }

    // Client node join.

    /** {@inheritDoc} */
    @Test
    @Override public void testJoinClientWithOutConfiguration() throws Exception {
        joinClientWithOutConfigurationTemplate().execute();
    }

    /** {@inheritDoc} */
    @Test
    @Override public void testJoinClientStaticCacheConfigurationOnJoin() throws Exception {
        joinClientStaticCacheConfigurationOnJoinTemplate().execute();
    }

    /** {@inheritDoc} */
    @Test
    @Override public void testJoinClientStaticCacheConfigurationInCluster() throws Exception {
        fail("https://issues.apache.org/jira/browse/IGNITE-5518");

        joinClientStaticCacheConfigurationInClusterTemplate().execute();
    }

    /** {@inheritDoc} */
    @Test
    @Override public void testJoinClientStaticCacheConfigurationSameOnBoth() throws Exception {
        joinClientStaticCacheConfigurationSameOnBothTemplate().execute();
    }

    /** {@inheritDoc} */
    @Test
    @Override public void testJoinClientStaticCacheConfigurationDifferentOnBoth() throws Exception {
        fail("https://issues.apache.org/jira/browse/IGNITE-5518");

        joinClientStaticCacheConfigurationDifferentOnBothTemplate().execute();
    }

    @Override public JoinNodeTestPlanBuilder joinClientWithOutConfigurationTemplate() throws Exception {
        return withOutConfigurationTemplate().nodeConfiguration(setClient);
    }

    @Override public JoinNodeTestPlanBuilder joinClientStaticCacheConfigurationOnJoinTemplate() throws Exception {
        return staticCacheConfigurationOnJoinTemplate().nodeConfiguration(setClient);
    }

    @Override public JoinNodeTestPlanBuilder joinClientStaticCacheConfigurationInClusterTemplate() throws Exception {
        return staticCacheConfigurationInClusterTemplate().nodeConfiguration(setClient);
    }

    @Override public JoinNodeTestPlanBuilder joinClientStaticCacheConfigurationSameOnBothTemplate() throws Exception {
        return staticCacheConfigurationSameOnBothTemplate().nodeConfiguration(setClient);
    }

    @Override public JoinNodeTestPlanBuilder joinClientStaticCacheConfigurationDifferentOnBothTemplate() throws Exception {
        return staticCacheConfigurationDifferentOnBothTemplate().nodeConfiguration(setClient);
    }
}
