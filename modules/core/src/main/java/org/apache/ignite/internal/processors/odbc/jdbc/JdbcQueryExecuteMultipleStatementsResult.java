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

package org.apache.ignite.internal.processors.odbc.jdbc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.ignite.binary.BinaryObjectException;
import org.apache.ignite.internal.binary.BinaryReaderExImpl;
import org.apache.ignite.internal.binary.BinaryWriterExImpl;
import org.apache.ignite.internal.processors.odbc.ClientListenerProtocolVersion;
import org.apache.ignite.internal.util.typedef.internal.S;

/**
 * JDBC query execute result for query with multiple SQL statements.
 */
public class JdbcQueryExecuteMultipleStatementsResult extends JdbcResult {
    /** Statements results. */
    private List<JdbcResultInfo> results;

    /** Query result rows for the first query. */
    private List<List<Object>> items;

    /** Flag indicating the query has no unfetched results for the first query. */
    private boolean last;

    /**
     * Default constructor.
     */
    JdbcQueryExecuteMultipleStatementsResult() {
        super(QRY_EXEC_MULT);
    }

    /**
     * @param results Statements results.
     * @param items Query result rows for the first query.
     * @param last Flag indicating the query has no unfetched results for the first query.
     */
    public JdbcQueryExecuteMultipleStatementsResult(List<JdbcResultInfo> results,
        List<List<Object>> items, boolean last) {
        super(QRY_EXEC_MULT);
        this.results = results;
        this.items = items;
        this.last = last;
    }

    /**
     * @return Update counts of query IDs.
     */
    public List<JdbcResultInfo> results() {
        return results;
    }

    /**
     * @return Query result rows for the first query.
     */
    public List<List<Object>> items() {
        return items;
    }

    /**
     * @return Flag indicating the query has no unfetched results for the first query.
     */
    public boolean isLast() {
        return last;
    }

    /** {@inheritDoc} */
    @Override public void writeBinary(BinaryWriterExImpl writer,
        ClientListenerProtocolVersion ver) throws BinaryObjectException {
        super.writeBinary(writer, ver);

        if (results != null && !results.isEmpty()) {
            writer.writeInt(results.size());

            for (JdbcResultInfo r : results)
                r.writeBinary(writer, ver);

            if (results.get(0).isQuery()) {
                writer.writeBoolean(last);

                JdbcUtils.writeItems(writer, items);
            }
        }
        else
            writer.writeInt(0);
    }


    /** {@inheritDoc} */
    @Override public void readBinary(BinaryReaderExImpl reader,
        ClientListenerProtocolVersion ver) throws BinaryObjectException {
        super.readBinary(reader, ver);

        int cnt = reader.readInt();

        if (cnt == 0)
            results = Collections.emptyList();
        else {
            results = new ArrayList<>(cnt);

            for (int i = 0; i < cnt; ++i) {
                JdbcResultInfo r = new JdbcResultInfo();

                r.readBinary(reader, ver);

                results.add(r);
            }

            if (results.get(0).isQuery()) {
                last = reader.readBoolean();

                items = JdbcUtils.readItems(reader);
            }
        }
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(JdbcQueryExecuteMultipleStatementsResult.class, this);
    }
}
