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

package org.apache.ignite.internal.binary.streams;

import org.apache.ignite.internal.util.GridUnsafe;

import static org.apache.ignite.internal.util.GridUnsafe.BIG_ENDIAN;

/**
 * Binary offheap output stream.
 */
public class BinaryOffheapOutputStream extends BinaryAbstractOutputStream {
    /** Pointer. */
    private long ptr;

    /** Length of bytes that cen be used before resize is necessary. */
    private int cap;

    /**
     * Constructor.
     *
     * @param cap Capacity.
     */
    public BinaryOffheapOutputStream(int cap) {
        this(0, cap);
    }

    /**
     * Constructor.
     *
     * @param ptr Pointer to existing address.
     * @param cap Capacity.
     */
    public BinaryOffheapOutputStream(long ptr, int cap) {
        this.ptr = ptr == 0 ? allocate(cap) : ptr;

        this.cap = cap;
    }

    /** {@inheritDoc} */
    @Override public void close() {
        release(ptr);
    }

    /** {@inheritDoc} */
    @Override public void ensureCapacity(int cnt) {
        if (cnt > cap) {
            int newCap = capacity(cap, cnt);

            ptr = reallocate(ptr, newCap);

            cap = newCap;
        }
    }

    /** {@inheritDoc} */
    @Override public byte[] array() {
        return arrayCopy();
    }

    /** {@inheritDoc} */
    @Override public byte[] arrayCopy() {
        byte[] res = new byte[pos];

        GridUnsafe.copyOffheapHeap(ptr, res, GridUnsafe.BYTE_ARR_OFF, pos);

        return res;
    }

    /**
     * @return Pointer.
     */
    public long pointer() {
        return ptr;
    }

    /** {@inheritDoc} */
    @Override public int capacity() {
        return cap;
    }

    /** {@inheritDoc} */
    @Override protected void writeByteAndShift(byte val) {
        GridUnsafe.putByte(ptr + pos++, val);
    }

    /** {@inheritDoc} */
    @Override protected void copyAndShift(Object src, long offset, int len) {
        GridUnsafe.copyHeapOffheap(src, offset, ptr + pos, len);

        shift(len);
    }

    /** {@inheritDoc} */
    @Override protected void writeShortFast(short val) {
        long addr = ptr + pos;

        if (BIG_ENDIAN)
            GridUnsafe.putShortLE(addr, val);
        else
            GridUnsafe.putShort(addr, val);
    }

    /** {@inheritDoc} */
    @Override protected void writeCharFast(char val) {
        long addr = ptr + pos;

        if (BIG_ENDIAN)
            GridUnsafe.putCharLE(addr, val);
        else
            GridUnsafe.putChar(addr, val);
    }

    /** {@inheritDoc} */
    @Override protected void writeIntFast(int val) {
        long addr = ptr + pos;

        if (BIG_ENDIAN)
            GridUnsafe.putIntLE(addr, val);
        else
            GridUnsafe.putInt(addr, val);
    }

    /** {@inheritDoc} */
    @Override protected void writeLongFast(long val) {
        long addr = ptr + pos;

        if (BIG_ENDIAN)
            GridUnsafe.putLongLE(addr, val);
        else
            GridUnsafe.putLong(addr, val);
    }

    /** {@inheritDoc} */
    @Override public boolean hasArray() {
        return false;
    }

    /** {@inheritDoc} */
    @Override public void unsafeWriteByte(byte val) {
        GridUnsafe.putByte(ptr + pos++, val);
    }

    /** {@inheritDoc} */
    @Override public void unsafeWriteShort(short val) {
        long addr = ptr + pos;

        if (BIG_ENDIAN)
            GridUnsafe.putShortLE(addr, val);
        else
            GridUnsafe.putShort(addr, val);

        shift(2);
    }

    /** {@inheritDoc} */
    @Override public void unsafeWriteShort(int pos, short val) {
        long addr = ptr + pos;

        if (BIG_ENDIAN)
            GridUnsafe.putShortLE(addr, val);
        else
            GridUnsafe.putShort(addr, val);
    }

    /** {@inheritDoc} */
    @Override public void unsafeWriteChar(char val) {
        long addr = ptr + pos;

        if (BIG_ENDIAN)
            GridUnsafe.putCharLE(addr, val);
        else
            GridUnsafe.putChar(addr, val);

        shift(2);
    }

    /** {@inheritDoc} */
    @Override public void unsafeWriteInt(int val) {
        long addr = ptr + pos;

        if (BIG_ENDIAN)
            GridUnsafe.putIntLE(addr, val);
        else
            GridUnsafe.putInt(addr, val);

        shift(4);
    }

    /** {@inheritDoc} */
    @Override public void unsafeWriteInt(int pos, int val) {
        long addr = ptr + pos;

        if (BIG_ENDIAN)
            GridUnsafe.putIntLE(addr, val);
        else
            GridUnsafe.putInt(addr, val);
    }

    /** {@inheritDoc} */
    @Override public void unsafeWriteLong(long val) {
        long addr = ptr + pos;

        if (BIG_ENDIAN)
            GridUnsafe.putLongLE(addr, val);
        else
            GridUnsafe.putLong(addr, val);

        shift(8);
    }

    /**
     * Allocate memory.
     *
     * @param cap Capacity.
     * @return Pointer.
     */
    protected long allocate(int cap) {
        return GridUnsafe.allocateMemory(cap);
    }

    /**
     * Reallocate memory.
     *
     * @param ptr Old pointer.
     * @param cap Capacity.
     * @return New pointer.
     */
    protected long reallocate(long ptr, int cap) {
        return GridUnsafe.reallocateMemory(ptr, cap);
    }

    /**
     * Release memory.
     *
     * @param ptr Pointer.
     */
    protected void release(long ptr) {
        GridUnsafe.freeMemory(ptr);
    }
}
