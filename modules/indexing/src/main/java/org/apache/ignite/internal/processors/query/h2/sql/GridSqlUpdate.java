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

package org.apache.ignite.internal.processors.query.h2.sql;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import org.h2.util.StatementBuilder;
import org.h2.util.StringUtils;

/** */
public class GridSqlUpdate extends GridSqlStatement {
    /** */
    private GridSqlElement target;

    /** */
    private ArrayList<GridSqlColumn> cols;

    /** */
    private LinkedHashMap<String, GridSqlElement> set;

    /** */
    private GridSqlElement where;

    /** */
    public GridSqlUpdate target(GridSqlElement target) {
        this.target = target;
        return this;
    }

    /** */
    public GridSqlElement target() {
        return target;
    }

    /** */
    public GridSqlUpdate cols(ArrayList<GridSqlColumn> cols) {
        this.cols = cols;
        return this;
    }

    /** */
    public ArrayList<GridSqlColumn> cols() {
        return cols;
    }


    /** */
    public GridSqlUpdate set(LinkedHashMap<String, GridSqlElement> set) {
        this.set = set;
        return this;
    }

    /** */
    public GridSqlUpdate where(GridSqlElement where) {
        this.where = where;
        return this;
    }

    /** */
    public GridSqlElement where() {
        return where;
    }

    /** */
    public LinkedHashMap<String, GridSqlElement> set() {
        return set;
    }

    /** {@inheritDoc} */
    @Override public String getSQL() {
        StatementBuilder buff = new StatementBuilder(explain() ? "EXPLAIN " : "");
        buff.append("UPDATE ")
            .append(target.getSQL())
            .append("\nSET\n");

        for (GridSqlColumn c : cols) {
            GridSqlElement e = set.get(c.columnName());
            buff.appendExceptFirst(",\n    ");
            buff.append(c.columnName()).append(" = ").append(e != null ? e.getSQL() : "DEFAULT");
        }

        if (where != null)
            buff.append("\nWHERE ").append(StringUtils.unEnclose(where.getSQL()));

        if (limit != null)
            buff.append("\nLIMIT ").append(StringUtils.unEnclose(limit.getSQL()));

        return buff.toString();
    }
}
