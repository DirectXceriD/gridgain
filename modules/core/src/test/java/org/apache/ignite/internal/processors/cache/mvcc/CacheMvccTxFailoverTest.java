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

package org.apache.ignite.internal.processors.cache.mvcc;

import java.util.concurrent.CyclicBarrier;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.IgniteTransactions;
import org.apache.ignite.cache.CacheAtomicityMode;
import org.apache.ignite.cache.CacheWriteSynchronizationMode;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.DataRegionConfiguration;
import org.apache.ignite.configuration.DataStorageConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.configuration.WALMode;
import org.apache.ignite.internal.IgniteEx;
import org.apache.ignite.internal.pagemem.wal.IgniteWriteAheadLogManager;
import org.apache.ignite.internal.processors.cache.WalStateManager;
import org.apache.ignite.internal.processors.cache.persistence.GridCacheDatabaseSharedManager;
import org.apache.ignite.internal.processors.cache.persistence.wal.FileWriteAheadLogManager;
import org.apache.ignite.internal.processors.timeout.GridTimeoutProcessor;
import org.apache.ignite.testframework.GridTestUtils;
import org.apache.ignite.testframework.junits.common.GridCommonAbstractTest;
import org.apache.ignite.transactions.Transaction;
import org.apache.ignite.transactions.TransactionConcurrency;
import org.apache.ignite.transactions.TransactionIsolation;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Check Tx state recovery from WAL.
 */
@RunWith(JUnit4.class)
public class CacheMvccTxFailoverTest extends GridCommonAbstractTest {
    /** {@inheritDoc} */
    @Override protected void beforeTest() throws Exception {
        cleanPersistenceDir();
    }

    /** {@inheritDoc} */
    @Override protected void afterTest() throws Exception {
        stopAllGrids();

        super.afterTest();
    }

    /** {@inheritDoc} */
    @Override protected IgniteConfiguration getConfiguration(String igniteInstanceName) throws Exception {
        return super.getConfiguration(igniteInstanceName)
            .setDataStorageConfiguration(new DataStorageConfiguration()
                .setDefaultDataRegionConfiguration(new DataRegionConfiguration()
                    .setMaxSize(100_000_000L)
                    .setPersistenceEnabled(true))
                .setWalMode(WALMode.BACKGROUND)
            )
            .setMvccVacuumFrequency(Long.MAX_VALUE)
            .setCacheConfiguration(cacheConfiguration());
    }

    /**
     * @return Cache configuration.
     */
    @SuppressWarnings("unchecked")
    protected CacheConfiguration<Integer, Integer> cacheConfiguration() {
        return defaultCacheConfiguration()
            .setNearConfiguration(null)
            .setAtomicityMode(CacheAtomicityMode.TRANSACTIONAL_SNAPSHOT)
            .setWriteSynchronizationMode(CacheWriteSynchronizationMode.FULL_SYNC);
    }

    /**
     * @throws Exception If fails.
     */
    @Test
    public void testSingleNodeTxMissedRollback() throws Exception {
        checkSingleNodeRestart(true, false, true);
    }

    /**
     * @throws Exception If fails.
     */
    @Test
    public void testSingleNodeTxMissedRollbackRecoverFromWAL() throws Exception {
        checkSingleNodeRestart(true, true, true);
    }

    /**
     * @throws Exception If fails.
     */
    @Test
    public void testSingleNodeTxMissedCommit() throws Exception {
        checkSingleNodeRestart(false, false, true);
    }

    /**
     * @throws Exception If fails.
     */
    @Test
    public void testSingleNodeTxMissedCommitRecoverFromWAL() throws Exception {
        checkSingleNodeRestart(false, true, true);
    }

    /**
     * @throws Exception If fails.
     */
    @Test
    public void testSingleNodeRollbackedTxRecoverFromWAL() throws Exception {
        checkSingleNodeRestart(true, true, false);
    }

    /**
     * @throws Exception If fails.
     */
    @Test
    public void testSingleNodeCommitedTxRecoverFromWAL() throws Exception {
        checkSingleNodeRestart(false, true, false);
    }


    /**
     * @param rollBack If {@code True} then Tx will be rolled backup, committed otherwise.
     * @param recoverFromWAL If {@code True} then Tx recovery from WAL will be checked,
     *                       binary recovery from latest checkpoint otherwise.
     * @param omitTxFinish If {@code True} then unfinished Tx state will be restored as if node fails during commit.
     * @throws Exception If fails.
     */
    public void checkSingleNodeRestart(boolean rollBack, boolean recoverFromWAL, boolean omitTxFinish) throws Exception {
        IgniteEx node = startGrid(0);

        node.cluster().active(true);

        IgniteCache<Integer, Integer> cache = node.getOrCreateCache(DEFAULT_CACHE_NAME);

        cache.put(1, 1);
        cache.put(2, 1);

        IgniteTransactions txs = node.transactions();

        IgniteWriteAheadLogManager wal = node.context().cache().context().wal();

        if (recoverFromWAL){
            //Force checkpoint. See for details: https://issues.apache.org/jira/browse/IGNITE-10187
            node.context().cache().context().database().waitForCheckpoint(null);

            ((GridCacheDatabaseSharedManager)node.context().cache().context().database()).enableCheckpoints(false).get();
        }

        GridTimeoutProcessor.CancelableTask flushTask = GridTestUtils.getFieldValue(wal, FileWriteAheadLogManager.class, "backgroundFlushSchedule");
        WalStateManager.WALDisableContext wctx = GridTestUtils.getFieldValue(wal, FileWriteAheadLogManager.class, "walDisableContext");

        // Disable checkpoint and WAL flusher.
        node.context().timeout().removeTimeoutObject(flushTask);

        try (Transaction tx = txs.txStart(TransactionConcurrency.PESSIMISTIC, TransactionIsolation.REPEATABLE_READ)) {
            assertEquals((Integer)1, cache.get(1));
            cache.put(2, 2);

            flushTask.onTimeout(); // Flush WAL.

            if (!recoverFromWAL){
                //Force checkpoint, then disable.
                node.context().cache().context().database().waitForCheckpoint(null);

                ((GridCacheDatabaseSharedManager)node.context().cache().context().database()).enableCheckpoints(false).get();
            }

            if (omitTxFinish)
                GridTestUtils.setFieldValue(wctx, "disableWal", true); // Disable wal.

            if (rollBack)
                tx.rollback();
            else
                tx.commit();
        }

        stopGrid(0);

        node = startGrid(0);

        node.cluster().active(true);

        cache = node.cache(DEFAULT_CACHE_NAME);

        assertEquals((Integer)1, cache.get(1));

        if (omitTxFinish || rollBack)
            assertEquals((Integer) 1, cache.get(2)); // Commit\rollback marker were saved neither in WAL nor in checkpoint.
        else
            assertEquals((Integer) 2, cache.get(2));

        cache.put(2, 3);

        assertEquals((Integer)3, cache.get(2));
    }


    /**
     * @throws Exception If fails.
     */
    @Ignore("https://issues.apache.org/jira/browse/IGNITE-10219")
    @Test
    public void testLostRollbackOnBackup() throws Exception {
        IgniteEx node = startGrid(0);

        startGrid(1);

        node.cluster().active(true);

        final CyclicBarrier barrier = new CyclicBarrier(2);

        GridTestUtils.runAsync(new Runnable() {
            @Override public void run() {
                try {
                    barrier.await();

                    stopGrid(1);

                    barrier.await();

                    startGrid(1);

                    barrier.await();
                }
                catch (Exception e) {
                    e.printStackTrace();
                    barrier.reset();
                }
            }
        });

        IgniteCache<Integer, Integer> cache = node.getOrCreateCache(DEFAULT_CACHE_NAME);

        Integer key = primaryKey(cache);

        cache.put(key, 0);

        IgniteTransactions txs = node.transactions();

        try (Transaction tx = txs.txStart(TransactionConcurrency.PESSIMISTIC, TransactionIsolation.REPEATABLE_READ)) {
            assertEquals((Integer)0, cache.get(key));

            cache.put(key, 1);

            barrier.await();

            barrier.await(); // Await backup node stop.

            Thread.sleep(1000);

            tx.rollback();
        }

        barrier.await();

        assertEquals((Integer)0, cache.get(key));

        cache.put(key, 2);

        assertEquals((Integer)2, cache.get(key));
    }
}
