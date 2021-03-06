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

import java.util.Collection;
import junit.framework.TestSuite;
import org.apache.ignite.internal.processors.cache.CacheNoAffinityExchangeTest;
import org.apache.ignite.internal.processors.cache.PartitionedAtomicCacheGetsDistributionTest;
import org.apache.ignite.internal.processors.cache.PartitionedTransactionalOptimisticCacheGetsDistributionTest;
import org.apache.ignite.internal.processors.cache.PartitionedTransactionalPessimisticCacheGetsDistributionTest;
import org.apache.ignite.internal.processors.cache.PartitionsExchangeCoordinatorFailoverTest;
import org.apache.ignite.internal.processors.cache.ReplicatedAtomicCacheGetsDistributionTest;
import org.apache.ignite.internal.processors.cache.ReplicatedTransactionalOptimisticCacheGetsDistributionTest;
import org.apache.ignite.internal.processors.cache.ReplicatedTransactionalPessimisticCacheGetsDistributionTest;
import org.apache.ignite.internal.processors.cache.datastructures.IgniteExchangeLatchManagerCoordinatorFailTest;
import org.apache.ignite.internal.processors.cache.distributed.CacheExchangeMergeTest;
import org.apache.ignite.internal.processors.cache.distributed.CacheParallelStartTest;
import org.apache.ignite.internal.processors.cache.distributed.CacheTryLockMultithreadedTest;
import org.apache.ignite.internal.processors.cache.distributed.GridCachePartitionEvictionDuringReadThroughSelfTest;
import org.apache.ignite.internal.processors.cache.distributed.IgniteCache150ClientsTest;
import org.apache.ignite.internal.processors.cache.distributed.IgniteCacheThreadLocalTxTest;
import org.apache.ignite.internal.processors.cache.distributed.IgniteOptimisticTxSuspendResumeTest;
import org.apache.ignite.internal.processors.cache.distributed.IgnitePessimisticTxSuspendResumeTest;
import org.apache.ignite.internal.processors.cache.transactions.TxLabelTest;
import org.apache.ignite.internal.processors.cache.transactions.TxMultiCacheAsyncOpsTest;
import org.apache.ignite.internal.processors.cache.transactions.TxOnCachesStartTest;
import org.apache.ignite.internal.processors.cache.transactions.TxOptimisticOnPartitionExchangeTest;
import org.apache.ignite.internal.processors.cache.transactions.TxOptimisticPrepareOnUnstableTopologyTest;
import org.apache.ignite.internal.processors.cache.transactions.TxRollbackAsyncNearCacheTest;
import org.apache.ignite.internal.processors.cache.transactions.TxRollbackAsyncTest;
import org.apache.ignite.internal.processors.cache.transactions.TxRollbackOnIncorrectParamsTest;
import org.apache.ignite.internal.processors.cache.transactions.TxRollbackOnTimeoutNearCacheTest;
import org.apache.ignite.internal.processors.cache.transactions.TxRollbackOnTimeoutNoDeadlockDetectionTest;
import org.apache.ignite.internal.processors.cache.transactions.TxRollbackOnTimeoutOnePhaseCommitTest;
import org.apache.ignite.internal.processors.cache.transactions.TxRollbackOnTimeoutTest;
import org.apache.ignite.internal.processors.cache.transactions.TxRollbackOnTopologyChangeTest;
import org.apache.ignite.internal.processors.cache.transactions.TxStateChangeEventTest;
import org.apache.ignite.testframework.GridTestUtils;
import org.junit.runner.RunWith;
import org.junit.runners.AllTests;

/**
 * Test suite.
 */
@RunWith(AllTests.class)
public class IgniteCacheTestSuite6 {
    /**
     * @return IgniteCache test suite.
     */
    public static TestSuite suite() {
        return suite(null);
    }

    /**
     * @param ignoredTests Ignored tests.
     * @return IgniteCache test suite.
     */
    public static TestSuite suite(Collection<Class> ignoredTests) {
        TestSuite suite = new TestSuite("IgniteCache Test Suite part 6");

        GridTestUtils.addTestIfNeeded(suite, GridCachePartitionEvictionDuringReadThroughSelfTest.class, ignoredTests);
        GridTestUtils.addTestIfNeeded(suite, IgniteOptimisticTxSuspendResumeTest.class, ignoredTests);
        GridTestUtils.addTestIfNeeded(suite, IgnitePessimisticTxSuspendResumeTest.class, ignoredTests);

        GridTestUtils.addTestIfNeeded(suite, CacheExchangeMergeTest.class, ignoredTests);

        GridTestUtils.addTestIfNeeded(suite, TxRollbackOnTimeoutTest.class, ignoredTests);
        GridTestUtils.addTestIfNeeded(suite, TxRollbackOnTimeoutNoDeadlockDetectionTest.class, ignoredTests);
        GridTestUtils.addTestIfNeeded(suite, TxRollbackOnTimeoutNearCacheTest.class, ignoredTests);
        GridTestUtils.addTestIfNeeded(suite, IgniteCacheThreadLocalTxTest.class, ignoredTests);
        GridTestUtils.addTestIfNeeded(suite, TxRollbackAsyncTest.class, ignoredTests);
        GridTestUtils.addTestIfNeeded(suite, TxRollbackAsyncNearCacheTest.class, ignoredTests);
        GridTestUtils.addTestIfNeeded(suite, TxRollbackOnTopologyChangeTest.class, ignoredTests);
        GridTestUtils.addTestIfNeeded(suite, TxRollbackOnTimeoutOnePhaseCommitTest.class, ignoredTests);

        GridTestUtils.addTestIfNeeded(suite, TxOptimisticPrepareOnUnstableTopologyTest.class, ignoredTests);

        GridTestUtils.addTestIfNeeded(suite, TxLabelTest.class, ignoredTests);
        GridTestUtils.addTestIfNeeded(suite, TxRollbackOnIncorrectParamsTest.class, ignoredTests);
        GridTestUtils.addTestIfNeeded(suite, TxStateChangeEventTest.class, ignoredTests);

        GridTestUtils.addTestIfNeeded(suite, TxMultiCacheAsyncOpsTest.class, ignoredTests);

        GridTestUtils.addTestIfNeeded(suite, TxOnCachesStartTest.class, ignoredTests);

        GridTestUtils.addTestIfNeeded(suite, IgniteCache150ClientsTest.class, ignoredTests);

//        TODO enable this test after IGNITE-6753, now it takes too long
//        GridTestUtils.addTestIfNeeded(suite, IgniteOutOfMemoryPropagationTest.class, ignoredTests);

        GridTestUtils.addTestIfNeeded(suite, ReplicatedAtomicCacheGetsDistributionTest.class, ignoredTests);
        GridTestUtils.addTestIfNeeded(suite, ReplicatedTransactionalOptimisticCacheGetsDistributionTest.class, ignoredTests);
        GridTestUtils.addTestIfNeeded(suite, ReplicatedTransactionalPessimisticCacheGetsDistributionTest.class, ignoredTests);

        GridTestUtils.addTestIfNeeded(suite, PartitionedAtomicCacheGetsDistributionTest.class, ignoredTests);
        GridTestUtils.addTestIfNeeded(suite, PartitionedTransactionalOptimisticCacheGetsDistributionTest.class, ignoredTests);
        GridTestUtils.addTestIfNeeded(suite, PartitionedTransactionalPessimisticCacheGetsDistributionTest.class, ignoredTests);

        GridTestUtils.addTestIfNeeded(suite, TxOptimisticOnPartitionExchangeTest.class, ignoredTests);

        GridTestUtils.addTestIfNeeded(suite, IgniteExchangeLatchManagerCoordinatorFailTest.class, ignoredTests);

        GridTestUtils.addTestIfNeeded(suite, PartitionsExchangeCoordinatorFailoverTest.class, ignoredTests);
        GridTestUtils.addTestIfNeeded(suite, CacheTryLockMultithreadedTest.class, ignoredTests);

        GridTestUtils.addTestIfNeeded(suite, CacheParallelStartTest.class, ignoredTests);

        GridTestUtils.addTestIfNeeded(suite, CacheNoAffinityExchangeTest.class, ignoredTests);

        //GridTestUtils.addTestIfNeeded(suite, CacheClientsConcurrentStartTest.class, ignoredTests);
        //GridTestUtils.addTestIfNeeded(suite, GridCacheRebalancingOrderingTest.class, ignoredTests);
        //GridTestUtils.addTestIfNeeded(suite, IgniteCacheClientMultiNodeUpdateTopologyLockTest.class, ignoredTests);

        return suite;
    }
}
