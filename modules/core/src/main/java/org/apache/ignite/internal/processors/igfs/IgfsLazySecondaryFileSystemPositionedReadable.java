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

package org.apache.ignite.internal.processors.igfs;

import org.apache.ignite.igfs.IgfsPath;
import org.apache.ignite.igfs.secondary.IgfsSecondaryFileSystem;
import org.apache.ignite.igfs.secondary.IgfsSecondaryFileSystemPositionedReadable;
import org.apache.ignite.internal.util.typedef.internal.S;

import java.io.IOException;

/**
 * Lazy readable entity which is opened on demand.
 */
public class IgfsLazySecondaryFileSystemPositionedReadable implements IgfsSecondaryFileSystemPositionedReadable {
    /** File system. */
    private final IgfsSecondaryFileSystem fs;

    /** Path. */
    private final IgfsPath path;

    /** Buffer size. */
    private final int bufSize;

    /** Synchronization mutex. */
    private final Object mux = new Object();

    /** Target stream. */
    private IgfsSecondaryFileSystemPositionedReadable target;

    /**
     * Constructor.
     *
     * @param fs File system.
     * @param path Path.
     * @param bufSize Buffer size.
     */
    public IgfsLazySecondaryFileSystemPositionedReadable(IgfsSecondaryFileSystem fs, IgfsPath path, int bufSize) {
        assert fs != null;
        assert path != null;

        this.fs = fs;
        this.path = path;
        this.bufSize = bufSize;
    }

    /** {@inheritDoc} */
    @Override public int read(long pos, byte[] buf, int off, int len) throws IOException {
        synchronized (mux) {
            if (target == null)
                target = fs.open(path, bufSize);
        }

        return target.read(pos, buf, off, len);
    }

    /** {@inheritDoc} */
    @Override public void close() throws IOException {
        synchronized (mux) {
            if (target != null)
                target.close();
        }
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(IgfsLazySecondaryFileSystemPositionedReadable.class, this);
    }
}
