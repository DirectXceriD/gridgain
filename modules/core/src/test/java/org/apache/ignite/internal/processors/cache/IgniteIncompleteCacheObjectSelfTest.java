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

package org.apache.ignite.internal.processors.cache;

import org.apache.ignite.IgniteCheckedException;
import org.apache.ignite.plugin.extensions.communication.MessageReader;
import org.apache.ignite.plugin.extensions.communication.MessageWriter;
import org.apache.ignite.testframework.junits.common.GridCommonAbstractTest;
import org.jetbrains.annotations.Nullable;
import org.junit.Assert;

import java.nio.ByteBuffer;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Simple test for arbitrary CacheObject reading/writing.
 */
@RunWith(JUnit4.class)
public class IgniteIncompleteCacheObjectSelfTest extends GridCommonAbstractTest {
    /**
     * Test case when requested data cut on cache object header.
     *
     * @throws Exception If failed.
     */
    @Test
    public void testIncompleteObject() throws Exception {
        final byte[] data = new byte[1024];

        ThreadLocalRandom.current().nextBytes(data);

        final ByteBuffer dataBuf = ByteBuffer.allocate(IncompleteCacheObject.HEAD_LEN + data.length);

        int off = 0;
        int len = 3;

        final TestCacheObject obj = new TestCacheObject((byte) 1);

        // Write part of the cache object and cut on header (3 bytes instead of 5)
        assert CacheObjectAdapter.putValue(obj.cacheObjectType(), dataBuf, off, len, data, 0);

        off += len;
        len = IncompleteCacheObject.HEAD_LEN - len + data.length;

        // Write rest data.
        assert CacheObjectAdapter.putValue(obj.cacheObjectType(), dataBuf, off, len, data, 0);

        assert !dataBuf.hasRemaining() : "Not all data were written.";

        dataBuf.clear();

        // Cut on header for reading.
        dataBuf.limit(3);

        final IncompleteCacheObject incompleteObj = new IncompleteCacheObject(dataBuf);

        incompleteObj.readData(dataBuf);

        assert !incompleteObj.isReady();

        assert !dataBuf.hasRemaining() : "Data were read incorrectly.";

        // Make rest data available.
        dataBuf.limit(dataBuf.capacity());

        incompleteObj.readData(dataBuf);

        assert incompleteObj.isReady();

        // Check that cache object data assembled correctly.
        assertEquals(obj.cacheObjectType(), incompleteObj.type());
        Assert.assertArrayEquals(data, incompleteObj.data());
    }

    /**
     *
     */
    private static class TestCacheObject implements CacheObject {
        /** */
        private static final long serialVersionUID = 0L;

        /** */
        private final byte type;

        /**
         * @param type Cache object type.
         */
        private TestCacheObject(final byte type) {
            this.type = type;
        }

        /** {@inheritDoc} */
        @Nullable @Override public <T> T value(final CacheObjectValueContext ctx, final boolean cpy) {
            return null;
        }

        /** {@inheritDoc} */
        @Override public byte[] valueBytes(final CacheObjectValueContext ctx) throws IgniteCheckedException {
            return new byte[0];
        }

        /** {@inheritDoc} */
        @Override public int valueBytesLength(final CacheObjectContext ctx) throws IgniteCheckedException {
            return 0;
        }

        /** {@inheritDoc} */
        @Override public boolean putValue(final ByteBuffer buf) throws IgniteCheckedException {
            return false;
        }

        /** {@inheritDoc} */
        @Override public int putValue(long addr) throws IgniteCheckedException {
            throw new UnsupportedOperationException();
        }

        /** {@inheritDoc} */
        @Override public boolean putValue(final ByteBuffer buf, final int off, final int len)
            throws IgniteCheckedException {
            return false;
        }

        /** {@inheritDoc} */
        @Override public byte cacheObjectType() {
            return type;
        }

        /** {@inheritDoc} */
        @Override public boolean isPlatformType() {
            return false;
        }

        /** {@inheritDoc} */
        @Override public CacheObject prepareForCache(final CacheObjectContext ctx) {
            return null;
        }

        /** {@inheritDoc} */
        @Override public void finishUnmarshal(final CacheObjectValueContext ctx, final ClassLoader ldr)
            throws IgniteCheckedException {
            // No-op
        }

        /** {@inheritDoc} */
        @Override public void prepareMarshal(final CacheObjectValueContext ctx) throws IgniteCheckedException {
            // No-op
        }

        /** {@inheritDoc} */
        @Override public boolean writeTo(final ByteBuffer buf, final MessageWriter writer) {
            return false;
        }

        /** {@inheritDoc} */
        @Override public boolean readFrom(final ByteBuffer buf, final MessageReader reader) {
            return false;
        }

        /** {@inheritDoc} */
        @Override public short directType() {
            return 0;
        }

        /** {@inheritDoc} */
        @Override public byte fieldsCount() {
            return 0;
        }

        /** {@inheritDoc} */
        @Override public void onAckReceived() {
            // No-op
        }
    }
}
