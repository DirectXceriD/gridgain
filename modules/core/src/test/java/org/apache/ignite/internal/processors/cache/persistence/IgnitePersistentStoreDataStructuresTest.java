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

package org.apache.ignite.internal.processors.cache.persistence;

import java.util.concurrent.TimeUnit;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteAtomicLong;
import org.apache.ignite.IgniteAtomicSequence;
import org.apache.ignite.IgniteCountDownLatch;
import org.apache.ignite.IgniteException;
import org.apache.ignite.IgniteLock;
import org.apache.ignite.IgniteQueue;
import org.apache.ignite.IgniteSemaphore;
import org.apache.ignite.IgniteSet;
import org.apache.ignite.configuration.CollectionConfiguration;
import org.apache.ignite.configuration.DataRegionConfiguration;
import org.apache.ignite.configuration.DataStorageConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.configuration.WALMode;
import org.apache.ignite.internal.IgniteFutureTimeoutCheckedException;
import org.apache.ignite.internal.IgniteInternalFuture;
import org.apache.ignite.testframework.GridTestUtils;
import org.apache.ignite.testframework.junits.common.GridCommonAbstractTest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 *
 */
@RunWith(JUnit4.class)
public class IgnitePersistentStoreDataStructuresTest extends GridCommonAbstractTest {
    /** */
    private static volatile boolean autoActivationEnabled = false;

    /** {@inheritDoc} */
    @Override protected IgniteConfiguration getConfiguration(String igniteInstanceName) throws Exception {
        IgniteConfiguration cfg = super.getConfiguration(igniteInstanceName);

        DataStorageConfiguration memCfg = new DataStorageConfiguration()
            .setDefaultDataRegionConfiguration(
                new DataRegionConfiguration().setMaxSize(200 * 1024 * 1024).setPersistenceEnabled(true))
            .setWalMode(WALMode.LOG_ONLY);

        cfg.setDataStorageConfiguration(memCfg);

        cfg.setAutoActivationEnabled(autoActivationEnabled);

        return cfg;
    }

    /** {@inheritDoc} */
    @Override protected void afterTestsStopped() throws Exception {
        cleanPersistenceDir();
    }

    /** {@inheritDoc} */
    @Override protected void beforeTest() throws Exception {
        super.beforeTest();

        cleanPersistenceDir();

        autoActivationEnabled = false;
    }

    /** {@inheritDoc} */
    @Override protected void afterTest() throws Exception {
        super.afterTest();

        stopAllGrids();
    }

    /**
     * @throws Exception If failed.
     */
    @Test
    public void testQueue() throws Exception {
        Ignite ignite = startGrids(4);

        ignite.active(true);

        IgniteQueue<Object> queue = ignite.queue("testQueue", 100, new CollectionConfiguration());

        for (int i = 0; i < 100; i++)
            queue.offer(i);

        stopAllGrids();

        ignite = startGrids(4);

        ignite.active(true);

        queue = ignite.queue("testQueue", 0, null);

        for (int i = 0; i < 100; i++)
            assertEquals(i, queue.poll());
    }

    /**
     * @throws Exception If failed.
     */
    @Test
    public void testAtomic() throws Exception {
        Ignite ignite = startGrids(4);

        ignite.active(true);

        IgniteAtomicLong atomicLong = ignite.atomicLong("testLong", 0, true);

        for (int i = 0; i < 100; i++)
            atomicLong.incrementAndGet();

        stopAllGrids();

        ignite = startGrids(4);

        ignite.active(true);

        atomicLong = ignite.atomicLong("testLong", 0, false);

        for (int i = 100; i != 0; )
            assertEquals(i--, atomicLong.getAndDecrement());
    }

    /**
     * @throws Exception If failed.
     */
    @Test
    public void testSequence() throws Exception {
        Ignite ignite = startGrids(4);

        ignite.active(true);

        IgniteAtomicSequence sequence = ignite.atomicSequence("testSequence", 0, true);

        int i = 0;

        while (i < 1000) {
            sequence.incrementAndGet();

            i++;
        }

        stopAllGrids();

        ignite = startGrids(4);

        ignite.active(true);

        sequence = ignite.atomicSequence("testSequence", 0, false);

        assertTrue(sequence.incrementAndGet() > i);
    }

    /**
     * @throws Exception If failed.
     */
    @Test
    public void testSequenceAfterAutoactivation() throws Exception {
        final String seqName = "testSequence";

        autoActivationEnabled = true;

        Ignite ignite = startGrids(2);

        ignite.cluster().active(true);

        ignite.atomicSequence(seqName, 0, true);

        stopAllGrids(true);

        final Ignite node = startGrids(2);

        IgniteInternalFuture fut = GridTestUtils.runAsync(() -> {
            while (true) {
                try {
                    // Should not hang.
                    node.atomicSequence(seqName, 0, false);

                    break;
                }
                catch (IgniteException e) {
                    // Can fail on not yet activated cluster. Retry until success.
                    assertTrue(e.getMessage()
                        .contains("Can not perform the operation because the cluster is inactive"));
                }
            }
        });

        try {
            fut.get(10, TimeUnit.SECONDS);
        }
        catch (IgniteFutureTimeoutCheckedException e) {
            fut.cancel();

            fail("Ignite was stuck on getting the atomic sequence after autoactivation.");
        }
    }

    /**
     * @throws Exception If failed.
     */
    @Test
    public void testSet() throws Exception {
        Ignite ignite = startGrids(4);

        ignite.active(true);

        IgniteSet<Object> set = ignite.set("testSet", new CollectionConfiguration());

        for (int i = 0; i < 100; i++)
            set.add(i);

        assertEquals(100, set.size());

        stopAllGrids();

        ignite = startGrids(4);

        ignite.active(true);

        set = ignite.set("testSet", null);

        assertFalse(set.add(99));

        for (int i = 0; i < 100; i++)
            assertTrue(set.contains(i));

        assertEquals(100, set.size());
    }

    /**
     * @throws Exception If failed.
     */
    @Test
    public void testLockVolatility() throws Exception {
        Ignite ignite = startGrids(4);

        ignite.active(true);

        IgniteLock lock = ignite.reentrantLock("test", false, true, true);

        assert lock != null;

        stopAllGrids();

        ignite = startGrids(4);

        ignite.active(true);

        lock = ignite.reentrantLock("test", false, true, false);

        assert lock == null;
    }

    /**
     * @throws Exception If failed.
     */
    @Test
    public void testSemaphoreVolatility() throws Exception {
        Ignite ignite = startGrids(4);

        ignite.active(true);

        IgniteSemaphore sem = ignite.semaphore("test", 10, false, true);

        assert sem != null;

        stopAllGrids();

        ignite = startGrids(4);

        ignite.active(true);

        sem = ignite.semaphore("test", 10, false, false);

        assert sem == null;
    }

    /**
     * @throws Exception If failed.
     */
    @Test
    public void testLatchVolatility() throws Exception {
        Ignite ignite = startGrids(4);

        ignite.active(true);

        IgniteCountDownLatch latch = ignite.countDownLatch("test", 10, false, true);

        assert latch != null;

        stopAllGrids();

        ignite = startGrids(4);

        ignite.active(true);

        latch = ignite.countDownLatch("test", 10, false, false);

        assert latch == null;
    }

}
