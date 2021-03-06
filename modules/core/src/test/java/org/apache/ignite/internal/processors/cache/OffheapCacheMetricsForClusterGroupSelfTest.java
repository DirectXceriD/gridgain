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

import java.util.concurrent.CountDownLatch;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.cache.CacheAtomicityMode;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.events.Event;
import org.apache.ignite.events.EventType;
import org.apache.ignite.lang.IgnitePredicate;
import org.apache.ignite.testframework.junits.common.GridCommonAbstractTest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.apache.ignite.events.EventType.EVT_NODE_METRICS_UPDATED;

/**
 * Test for cluster wide offheap cache metrics.
 */
@RunWith(JUnit4.class)
public class OffheapCacheMetricsForClusterGroupSelfTest extends GridCommonAbstractTest {
    /** Grid count. */
    private static final int GRID_CNT = 3;

    /** Client count */
    private static final int CLIENT_CNT = 3;

    /** Grid client mode */
    private boolean clientMode;

    /** {@inheritDoc} */
    @Override protected IgniteConfiguration getConfiguration(String igniteInstanceName) throws Exception {
        IgniteConfiguration cfg = super.getConfiguration(igniteInstanceName);

        cfg.setClientMode(clientMode);

        return cfg;
    }

    /** {@inheritDoc} */
    @Override protected void beforeTestsStarted() throws Exception {
        // start grids
        clientMode = false;
        for (int i = 0; i < GRID_CNT; i++)
            startGrid("server-" + i);

        // start clients
        clientMode = true;
        for (int i = 0; i < CLIENT_CNT; i++)
            startGrid("client-" + i);
    }

    /** */
    @Test
    public void testGetOffHeapPrimaryEntriesCount() throws Exception {
        String cacheName = "testGetOffHeapPrimaryEntriesCount";
        IgniteCache<Integer, Integer> cache = grid("client-0").createCache(cacheConfiguration(cacheName));

        for (int i = 0; i < 1000; i++)
            cache.put(i, i);

        awaitMetricsUpdate();

        assertGetOffHeapPrimaryEntriesCount(cacheName, 1000);

        for (int j = 0; j < 1000; j++)
            cache.get(j);

        awaitMetricsUpdate();

        assertGetOffHeapPrimaryEntriesCount(cacheName, 1000);

        cache = grid("client-1").cache(cacheName);

        for (int j = 0; j < 1000; j++)
            cache.get(j);

        awaitMetricsUpdate();

        assertGetOffHeapPrimaryEntriesCount(cacheName, 1000);
    }

    /**
     * Wait for {@link EventType#EVT_NODE_METRICS_UPDATED} event will be receieved.
     */
    private void awaitMetricsUpdate() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch((GRID_CNT + 1) * 2);

        IgnitePredicate<Event> lsnr = new IgnitePredicate<Event>() {
            @Override public boolean apply(Event ignore) {
                latch.countDown();

                return true;
            }
        };

        for (int i = 0; i < GRID_CNT; i++)
            grid("server-" + i).events().localListen(lsnr, EVT_NODE_METRICS_UPDATED);

        latch.await();
    }

    private void assertGetOffHeapPrimaryEntriesCount(String cacheName, int count) throws Exception {
        long localPrimary = 0L;
        long localBackups = 0L;

        for (int i = 0; i < GRID_CNT; i++) {
            IgniteCache<Integer, Integer> cache = grid("server-" + i).cache(cacheName);
            assertEquals(count, cache.metrics().getOffHeapPrimaryEntriesCount());
            assertEquals(count, cache.mxBean().getOffHeapPrimaryEntriesCount());
            assertEquals(count, cache.metrics().getOffHeapBackupEntriesCount());
            assertEquals(count, cache.mxBean().getOffHeapBackupEntriesCount());

            localPrimary += cache.localMxBean().getOffHeapPrimaryEntriesCount();
            localBackups += cache.localMxBean().getOffHeapPrimaryEntriesCount();
        }

        assertEquals(count, localPrimary);
        assertEquals(count, localBackups);

        for (int i = 0; i < CLIENT_CNT; i++) {
            IgniteCache<Integer, Integer> cache = grid("client-" + i).cache(cacheName);
            assertEquals(count, cache.metrics().getOffHeapPrimaryEntriesCount());
            assertEquals(count, cache.mxBean().getOffHeapPrimaryEntriesCount());
            assertEquals(count, cache.metrics().getOffHeapBackupEntriesCount());
            assertEquals(count, cache.mxBean().getOffHeapBackupEntriesCount());
            assertEquals(0L, cache.localMxBean().getOffHeapPrimaryEntriesCount());
            assertEquals(0L, cache.localMxBean().getOffHeapBackupEntriesCount());
        }
    }

    /**
     * @param cacheName Cache name.
     * @return Cache configuration.
     */
    private static CacheConfiguration<Integer, Integer> cacheConfiguration(String cacheName) {
        CacheConfiguration<Integer, Integer> cfg = new CacheConfiguration<>(cacheName);

        cfg.setBackups(1);
        cfg.setStatisticsEnabled(true);
        cfg.setAtomicityMode(CacheAtomicityMode.TRANSACTIONAL);
        return cfg;
    }
}
