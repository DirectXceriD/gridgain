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

import javax.cache.expiry.Duration;
import javax.cache.expiry.TouchedExpiryPolicy;
import org.apache.ignite.cache.CacheMode;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.internal.IgniteKernal;
import org.apache.ignite.internal.util.typedef.CAX;
import org.apache.ignite.internal.util.typedef.internal.U;
import org.apache.ignite.testframework.GridTestUtils;
import org.apache.ignite.testframework.MvccFeatureChecker;
import org.apache.ignite.testframework.junits.common.GridCommonAbstractTest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.apache.ignite.cache.CacheMode.LOCAL;
import static org.apache.ignite.cache.CacheMode.PARTITIONED;
import static org.apache.ignite.cache.CacheMode.REPLICATED;

/**
 * TTL manager self test.
 */
@RunWith(JUnit4.class)
public class GridCacheTtlManagerSelfTest extends GridCommonAbstractTest {
    /** Test cache mode. */
    protected CacheMode cacheMode;

    /** {@inheritDoc} */
    @Override protected void beforeTestsStarted() throws Exception {
        MvccFeatureChecker.failIfNotSupported(MvccFeatureChecker.Feature.EXPIRATION);

        super.beforeTestsStarted();
    }

    /** {@inheritDoc} */
    @Override protected IgniteConfiguration getConfiguration(String igniteInstanceName) throws Exception {
        IgniteConfiguration cfg = super.getConfiguration(igniteInstanceName);

        CacheConfiguration ccfg = new CacheConfiguration(DEFAULT_CACHE_NAME);

        ccfg.setCacheMode(cacheMode);
        ccfg.setEagerTtl(true);

        cfg.setCacheConfiguration(ccfg);

        return cfg;
    }

    /**
     * @throws Exception If failed.
     */
    @Test
    public void testLocalTtl() throws Exception {
        checkTtl(LOCAL);
    }

    /**
     * @throws Exception If failed.
     */
    @Test
    public void testPartitionedTtl() throws Exception {
        checkTtl(PARTITIONED);
    }

    /**
     * @throws Exception If failed.
     */
    @Test
    public void testReplicatedTtl() throws Exception {
        checkTtl(REPLICATED);
    }

    /**
     * @param mode Cache mode.
     * @throws Exception If failed.
     */
    private void checkTtl(CacheMode mode) throws Exception {
        cacheMode = mode;

        final IgniteKernal g = (IgniteKernal)startGrid(0);

        try {
            final String key = "key";

            g.cache(DEFAULT_CACHE_NAME).withExpiryPolicy(
                    new TouchedExpiryPolicy(new Duration(MILLISECONDS, 1000))).put(key, 1);

            assertEquals(1, g.cache(DEFAULT_CACHE_NAME).get(key));

            U.sleep(1100);

            GridTestUtils.retryAssert(log, 10, 100, new CAX() {
                @Override public void applyx() {
                    // Check that no more entries left in the map.
                    assertNull(g.cache(DEFAULT_CACHE_NAME).get(key));

                    if (!g.internalCache(DEFAULT_CACHE_NAME).context().deferredDelete())
                        assertNull(g.internalCache(DEFAULT_CACHE_NAME).map().getEntry(g.internalCache(DEFAULT_CACHE_NAME).context(),
                            g.internalCache(DEFAULT_CACHE_NAME).context().toCacheKeyObject(key)));
                }
            });
        }
        finally {
            stopAllGrids();
        }
    }
}
