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

package org.apache.ignite.internal.processors.cache.query;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import org.apache.ignite.internal.util.typedef.F;
import org.apache.ignite.internal.util.typedef.internal.U;
import org.jetbrains.annotations.Nullable;

/**
 * Immutable query metrics key used to group metrics.
 */
class GridCacheQueryMetricsKey implements Externalizable {
    /** */
    private static final long serialVersionUID = 0L;

    /** */
    private GridCacheQueryType type;

    /** */
    private Class<?> cls;

    /** */
    private String clause;

    /**
     * Constructs key.
     *
     * @param type Query type.
     * @param cls Query return type.
     * @param clause Query clause.
     */
    GridCacheQueryMetricsKey(@Nullable GridCacheQueryType type,
        @Nullable Class<?> cls, @Nullable String clause) {
        this.type = type;
        this.cls = cls;
        this.clause = clause;
    }

    /**
     * Required by {@link Externalizable}.
     */
    public GridCacheQueryMetricsKey() {
        // No-op.
    }

    /**
     * @return Query type.
     */
    GridCacheQueryType type() {
        return type;
    }

    /**
     * @return Query return type.
     */
    Class<?> queryClass() {
        return cls;
    }

    /**
     * @return Query clause.
     */
    String clause() {
        return clause;
    }

    /** {@inheritDoc} */
    @Override public boolean equals(Object obj) {
        if (obj == this)
            return true;

        if (!(obj instanceof GridCacheQueryMetricsKey))
            return false;

        GridCacheQueryMetricsKey oth = (GridCacheQueryMetricsKey)obj;

        return oth.type() == type && F.eq(oth.queryClass(), cls) && F.eq(oth.clause(), clause);
    }

    /** {@inheritDoc} */
    @Override public int hashCode() {
        return (type != null ? type.ordinal() : -1) +
            31 * (cls != null ? cls.hashCode() : 0) +
            31 * 31 * (clause != null ? clause.hashCode() : 0);
    }

    /** {@inheritDoc} */
    @Override public void writeExternal(ObjectOutput out) throws IOException {
        U.writeEnum(out, type);
        out.writeObject(cls);
        U.writeString(out, clause);
    }

    /** {@inheritDoc} */
    @Override public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        type = GridCacheQueryType.fromOrdinal(in.readByte());
        cls = (Class<?>)in.readObject();
        clause = U.readString(in);
    }
}