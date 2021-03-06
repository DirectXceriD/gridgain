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

package org.apache.ignite.cache.store.cassandra.session;

import com.datastax.driver.core.Row;
import com.datastax.driver.core.SimpleStatement;
import com.datastax.driver.core.Statement;
import java.util.concurrent.Callable;
import org.apache.ignite.IgniteException;
import org.apache.ignite.IgniteLogger;
import org.apache.ignite.cache.store.cassandra.persistence.PersistenceController;
import org.apache.ignite.lang.IgniteBiInClosure;

/**
 * Worker for load cache using custom user query.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 */
public class LoadCacheCustomQueryWorker<K, V> implements Callable<Void> {
    /** Cassandra session to execute CQL query */
    private final CassandraSession ses;

    /** Statement. */
    private final Statement stmt;

    /** Persistence controller */
    private final PersistenceController ctrl;

    /** Logger */
    private final IgniteLogger log;

    /** Closure for loaded values. */
    private final IgniteBiInClosure<K, V> clo;

    /**
     * @param ses Session.
     * @param qry Query.
     * @param ctrl Control.
     * @param log Logger.
     * @param clo Closure for loaded values.
     */
    public LoadCacheCustomQueryWorker(CassandraSession ses, String qry, PersistenceController ctrl,
                                      IgniteLogger log, IgniteBiInClosure<K, V> clo) {
        this(ses, new SimpleStatement(qry.trim().endsWith(";") ? qry : qry + ';'), ctrl, log, clo);
    }

    /**
     * @param ses Session.
     * @param stmt Statement.
     * @param ctrl Control.
     * @param log Logger.
     * @param clo Closure for loaded values.
     */
    public LoadCacheCustomQueryWorker(CassandraSession ses, Statement stmt, PersistenceController ctrl,
                                      IgniteLogger log, IgniteBiInClosure<K, V> clo) {
        this.ses = ses;
        this.stmt = stmt;
        this.ctrl = ctrl;
        this.log = log;
        this.clo = clo;
    }

    /** {@inheritDoc} */
    @Override public Void call() throws Exception {
        ses.execute(new BatchLoaderAssistant() {
            /** {@inheritDoc} */
            @Override public String operationName() {
                return "loadCache";
            }

            /** {@inheritDoc} */
            @Override public Statement getStatement() {
                return stmt;
            }

            /** {@inheritDoc} */
            @Override public void process(Row row) {
                K key;
                V val;

                try {
                    key = (K)ctrl.buildKeyObject(row);
                }
                catch (Throwable e) {
                    log.error("Failed to build Ignite key object from provided Cassandra row", e);

                    throw new IgniteException("Failed to build Ignite key object from provided Cassandra row", e);
                }

                try {
                    val = (V)ctrl.buildValueObject(row);
                }
                catch (Throwable e) {
                    log.error("Failed to build Ignite value object from provided Cassandra row", e);

                    throw new IgniteException("Failed to build Ignite value object from provided Cassandra row", e);
                }

                clo.apply(key, val);
            }
        });

        return null;
    }
}
