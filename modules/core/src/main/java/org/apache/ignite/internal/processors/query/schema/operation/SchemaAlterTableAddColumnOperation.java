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

package org.apache.ignite.internal.processors.query.schema.operation;

import java.util.List;
import java.util.UUID;
import org.apache.ignite.internal.processors.query.QueryField;
import org.apache.ignite.internal.util.typedef.internal.S;

/**
 * Schema index drop operation.
 */
public class SchemaAlterTableAddColumnOperation extends SchemaAbstractAlterTableOperation {
    /** */
    private static final long serialVersionUID = 0L;

    /** Target table name. */
    private final String tblName;

    /** Columns to add. */
    private final List<QueryField> cols;

    /** Ignore operation if target table doesn't exist. */
    private final boolean ifTblExists;

    /** Ignore operation if column exists. */
    private final boolean ifNotExists;

    /**
     * Constructor.
     *
     * @param opId Operation id.
     * @param schemaName Schema name.
     * @param tblName Target table name.
     * @param cols Columns to add.
     * @param ifTblExists Ignore operation if target table doesn't exist.
     * @param ifNotExists Ignore operation if column exists.
     */
    public SchemaAlterTableAddColumnOperation(UUID opId, String cacheName, String schemaName, String tblName,
        List<QueryField> cols, boolean ifTblExists, boolean ifNotExists) {
        super(opId, cacheName, schemaName);

        this.tblName = tblName;
        this.cols = cols;
        this.ifTblExists = ifTblExists;
        this.ifNotExists = ifNotExists;
    }

    /**
     * @return Ignore operation if table doesn't exist.
     */
    public boolean ifTableExists() {
        return ifTblExists;
    }

    /**
     * @return Columns to add.
     */
    public List<QueryField> columns() {
        return cols;
    }

    /**
     * @return Quietly abort this command if column exists (honored only in single column case).
     */
    public boolean ifNotExists() {
        return ifNotExists;
    }

    /**
     * @return Target table name.
     */
    public String tableName() {
        return tblName;
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(SchemaAlterTableAddColumnOperation.class, this, "parent", super.toString());
    }
}
