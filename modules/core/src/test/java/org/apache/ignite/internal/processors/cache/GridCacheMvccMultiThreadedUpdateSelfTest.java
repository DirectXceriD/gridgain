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

package org.apache.ignite.internal.processors.cache;

import java.util.concurrent.Callable;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.IgniteTransactions;
import org.apache.ignite.cache.CacheAtomicityMode;
import org.apache.ignite.testframework.GridTestUtils;
import org.apache.ignite.testframework.MvccFeatureChecker;
import org.apache.ignite.transactions.Transaction;
import org.junit.Test;

import static org.apache.ignite.transactions.TransactionConcurrency.PESSIMISTIC;
import static org.apache.ignite.transactions.TransactionIsolation.REPEATABLE_READ;

/**
 * Multithreaded update test.
 */
public class GridCacheMvccMultiThreadedUpdateSelfTest extends GridCacheOffHeapMultiThreadedUpdateAbstractSelfTest {
    /** */
    public static final int THREADS = 5;

    /** {@inheritDoc} */
    @Override protected long getTestTimeout() {
        return 5 * 60_000;
    }

    /** {@inheritDoc} */
    @Override protected CacheAtomicityMode atomicityMode() {
        return CacheAtomicityMode.TRANSACTIONAL_SNAPSHOT;
    }

    /** {@inheritDoc} */
    @Override protected void beforeTestsStarted() throws Exception {
        if (MvccFeatureChecker.forcedMvcc())
            fail("https://issues.apache.org/jira/browse/IGNITE-9470");

        super.beforeTestsStarted();
    }

    /**
     * @throws Exception If failed.
     */
    @Test
    public void testTransformTx() throws Exception {
        testTransformTx(keyForNode(0));

        if (gridCount() > 1)
            testTransformTx(keyForNode(1));
    }

    /**
     * @param key Key.
     * @throws Exception If failed.
     */
    private void testTransformTx(final Integer key) throws Exception {
        final IgniteCache<Integer, Integer> cache = grid(0).cache(DEFAULT_CACHE_NAME);

        cache.put(key, 0);

        final int ITERATIONS_PER_THREAD = iterations();

        GridTestUtils.runMultiThreaded(new Callable<Void>() {
            @Override public Void call() throws Exception {
                IgniteTransactions txs = ignite(0).transactions();

                for (int i = 0; i < ITERATIONS_PER_THREAD && !failed; i++) {
                    if (i % 500 == 0)
                        log.info("Iteration " + i);

                    try (Transaction tx = txs.txStart(PESSIMISTIC, REPEATABLE_READ)) {
                        cache.invoke(key, new IncProcessor());

                        tx.commit();
                    }
                }

                return null;
            }
        }, THREADS, "transform");

        for (int i = 0; i < gridCount(); i++) {
            Integer val = (Integer)grid(i).cache(DEFAULT_CACHE_NAME).get(key);

            assertEquals("Unexpected value for grid " + i, (Integer)(ITERATIONS_PER_THREAD * THREADS), val);
        }

        if (failed) {
            for (int g = 0; g < gridCount(); g++)
                info("Value for cache [g=" + g + ", val=" + grid(g).cache(DEFAULT_CACHE_NAME).get(key) + ']');

            assertFalse(failed);
        }
    }

    /**
     * @throws Exception If failed.
     */
    @Test
    public void testPutTxPessimistic() throws Exception {
        testPutTx(keyForNode(0));

        if (gridCount() > 1)
            testPutTx(keyForNode(1));
    }

    /**
     * @param key Key.
     * @throws Exception If failed.
     */
    private void testPutTx(final Integer key) throws Exception {
        final IgniteCache<Integer, Integer> cache = grid(0).cache(DEFAULT_CACHE_NAME);

        cache.put(key, 0);

        final int ITERATIONS_PER_THREAD = iterations();

        GridTestUtils.runMultiThreaded(new Callable<Void>() {
            @Override public Void call() throws Exception {
                for (int i = 0; i < ITERATIONS_PER_THREAD; i++) {
                    if (i % 500 == 0)
                        log.info("Iteration " + i);

                    try (Transaction tx = grid(0).transactions().txStart(PESSIMISTIC, REPEATABLE_READ)) {
                        Integer val = cache.getAndPut(key, i);

                        assertNotNull(val);

                        tx.commit();
                    }
                }

                return null;
            }
        }, THREADS, "put");

        for (int i = 0; i < gridCount(); i++) {
            Integer val = (Integer)grid(i).cache(DEFAULT_CACHE_NAME).get(key);

            assertNotNull("Unexpected value for grid " + i, val);
        }
    }

    /**
     * @throws Exception If failed.
     */
    @Test
    public void testPutxIfAbsentTxPessimistic() throws Exception {
        testPutxIfAbsentTx(keyForNode(0));

        if (gridCount() > 1)
            testPutxIfAbsentTx(keyForNode(1));
    }

    /**
     * @param key Key.
     * @throws Exception If failed.
     */
    private void testPutxIfAbsentTx(final Integer key) throws Exception {
        final IgniteCache<Integer, Integer> cache = grid(0).cache(DEFAULT_CACHE_NAME);

        cache.put(key, 0);

        final int ITERATIONS_PER_THREAD = iterations();

        GridTestUtils.runMultiThreaded(new Callable<Void>() {
            @Override public Void call() throws Exception {
                for (int i = 0; i < ITERATIONS_PER_THREAD && !failed; i++) {
                    if (i % 500 == 0)
                        log.info("Iteration " + i);

                    try (Transaction tx = grid(0).transactions().txStart(PESSIMISTIC, REPEATABLE_READ)) {
                        cache.putIfAbsent(key, 100);

                        tx.commit();
                    }
                }

                return null;
            }
        }, THREADS, "putxIfAbsent");

        for (int i = 0; i < gridCount(); i++) {
            Integer val = (Integer)grid(i).cache(DEFAULT_CACHE_NAME).get(key);

            assertEquals("Unexpected value for grid " + i, (Integer)0, val);
        }

        assertFalse(failed);
    }
}
