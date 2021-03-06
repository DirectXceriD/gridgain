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

package org.apache.ignite.internal.processors.query.h2.twostep.msg;

import java.util.Collection;
import java.util.Iterator;
import org.apache.ignite.IgniteCheckedException;
import org.apache.ignite.internal.GridKernalContext;
import org.apache.ignite.internal.processors.cache.query.QueryTable;
import org.apache.ignite.internal.processors.query.h2.opt.GridH2ValueCacheObject;
import org.apache.ignite.plugin.extensions.communication.Message;
import org.apache.ignite.plugin.extensions.communication.MessageFactory;
import org.h2.value.Value;
import org.jetbrains.annotations.Nullable;

/**
 * H2 Value message factory.
 */
public class GridH2ValueMessageFactory implements MessageFactory {
    /** {@inheritDoc} */
    @Nullable @Override public Message create(short type) {
        switch (type) {
            case -4:
                return GridH2Null.INSTANCE;

            case -5:
                return new GridH2Boolean();

            case -6:
                return new GridH2Byte();

            case -7:
                return new GridH2Short();

            case -8:
                return new GridH2Integer();

            case -9:
                return new GridH2Long();

            case -10:
                return new GridH2Decimal();

            case -11:
                return new GridH2Double();

            case -12:
                return new GridH2Float();

            case -13:
                return new GridH2Time();

            case -14:
                return new GridH2Date();

            case -15:
                return new GridH2Timestamp();

            case -16:
                return new GridH2Bytes();

            case -17:
                return new GridH2String();

            case -18:
                return new GridH2Array();

            case -19:
                return new GridH2JavaObject();

            case -20:
                return new GridH2Uuid();

            case -21:
                return new GridH2Geometry();

            case -22:
                return new GridH2CacheObject();

            case -30:
                return new GridH2IndexRangeRequest();

            case -31:
                return new GridH2IndexRangeResponse();

            case -32:
                return new GridH2RowMessage();

            case -33:
                return new GridH2QueryRequest();

            case -34:
                return new GridH2RowRange();

            case -35:
                return new GridH2RowRangeBounds();

            case -54:
                return new QueryTable();

            case -55:
                return new GridH2DmlRequest();

            case -56:
                return new GridH2DmlResponse();

            case -57:
                return new GridH2SelectForUpdateTxDetails();
        }

        return null;
    }

    /**
     * @param src Source values.
     * @param dst Destination collection.
     * @param cnt Number of columns to actually send.
     * @return Destination collection.
     * @throws IgniteCheckedException If failed.
     */
    public static Collection<Message> toMessages(Collection<Value[]> src, Collection<Message> dst, int cnt)
        throws IgniteCheckedException {
        for (Value[] row : src) {
            assert row.length >= cnt;

            for (int i = 0; i < cnt; i++)
                dst.add(toMessage(row[i]));
        }

        return dst;
    }

    /**
     * @param src Source iterator.
     * @param dst Array to fill with values.
     * @param ctx Kernal context.
     * @return Filled array.
     * @throws IgniteCheckedException If failed.
     */
    public static Value[] fillArray(Iterator<? extends Message> src, Value[] dst, GridKernalContext ctx)
        throws IgniteCheckedException {
        for (int i = 0; i < dst.length; i++) {
            Message msg = src.next();

            dst[i] = ((GridH2ValueMessage)msg).value(ctx);
        }

        return dst;
    }

    /**
     * @param v Value.
     * @return Message.
     * @throws IgniteCheckedException If failed.
     */
    public static GridH2ValueMessage toMessage(Value v) throws IgniteCheckedException {
        switch (v.getType()) {
            case Value.NULL:
                return GridH2Null.INSTANCE;

            case Value.BOOLEAN:
                return new GridH2Boolean(v);

            case Value.BYTE:
                return new GridH2Byte(v);

            case Value.SHORT:
                return new GridH2Short(v);

            case Value.INT:
                return new GridH2Integer(v);

            case Value.LONG:
                return new GridH2Long(v);

            case Value.DECIMAL:
                return new GridH2Decimal(v);

            case Value.DOUBLE:
                return new GridH2Double(v);

            case Value.FLOAT:
                return new GridH2Float(v);

            case Value.DATE:
                return new GridH2Date(v);

            case Value.TIME:
                return new GridH2Time(v);

            case Value.TIMESTAMP:
                return new GridH2Timestamp(v);

            case Value.BYTES:
                return new GridH2Bytes(v);

            case Value.STRING:
            case Value.STRING_FIXED:
            case Value.STRING_IGNORECASE:
                return new GridH2String(v);

            case Value.ARRAY:
                return new GridH2Array(v);

            case Value.JAVA_OBJECT:
                if (v instanceof GridH2ValueCacheObject)
                    return new GridH2CacheObject((GridH2ValueCacheObject)v);

                return new GridH2JavaObject(v);

            case Value.UUID:
                return new GridH2Uuid(v);

            case Value.GEOMETRY:
                return new GridH2Geometry(v);

            default:
                throw new IllegalStateException("Unsupported H2 type: " + v.getType());
        }
    }
}