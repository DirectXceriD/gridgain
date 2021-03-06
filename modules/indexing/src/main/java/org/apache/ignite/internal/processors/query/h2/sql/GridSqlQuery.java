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
import java.util.List;
import org.h2.util.StatementBuilder;
import org.h2.util.StringUtils;

/**
 * SQL Query AST.
 */
public abstract class GridSqlQuery extends GridSqlStatement implements GridSqlAst {
    /** */
    public static final int OFFSET_CHILD = 0;

    /** */
    public static final int LIMIT_CHILD = 1;

    /** */
    protected List<GridSqlSortColumn> sort = new ArrayList<>();

    /** */
    private GridSqlAst offset;

    /**
     * @return Offset.
     */
    public GridSqlAst offset() {
        return offset;
    }

    /**
     * @param offset Offset.
     */
    public void offset(GridSqlAst offset) {
        this.offset = offset;
    }

    /**
     * @return Sort.
     */
    public List<GridSqlSortColumn> sort() {
        return sort;
    }

    /**
     *
     */
    public void clearSort() {
        sort = new ArrayList<>();
    }

    /**
     * @param sortCol The sort column.
     */
    public void addSort(GridSqlSortColumn sortCol) {
        sort.add(sortCol);
    }

    /**
     * @return Number of visible columns.
     */
    protected abstract int visibleColumns();

    /**
     * @param col Column index.
     * @return Expression for column index.
     */
    protected abstract GridSqlAst column(int col);

    /** {@inheritDoc} */
    @Override public GridSqlType resultType() {
        return GridSqlType.RESULT_SET;
    }

    /** {@inheritDoc} */
    @Override public <E extends GridSqlAst> E child() {
        return child(0);
    }

    /** {@inheritDoc} */
    @Override public <E extends GridSqlAst> E child(int childIdx) {
        switch (childIdx) {
            case OFFSET_CHILD:
                return maskNull(offset, GridSqlPlaceholder.EMPTY);

            case LIMIT_CHILD:
                return maskNull(limit, GridSqlPlaceholder.EMPTY);

            default:
                throw new IllegalStateException("Child index: " + childIdx);
        }
    }

    /**
     * @param x Element.
     * @return Empty placeholder if the element is {@code null}.
     */
    @SuppressWarnings("unchecked")
    protected static <E extends GridSqlAst> E maskNull(GridSqlAst x, GridSqlAst dflt) {
        return (E)(x == null ? dflt : x);
    }

    /** {@inheritDoc} */
    @Override public <E extends GridSqlAst> void child(int childIdx, E child) {
        switch (childIdx) {
            case OFFSET_CHILD:
                offset = child;

                break;

            case LIMIT_CHILD:
                limit = child;

                break;

            default:
                throw new IllegalStateException("Child index: " + childIdx);
        }
    }

    /**
     * @return If this is a simple query with no conditions, expressions, sorting, etc...
     */
    public abstract boolean simpleQuery();

    /**
     * @param buff Statement builder.
     */
    protected void getSortLimitSQL(StatementBuilder buff) {
        if (!sort.isEmpty()) {
            buff.append("\nORDER BY ");

            int visibleCols = visibleColumns();

            buff.resetCount();

            for (GridSqlSortColumn col : sort) {
                buff.appendExceptFirst(", ");

                int idx = col.column();

                assert idx >= 0 : idx;

                if (idx < visibleCols)
                    buff.append(idx + 1);
                else {
                    GridSqlAst expr = column(idx);

                    if (expr == null) // For plain select should never be null, for union H2 itself can't parse query.
                        throw new IllegalStateException("Failed to build query: " + buff.toString());

                    if (expr instanceof GridSqlAlias)
                        expr = expr.child(0);

                    buff.append('=').append(StringUtils.unEnclose(expr.getSQL()));
                }

                if (!col.asc())
                    buff.append(" DESC");

                if (col.nullsFirst())
                    buff.append(" NULLS FIRST");
                else if (col.nullsLast())
                    buff.append(" NULLS LAST");
            }
        }

        if (limit != null)
            buff.append(" LIMIT ").append(StringUtils.unEnclose(limit.getSQL()));

        if (offset != null)
            buff.append(" OFFSET ").append(StringUtils.unEnclose(offset.getSQL()));
    }
}