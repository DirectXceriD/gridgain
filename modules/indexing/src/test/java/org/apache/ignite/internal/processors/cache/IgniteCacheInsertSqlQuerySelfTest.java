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
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.Callable;
import javax.cache.CacheException;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.cache.query.SqlFieldsQuery;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.testframework.GridTestUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.apache.ignite.internal.processors.cache.IgniteCacheUpdateSqlQuerySelfTest.AllTypes;

/**
 *
 */
@SuppressWarnings("unchecked")
@RunWith(JUnit4.class)
public class IgniteCacheInsertSqlQuerySelfTest extends IgniteCacheAbstractInsertSqlQuerySelfTest {
    /** {@inheritDoc} */
    @Override protected IgniteConfiguration getConfiguration(String igniteInstanceName) throws Exception {
        IgniteConfiguration cfg = super.getConfiguration(igniteInstanceName);

        cfg.setPeerClassLoadingEnabled(false);

        return cfg;
    }

    /**
     *
     */
    @Test
    public void testInsertWithExplicitKey() {
        IgniteCache<String, Person> p = ignite(0).cache("S2P").withKeepBinary();

        p.query(new SqlFieldsQuery("insert into Person (_key, id, firstName) values ('s', ?, ?), " +
            "('a', 2, 'Alex')").setArgs(1, "Sergi"));

        assertEquals(createPerson(1, "Sergi"), p.get("s"));

        assertEquals(createPerson(2, "Alex"), p.get("a"));
    }

    /**
     *
     */
    @Test
    public void testInsertFromSubquery() {
        IgniteCache p = ignite(0).cache("S2P").withKeepBinary();

        p.query(new SqlFieldsQuery("insert into String (_key, _val) values ('s', ?), " +
            "('a', ?)").setArgs("Sergi", "Alex").setLocal(true));

        assertEquals("Sergi", p.get("s"));
        assertEquals("Alex", p.get("a"));

        p.query(new SqlFieldsQuery("insert into Person(_key, id, firstName) " +
            "(select substring(lower(_val), 0, 2), cast(length(_val) as int), _val from String)"));

        assertEquals(createPerson(5, "Sergi"), p.get("se"));

        assertEquals(createPerson(4, "Alex"), p.get("al"));
    }

    /**
     *
     */
    @Test
    public void testInsertWithExplicitPrimitiveKey() {
        IgniteCache<Integer, Person> p = ignite(0).cache("I2P").withKeepBinary();

        p.query(new SqlFieldsQuery(
            "insert into Person (_key, id, firstName) values (cast('1' as int), ?, ?), (2, (5 - 3), 'Alex')")
            .setArgs(1, "Sergi"));

        assertEquals(createPerson(1, "Sergi"), p.get(1));

        assertEquals(createPerson(2, "Alex"), p.get(2));
    }

    /**
     *
     */
    @Test
    public void testInsertWithDynamicKeyInstantiation() {
        IgniteCache<Key, Person> p = ignite(0).cache("K2P").withKeepBinary();

        p.query(new SqlFieldsQuery(
            "insert into Person (key, id, firstName) values (1, ?, ?), (2, 2, 'Alex')").setArgs(1, "Sergi"));

        assertEquals(createPerson(1, "Sergi"), p.get(new Key(1)));

        assertEquals(createPerson(2, "Alex"), p.get(new Key(2)));
    }

    /**
     * Test insert with implicit column names.
     */
    @Test
    public void testImplicitColumnNames() {
        IgniteCache<Key, Person> p = ignite(0).cache("K2P").withKeepBinary();

        p.query(new SqlFieldsQuery(
            "insert into Person values (1, 1, 'Vova')")).getAll();

        assertEquals(createPerson(1, "Vova"), p.get(new Key(1)));

        p.query(new SqlFieldsQuery(
            "insert into Person values (2, 2, 'Sergi'), (3, 3, 'Alex')")).getAll();

        assertEquals(createPerson(2, "Sergi"), p.get(new Key(2)));
        assertEquals(createPerson(3, "Alex"), p.get(new Key(3)));
    }

    /**
     *
     */
    @Test
    public void testFieldsCaseSensitivity() {
        IgniteCache<Key2, Person> p = ignite(0).cache("K22P").withKeepBinary();

        p.query(new SqlFieldsQuery("insert into \"Person2\" (\"Id\", \"id\", \"firstName\", \"IntVal\") " +
            "values (1, ?, ?, 5),  (2, 3, 'Alex', 6)").setArgs(4, "Sergi"));

        assertEquals(createPerson2(4, "Sergi", 5), p.get(new Key2(1)));

        assertEquals(createPerson2(3, "Alex", 6), p.get(new Key2(2)));
    }

    /**
     *
     */
    @Test
    public void testPrimitives() {
        IgniteCache<Integer, Integer> p = ignite(0).cache("I2I");

        p.query(new SqlFieldsQuery("insert into Integer(_key, _val) values (1, ?), " +
            "(?, 4)").setArgs(2, 3));

        assertEquals(2, (int)p.get(1));

        assertEquals(4, (int)p.get(3));
    }

    /**
     *
     */
    @Test
    public void testDuplicateKeysException() {
        final IgniteCache<Integer, Integer> p = ignite(0).cache("I2I");

        p.clear();

        p.put(3, 5);

        GridTestUtils.assertThrows(log, new Callable<Void>() {
            /** {@inheritDoc} */
            @Override public Void call() throws Exception {
                p.query(new SqlFieldsQuery("insert into Integer(_key, _val) values (1, ?), " +
                    "(?, 4), (5, 6)").setArgs(2, 3));

                return null;
            }
        }, CacheException.class, "Failed to INSERT some keys because they are already in cache [keys=[3]]");

        assertEquals(2, (int)p.get(1));
        assertEquals(5, (int)p.get(3));
        assertEquals(6, (int)p.get(5));
    }

    /**
     *
     */
    @Test
    public void testUuidHandling() {
        IgniteCache<UUID, Integer> p = ignite(0).cache("U2I");

        UUID id = UUID.randomUUID();

        p.query(new SqlFieldsQuery("insert into Integer(_key, _val) values (?, ?)").setArgs(id, 1));

        assertEquals(1, (int)p.get(id));
    }

    /**
     *
     */
    @Test
    public void testNestedFieldsHandling() {
        IgniteCache<Integer, AllTypes> p = ignite(0).cache("I2AT");

        p.query(new SqlFieldsQuery(
            "insert into AllTypes(_key, innerTypeCol, arrListCol, _val, innerStrCol) values (1, ?, ?, ?, 'sss')").
            setArgs(
                new AllTypes.InnerType(50L),
                new ArrayList<>(Arrays.asList(3L, 2L, 1L)),
                new AllTypes(1L)
            )
        );

        AllTypes res = p.get(1);

        AllTypes.InnerType resInner = new AllTypes.InnerType(50L);

        resInner.innerStrCol = "sss";
        resInner.arrListCol = new ArrayList<>(Arrays.asList(3L, 2L, 1L));

        assertEquals(resInner, res.innerTypeCol);
    }

    /**
     * Check that few sequential start-stops of the cache do not affect work of DML.
     */
    @Test
    public void testCacheRestartHandling() {
        for (int i = 0; i < 4; i++) {
            IgniteCache<Integer, IgniteCacheUpdateSqlQuerySelfTest.AllTypes> p =
                ignite(0).getOrCreateCache(cacheConfig("I2AT", true, false, Integer.class,
                    IgniteCacheUpdateSqlQuerySelfTest.AllTypes.class));

            p.query(new SqlFieldsQuery("insert into AllTypes(_key, _val, dateCol) values (1, ?, null)")
                .setArgs(new IgniteCacheUpdateSqlQuerySelfTest.AllTypes(1L)));

            p.destroy();
        }
    }
}
