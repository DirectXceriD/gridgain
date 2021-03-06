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

package org.apache.ignite.internal.processors.odbc.odbc;

import org.apache.ignite.binary.BinaryRawWriter;
import org.apache.ignite.internal.binary.BinaryUtils;
import org.apache.ignite.internal.processors.odbc.ClientListenerProtocolVersion;
import org.apache.ignite.internal.processors.query.GridQueryFieldMetadata;
import org.apache.ignite.internal.util.typedef.internal.S;

/**
 * SQL listener column metadata.
 */
public class OdbcColumnMeta {
    /** Cache name. */
    private final String schemaName;

    /** Table name. */
    private final String tableName;

    /** Column name. */
    public final String columnName;

    /** Data type. */
    private final Class<?> dataType;

    /** Precision. */
    public final int precision;

    /** Scale. */
    public final int scale;

    /** Client version. */
    private final ClientListenerProtocolVersion ver;

    /**
     * @param schemaName Cache name.
     * @param tableName Table name.
     * @param columnName Column name.
     * @param dataType Data type.
     * @param precision Precision.
     * @param scale Scale.
     * @param ver Client version.
     */
    public OdbcColumnMeta(String schemaName, String tableName, String columnName, Class<?> dataType,
        int precision, int scale, ClientListenerProtocolVersion ver) {
        this.schemaName = OdbcUtils.addQuotationMarksIfNeeded(schemaName);
        this.tableName = tableName;
        this.columnName = columnName;
        this.dataType = dataType;
        this.precision = precision;
        this.scale = scale;
        this.ver = ver;
    }

    /**
     * @param info Field metadata.
     * @param ver Client version.
     */
    public OdbcColumnMeta(GridQueryFieldMetadata info, ClientListenerProtocolVersion ver) {
        this.schemaName = OdbcUtils.addQuotationMarksIfNeeded(info.schemaName());
        this.tableName = info.typeName();
        this.columnName = info.fieldName();
        this.precision = info.precision();
        this.scale = info.scale();
        this.ver = ver;

        Class<?> type;

        try {
            type = Class.forName(info.fieldTypeName());
        }
        catch (Exception ignored) {
            type = Object.class;
        }

        this.dataType = type;
    }

    /** {@inheritDoc} */
    @Override public int hashCode() {
        int hash = schemaName.hashCode();

        hash = 31 * hash + tableName.hashCode();
        hash = 31 * hash + columnName.hashCode();
        hash = 31 * hash + dataType.hashCode();
        hash = 31 * hash + Integer.hashCode(precision);
        hash = 31 * hash + Integer.hashCode(scale);

        return hash;
    }

    /** {@inheritDoc} */
    @Override public boolean equals(Object o) {
        if (o instanceof OdbcColumnMeta) {
            OdbcColumnMeta other = (OdbcColumnMeta) o;

            return this == other || schemaName.equals(other.schemaName) && tableName.equals(other.tableName) &&
                columnName.equals(other.columnName) && dataType.equals(other.dataType) &&
                precision == other.precision && scale == other.scale;
        }

        return false;
    }

    /**
     * Write in a binary format.
     *
     * @param writer Binary writer.
     */
    public void write(BinaryRawWriter writer) {
        writer.writeString(schemaName);
        writer.writeString(tableName);
        writer.writeString(columnName);

        byte typeId = BinaryUtils.typeByClass(dataType);

        writer.writeByte(typeId);

        if (ver.compareTo(OdbcConnectionContext.VER_2_7_0) >= 0) {
            writer.writeInt(precision);
            writer.writeInt(scale);
        }
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(OdbcColumnMeta.class, this);
    }
}
