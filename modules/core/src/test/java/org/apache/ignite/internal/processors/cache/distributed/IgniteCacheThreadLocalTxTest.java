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

import java.util.concurrent.ThreadLocalRandom;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.IgniteTransactions;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.internal.IgniteKernal;
import org.apache.ignite.internal.util.typedef.G;
import org.apache.ignite.lang.IgniteFuture;
import org.apache.ignite.testframework.MvccFeatureChecker;
import org.apache.ignite.testframework.junits.common.GridCommonAbstractTest;
import org.apache.ignite.transactions.Transaction;
import org.apache.ignite.transactions.TransactionConcurrency;
import org.apache.ignite.transactions.TransactionIsolation;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.apache.ignite.cache.CacheAtomicityMode.TRANSACTIONAL;

/**
 *
 */
@RunWith(JUnit4.class)
public class IgniteCacheThreadLocalTxTest extends GridCommonAbstractTest {
    /** */
    private boolean client;

    /** {@inheritDoc} */
    @Override protected IgniteConfiguration getConfiguration(String igniteInstanceName) throws Exception {
        IgniteConfiguration cfg = super.getConfiguration(igniteInstanceName);

        cfg.setClientMode(client);

        return cfg;
    }

    /** {@inheritDoc} */
    @Override protected void afterTest() throws Exception {
        stopAllGrids();

        super.afterTest();
    }

    /**
     * @throws Exception If failed.
     */
    @Test
    public void testSingleNode() throws Exception {
        threadLocalTx(startGrid(0));
    }

    /**
     * @throws Exception If failed.
     */
    @Test
    public void testMultiNode() throws Exception {
        startGridsMultiThreaded(4);

        client = true;

        startGrid(4);

        for (Ignite node : G.allGrids())
            threadLocalTx(node);
    }

    /**
     * @param node Node.
     * @throws Exception If failed.
     */
    @SuppressWarnings("unchecked")
    private void threadLocalTx(Ignite node) throws Exception {
        CacheConfiguration ccfg = new CacheConfiguration(DEFAULT_CACHE_NAME);

        ccfg.setAtomicityMode(TRANSACTIONAL);
        ccfg.setBackups(2);

        IgniteCache<Object, Object> cache = node.getOrCreateCache(ccfg);

        checkNoTx(node);

        boolean[] reads = {true, false};
        boolean[] writes = {true, false};
        int endOps = 5;

        for (TransactionConcurrency concurrency : TransactionConcurrency.values()) {
            for (TransactionIsolation isolation : TransactionIsolation.values()) {
                if (MvccFeatureChecker.forcedMvcc() && !MvccFeatureChecker.isSupported(concurrency, isolation))
                    continue;

                for (boolean read : reads) {
                    for (boolean write : writes) {
                        for (int i = 0; i < endOps; i++)
                            checkTx(concurrency, isolation, node, cache, read, write, i);
                    }
                }
            }
        }

        checkNoTx(node);

        cache.put(1, 1);

        checkNoTx(node);
    }

    /**
     * @param concurrency Tx concurrency.
     * @param isolation Tx isolation.
     * @param node Node.
     * @param cache Cache.
     * @param read {@code True} if read in tx.
     * @param write {@code True} if write in tx.
     * @param endOp Operation to test.
     */
    private void checkTx(TransactionConcurrency concurrency,
        TransactionIsolation isolation,
        Ignite node,
        IgniteCache<Object, Object> cache,
        boolean read,
        boolean write,
        int endOp) {
        IgniteTransactions txs = node.transactions();

        checkNoTx(node);

        Transaction tx = txs.txStart(concurrency, isolation);

        assertEquals(tx, txs.tx());

        try {
            txs.txStart(concurrency, isolation);

            fail();
        }
        catch (IllegalStateException expected) {
            // No-op.
        }

        if (read)
            cache.get(ThreadLocalRandom.current().nextInt(100_000));

        if (write)
            cache.put(ThreadLocalRandom.current().nextInt(100_000), 1);


        try {
            txs.txStart(concurrency, isolation);

            fail();
        }
        catch (IllegalStateException expected) {
            // No-op.
        }

        assertEquals(tx, txs.tx());

        IgniteFuture fut = null;

        switch (endOp) {
            case 0:
                tx.commit();

                break;

            case 1:
                fut = tx.commitAsync();

                break;

            case 2:
                tx.rollback();

                break;

            case 3:
                fut = tx.rollbackAsync();

                break;

            case 4:
                tx.close();

                break;

            default:
                fail();
        }

        if (fut != null)
            fut.get();

        checkNoTx(node);
    }

    /**
     * @param node Node.
     */
    private void checkNoTx(Ignite node) {
        IgniteTransactions txs = node.transactions();

        assertNull(txs.tx());
        assertNull(((IgniteKernal)node).context().cache().context().tm().tx());
    }
}
