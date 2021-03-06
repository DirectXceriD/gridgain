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

package org.apache.ignite.testsuites;

import junit.framework.JUnit4TestAdapter;
import junit.framework.TestSuite;
import org.apache.ignite.examples.BasicExamplesMultiNodeSelfTest;
import org.apache.ignite.examples.BasicExamplesSelfTest;
import org.apache.ignite.examples.CacheClientBinaryExampleTest;
import org.apache.ignite.examples.CacheContinuousQueryExamplesSelfTest;
import org.apache.ignite.examples.CacheExamplesMultiNodeSelfTest;
import org.apache.ignite.examples.CacheExamplesSelfTest;
import org.apache.ignite.examples.CheckpointExamplesSelfTest;
import org.apache.ignite.examples.ClusterGroupExampleSelfTest;
import org.apache.ignite.examples.ComputeClientBinaryExampleTest;
import org.apache.ignite.examples.ContinuationExamplesMultiNodeSelfTest;
import org.apache.ignite.examples.ContinuationExamplesSelfTest;
import org.apache.ignite.examples.ContinuousMapperExamplesMultiNodeSelfTest;
import org.apache.ignite.examples.ContinuousMapperExamplesSelfTest;
import org.apache.ignite.examples.DeploymentExamplesMultiNodeSelfTest;
import org.apache.ignite.examples.DeploymentExamplesSelfTest;
import org.apache.ignite.examples.EncryptedCacheExampleSelfTest;
import org.apache.ignite.examples.EventsExamplesMultiNodeSelfTest;
import org.apache.ignite.examples.EventsExamplesSelfTest;
import org.apache.ignite.examples.IgfsExamplesSelfTest;
import org.apache.ignite.examples.LifecycleExamplesSelfTest;
import org.apache.ignite.examples.MemcacheRestExamplesMultiNodeSelfTest;
import org.apache.ignite.examples.MemcacheRestExamplesSelfTest;
import org.apache.ignite.examples.MessagingExamplesSelfTest;
import org.apache.ignite.examples.MonteCarloExamplesMultiNodeSelfTest;
import org.apache.ignite.examples.MonteCarloExamplesSelfTest;
import org.apache.ignite.examples.SpringBeanExamplesSelfTest;
import org.apache.ignite.examples.SpringDataExampleSelfTest;
import org.apache.ignite.examples.SqlExamplesSelfTest;
import org.apache.ignite.examples.TaskExamplesMultiNodeSelfTest;
import org.apache.ignite.examples.TaskExamplesSelfTest;
import org.junit.runner.RunWith;
import org.junit.runners.AllTests;

/**
 * Examples test suite.
 * <p>
 * Contains all Ignite examples tests.</p>
 */
@RunWith(AllTests.class)
public class IgniteExamplesSelfTestSuite {
    /**
     * @return Suite.
     */
    public static TestSuite suite() {
//        System.setProperty(IGNITE_OVERRIDE_MCAST_GRP,
//            GridTestUtils.getNextMulticastGroup(IgniteExamplesSelfTestSuite.class));

        TestSuite suite = new TestSuite("Ignite Examples Test Suite");

        suite.addTest(new JUnit4TestAdapter(CacheExamplesSelfTest.class));
        suite.addTest(new JUnit4TestAdapter(SqlExamplesSelfTest.class));
        suite.addTest(new JUnit4TestAdapter(BasicExamplesSelfTest.class));
        suite.addTest(new JUnit4TestAdapter(ContinuationExamplesSelfTest.class));
        suite.addTest(new JUnit4TestAdapter(ContinuousMapperExamplesSelfTest.class));
        suite.addTest(new JUnit4TestAdapter(DeploymentExamplesSelfTest.class));
        suite.addTest(new JUnit4TestAdapter(EventsExamplesSelfTest.class));
        suite.addTest(new JUnit4TestAdapter(LifecycleExamplesSelfTest.class));
        suite.addTest(new JUnit4TestAdapter(MessagingExamplesSelfTest.class));
        suite.addTest(new JUnit4TestAdapter(MemcacheRestExamplesSelfTest.class));
        suite.addTest(new JUnit4TestAdapter(MonteCarloExamplesSelfTest.class));
        suite.addTest(new JUnit4TestAdapter(TaskExamplesSelfTest.class));
        suite.addTest(new JUnit4TestAdapter(SpringBeanExamplesSelfTest.class));
        suite.addTest(new JUnit4TestAdapter(SpringDataExampleSelfTest.class));
        suite.addTest(new JUnit4TestAdapter(IgfsExamplesSelfTest.class));
        suite.addTest(new JUnit4TestAdapter(CheckpointExamplesSelfTest.class));
        suite.addTest(new JUnit4TestAdapter(ClusterGroupExampleSelfTest.class));
        suite.addTest(new JUnit4TestAdapter(CacheContinuousQueryExamplesSelfTest.class));

        // Multi-node.
        suite.addTest(new JUnit4TestAdapter(CacheExamplesMultiNodeSelfTest.class));
        suite.addTest(new JUnit4TestAdapter(BasicExamplesMultiNodeSelfTest.class));
        suite.addTest(new JUnit4TestAdapter(ContinuationExamplesMultiNodeSelfTest.class));
        suite.addTest(new JUnit4TestAdapter(ContinuousMapperExamplesMultiNodeSelfTest.class));
        suite.addTest(new JUnit4TestAdapter(DeploymentExamplesMultiNodeSelfTest.class));
        suite.addTest(new JUnit4TestAdapter(EventsExamplesMultiNodeSelfTest.class));
        suite.addTest(new JUnit4TestAdapter(TaskExamplesMultiNodeSelfTest.class));
        suite.addTest(new JUnit4TestAdapter(MemcacheRestExamplesMultiNodeSelfTest.class));
        suite.addTest(new JUnit4TestAdapter(MonteCarloExamplesMultiNodeSelfTest.class));

        // Binary.
        suite.addTest(new JUnit4TestAdapter(CacheClientBinaryExampleTest.class));
        suite.addTest(new JUnit4TestAdapter(ComputeClientBinaryExampleTest.class));

        // ML Grid.
        suite.addTest(new JUnit4TestAdapter(IgniteExamplesMLTestSuite.class));

        // Encryption.
        suite.addTest(new JUnit4TestAdapter(EncryptedCacheExampleSelfTest.class));

        return suite;
    }
}
