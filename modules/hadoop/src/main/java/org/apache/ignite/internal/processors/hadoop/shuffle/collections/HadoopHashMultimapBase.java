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

package org.apache.ignite.internal.processors.hadoop.shuffle.collections;

import java.util.Iterator;
import org.apache.ignite.IgniteCheckedException;
import org.apache.ignite.IgniteException;
import org.apache.ignite.internal.processors.hadoop.HadoopJobInfo;
import org.apache.ignite.internal.processors.hadoop.HadoopSerialization;
import org.apache.ignite.internal.processors.hadoop.HadoopTaskContext;
import org.apache.ignite.internal.processors.hadoop.HadoopTaskInput;
import org.apache.ignite.internal.util.offheap.unsafe.GridUnsafeMemory;

/**
 * Base class for hash multimaps.
 */
public abstract class HadoopHashMultimapBase extends HadoopMultimapBase {
    /**
     * @param jobInfo Job info.
     * @param mem Memory.
     */
    protected HadoopHashMultimapBase(HadoopJobInfo jobInfo, GridUnsafeMemory mem) {
        super(jobInfo, mem);
    }

    /** {@inheritDoc} */
    @Override public boolean visit(boolean ignoreLastVisited, Visitor v) throws IgniteCheckedException {
        throw new UnsupportedOperationException("visit");
    }

    /** {@inheritDoc} */
    @Override public HadoopTaskInput input(HadoopTaskContext taskCtx) throws IgniteCheckedException {
        return new Input(taskCtx);
    }

    /**
     * @return Hash table capacity.
     */
    public abstract int capacity();

    /**
     * @param idx Index in hash table.
     * @return Meta page pointer.
     */
    protected abstract long meta(int idx);

    /**
     * @param meta Meta pointer.
     * @return Key hash.
     */
    protected int keyHash(long meta) {
        return mem.readInt(meta);
    }

    /**
     * @param meta Meta pointer.
     * @return Key size.
     */
    protected int keySize(long meta) {
        return mem.readInt(meta + 4);
    }

    /**
     * @param meta Meta pointer.
     * @return Key pointer.
     */
    protected long key(long meta) {
        return mem.readLong(meta + 8);
    }

    /**
     * @param meta Meta pointer.
     * @return Value pointer.
     */
    protected long value(long meta) {
        return mem.readLong(meta + 16);
    }
    /**
     * @param meta Meta pointer.
     * @param val Value pointer.
     */
    protected void value(long meta, long val) {
        mem.writeLong(meta + 16, val);
    }

    /**
     * @param meta Meta pointer.
     * @return Collision pointer.
     */
    protected long collision(long meta) {
        return mem.readLong(meta + 24);
    }

    /**
     * @param meta Meta pointer.
     * @param collision Collision pointer.
     */
    protected void collision(long meta, long collision) {
        assert meta != collision : meta;

        mem.writeLong(meta + 24, collision);
    }

    /**
     * Reader for key and value.
     */
    protected class Reader extends ReaderBase {
        /**
         * @param ser Serialization.
         */
        protected Reader(HadoopSerialization ser) {
            super(ser);
        }

        /**
         * @param meta Meta pointer.
         * @return Key.
         */
        public Object readKey(long meta) {
            assert meta > 0 : meta;

            try {
                return read(key(meta), keySize(meta));
            }
            catch (IgniteCheckedException e) {
                throw new IgniteException(e);
            }
        }
    }

    /**
     * Task input.
     */
    protected class Input implements HadoopTaskInput {
        /** */
        private int idx = -1;

        /** */
        private long metaPtr;

        /** */
        private final int cap;

        /** */
        private final Reader keyReader;

        /** */
        private final Reader valReader;

        /**
         * @param taskCtx Task context.
         * @throws IgniteCheckedException If failed.
         */
        public Input(HadoopTaskContext taskCtx) throws IgniteCheckedException {
            cap = capacity();

            keyReader = new Reader(taskCtx.keySerialization());
            valReader = new Reader(taskCtx.valueSerialization());
        }

        /** {@inheritDoc} */
        @Override public boolean next() {
            if (metaPtr != 0) {
                metaPtr = collision(metaPtr);

                if (metaPtr != 0)
                    return true;
            }

            while (++idx < cap) { // Scan table.
                metaPtr = meta(idx);

                if (metaPtr != 0)
                    return true;
            }

            return false;
        }

        /** {@inheritDoc} */
        @Override public Object key() {
            return keyReader.readKey(metaPtr);
        }

        /** {@inheritDoc} */
        @Override public Iterator<?> values() {
            return new ValueIterator(value(metaPtr), valReader);
        }

        /** {@inheritDoc} */
        @Override public void close() throws IgniteCheckedException {
            keyReader.close();
            valReader.close();
        }
    }
}