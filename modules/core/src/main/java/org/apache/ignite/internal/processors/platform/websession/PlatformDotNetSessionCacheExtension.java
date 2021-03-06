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

package org.apache.ignite.internal.processors.platform.websession;

import org.apache.ignite.IgniteCheckedException;
import org.apache.ignite.internal.binary.BinaryRawReaderEx;
import org.apache.ignite.internal.binary.BinaryRawWriterEx;
import org.apache.ignite.internal.processors.platform.cache.PlatformCache;
import org.apache.ignite.internal.processors.platform.cache.PlatformCacheExtension;
import org.apache.ignite.internal.processors.platform.memory.PlatformMemory;
import org.apache.ignite.internal.processors.platform.utils.PlatformWriterClosure;
import org.apache.ignite.internal.util.typedef.internal.S;

import java.sql.Timestamp;
import java.util.UUID;

/**
 * Custom entry processor invoker.
 */
public class PlatformDotNetSessionCacheExtension implements PlatformCacheExtension {
    /** Extension ID. */
    private static final int EXT_ID = 0;

    /** Operation: session lock. */
    private static final int OP_LOCK = 1;

    /** Operation: session set/unlock. */
    private static final int OP_SET_AND_UNLOCK = 2;

    /** Operation: session get without lock. */
    private static final int OP_GET = 3;

    /** Operation: session put without lock. */
    private static final int OP_PUT = 4;

    /** Operation: session remove without lock. */
    private static final int OP_REMOVE = 5;

    /** {@inheritDoc} */
    @Override public int id() {
        return EXT_ID;
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override public long processInOutStreamLong(PlatformCache target, int type, BinaryRawReaderEx reader,
        PlatformMemory mem) throws IgniteCheckedException {
        switch (type) {
            case OP_LOCK: {
                String key = reader.readString();
                UUID lockNodeId = reader.readUuid();
                long lockId = reader.readLong();
                Timestamp lockTime = reader.readTimestamp();

                final PlatformDotNetSessionLockResult res = (PlatformDotNetSessionLockResult)
                    target.rawCache().invoke(key, new PlatformDotNetSessionLockProcessor(lockNodeId, lockId, lockTime));

                return target.writeResult(mem, res, new PlatformWriterClosure() {
                    @Override public void write(BinaryRawWriterEx writer, Object val) {
                        res.writeBinary(writer);
                    }
                });
            }

            case OP_SET_AND_UNLOCK: {
                String key = reader.readString();

                PlatformDotNetSessionSetAndUnlockProcessor proc;

                if (reader.readBoolean()) {
                    PlatformDotNetSessionData data = new PlatformDotNetSessionData();

                    data.readBinary(reader);

                    proc = new PlatformDotNetSessionSetAndUnlockProcessor(data);
                }
                else {
                    UUID lockNodeId = reader.readUuid();
                    long lockId = reader.readLong();

                    proc = new PlatformDotNetSessionSetAndUnlockProcessor(lockNodeId, lockId);
                }

                target.rawCache().invoke(key, proc);

                return target.writeResult(mem, null);
            }

            case OP_GET: {
                String key = reader.readString();

                final PlatformDotNetSessionData data = (PlatformDotNetSessionData)target.rawCache().get(key);

                return target.writeResult(mem, data, new PlatformWriterClosure() {
                    @Override public void write(BinaryRawWriterEx writer, Object val) {
                        data.writeBinary(writer);
                    }
                });
            }

            case OP_PUT: {
                String key = reader.readString();

                PlatformDotNetSessionData data = new PlatformDotNetSessionData();

                data.readBinary(reader);

                target.rawCache().put(key, data);

                return target.writeResult(mem, null);
            }

            case OP_REMOVE: {
                String key = reader.readString();

                target.rawCache().remove(key);

                return target.writeResult(mem, null);
            }
        }

        throw new IgniteCheckedException("Unsupported operation type: " + type);
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(PlatformDotNetSessionCacheExtension.class, this);
    }
}
