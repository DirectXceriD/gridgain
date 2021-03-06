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
import org.apache.ignite.internal.GridFactoryVmShutdownTest;
import org.apache.ignite.internal.managers.GridManagerMxBeanIllegalArgumentHandleTest;
import org.apache.ignite.internal.processors.cache.datastructures.GridCacheMultiNodeDataStructureTest;
import org.apache.ignite.internal.processors.cache.distributed.replicated.preloader.GridCacheReplicatedPreloadUndeploysTest;
import org.apache.ignite.internal.processors.cache.persistence.file.FileDownloaderTest;
import org.apache.ignite.internal.processors.cache.persistence.pagemem.PagesWriteThrottleSandboxTest;
import org.apache.ignite.internal.processors.compute.GridComputeJobExecutionErrorToLogManualTest;
import org.apache.ignite.internal.util.future.GridFutureQueueTest;
import org.apache.ignite.internal.util.nio.GridRoundTripTest;
import org.apache.ignite.jvmtest.BlockingQueueTest;
import org.apache.ignite.jvmtest.FileIOTest;
import org.apache.ignite.jvmtest.FileLocksTest;
import org.apache.ignite.jvmtest.LinkedHashMapTest;
import org.apache.ignite.jvmtest.MultipleFileIOTest;
import org.apache.ignite.jvmtest.NetworkFailureTest;
import org.apache.ignite.jvmtest.QueueSizeCounterMultiThreadedTest;
import org.apache.ignite.jvmtest.ReadWriteLockMultiThreadedTest;
import org.apache.ignite.jvmtest.RegExpTest;
import org.apache.ignite.jvmtest.ServerSocketMultiThreadedTest;
import org.apache.ignite.lang.GridSystemCurrentTimeMillisTest;
import org.apache.ignite.lang.GridThreadPriorityTest;
import org.apache.ignite.startup.servlet.GridServletLoaderTest;
import org.junit.runner.RunWith;
import org.junit.runners.AllTests;

/**
 * Tests suite for orphaned tests.
 */
@RunWith(AllTests.class)
public class IgniteLostAndFoundTestSuite {
    /**
     * @return Tests suite for orphaned tests (not in any test sute previously).
     */
    public static TestSuite suite() {
        TestSuite suite = new TestSuite("Ignite List And Found Test Suite");

        suite.addTest(new JUnit4TestAdapter(FileIOTest.class));
        suite.addTest(new JUnit4TestAdapter(FileLocksTest.class));
        suite.addTest(new JUnit4TestAdapter(GridComputeJobExecutionErrorToLogManualTest.class));
        suite.addTest(new JUnit4TestAdapter(GridManagerMxBeanIllegalArgumentHandleTest.class));
        suite.addTest(new JUnit4TestAdapter(GridRoundTripTest.class));
        suite.addTest(new JUnit4TestAdapter(GridServletLoaderTest.class));

        suite.addTest(new JUnit4TestAdapter(LinkedHashMapTest.class));
        suite.addTest(new JUnit4TestAdapter(NetworkFailureTest.class));
        suite.addTest(new JUnit4TestAdapter(PagesWriteThrottleSandboxTest.class));
        suite.addTest(new JUnit4TestAdapter(QueueSizeCounterMultiThreadedTest.class));
        suite.addTest(new JUnit4TestAdapter(ReadWriteLockMultiThreadedTest.class));
        suite.addTest(new JUnit4TestAdapter(RegExpTest.class));
        suite.addTest(new JUnit4TestAdapter(ServerSocketMultiThreadedTest.class));


        // Non-JUnit classes with Test in name, which should be either converted to JUnit or removed in the future
        // Main classes:
        Class[] _$ = new Class[] {
            GridCacheReplicatedPreloadUndeploysTest.class,
            GridCacheMultiNodeDataStructureTest.class,
            GridFactoryVmShutdownTest.class,
            GridFutureQueueTest.class,
            GridThreadPriorityTest.class,
            GridSystemCurrentTimeMillisTest.class,
            BlockingQueueTest.class,
            MultipleFileIOTest.class
        };

        return suite;
    }
}
