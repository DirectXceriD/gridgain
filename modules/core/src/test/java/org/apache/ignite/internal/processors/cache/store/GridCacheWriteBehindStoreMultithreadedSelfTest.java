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

package org.apache.ignite.internal.processors.cache.store;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.apache.ignite.internal.IgniteInterruptedCheckedException;
import org.apache.ignite.internal.util.typedef.internal.U;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Multithreaded tests for {@link org.apache.ignite.internal.processors.cache.store.GridCacheWriteBehindStore}.
 */
@RunWith(JUnit4.class)
public class GridCacheWriteBehindStoreMultithreadedSelfTest extends GridCacheWriteBehindStoreAbstractSelfTest {
    /**
     * This test performs complex set of operations on store with coalescing from multiple threads.
     *
     * @throws Exception If failed.
     */
    @Test
    public void testPutGetRemoveWithCoalescing() throws Exception {
        testPutGetRemove(true);
    }

    /**
     * This test performs complex set of operations on store without coalescing from multiple threads.
     *
     * @throws Exception If failed.
     */
    @Test
    public void testPutGetRemoveWithoutCoalescing() throws Exception {
        testPutGetRemove(false);
    }

    /**
     * This test performs complex set of operations on store from multiple threads.
     *
     * @throws Exception If failed.
     */
    private void testPutGetRemove(boolean writeCoalescing) throws Exception {
        initStore(2, writeCoalescing);

        Set<Integer> exp;

        try {
            exp = runPutGetRemoveMultithreaded(10, 10);
        }
        finally {
            shutdownStore();
        }

        Map<Integer, String> map = delegate.getMap();

        Collection<Integer> extra = new HashSet<>(map.keySet());

        extra.removeAll(exp);

        assertTrue("The underlying store contains extra keys: " + extra, extra.isEmpty());

        Collection<Integer> missing = new HashSet<>(exp);

        missing.removeAll(map.keySet());

        assertTrue("Missing keys in the underlying store: " + missing, missing.isEmpty());

        for (Integer key : exp)
            assertEquals("Invalid value for key " + key, "val" + key, map.get(key));
    }

    /**
     * Tests that cache with write coalescing would keep values if underlying store fails.
     *
     * @throws Exception if failed.
     */
    @Test
    public void testStoreFailureWithCoalescing() throws Exception {
        testStoreFailure(true);
    }

    /**
     * Tests that cache without write coalescing would keep values if underlying store fails.
     *
     * @throws Exception if failed.
     */
    @Test
    public void testStoreFailureWithoutCoalescing() throws Exception {
        testStoreFailure(false);
    }

    /**
     * Tests that cache would keep values if underlying store fails.
     *
     * @throws Exception If failed.
     */
    private void testStoreFailure(boolean writeCoalescing) throws Exception {
        delegate.setShouldFail(true);

        initStore(2, writeCoalescing);

        Set<Integer> exp;

        try {
            Thread timer = new Thread(new Runnable() {
                @Override public void run() {
                    try {
                        U.sleep(FLUSH_FREQUENCY+50);
                    } catch (IgniteInterruptedCheckedException e) {
                        assertTrue("Timer was interrupted", false);
                    }
                    delegate.setShouldFail(false);
                }
            });
            timer.start();
            exp = runPutGetRemoveMultithreaded(10, 10);

            timer.join();

            info(">>> There are " + store.getWriteBehindErrorRetryCount() + " entries in RETRY state");

            // Despite that we set shouldFail flag to false, flush thread may just have caught an exception.
            // If we move store to the stopping state right away, this value will be lost. That's why this sleep
            // is inserted here to let all exception handlers in write-behind store exit.
            U.sleep(1000);
        }
        finally {
            shutdownStore();
        }

        Map<Integer, String> map = delegate.getMap();

        Collection<Integer> extra = new HashSet<>(map.keySet());

        extra.removeAll(exp);

        assertTrue("The underlying store contains extra keys: " + extra, extra.isEmpty());

        Collection<Integer> missing = new HashSet<>(exp);

        missing.removeAll(map.keySet());

        assertTrue("Missing keys in the underlying store: " + missing, missing.isEmpty());

        for (Integer key : exp)
            assertEquals("Invalid value for key " + key, "val" + key, map.get(key));
    }

    /**
     * Tests store (with write coalescing) consistency in case of high put rate,
     * when flush is performed from the same thread as put or remove operation.
     *
     * @throws Exception If failed.
     */
    @Test
    public void testFlushFromTheSameThreadWithCoalescing() throws Exception {
        testFlushFromTheSameThread(true);
    }

    /**
     * Tests store (without write coalescing) consistency in case of high put rate,
     * when flush is performed from the same thread as put or remove operation.
     *
     * @throws Exception If failed.
     */
    @Test
    public void testFlushFromTheSameThreadWithoutCoalescing() throws Exception {
        testFlushFromTheSameThread(false);
    }

    /**
     * Tests store consistency in case of high put rate, when flush is performed from the same thread
     * as put or remove operation.
     *
     * @param writeCoalescing write coalescing flag.
     * @throws Exception If failed.
     */
    private void testFlushFromTheSameThread(boolean writeCoalescing) throws Exception {
        // 50 milliseconds should be enough.
        delegate.setOperationDelay(50);

        initStore(2, writeCoalescing);

        Set<Integer> exp;

        int start = store.getWriteBehindTotalCriticalOverflowCount();

        try {
            //We will have in total 5 * CACHE_SIZE keys that should be enough to grow map size to critical value.
            exp = runPutGetRemoveMultithreaded(5, CACHE_SIZE);
        }
        finally {
            log.info(">>> Done inserting, shutting down the store");

            shutdownStore();
        }

        // Restore delay.
        delegate.setOperationDelay(0);

        Map<Integer, String> map = delegate.getMap();

        int end = store.getWriteBehindTotalCriticalOverflowCount();

        log.info(">>> There are " + exp.size() + " keys in store, " + (end - start) + " overflows detected");

        assertTrue("No cache overflows detected (a bug or too few keys or too few delay?)", end > start);

        Collection<Integer> extra = new HashSet<>(map.keySet());

        extra.removeAll(exp);

        assertTrue("The underlying store contains extra keys: " + extra, extra.isEmpty());

        Collection<Integer> missing = new HashSet<>(exp);

        missing.removeAll(map.keySet());

        assertTrue("Missing keys in the underlying store: " + missing, missing.isEmpty());

        for (Integer key : exp)
            assertEquals("Invalid value for key " + key, "val" + key, map.get(key));
    }
}
