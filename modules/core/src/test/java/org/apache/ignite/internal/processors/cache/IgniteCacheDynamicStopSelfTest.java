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
import javax.cache.Cache;
import javax.cache.CacheException;
import org.apache.ignite.IgniteDataStreamer;
import org.apache.ignite.cache.CacheMode;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.internal.IgniteInternalFuture;
import org.apache.ignite.internal.util.typedef.CI1;
import org.apache.ignite.lang.IgniteFuture;
import org.apache.ignite.testframework.GridTestUtils;
import org.apache.ignite.testframework.junits.common.GridCommonAbstractTest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 *
 */
@RunWith(JUnit4.class)
public class IgniteCacheDynamicStopSelfTest extends GridCommonAbstractTest {
    /** {@inheritDoc} */
    @Override protected void beforeTestsStarted() throws Exception {
        super.beforeTestsStarted();

        startGrids(4);
    }

    /**
     * @throws Exception If failed.
     */
    @Test
    public void testStopStartCacheWithDataLoaderNoOverwrite() throws Exception {
        checkStopStartCacheWithDataLoader(false);
    }

    /**
     * @throws Exception If failed.
     */
    @Test
    public void testStopStartCacheWithDataLoaderOverwrite() throws Exception {
        checkStopStartCacheWithDataLoader(true);
    }

    /**
     * @param allowOverwrite Allow overwrite flag for streamer.
     * @throws Exception If failed.
     */
    public void checkStopStartCacheWithDataLoader(final boolean allowOverwrite) throws Exception {
        CacheConfiguration ccfg = new CacheConfiguration(DEFAULT_CACHE_NAME);

        ccfg.setCacheMode(CacheMode.PARTITIONED);

        ignite(0).createCache(ccfg);

        final AtomicBoolean stop = new AtomicBoolean();

        IgniteInternalFuture<Object> fut = GridTestUtils.runAsync(new Callable<Object>() {
            /** {@inheritDoc} */
            @Override public Object call() throws Exception {
                while (!stop.get()) {
                    try (IgniteDataStreamer<Integer, Integer> str = ignite(0).dataStreamer(DEFAULT_CACHE_NAME)) {
                        str.allowOverwrite(allowOverwrite);

                        int i = 0;

                        while (!stop.get()) {
                            try {
                                str.addData(i % 10_000, i).listen(new CI1<IgniteFuture<?>>() {
                                    @Override public void apply(IgniteFuture<?> f) {
                                        try {
                                            f.get();
                                        }
                                        catch (CacheException ignore) {
                                            // This may be debugged.
                                        }
                                    }
                                });
                            }
                            catch (IllegalStateException ignored) {
                                break;
                            }

                            if (i > 0 && i % 10000 == 0)
                                info("Added: " + i);

                            i++;
                        }
                    }
                    catch (IllegalStateException | CacheException ignored) {
                        // This may be debugged.
                    }
                }

                return null;
            }
        });

        try {
            Thread.sleep(500);

            ignite(0).destroyCache(DEFAULT_CACHE_NAME);

            Thread.sleep(500);

            ignite(0).createCache(ccfg);

            Thread.sleep(1000);
        }
        finally {
            stop.set(true);
        }

        fut.get();

        int cnt = 0;

        for (Cache.Entry<Object, Object> ignored : ignite(0).cache(DEFAULT_CACHE_NAME))
            cnt++;

        info(">>> cnt=" + cnt);

        ignite(0).destroyCache(DEFAULT_CACHE_NAME);
    }
}
