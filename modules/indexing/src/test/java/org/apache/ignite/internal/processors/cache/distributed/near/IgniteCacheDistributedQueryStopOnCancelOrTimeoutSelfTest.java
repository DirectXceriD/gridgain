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
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import javax.cache.CacheException;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.IgniteCheckedException;
import org.apache.ignite.cache.query.QueryCursor;
import org.apache.ignite.cache.query.SqlFieldsQuery;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.internal.IgniteEx;
import org.apache.ignite.cache.query.QueryCancelledException;
import org.apache.ignite.internal.processors.GridProcessor;
import org.apache.ignite.internal.processors.query.h2.IgniteH2Indexing;
import org.apache.ignite.internal.util.typedef.G;
import org.apache.ignite.internal.util.typedef.internal.U;
import org.apache.ignite.testframework.junits.common.GridCommonAbstractTest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests distributed SQL queries cancel by user or timeout.
 */
@RunWith(JUnit4.class)
public class IgniteCacheDistributedQueryStopOnCancelOrTimeoutSelfTest extends GridCommonAbstractTest {
    /** Grids count. */
    private static final int GRIDS_CNT = 3;

    /** Cache size. */
    public static final int CACHE_SIZE = 10_000;

    /** Value size. */
    public static final int VAL_SIZE = 16;

    /** */
    private static final String QRY_1 = "select a._val, b._val from String a, String b";

    /** */
    private static final String QRY_2 = "select a._key, count(*) from String a group by a._key";

    /** */
    private static final String QRY_3 = "select a._val from String a";

    /** {@inheritDoc} */
    @Override protected void beforeTestsStarted() throws Exception {
        super.beforeTestsStarted();

        startGridsMultiThreaded(GRIDS_CNT);
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

        for (Ignite g : G.allGrids())
            g.cache(DEFAULT_CACHE_NAME).removeAll();
    }

    /** */
    @Test
    public void testRemoteQueryExecutionTimeout() throws Exception {
        testQueryCancel(CACHE_SIZE, VAL_SIZE, QRY_1, 500, TimeUnit.MILLISECONDS, true);
    }

    /** */
    @Test
    public void testRemoteQueryWithMergeTableTimeout() throws Exception {
        testQueryCancel(CACHE_SIZE, VAL_SIZE, QRY_2, 500, TimeUnit.MILLISECONDS, true);
    }

    /** */
    @Test
    public void testRemoteQueryExecutionCancel0() throws Exception {
        testQueryCancel(CACHE_SIZE, VAL_SIZE, QRY_1, 1, TimeUnit.MILLISECONDS, false);
    }

    /** */
    @Test
    public void testRemoteQueryExecutionCancel1() throws Exception {
        testQueryCancel(CACHE_SIZE, VAL_SIZE, QRY_1, 500, TimeUnit.MILLISECONDS, false);
    }

    /** */
    @Test
    public void testRemoteQueryExecutionCancel2() throws Exception {
        testQueryCancel(CACHE_SIZE, VAL_SIZE, QRY_1, 1, TimeUnit.SECONDS, false);
    }

    /** */
    @Test
    public void testRemoteQueryExecutionCancel3() throws Exception {
        testQueryCancel(CACHE_SIZE, VAL_SIZE, QRY_1, 3, TimeUnit.SECONDS, false);
    }

    /** */
    @Test
    public void testRemoteQueryWithMergeTableCancel0() throws Exception {
        testQueryCancel(CACHE_SIZE, VAL_SIZE, QRY_2, 1, TimeUnit.MILLISECONDS, false);
    }

    /** */
    @Test
    public void testRemoteQueryWithMergeTableCancel1() throws Exception {
        testQueryCancel(CACHE_SIZE, VAL_SIZE, QRY_2, 500, TimeUnit.MILLISECONDS, false);
    }

    /** */
    @Test
    public void testRemoteQueryWithMergeTableCancel2() throws Exception {
        testQueryCancel(CACHE_SIZE, VAL_SIZE, QRY_2, 1_500, TimeUnit.MILLISECONDS, false);
    }

    /** */
    @Test
    public void testRemoteQueryWithMergeTableCancel3() throws Exception {
        testQueryCancel(CACHE_SIZE, VAL_SIZE, QRY_2, 3, TimeUnit.SECONDS, false);
    }

    /** */
    @Test
    public void testRemoteQueryWithoutMergeTableCancel0() throws Exception {
        testQueryCancel(CACHE_SIZE, VAL_SIZE, QRY_3, 1, TimeUnit.MILLISECONDS, false);
    }

    /** */
    @Test
    public void testRemoteQueryWithoutMergeTableCancel1() throws Exception {
        testQueryCancel(CACHE_SIZE, VAL_SIZE, QRY_3, 500, TimeUnit.MILLISECONDS, false);
    }

    /** */
    @Test
    public void testRemoteQueryWithoutMergeTableCancel2() throws Exception {
        testQueryCancel(CACHE_SIZE, VAL_SIZE, QRY_3, 1_000, TimeUnit.MILLISECONDS, false);
    }

    /** */
    @Test
    public void testRemoteQueryWithoutMergeTableCancel3() throws Exception {
        testQueryCancel(CACHE_SIZE, VAL_SIZE, QRY_3, 3, TimeUnit.SECONDS, false);
    }

    /** */
    @Test
    public void testRemoteQueryAlreadyFinishedStop() throws Exception {
        testQueryCancel(100, VAL_SIZE, QRY_3, 3, TimeUnit.SECONDS, false);
    }

    /** */
    private void testQueryCancel(int keyCnt, int valSize, String sql, int timeoutUnits, TimeUnit timeUnit,
                                 boolean timeout) throws Exception {
        try (Ignite client = startGrid("client")) {

            IgniteCache<Object, Object> cache = client.cache(DEFAULT_CACHE_NAME);

            assertEquals(0, cache.localSize());

            int p = 1;
            for (int i = 1; i <= keyCnt; i++) {
                char[] tmp = new char[valSize];
                Arrays.fill(tmp, ' ');
                cache.put(i, new String(tmp));

                if (i/(float)keyCnt >= p/10f) {
                    log().info("Loaded " + i + " of " + keyCnt);

                    p++;
                }
            }

            assertEquals(0, cache.localSize());

            SqlFieldsQuery qry = new SqlFieldsQuery(sql);

            final QueryCursor<List<?>> cursor;
            if (timeout) {
                qry.setTimeout(timeoutUnits, timeUnit);

                cursor = cache.query(qry);
            } else {
                cursor = cache.query(qry);

                client.scheduler().runLocal(new Runnable() {
                    @Override public void run() {
                        cursor.close();
                    }
                }, timeoutUnits, timeUnit);
            }

            try(QueryCursor<List<?>> ignored = cursor) {
                cursor.iterator();
            }
            catch (CacheException ex) {
                log().error("Got expected exception", ex);

                assertTrue("Must throw correct exception", ex.getCause() instanceof QueryCancelledException);
            }

            // Give some time to clean up.
            Thread.sleep(TimeUnit.MILLISECONDS.convert(timeoutUnits, timeUnit) + 3_000);

            checkCleanState();
        }
    }

    /**
     * Validates clean state on all participating nodes after query cancellation.
     */
    @SuppressWarnings("unchecked")
    private void checkCleanState() throws IgniteCheckedException {
        for (int i = 0; i < GRIDS_CNT; i++) {
            IgniteEx grid = grid(i);

            // Validate everything was cleaned up.
            ConcurrentMap<UUID, ?> map = U.field(((IgniteH2Indexing)U.field((GridProcessor)U.field(
                grid.context(), "qryProc"), "idx")).mapQueryExecutor(), "qryRess");

            String msg = "Map executor state is not cleared";

            // TODO FIXME Current implementation leaves map entry for each node that's ever executed a query.
            for (Object result : map.values()) {
                Map<Long, ?> m = U.field(result, "res");

                assertEquals(msg, 0, m.size());
            }
        }
    }
}
