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
import org.apache.ignite.internal.processors.cache.GridCacheTcpClientDiscoveryMultiThreadedTest;
import org.apache.ignite.testframework.GridTestUtils;
import org.junit.runner.RunWith;
import org.junit.runners.AllTests;

import static org.apache.ignite.internal.processors.cache.distributed.GridCacheClientModesTcpClientDiscoveryAbstractTest.CaseClientPartitionedAtomic;
import static org.apache.ignite.internal.processors.cache.distributed.GridCacheClientModesTcpClientDiscoveryAbstractTest.CaseClientPartitionedTransactional;
import static org.apache.ignite.internal.processors.cache.distributed.GridCacheClientModesTcpClientDiscoveryAbstractTest.CaseClientReplicatedAtomic;
import static org.apache.ignite.internal.processors.cache.distributed.GridCacheClientModesTcpClientDiscoveryAbstractTest.CaseClientReplicatedTransactional;
import static org.apache.ignite.internal.processors.cache.distributed.GridCacheClientModesTcpClientDiscoveryAbstractTest.CaseNearPartitionedAtomic;
import static org.apache.ignite.internal.processors.cache.distributed.GridCacheClientModesTcpClientDiscoveryAbstractTest.CaseNearPartitionedTransactional;
import static org.apache.ignite.internal.processors.cache.distributed.GridCacheClientModesTcpClientDiscoveryAbstractTest.CaseNearReplicatedAtomic;
import static org.apache.ignite.internal.processors.cache.distributed.GridCacheClientModesTcpClientDiscoveryAbstractTest.CaseNearReplicatedTransactional;

/**
 * Tests a cache with TcpClientDiscovery SPI being enabled.
 */
@RunWith(AllTests.class)
public class IgniteCacheTcpClientDiscoveryTestSuite {
    /**
     * @return Suite.
     */
    public static TestSuite suite() {
        return suite(null);
    }

    /**
     * @param ignoredTests Tests to ignore.
     * @return Test suite.
     */
    public static TestSuite suite(Collection<Class> ignoredTests) {
        TestSuite suite = new TestSuite("Cache + TcpClientDiscovery SPI test suite.");

        GridTestUtils.addTestIfNeeded(suite, CaseNearPartitionedAtomic.class, ignoredTests);
        GridTestUtils.addTestIfNeeded(suite, CaseNearPartitionedTransactional.class, ignoredTests);
        GridTestUtils.addTestIfNeeded(suite, CaseNearReplicatedAtomic.class, ignoredTests);
        GridTestUtils.addTestIfNeeded(suite, CaseNearReplicatedTransactional.class, ignoredTests);
        GridTestUtils.addTestIfNeeded(suite, CaseClientPartitionedAtomic.class, ignoredTests);
        GridTestUtils.addTestIfNeeded(suite, CaseClientPartitionedTransactional.class, ignoredTests);
        GridTestUtils.addTestIfNeeded(suite, CaseClientReplicatedAtomic.class, ignoredTests);
        GridTestUtils.addTestIfNeeded(suite, CaseClientReplicatedTransactional.class, ignoredTests);
        GridTestUtils.addTestIfNeeded(suite, GridCacheTcpClientDiscoveryMultiThreadedTest.class, ignoredTests);

        return suite;
    }
}
