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

package org.apache.ignite.internal.processors.cache.distributed.near;

import javax.cache.processor.EntryProcessor;
import javax.cache.processor.EntryProcessorException;
import javax.cache.processor.MutableEntry;
import org.apache.ignite.IgniteCache;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Partitioned atomic cache metrics test.
 */
@RunWith(JUnit4.class)
public class GridCacheAtomicPartitionedTckMetricsSelfTestImpl extends GridCacheAtomicPartitionedMetricsSelfTest {
    /** {@inheritDoc} */
    @Override protected int gridCount() {
        return 1;
    }

    /**
     * @throws Exception If failed.
     */
    @Test
    public void testEntryProcessorRemove() throws Exception {
        IgniteCache<Integer, Integer> cache = grid(0).cache(DEFAULT_CACHE_NAME);

        cache.put(1, 20);

        int result = cache.invoke(1, new EntryProcessor<Integer, Integer, Integer>() {
            @Override public Integer process(MutableEntry<Integer, Integer> entry, Object... arguments)
                    throws EntryProcessorException {
                Integer result = entry.getValue();

                entry.remove();

                return result;
            }
        });

        assertEquals(1L, cache.localMetrics().getCachePuts());

        assertEquals(20, result);
        assertEquals(1L, cache.localMetrics().getCacheHits());
        assertEquals(100.0f, cache.localMetrics().getCacheHitPercentage());
        assertEquals(0L, cache.localMetrics().getCacheMisses());
        assertEquals(0f, cache.localMetrics().getCacheMissPercentage());
        assertEquals(1L, cache.localMetrics().getCachePuts());
        assertEquals(1L, cache.localMetrics().getCacheRemovals());
        assertEquals(0L, cache.localMetrics().getCacheEvictions());
        assert cache.localMetrics().getAveragePutTime() >= 0;
        assert cache.localMetrics().getAverageGetTime() >= 0;
        assert cache.localMetrics().getAverageRemoveTime() >= 0;
    }

    /**
     * @throws Exception If failed.
     */
    @Test
    public void testCacheStatistics() throws Exception {
        IgniteCache<Integer, Integer> cache = grid(0).cache(DEFAULT_CACHE_NAME);

        cache.put(1, 10);

        assertEquals(0, cache.localMetrics().getCacheRemovals());
        assertEquals(1, cache.localMetrics().getCachePuts());

        cache.remove(1);

        assertEquals(0, cache.localMetrics().getCacheHits());
        assertEquals(1, cache.localMetrics().getCacheRemovals());
        assertEquals(1, cache.localMetrics().getCachePuts());

        cache.remove(1);

        assertEquals(0, cache.localMetrics().getCacheHits());
        assertEquals(0, cache.localMetrics().getCacheMisses());
        assertEquals(1, cache.localMetrics().getCacheRemovals());
        assertEquals(1, cache.localMetrics().getCachePuts());

        cache.put(1, 10);
        assertTrue(cache.remove(1, 10));

        assertEquals(1, cache.localMetrics().getCacheHits());
        assertEquals(0, cache.localMetrics().getCacheMisses());
        assertEquals(2, cache.localMetrics().getCacheRemovals());
        assertEquals(2, cache.localMetrics().getCachePuts());

        assertFalse(cache.remove(1, 10));

        assertEquals(1, cache.localMetrics().getCacheHits());
        assertEquals(1, cache.localMetrics().getCacheMisses());
        assertEquals(2, cache.localMetrics().getCacheRemovals());
        assertEquals(2, cache.localMetrics().getCachePuts());
    }

    /**
     * @throws Exception If failed.
     */
    @Test
    public void testConditionReplace() throws Exception {
        IgniteCache<Integer, Integer> cache = grid(0).cache(DEFAULT_CACHE_NAME);

        long hitCount = 0;
        long missCount = 0;
        long putCount = 0;

        boolean result = cache.replace(1, 0, 10);

        ++missCount;
        assertFalse(result);

        assertEquals(missCount, cache.localMetrics().getCacheMisses());
        assertEquals(hitCount, cache.localMetrics().getCacheHits());
        assertEquals(putCount, cache.localMetrics().getCachePuts());

        assertNull(cache.localPeek(1));

        cache.put(1, 10);
        ++putCount;

        assertEquals(missCount, cache.localMetrics().getCacheMisses());
        assertEquals(hitCount, cache.localMetrics().getCacheHits());
        assertEquals(putCount, cache.localMetrics().getCachePuts());

        assertNotNull(cache.localPeek(1));

        result = cache.replace(1, 10, 20);

        assertTrue(result);
        ++hitCount;
        ++putCount;

        assertEquals(missCount, cache.localMetrics().getCacheMisses());
        assertEquals(hitCount, cache.localMetrics().getCacheHits());
        assertEquals(putCount, cache.localMetrics().getCachePuts());

        result = cache.replace(1, 40, 50);

        assertFalse(result);
        ++hitCount;

        assertEquals(hitCount, cache.localMetrics().getCacheHits());
        assertEquals(putCount, cache.localMetrics().getCachePuts());
        assertEquals(missCount, cache.localMetrics().getCacheMisses());
    }

    /**
     * @throws Exception If failed.
     */
    @Test
    public void testPutIfAbsent() throws Exception {
        IgniteCache<Integer, Integer> cache = grid(0).cache(DEFAULT_CACHE_NAME);

        long hitCount = 0;
        long missCount = 0;
        long putCount = 0;

        boolean result = cache.putIfAbsent(1, 1);

        ++putCount;
        ++missCount;

        assertTrue(result);

        assertEquals(missCount, cache.localMetrics().getCacheMisses());
        assertEquals(hitCount, cache.localMetrics().getCacheHits());
        assertEquals(putCount, cache.localMetrics().getCachePuts());

        result = cache.putIfAbsent(1, 1);

        ++hitCount;

        cache.containsKey(123);

        assertFalse(result);
        assertEquals(hitCount, cache.localMetrics().getCacheHits());
        assertEquals(putCount, cache.localMetrics().getCachePuts());
        assertEquals(missCount, cache.localMetrics().getCacheMisses());
    }
}
