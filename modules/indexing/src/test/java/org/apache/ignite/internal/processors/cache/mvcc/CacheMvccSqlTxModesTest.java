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
package org.apache.ignite.internal.processors.cache.mvcc;

import java.util.concurrent.Callable;
import javax.cache.CacheException;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.cache.CacheMode;
import org.apache.ignite.cache.query.SqlFieldsQuery;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.internal.IgniteEx;
import org.apache.ignite.testframework.GridTestUtils;
import org.apache.ignite.transactions.Transaction;
import org.junit.Test;

import static org.apache.ignite.cache.CacheAtomicityMode.TRANSACTIONAL;
import static org.apache.ignite.cache.CacheAtomicityMode.TRANSACTIONAL_SNAPSHOT;
import static org.apache.ignite.cache.CacheMode.PARTITIONED;
import static org.apache.ignite.transactions.TransactionConcurrency.OPTIMISTIC;
import static org.apache.ignite.transactions.TransactionConcurrency.PESSIMISTIC;
import static org.apache.ignite.transactions.TransactionIsolation.READ_COMMITTED;
import static org.apache.ignite.transactions.TransactionIsolation.REPEATABLE_READ;
import static org.apache.ignite.transactions.TransactionIsolation.SERIALIZABLE;

/**
 *
 */
public class CacheMvccSqlTxModesTest extends CacheMvccAbstractTest {
    /** {@inheritDoc} */
    @Override protected CacheMode cacheMode() {
        return PARTITIONED;
    }

    /**
     * @throws Exception If failed
     */
    @Test
    public void testSqlTransactionModesNoMvcc() throws Exception {
        IgniteEx node = startGrid(0);

        IgniteCache<Object, Object> nonMvccCache = node.createCache(new CacheConfiguration<>("no-mvcc-cache")
            .setAtomicityMode(TRANSACTIONAL).setIndexedTypes(Integer.class, Integer.class));

        nonMvccCache.put(1,1);

        try (Transaction tx = node.transactions().txStart(OPTIMISTIC, READ_COMMITTED)) {
            nonMvccCache.query(new SqlFieldsQuery("SELECT * FROM Integer")).getAll();

            tx.commit();
        }

        try (Transaction tx = node.transactions().txStart(OPTIMISTIC, REPEATABLE_READ)) {
            nonMvccCache.query(new SqlFieldsQuery("SELECT * FROM Integer")).getAll();

            tx.commit();
        }

        try (Transaction tx = node.transactions().txStart(OPTIMISTIC, SERIALIZABLE)) {
            nonMvccCache.query(new SqlFieldsQuery("SELECT * FROM Integer")).getAll();

            tx.commit();
        }

        try (Transaction tx = node.transactions().txStart(PESSIMISTIC, READ_COMMITTED)) {
            nonMvccCache.query(new SqlFieldsQuery("SELECT * FROM Integer")).getAll();

            tx.commit();
        }

        try (Transaction tx = node.transactions().txStart(PESSIMISTIC, REPEATABLE_READ)) {
            nonMvccCache.query(new SqlFieldsQuery("SELECT * FROM Integer")).getAll();

            tx.commit();
        }

        try (Transaction tx = node.transactions().txStart(PESSIMISTIC, SERIALIZABLE)) {
            nonMvccCache.query(new SqlFieldsQuery("SELECT * FROM Integer")).getAll();

            tx.commit();
        }

        try (Transaction tx = node.transactions().txStart(OPTIMISTIC, REPEATABLE_READ)) {
            nonMvccCache.query(new SqlFieldsQuery("SELECT * FROM Integer").setLocal(true)).getAll();

            tx.commit();
        }

        try (Transaction tx = node.transactions().txStart(PESSIMISTIC, SERIALIZABLE)) {
            nonMvccCache.query(new SqlFieldsQuery("SELECT * FROM Integer").setLocal(true)).getAll();

            tx.commit();
        }
    }

    /**
     * @throws Exception If failed
     */
    @Test
    public void testSqlTransactionModesMvcc() throws Exception {
        IgniteEx node = startGrid(0);

        IgniteCache<Object, Object> mvccCache = node.createCache(new CacheConfiguration<>("mvcc-cache")
            .setAtomicityMode(TRANSACTIONAL_SNAPSHOT).setIndexedTypes(Integer.class, Integer.class));

        mvccCache.put(1,1);

        GridTestUtils.assertThrows(log, new Callable<Void>() {
            @Override public Void call() throws Exception {
                try (Transaction tx = node.transactions().txStart(OPTIMISTIC, READ_COMMITTED)) {
                    mvccCache.query(new SqlFieldsQuery("SELECT * FROM Integer")).getAll();

                    tx.commit();
                }

                return null;
            }
        }, CacheException.class, "Only pessimistic transactions are supported when MVCC is enabled");

        GridTestUtils.assertThrows(log, new Callable<Void>() {
            @Override public Void call() throws Exception {
                try (Transaction tx = node.transactions().txStart(OPTIMISTIC, REPEATABLE_READ)) {
                    mvccCache.query(new SqlFieldsQuery("SELECT * FROM Integer")).getAll();

                    tx.commit();
                }

                return null;
            }
        }, CacheException.class, "Only pessimistic transactions are supported when MVCC is enabled");

        GridTestUtils.assertThrows(log, new Callable<Void>() {
            @Override public Void call() throws Exception {
                try (Transaction tx = node.transactions().txStart(OPTIMISTIC, SERIALIZABLE)) {
                    mvccCache.query(new SqlFieldsQuery("SELECT * FROM Integer")).getAll();

                    tx.commit();
                }

                return null;
            }
        }, CacheException.class, "Only pessimistic transactions are supported when MVCC is enabled");

        try (Transaction tx = node.transactions().txStart(PESSIMISTIC, READ_COMMITTED)) {
            mvccCache.query(new SqlFieldsQuery("SELECT * FROM Integer")).getAll();

            tx.commit();
        }

        try (Transaction tx = node.transactions().txStart(PESSIMISTIC, REPEATABLE_READ)) {
            mvccCache.query(new SqlFieldsQuery("SELECT * FROM Integer")).getAll();

            tx.commit();
        }

        try (Transaction tx = node.transactions().txStart(PESSIMISTIC, SERIALIZABLE)) {
            mvccCache.query(new SqlFieldsQuery("SELECT * FROM Integer")).getAll();

            tx.commit();
        }
    }

    /**
     * @throws Exception If failed
     */
    @Test
    public void testConsequentMvccNonMvccOperations() throws Exception {
        IgniteEx node = startGrid(0);

        IgniteCache<Object, Object> mvccCache = node.createCache(new CacheConfiguration<>("mvcc-cache")
            .setAtomicityMode(TRANSACTIONAL_SNAPSHOT).setIndexedTypes(Integer.class, Integer.class));

        IgniteCache<Object, Object> nonMvccCache = node.createCache(new CacheConfiguration<>("no-mvcc-cache")
            .setAtomicityMode(TRANSACTIONAL).setIndexedTypes(Integer.class, Integer.class));

        try (Transaction tx = node.transactions().txStart(PESSIMISTIC, REPEATABLE_READ)) {
            nonMvccCache.put(1, 1);

            tx.commit();
        }

        try (Transaction tx = node.transactions().txStart(PESSIMISTIC, REPEATABLE_READ)) {
            mvccCache.query(new SqlFieldsQuery("INSERT INTO Integer (_key, _val) VALUES (3,3)")).getAll();

            tx.commit();
        }

        try (Transaction tx = node.transactions().txStart(PESSIMISTIC, REPEATABLE_READ)) {
            nonMvccCache.put(2, 2);

            tx.commit();
        }

        try (Transaction tx = node.transactions().txStart(PESSIMISTIC, REPEATABLE_READ)) {
            mvccCache.query(new SqlFieldsQuery("INSERT INTO Integer (_key, _val) VALUES (5,5)")).getAll();

            tx.commit();
        }
    }
}
