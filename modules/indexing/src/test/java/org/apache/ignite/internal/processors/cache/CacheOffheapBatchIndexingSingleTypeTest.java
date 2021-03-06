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

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import javax.cache.processor.MutableEntry;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.cache.CacheEntryProcessor;
import org.apache.ignite.cache.query.QueryCursor;
import org.apache.ignite.cache.query.SqlFieldsQuery;
import org.apache.ignite.configuration.CacheConfiguration;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests various cache operations with indexing enabled.
 * Cache contains single type.
 */
@RunWith(JUnit4.class)
public class CacheOffheapBatchIndexingSingleTypeTest extends CacheOffheapBatchIndexingBaseTest {
    /**
     * Tests removal using EntryProcessor.
     *
     * @throws Exception If failed.
     */
    @Test
    public void testBatchRemove() throws Exception {
        Ignite ignite = grid(0);

        CacheConfiguration<Object, Object> ccfg = cacheConfiguration(
            new Class<?>[] {Integer.class, CacheOffheapBatchIndexingBaseTest.Organization.class});

        final IgniteCache<Object, Object> cache = ignite.createCache(ccfg);

        try {
            int iterations = 50;

            while (iterations-- >= 0) {
                int total = 1000;

                for (int id = 0; id < total; id++)
                    cache.put(id, new CacheOffheapBatchIndexingBaseTest.Organization(id, "Organization " + id));

                cache.invoke(0, new CacheEntryProcessor<Object, Object, Object>() {
                    @Override public Object process(MutableEntry<Object, Object> entry, Object... args) {
                        entry.remove();

                        return null;
                    }
                });

                QueryCursor<List<?>> q = cache.query(new SqlFieldsQuery("select _key,_val from Organization where id=0"));

                assertEquals(0, q.getAll().size());

                q = cache.query(new SqlFieldsQuery("select _key,_val from Organization where id=1"));

                assertEquals(1, q.getAll().size());

                assertEquals(total - 1, cache.size());

                cache.removeAll();
            }
        }
        finally {
            cache.destroy();
        }
    }

    /**
     *
     */
    @Test
    public void testPutAllAndStreamer() {
        doStreamerBatchTest(50,
            1_000,
            new Class<?>[] {Integer.class, CacheOffheapBatchIndexingBaseTest.Organization.class},
            true);
    }

    /**
     *
     */
    @Test
    public void testPuAllSingleEntity() {
        doStreamerBatchTest(50,
            1_000,
            new Class<?>[] {Integer.class, CacheOffheapBatchIndexingBaseTest.Organization.class},
            false);
    }

    /**
     * @param iterations Number of iterations.
     * @param entitiesCnt Number of entities to put.
     * @param entityClasses Entity classes.

     * @param preloadInStreamer Data preload flag.
     */
    private void doStreamerBatchTest(int iterations,
        int entitiesCnt,
        Class<?>[] entityClasses,
        boolean preloadInStreamer) {
        Ignite ignite = grid(0);

        final IgniteCache<Object, Object> cache =
            ignite.createCache(cacheConfiguration(entityClasses));

        try {
            if (preloadInStreamer)
                preload(cache.getName());

            while (iterations-- >= 0) {
                Map<Integer, Organization> putMap1 = new TreeMap<>();

                for (int i = 0; i < entitiesCnt; i++)
                    putMap1.put(i, new Organization(i, String.valueOf(i)));

                cache.putAll(putMap1);

                Map<Integer, Organization> putMap2 = new TreeMap<>();

                for (int i = entitiesCnt / 2; i < entitiesCnt * 3 / 2; i++) {
                    cache.remove(i);

                    putMap2.put(i, new Organization(i, String.valueOf(i)));
                }

                cache.putAll(putMap2);
            }
        }
        finally {
            cache.destroy();
        }
    }
}
