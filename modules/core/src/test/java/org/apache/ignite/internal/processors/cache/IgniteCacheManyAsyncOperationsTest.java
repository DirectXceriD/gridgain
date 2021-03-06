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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.cache.CacheAtomicityMode;
import org.apache.ignite.cache.CacheMode;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.configuration.NearCacheConfiguration;
import org.apache.ignite.lang.IgniteFuture;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.apache.ignite.cache.CacheAtomicityMode.TRANSACTIONAL;
import static org.apache.ignite.cache.CacheMode.PARTITIONED;

/**
 *
 */
@RunWith(JUnit4.class)
public class IgniteCacheManyAsyncOperationsTest extends IgniteCacheAbstractTest {
    /** {@inheritDoc} */
    @Override protected int gridCount() {
        return 2;
    }

    /** {@inheritDoc} */
    @Override protected CacheMode cacheMode() {
        return PARTITIONED;
    }

    /** {@inheritDoc} */
    @Override protected CacheAtomicityMode atomicityMode() {
        return TRANSACTIONAL;
    }

    /** {@inheritDoc} */
    @Override protected NearCacheConfiguration nearConfiguration() {
        return null;
    }

    /** {@inheritDoc} */
    @Override protected IgniteConfiguration getConfiguration(String igniteInstanceName) throws Exception {
        IgniteConfiguration cfg = super.getConfiguration(igniteInstanceName);

        if (igniteInstanceName.equals(getTestIgniteInstanceName(2)))
            cfg.setClientMode(true);

        return cfg;
    }

    /**
     * @throws Exception If failed.
     */
    @Test
    public void testManyAsyncOperations() throws Exception {
        try (Ignite client = startGrid(gridCount())) {
            assertTrue(client.configuration().isClientMode());

            IgniteCache<Object, Object> cache = client.cache(DEFAULT_CACHE_NAME);

            final int ASYNC_OPS = cache.getConfiguration(CacheConfiguration.class).getMaxConcurrentAsyncOperations();

            log.info("Number of async operations: " + ASYNC_OPS);

            Map<Integer, byte[]> map = new HashMap<>();

            for (int i = 0; i < 100; i++)
                map.put(i, new byte[128]);

            for (int iter = 0; iter < 3; iter++) {
                log.info("Iteration: " + iter);

                List<IgniteFuture<?>> futs = new ArrayList<>(ASYNC_OPS);

                for (int i = 0; i < ASYNC_OPS; i++) {
                    futs.add(cache.putAllAsync(map));

                    if (i % 50 == 0)
                        log.info("Created futures: " + (i + 1));
                }

                for (int i = 0; i < ASYNC_OPS; i++) {
                    IgniteFuture<?> fut = futs.get(i);

                    fut.get();

                    if (i % 50 == 0)
                        log.info("Done: " + (i + 1));
                }
            }
        }
    }
}
