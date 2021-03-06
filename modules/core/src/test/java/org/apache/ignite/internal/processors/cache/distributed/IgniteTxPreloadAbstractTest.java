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

package org.apache.ignite.internal.processors.cache.distributed;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import javax.cache.processor.EntryProcessor;
import javax.cache.processor.EntryProcessorResult;
import javax.cache.processor.MutableEntry;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.IgniteTransactions;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.internal.IgniteInternalFuture;
import org.apache.ignite.internal.processors.cache.GridCacheAbstractSelfTest;
import org.apache.ignite.testframework.GridTestUtils;
import org.apache.ignite.testframework.MvccFeatureChecker;
import org.apache.ignite.transactions.Transaction;
import org.apache.ignite.transactions.TransactionConcurrency;
import org.apache.ignite.transactions.TransactionIsolation;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.apache.ignite.cache.CacheRebalanceMode.ASYNC;
import static org.apache.ignite.cache.CacheWriteSynchronizationMode.FULL_SYNC;
import static org.apache.ignite.transactions.TransactionConcurrency.OPTIMISTIC;
import static org.apache.ignite.transactions.TransactionConcurrency.PESSIMISTIC;

/**
 * Tests transaction during cache preloading.
 */
@RunWith(JUnit4.class)
public abstract class IgniteTxPreloadAbstractTest extends GridCacheAbstractSelfTest {
    /** */
    private static final int GRID_CNT = 6;

    /** */
    private static volatile boolean keyNotLoaded;

    /** {@inheritDoc} */
    @Override protected void beforeTest() throws Exception {
        keyNotLoaded = false;

        startGrid(0);
    }

    /** {@inheritDoc} */
    @Override protected void afterTest() throws Exception {
        stopAllGrids();
    }

    /** {@inheritDoc} */
    @Override protected void beforeTestsStarted() throws Exception {
        // No-op.
    }

    /** {@inheritDoc} */
    @Override protected int gridCount() {
        return GRID_CNT;
    }

    /**
     * @throws Exception If failed.
     */
    @Test
    public void testRemoteTxPreloading() throws Exception {
        IgniteCache<String, Integer> cache = jcache(0);

        for (int i = 0; i < 10_000; i++)
            cache.put(String.valueOf(i), 0);

        final AtomicInteger gridIdx = new AtomicInteger(1);

        IgniteInternalFuture<?> fut = GridTestUtils.runMultiThreadedAsync(
            new Callable<Object>() {
                @Nullable @Override public Object call() throws Exception {
                    int idx = gridIdx.getAndIncrement();

                    startGrid(idx);

                    return null;
                }
            },
            GRID_CNT - 1,
            "grid-starter-" + getName()
        );

        waitForRemoteNodes(grid(0), 2);

        Set<String> keys = new HashSet<>();

        for (int i = 0; i < 10; i++)
            keys.add(String.valueOf(i * 1000));

        Map<String, EntryProcessorResult<Integer>> resMap = cache.invokeAll(keys,
            new EntryProcessor<String, Integer, Integer>() {
                @Override public Integer process(MutableEntry<String, Integer> e, Object... args) {
                    Integer val = e.getValue();

                    if (val == null) {
                        keyNotLoaded = true;

                        e.setValue(1);

                        return null;
                    }

                    e.setValue(val + 1);

                    return val;
                }
            }
        );

        assertFalse(keyNotLoaded);

        for (String key : keys) {
            EntryProcessorResult<Integer> res = resMap.get(key);

            assertNotNull(res);
            assertEquals(0, (Object)res.get());
        }

        fut.get();

        for (int i = 0; i < GRID_CNT; i++) {
            for (String key : keys)
                assertEquals("Unexpected value for cache " + i, (Integer)1, jcache(i).get(key));
        }
    }

    /**
     * @throws Exception If failed.
     */
    @Test
    public void testLocalTxPreloadingOptimistic() throws Exception {
        if (!MvccFeatureChecker.forcedMvcc()) // Do not check optimistic tx for mvcc.
            testLocalTxPreloading(OPTIMISTIC);
    }

    /**
     * @throws Exception If failed.
     */
    @Test
    public void testLocalTxPreloadingPessimistic() throws Exception {
        testLocalTxPreloading(PESSIMISTIC);
    }

    /**
     * Tries to execute transaction doing transform when target key is not yet preloaded.
     *
     * @param txConcurrency Transaction concurrency;
     * @throws Exception If failed.
     */
    private void testLocalTxPreloading(TransactionConcurrency txConcurrency) throws Exception {
        Map<String, Integer> map = new HashMap<>();

        for (int i = 0; i < 10000; i++)
            map.put(String.valueOf(i), 0);

        IgniteCache<String, Integer> cache0 = jcache(0);

        cache0.putAll(map);

        final String TX_KEY = "9000";

        int expVal = 0;

        for (int i = 1; i < GRID_CNT; i++) {
            assertEquals((Integer)expVal, cache0.get(TX_KEY));

            startGrid(i);

            IgniteCache<String, Integer> cache = jcache(i);

            IgniteTransactions txs = ignite(i).transactions();

            try (Transaction tx = txs.txStart(txConcurrency, TransactionIsolation.REPEATABLE_READ)) {
                cache.invoke(TX_KEY, new EntryProcessor<String, Integer, Void>() {
                    @Override public Void process(MutableEntry<String, Integer> e, Object... args) {
                        Integer val = e.getValue();

                        if (val == null) {
                            keyNotLoaded = true;

                            e.setValue(1);

                            return null;
                        }

                        e.setValue(val + 1);

                        return null;
                    }
                });

                tx.commit();
            }

            assertFalse(keyNotLoaded);

            expVal++;

            assertEquals((Integer)expVal, cache.get(TX_KEY));
        }

        for (int i = 0; i < GRID_CNT; i++)
            assertEquals("Unexpected value for cache " + i, (Integer)expVal, jcache(i).get(TX_KEY));
    }

    /** {@inheritDoc} */
    @Override protected CacheConfiguration cacheConfiguration(String igniteInstanceName) throws Exception {
        CacheConfiguration cfg = super.cacheConfiguration(igniteInstanceName);

        cfg.setRebalanceMode(ASYNC);
        cfg.setWriteSynchronizationMode(FULL_SYNC);
        cfg.setCacheStoreFactory(null);
        cfg.setReadThrough(false);
        cfg.setWriteThrough(false);

        return cfg;
    }
}
