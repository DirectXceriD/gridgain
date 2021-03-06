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

import java.io.Serializable;
import java.util.concurrent.ThreadLocalRandom;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.cache.eviction.lru.LruEvictionPolicy;
import org.apache.ignite.cache.query.SqlQuery;
import org.apache.ignite.cache.query.annotations.QuerySqlField;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.DataRegionConfiguration;
import org.apache.ignite.configuration.DataStorageConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.internal.util.typedef.internal.U;
import org.apache.ignite.lang.IgniteInClosure;
import org.apache.ignite.testframework.GridTestUtils;
import org.apache.ignite.testframework.junits.common.GridCommonAbstractTest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 *
 */
@RunWith(JUnit4.class)
public class CacheQueryEvictDataLostTest extends GridCommonAbstractTest {
    /** */
    private static final int KEYS = 100_000;

    /**
     *
     */
    public CacheQueryEvictDataLostTest() {
        super(true);
    }

    /** {@inheritDoc} */
    @Override protected IgniteConfiguration getConfiguration() throws Exception {
        IgniteConfiguration cfg = super.getConfiguration();

        CacheConfiguration<Object, Object> ccfg = new CacheConfiguration<>(DEFAULT_CACHE_NAME);

        ccfg.setName("cache-1");
        ccfg.setEvictionPolicy(new LruEvictionPolicy(10));
        ccfg.setOnheapCacheEnabled(true);
        ccfg.setIndexedTypes(Integer.class, TestData.class);

        cfg.setCacheConfiguration(ccfg);

        DataStorageConfiguration memCfg = new DataStorageConfiguration()
            .setDefaultDataRegionConfiguration(
                new DataRegionConfiguration()
                    .setMaxSize(256L * 1024 * 1024));
        cfg.setDataStorageConfiguration(memCfg);

        return cfg;
    }

    /**
     * @throws Exception If failed.
     */
    @Test
    public void testQueryDataLost() throws Exception {
        final long stopTime = U.currentTimeMillis() + 30_000;

        GridTestUtils.runMultiThreaded(new IgniteInClosure<Integer>() {
            void putGet(IgniteCache<Object, Object> cache) {
                ThreadLocalRandom rnd = ThreadLocalRandom.current();

                for (int i = 0; i < KEYS; i++) {
                    cache.put(rnd.nextInt(KEYS), new TestData(i));

                    cache.get(rnd.nextInt(KEYS));
                }
            }

            void query(IgniteCache<Object, Object> cache) {
                SqlQuery<Object, Object> qry1 = new SqlQuery<>(TestData.class, "_key > ?");
                qry1.setArgs(KEYS / 2);

                cache.query(qry1).getAll();

                SqlQuery<Object, Object> qry2 = new SqlQuery<>(TestData.class, "idxVal > ?");
                qry2.setArgs(KEYS / 2);

                cache.query(qry2).getAll();
            }

            @Override public void apply(Integer idx) {
                IgniteCache<Object, Object> cache1 = grid().cache("cache-1");

                while (U.currentTimeMillis() < stopTime) {
                    if (idx == 0)
                        putGet(cache1);
                    else
                        query(cache1);
                }
            }
        }, 10, "test-thread");
    }

    /**
     *
     */
    static class TestData implements Serializable {
        /** */
        @QuerySqlField(index = true)
        private int idxVal;

        /**
         * @param idxVal Value.
         */
        public TestData(int idxVal) {
            this.idxVal = idxVal;
        }
    }
}
