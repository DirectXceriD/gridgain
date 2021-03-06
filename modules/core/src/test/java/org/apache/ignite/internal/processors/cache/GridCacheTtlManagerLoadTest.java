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
import java.util.concurrent.atomic.AtomicBoolean;
import javax.cache.expiry.Duration;
import javax.cache.expiry.TouchedExpiryPolicy;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.internal.IgniteInternalFuture;
import org.apache.ignite.internal.IgniteKernal;
import org.apache.ignite.internal.util.typedef.internal.U;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.apache.ignite.cache.CacheMode.REPLICATED;

/**
 * Check ttl manager for memory leak.
 */
@RunWith(JUnit4.class)
public class GridCacheTtlManagerLoadTest extends GridCacheTtlManagerSelfTest {
    /**
     * @throws Exception If failed.
     */
    @Test
    public void testLoad() throws Exception {
        cacheMode = REPLICATED;

        final IgniteKernal g = (IgniteKernal)startGrid(0);

        try {
            final AtomicBoolean stop = new AtomicBoolean();

            IgniteInternalFuture<?> fut = multithreadedAsync(new Callable<Object>() {
                @Override public Object call() throws Exception {
                    IgniteCache<Object,Object> cache = g.cache(DEFAULT_CACHE_NAME).
                        withExpiryPolicy(new TouchedExpiryPolicy(new Duration(MILLISECONDS, 1000)));

                    long key = 0;

                    while (!stop.get()) {
                        cache.put(key, key);

                        key++;
                    }

                    return null;
                }
            }, 1);

            GridCacheTtlManager ttlMgr = g.internalCache(DEFAULT_CACHE_NAME).context().ttl();

            for (int i = 0; i < 5; i++) {
                U.sleep(5000);

                ttlMgr.printMemoryStats();
            }

            stop.set(true);

            fut.get();
        }
        finally {
            stopAllGrids();
        }
    }

    /** {@inheritDoc} */
    @Override protected long getTestTimeout() {
        return Long.MAX_VALUE;
    }
}
