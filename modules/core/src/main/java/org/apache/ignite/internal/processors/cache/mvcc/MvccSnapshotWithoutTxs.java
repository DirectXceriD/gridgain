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

import java.nio.ByteBuffer;
import org.apache.ignite.internal.managers.communication.GridIoMessageFactory;
import org.apache.ignite.internal.util.typedef.internal.S;
import org.apache.ignite.plugin.extensions.communication.MessageReader;
import org.apache.ignite.plugin.extensions.communication.MessageWriter;

/**
 *
 */
public class MvccSnapshotWithoutTxs implements MvccSnapshot {
    /** */
    private static final long serialVersionUID = 0L;

    /** */
    private long crdVer;

    /** */
    private long cntr;

    /** */
    private long cleanupVer;

    /** */
    private int opCntr;

    /**
     * Required by {@link GridIoMessageFactory}.
     */
    public MvccSnapshotWithoutTxs() {
        // No-op.
    }

    /**
     * @param crdVer Coordinator version.
     * @param cntr Counter.
     * @param cleanupVer Cleanup version.
     */
    public MvccSnapshotWithoutTxs(long crdVer, long cntr, int opCntr, long cleanupVer) {
        this.crdVer = crdVer;
        this.cntr = cntr;
        this.cleanupVer = cleanupVer;
        this.opCntr = opCntr;
    }

    /** {@inheritDoc} */
    @Override public MvccLongList activeTransactions() {
        return MvccEmptyLongList.INSTANCE;
    }

    /** {@inheritDoc} */
    @Override public long coordinatorVersion() {
        return crdVer;
    }

    /** {@inheritDoc} */
    @Override public long cleanupVersion() {
        return cleanupVer;
    }

    /** {@inheritDoc} */
    @Override public long counter() {
        return cntr;
    }

    /** {@inheritDoc} */
    @Override public int operationCounter() {
        return opCntr;
    }

    /** {@inheritDoc} */
    @Override public void incrementOperationCounter() {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc} */
    @Override public MvccSnapshot withoutActiveTransactions() {
        return this;
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
                if (!writer.writeLong("cleanupVer", cleanupVer))
                    return false;

                writer.incrementState();

            case 1:
                if (!writer.writeLong("cntr", cntr))
                    return false;

                writer.incrementState();

            case 2:
                if (!writer.writeLong("crdVer", crdVer))
                    return false;

                writer.incrementState();

            case 3:
                if (!writer.writeInt("opCntr", opCntr))
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
                cleanupVer = reader.readLong("cleanupVer");

                if (!reader.isLastRead())
                    return false;

                reader.incrementState();

            case 1:
                cntr = reader.readLong("cntr");

                if (!reader.isLastRead())
                    return false;

                reader.incrementState();

            case 2:
                crdVer = reader.readLong("crdVer");

                if (!reader.isLastRead())
                    return false;

                reader.incrementState();

            case 3:
                opCntr = reader.readInt("opCntr");

                if (!reader.isLastRead())
                    return false;

                reader.incrementState();

        }

        return reader.afterMessageRead(MvccSnapshotWithoutTxs.class);
    }

    /** {@inheritDoc} */
    @Override public short directType() {
        return 150;
    }

    /** {@inheritDoc} */
    @Override public byte fieldsCount() {
        return 4;
    }

    /** {@inheritDoc} */
    @Override public void onAckReceived() {
        // No-op.
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(MvccSnapshotWithoutTxs.class, this);
    }
}
