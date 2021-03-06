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

package org.apache.ignite.internal.processors.cache.persistence.wal.serializer.io;

import java.io.IOException;
import java.nio.ByteBuffer;
import org.apache.ignite.IgniteCheckedException;
import org.apache.ignite.internal.pagemem.wal.WALPointer;
import org.apache.ignite.internal.pagemem.wal.record.WALRecord;
import org.apache.ignite.internal.processors.cache.persistence.wal.ByteBufferBackedDataInput;

/**
 * Internal interface to provide size, read and write operations of WAL records
 * including record header and data.
 */
public interface RecordIO {
    /**
     * Calculates and returns size of record data and headers.
     *
     * @param record WAL record.
     * @return Size in bytes.
     * @throws IgniteCheckedException If it's unable to calculate size of record.
     */
    int sizeWithHeaders(WALRecord record) throws IgniteCheckedException;

    /**
     * Reads record data with headers from {@code in}.
     *
     * @param in Buffer to read.
     * @param expPtr Expected WAL pointer for record. Used to validate actual position against expected from the file.
     * @return WAL record.
     * @throws IOException In case of I/O problems.
     * @throws IgniteCheckedException If it's unable to read record.
     */
    WALRecord readWithHeaders(ByteBufferBackedDataInput in, WALPointer expPtr) throws IOException, IgniteCheckedException;

    /**
     * Writes record data with headers to {@code buf}.
     *
     * @param record WAL record.
     * @param buf Buffer to write.
     * @throws IgniteCheckedException If it's unable to write record.
     */
    void writeWithHeaders(WALRecord record, ByteBuffer buf) throws IgniteCheckedException;
}
