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
import org.apache.ignite.internal.processors.cache.query.continuous.CacheContinuousBatchAckTest;
import org.apache.ignite.internal.processors.cache.query.continuous.CacheContinuousQueryAsyncFilterListenerTest;
import org.apache.ignite.internal.processors.cache.query.continuous.CacheContinuousQueryOperationP2PTest;
import org.apache.ignite.internal.processors.cache.query.continuous.CacheContinuousQueryOrderingEventTest;
import org.apache.ignite.internal.processors.cache.query.continuous.CacheContinuousQueryRandomOperationsTest;
import org.apache.ignite.internal.processors.cache.query.continuous.CacheContinuousWithTransformerPartitionedSelfTest;
import org.apache.ignite.internal.processors.cache.query.continuous.CacheContinuousWithTransformerRandomOperationsTest;
import org.apache.ignite.internal.processors.cache.query.continuous.CacheKeepBinaryIterationNearEnabledTest;
import org.apache.ignite.internal.processors.cache.query.continuous.CacheKeepBinaryIterationStoreEnabledTest;
import org.apache.ignite.internal.processors.cache.query.continuous.CacheKeepBinaryIterationTest;
import org.apache.ignite.internal.processors.cache.query.continuous.ContinuousQueryMarshallerTest;
import org.apache.ignite.internal.processors.cache.query.continuous.GridCacheContinuousQueryLocalAtomicSelfTest;
import org.apache.ignite.internal.processors.cache.query.continuous.GridCacheContinuousQueryMultiNodesFilteringTest;
import org.apache.ignite.internal.processors.cache.query.continuous.GridCacheContinuousQueryPartitionAtomicOneNodeTest;
import org.apache.ignite.internal.processors.cache.query.continuous.GridCacheContinuousQueryPartitionedOnlySelfTest;
import org.apache.ignite.internal.processors.cache.query.continuous.GridCacheContinuousQueryReplicatedAtomicOneNodeTest;
import org.apache.ignite.internal.processors.cache.query.continuous.GridCacheContinuousQueryReplicatedAtomicSelfTest;
import org.apache.ignite.internal.processors.cache.query.continuous.IgniteCacheContinuousQueryClientTest;
import org.junit.runner.RunWith;
import org.junit.runners.AllTests;

/**
 * Test suite for cache queries.
 */
@RunWith(AllTests.class)
public class IgniteCacheQuerySelfTestSuite6 {
    /**
     * @return Test suite.
     */
    public static TestSuite suite() {
        TestSuite suite = new TestSuite("Ignite Cache Continuous Queries Test Suite 3");

        // Continuous queries 3.
        suite.addTest(new JUnit4TestAdapter(GridCacheContinuousQueryPartitionAtomicOneNodeTest.class));
        suite.addTest(new JUnit4TestAdapter(CacheContinuousWithTransformerPartitionedSelfTest.class));
        suite.addTest(new JUnit4TestAdapter(GridCacheContinuousQueryLocalAtomicSelfTest.class));
        suite.addTest(new JUnit4TestAdapter(GridCacheContinuousQueryReplicatedAtomicOneNodeTest.class));
        suite.addTest(new JUnit4TestAdapter(ContinuousQueryMarshallerTest.class));
        suite.addTest(new JUnit4TestAdapter(GridCacheContinuousQueryReplicatedAtomicSelfTest.class));
        suite.addTest(new JUnit4TestAdapter(CacheKeepBinaryIterationTest.class));
        suite.addTest(new JUnit4TestAdapter(GridCacheContinuousQueryMultiNodesFilteringTest.class));
        suite.addTest(new JUnit4TestAdapter(CacheKeepBinaryIterationStoreEnabledTest.class));
        suite.addTest(new JUnit4TestAdapter(CacheKeepBinaryIterationNearEnabledTest.class));
        suite.addTest(new JUnit4TestAdapter(GridCacheContinuousQueryPartitionedOnlySelfTest.class));
        suite.addTest(new JUnit4TestAdapter(CacheContinuousQueryOperationP2PTest.class));
        suite.addTest(new JUnit4TestAdapter(CacheContinuousBatchAckTest.class));
        suite.addTest(new JUnit4TestAdapter(CacheContinuousQueryOrderingEventTest.class));
        suite.addTest(new JUnit4TestAdapter(IgniteCacheContinuousQueryClientTest.class));
        suite.addTest(new JUnit4TestAdapter(CacheContinuousQueryAsyncFilterListenerTest.class));
        suite.addTest(new JUnit4TestAdapter(CacheContinuousWithTransformerRandomOperationsTest.class));
        suite.addTest(new JUnit4TestAdapter(CacheContinuousQueryRandomOperationsTest.class));

        return suite;
    }
}
