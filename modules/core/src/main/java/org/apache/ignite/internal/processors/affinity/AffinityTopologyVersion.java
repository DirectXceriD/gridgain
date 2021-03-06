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

package org.apache.ignite.internal.processors.affinity;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.nio.ByteBuffer;
import org.apache.ignite.internal.util.typedef.internal.S;
import org.apache.ignite.plugin.extensions.communication.Message;
import org.apache.ignite.plugin.extensions.communication.MessageReader;
import org.apache.ignite.plugin.extensions.communication.MessageWriter;

/**
 *
 */
public class AffinityTopologyVersion implements Comparable<AffinityTopologyVersion>, Externalizable, Message {
    /** */
    private static final long serialVersionUID = 0L;

    /** */
    public static final AffinityTopologyVersion NONE = new AffinityTopologyVersion(-1, 0);

    /** */
    public static final AffinityTopologyVersion ZERO = new AffinityTopologyVersion(0, 0);

    /** */
    private long topVer;

    /** */
    private int minorTopVer;

    /**
     * Empty constructor required by {@link Externalizable}.
     */
    public AffinityTopologyVersion() {
        // No-op.
    }

    /**
     * @param topVer Topology version.
     */
    public AffinityTopologyVersion(long topVer) {
        this.topVer = topVer;
    }

    /**
     * @param topVer Topology version.
     * @param minorTopVer Minor topology version.
     */
    public AffinityTopologyVersion(
        long topVer,
        int minorTopVer
    ) {
        this.topVer = topVer;
        this.minorTopVer = minorTopVer;
    }

    /**
     * @return {@code True} if this is real topology version (neither {@link #NONE} nor {@link #ZERO}.
     */
    public boolean initialized() {
        return topVer > 0;
    }

    /**
     * @return Topology version with incremented minor version.
     */
    public AffinityTopologyVersion nextMinorVersion() {
        assert topVer > 0;

        return new AffinityTopologyVersion(topVer, minorTopVer + 1);
    }

    /**
     * @return Topology version.
     */
    public long topologyVersion() {
        return topVer;
    }

    /**
     * @return Minor topology version.
     */
    public int minorTopologyVersion() {
        return minorTopVer;
    }

    /** {@inheritDoc} */
    @Override public int compareTo(AffinityTopologyVersion o) {
        int cmp = Long.compare(topVer, o.topVer);

        if (cmp == 0)
            return Integer.compare(minorTopVer, o.minorTopVer);

        return cmp;
    }

    /**
     * @param lower Lower bound.
     * @param upper Upper bound.
     * @return {@code True} if this topology version is within provided bounds (inclusive).
     */
    public boolean isBetween(AffinityTopologyVersion lower, AffinityTopologyVersion upper) {
        return compareTo(lower) >= 0 && compareTo(upper) <= 0;
    }

    /** {@inheritDoc} */
    @Override public void onAckReceived() {
        // No-op.
    }

    /** {@inheritDoc} */
    @Override public boolean equals(Object o) {
        if (this == o)
            return true;

        if (!(o instanceof AffinityTopologyVersion))
            return false;

        AffinityTopologyVersion that = (AffinityTopologyVersion)o;

        return minorTopVer == that.minorTopVer && topVer == that.topVer;
    }

    /** {@inheritDoc} */
    @Override public int hashCode() {
        return 31 * (int)topVer + minorTopVer;
    }

    /** {@inheritDoc} */
    @Override public void writeExternal(ObjectOutput out) throws IOException {
        out.writeLong(topVer);
        out.writeInt(minorTopVer);
    }

    /** {@inheritDoc} */
    @Override public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        topVer = in.readLong();
        minorTopVer = in.readInt();
    }

    /** {@inheritDoc} */
    @Override public boolean writeTo(ByteBuffer buf, MessageWriter writer) {
        writer.setBuffer(buf);

        if (!writer.isHeaderWritten()) {
            if (!writer.writeHeader(directType(), fieldsCount()))
                return false;

            writer.onHeaderWritten();
        }

        switch (writer.state()) {
            case 0:
                if (!writer.writeInt("minorTopVer", minorTopVer))
                    return false;

                writer.incrementState();

            case 1:
                if (!writer.writeLong("topVer", topVer))
                    return false;

                writer.incrementState();

        }

        return true;
    }

    /** {@inheritDoc} */
    @Override public boolean readFrom(ByteBuffer buf, MessageReader reader) {
        reader.setBuffer(buf);

        if (!reader.beforeMessageRead())
            return false;

        switch (reader.state()) {
            case 0:
                minorTopVer = reader.readInt("minorTopVer");

                if (!reader.isLastRead())
                    return false;

                reader.incrementState();

            case 1:
                topVer = reader.readLong("topVer");

                if (!reader.isLastRead())
                    return false;

                reader.incrementState();

        }

        return reader.afterMessageRead(AffinityTopologyVersion.class);
    }

    /** {@inheritDoc} */
    @Override public short directType() {
        return 111;
    }

    /** {@inheritDoc} */
    @Override public byte fieldsCount() {
        return 2;
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(AffinityTopologyVersion.class, this);
    }
}
