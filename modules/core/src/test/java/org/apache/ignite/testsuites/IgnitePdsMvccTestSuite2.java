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
import java.util.HashSet;
import junit.framework.TestSuite;
import org.apache.ignite.IgniteSystemProperties;
import org.apache.ignite.internal.processors.cache.persistence.IgniteDataStorageMetricsSelfTest;
import org.apache.ignite.internal.processors.cache.persistence.IgnitePdsExchangeDuringCheckpointTest;
import org.apache.ignite.internal.processors.cache.persistence.IgnitePdsPageSizesTest;
import org.apache.ignite.internal.processors.cache.persistence.IgnitePersistentStoreDataStructuresTest;
import org.apache.ignite.internal.processors.cache.persistence.LocalWalModeNoChangeDuringRebalanceOnNonNodeAssignTest;
import org.apache.ignite.internal.processors.cache.persistence.baseline.IgniteAbsentEvictionNodeOutOfBaselineTest;
import org.apache.ignite.internal.processors.cache.persistence.db.IgnitePdsReserveWalSegmentsTest;
import org.apache.ignite.internal.processors.cache.persistence.db.IgnitePdsReserveWalSegmentsWithCompactionTest;
import org.apache.ignite.internal.processors.cache.persistence.db.filename.IgniteUidAsConsistentIdMigrationTest;
import org.apache.ignite.internal.processors.cache.persistence.db.wal.FsyncWalRolloverDoesNotBlockTest;
import org.apache.ignite.internal.processors.cache.persistence.db.wal.IgniteWALTailIsReachedDuringIterationOverArchiveTest;
import org.apache.ignite.internal.processors.cache.persistence.db.wal.IgniteWalFormatFileFailoverTest;
import org.apache.ignite.internal.processors.cache.persistence.db.wal.IgniteWalIteratorExceptionDuringReadTest;
import org.apache.ignite.internal.processors.cache.persistence.db.wal.IgniteWalIteratorSwitchSegmentTest;
import org.apache.ignite.internal.processors.cache.persistence.db.wal.IgniteWalSerializerVersionTest;
import org.apache.ignite.internal.processors.cache.persistence.db.wal.WalCompactionTest;
import org.apache.ignite.internal.processors.cache.persistence.db.wal.WalRolloverTypesTest;
import org.apache.ignite.internal.processors.cache.persistence.db.wal.crc.IgniteDataIntegrityTests;
import org.apache.ignite.internal.processors.cache.persistence.db.wal.crc.IgniteFsyncReplayWalIteratorInvalidCrcTest;
import org.apache.ignite.internal.processors.cache.persistence.db.wal.crc.IgnitePureJavaCrcCompatibility;
import org.apache.ignite.internal.processors.cache.persistence.db.wal.crc.IgniteReplayWalIteratorInvalidCrcTest;
import org.apache.ignite.internal.processors.cache.persistence.db.wal.crc.IgniteStandaloneWalIteratorInvalidCrcTest;
import org.apache.ignite.internal.processors.cache.persistence.wal.reader.StandaloneWalRecordsIteratorTest;

/**
 *
 */
public class IgnitePdsMvccTestSuite2 extends TestSuite {
    /**
     * @return Suite.
     */
    public static TestSuite suite() {
        System.setProperty(IgniteSystemProperties.IGNITE_FORCE_MVCC_MODE_IN_TESTS, "true");

        TestSuite suite = new TestSuite("Ignite persistent Store Mvcc Test Suite 2");

        Collection<Class> ignoredTests = new HashSet<>();

        // Classes that are contained mvcc test already.
        ignoredTests.add(LocalWalModeNoChangeDuringRebalanceOnNonNodeAssignTest.class);

        // Atomic caches
        ignoredTests.add(IgnitePersistentStoreDataStructuresTest.class);

        // Skip irrelevant test
        ignoredTests.add(IgniteDataIntegrityTests.class);
        ignoredTests.add(IgniteStandaloneWalIteratorInvalidCrcTest.class);
        ignoredTests.add(IgniteReplayWalIteratorInvalidCrcTest.class);
        ignoredTests.add(IgniteFsyncReplayWalIteratorInvalidCrcTest.class);
        ignoredTests.add(IgnitePureJavaCrcCompatibility.class);
        ignoredTests.add(IgniteAbsentEvictionNodeOutOfBaselineTest.class);

        ignoredTests.add(IgnitePdsPageSizesTest.class);
        ignoredTests.add(IgniteDataStorageMetricsSelfTest.class);
        ignoredTests.add(IgniteWalFormatFileFailoverTest.class);
        ignoredTests.add(IgnitePdsExchangeDuringCheckpointTest.class);
        ignoredTests.add(IgnitePdsReserveWalSegmentsTest.class);
        ignoredTests.add(IgnitePdsReserveWalSegmentsWithCompactionTest.class);

        ignoredTests.add(IgniteUidAsConsistentIdMigrationTest.class);
        ignoredTests.add(IgniteWalSerializerVersionTest.class);
        ignoredTests.add(WalCompactionTest.class);
        ignoredTests.add(IgniteWalIteratorSwitchSegmentTest.class);
        ignoredTests.add(IgniteWalIteratorExceptionDuringReadTest.class);
        ignoredTests.add(StandaloneWalRecordsIteratorTest.class);
        ignoredTests.add(IgniteWALTailIsReachedDuringIterationOverArchiveTest.class);
        ignoredTests.add(WalRolloverTypesTest.class);
        ignoredTests.add(FsyncWalRolloverDoesNotBlockTest.class);

        suite.addTest(IgnitePdsTestSuite2.suite(ignoredTests));

        return suite;
    }
}
