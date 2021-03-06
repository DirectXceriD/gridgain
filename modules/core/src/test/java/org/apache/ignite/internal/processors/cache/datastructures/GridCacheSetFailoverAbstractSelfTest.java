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

package org.apache.ignite.internal.processors.cache.datastructures;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.ignite.IgniteException;
import org.apache.ignite.IgniteSet;
import org.apache.ignite.cache.CacheMode;
import org.apache.ignite.internal.IgniteInternalFuture;
import org.apache.ignite.internal.processors.cache.GridCacheAdapter;
import org.apache.ignite.internal.processors.cache.GridCacheEntryEx;
import org.apache.ignite.internal.processors.cache.GridCacheMapEntry;
import org.apache.ignite.internal.processors.datastructures.SetItemKey;
import org.apache.ignite.internal.util.typedef.internal.U;
import org.apache.ignite.lang.IgniteUuid;
import org.apache.ignite.testframework.GridTestUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.apache.ignite.cache.CacheMode.PARTITIONED;

/**
 * Set failover tests.
 */
@RunWith(JUnit4.class)
public abstract class GridCacheSetFailoverAbstractSelfTest extends IgniteCollectionAbstractTest {
    /** */
    private static final String SET_NAME = "testFailoverSet";

    /** */
    private static final long TEST_DURATION = 60_000;

    /** {@inheritDoc} */
    @Override protected int gridCount() {
        return 4;
    }

    /** {@inheritDoc} */
    @Override protected void beforeTestsStarted() throws Exception {
        // No-op.
    }

    /** {@inheritDoc} */
    @Override protected void afterTestsStopped() throws Exception {
        // No-op.
    }

    /** {@inheritDoc} */
    @Override protected void beforeTest() throws Exception {
        startGrids(gridCount());
    }

    /** {@inheritDoc} */
    @Override protected void afterTest() throws Exception {
        stopAllGrids();
    }

    /** {@inheritDoc} */
    @Override protected CacheMode collectionCacheMode() {
        return PARTITIONED;
    }

    /** {@inheritDoc} */
    @Override protected long getTestTimeout() {
        return TEST_DURATION + 60_000;
    }

    /**
     * @throws Exception If failed.
     */
    @SuppressWarnings("WhileLoopReplaceableByForEach")
    @Test
    public void testNodeRestart() throws Exception {
        IgniteSet<Integer> set = grid(0).set(SET_NAME, config(false));

        final int ITEMS = 10_000;

        Collection<Integer> items = new ArrayList<>(ITEMS);

        for (int i = 0; i < ITEMS; i++)
            items.add(i);

        set.addAll(items);

        assertEquals(ITEMS, set.size());

        AtomicBoolean stop = new AtomicBoolean();

        IgniteInternalFuture<?> killFut = startNodeKiller(stop);

        long stopTime = System.currentTimeMillis() + TEST_DURATION;

        try {
            ThreadLocalRandom rnd = ThreadLocalRandom.current();

            while (System.currentTimeMillis() < stopTime) {
                for (int i = 0; i < 10; i++) {
                    try {
                        int size = set.size();

                        assertEquals(ITEMS, size);
                    }
                    catch (IgniteException ignore) {
                        // No-op.
                    }

                    try {
                        Iterator<Integer> iter = set.iterator();

                        int cnt = 0;

                        while (iter.hasNext()) {
                            assertNotNull(iter.next());

                            cnt++;
                        }

                        assertEquals(ITEMS, cnt);
                    }
                    catch (IgniteException ignore) {
                        // No-op.
                    }

                    int val = rnd.nextInt(ITEMS);

                    assertTrue("Not contains: " + val, set.contains(val));

                    val = ITEMS + rnd.nextInt(ITEMS);

                    assertFalse("Contains: " + val, set.contains(val));
                }

                log.info("Remove set.");

                set.close();

                log.info("Create new set.");

                set = grid(0).set(SET_NAME, config(false));

                set.addAll(items);
            }
        }
        finally {
            stop.set(true);
        }

        killFut.get();

        set.close();

        if (false) { // TODO IGNITE-600: enable check when fixed.
            int cnt = 0;

            Set<IgniteUuid> setIds = new HashSet<>();

            for (int i = 0; i < gridCount(); i++) {
                GridCacheAdapter cache = grid(i).context().cache().internalCache(DEFAULT_CACHE_NAME);

                Iterator<GridCacheMapEntry> entries = cache.map().entries(cache.context().cacheId()).iterator();

                while (entries.hasNext()) {
                    GridCacheEntryEx entry = entries.next();

                    if (entry.hasValue()) {
                        cnt++;

                        if (entry.key() instanceof SetItemKey) {
                            SetItemKey setItem = (SetItemKey)entry.key();

                            if (setIds.add(setItem.setId()))
                                log.info("Unexpected set item [setId=" + setItem.setId() +
                                    ", grid: " + grid(i).name() +
                                    ", entry=" + entry + ']');
                        }
                    }
                }
            }

            assertEquals("Found unexpected cache entries", 0, cnt);
        }
    }

    /**
     * Starts thread restarting random node.
     *
     * @param stop Stop flag.
     * @return Future completing when thread finishes.
     */
    private IgniteInternalFuture<?> startNodeKiller(final AtomicBoolean stop) {
        return GridTestUtils.runAsync(new Callable<Void>() {
            @Override public Void call() throws Exception {
                ThreadLocalRandom rnd = ThreadLocalRandom.current();

                while (!stop.get()) {
                    int idx = rnd.nextInt(1, gridCount());

                    U.sleep(rnd.nextLong(2000, 3000));

                    log.info("Killing node: " + idx);

                    stopGrid(idx);

                    U.sleep(rnd.nextLong(500, 1000));

                    startGrid(idx);
                }

                return null;
            }
        });
    }
}
