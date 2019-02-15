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

import java.util.Arrays;
import java.util.List;
import javax.cache.CacheException;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.IgniteException;
import org.apache.ignite.cache.query.QueryCursor;
import org.apache.ignite.cache.query.SqlFieldsQuery;
import org.apache.ignite.cache.query.annotations.QuerySqlFunction;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.internal.IgniteInternalFuture;
import org.apache.ignite.internal.util.typedef.G;
import org.apache.ignite.testframework.junits.common.GridCommonAbstractTest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests distributed SQL query cancel related scenarios.
 */
@RunWith(JUnit4.class)
public class IgniteCacheDistributedQueryCancelSelfTest extends GridCommonAbstractTest {
    /** Grids count. */
    private static final int GRIDS_COUNT = 3;

    /** Cache size. */
    public static final int CACHE_SIZE = 10_000;

    /** Value size. */
    public static final int VAL_SIZE = 16;

    /** */
    private static final String QUERY = "select a._val, b._val from String a, String b";

    /** {@inheritDoc} */
    @Override protected void beforeTest() throws Exception {
        super.beforeTest();

        startGridsMultiThreaded(GRIDS_COUNT);
    }

    /** {@inheritDoc} */
    @Override protected IgniteConfiguration getConfiguration(String igniteInstanceName) throws Exception {
        IgniteConfiguration cfg = super.getConfiguration(igniteInstanceName);

        CacheConfiguration<Integer, String> ccfg = new CacheConfiguration<>(DEFAULT_CACHE_NAME);
        ccfg.setIndexedTypes(Integer.class, String.class);

        cfg.setCacheConfiguration(ccfg);

        if ("client".equals(igniteInstanceName))
            cfg.setClientMode(true);

        return cfg;
    }

    /** {@inheritDoc} */
    @Override protected void afterTest() throws Exception {
        super.afterTest();

        stopAllGrids();
    }

    /** */
    @Test
    public void testQueryCancelsOnGridShutdown() throws Exception {
        try (Ignite client = startGrid("client")) {

            IgniteCache<Object, Object> cache = client.cache(DEFAULT_CACHE_NAME);

            assertEquals(0, cache.localSize());

            int p = 1;
            for (int i = 1; i <= CACHE_SIZE; i++) {
                char[] tmp = new char[VAL_SIZE];
                Arrays.fill(tmp, ' ');
                cache.put(i, new String(tmp));

                if (i / (float)CACHE_SIZE >= p / 10f) {
                    log().info("Loaded " + i + " of " + CACHE_SIZE);

                    p++;
                }
            }

            SqlFieldsQuery qry = new SqlFieldsQuery(QUERY);

            IgniteInternalFuture<?> fut = multithreadedAsync(new Runnable() {
                @Override public void run() {
                    try {
                        Thread.sleep(1_500);
                    }
                    catch (InterruptedException e) {
                        throw new IgniteException(e);
                    }

                    for (Ignite g : G.allGrids())
                        if (!g.configuration().isClientMode())
                            stopGrid(g.name(), true);
                }
            }, 1);

            try {
                final QueryCursor<List<?>> cursor = cache.query(qry);

                cursor.iterator();
            }
            catch (CacheException ignored) {
                // No-op.
            }

            fut.get();

            // Test must exit gracefully.
        }
    }

    /** */
    @Test
    public void testQueryResponseFailCode() throws Exception {
        try (Ignite client = startGrid("client")) {

            CacheConfiguration<Integer, Integer> cfg = new CacheConfiguration<>(DEFAULT_CACHE_NAME);
            cfg.setSqlFunctionClasses(Functions.class);
            cfg.setIndexedTypes(Integer.class, Integer.class);
            cfg.setName("test");

            IgniteCache<Integer, Integer> cache = client.getOrCreateCache(cfg);

            cache.put(1, 1);

            QueryCursor<List<?>> qry = cache.query(new SqlFieldsQuery("select fail() from Integer"));

            try {
                qry.getAll();

                fail();
            }
            catch (Exception e) {
                assertTrue(e instanceof CacheException);
            }
        }
    }

    /** */
    public static class Functions {
        /** */
        @QuerySqlFunction
        public static int fail() {
            throw new IllegalArgumentException();
        }
    }
}
