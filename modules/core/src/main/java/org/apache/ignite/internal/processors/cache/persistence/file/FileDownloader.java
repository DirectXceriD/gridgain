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

package org.apache.ignite.internal.processors.cache.persistence.file;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.FileChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.ignite.IgniteCheckedException;
import org.apache.ignite.IgniteException;
import org.apache.ignite.IgniteLogger;
import org.apache.ignite.internal.IgniteInternalFuture;
import org.apache.ignite.internal.util.future.GridFutureAdapter;
import org.apache.ignite.internal.util.lang.IgniteInClosureX;
import org.apache.ignite.internal.util.typedef.internal.U;

/**
 * Part of direct node to node file downloading
 */
public class FileDownloader {
    /** */
    private final IgniteLogger log;

    /** */
    private static final int CHUNK_SIZE = 1024 * 1024;

    /** */
    private final Path path;

    /** */
    private final AtomicLong size = new AtomicLong(-1);

    /** */
    private ServerSocketChannel serverChannel;

    /** */
    private volatile GridFutureAdapter<?> finishFut;

    /** */
    private final GridFutureAdapter<?> initFut = new GridFutureAdapter<>();

    /**
     *
     */
    public FileDownloader(IgniteLogger log, Path path) {
        this.log = log;
        this.path = path;
    }

    /**
     *
     */
    public InetSocketAddress start() throws IgniteCheckedException {
        try {
            ServerSocketChannel ch = ServerSocketChannel.open();

            ch.bind(null);

            serverChannel = ch;

            return (InetSocketAddress)ch.getLocalAddress();
        }
        catch (Exception ex) {
            throw new IgniteCheckedException(ex);
        }
    }

    /**
     *
     */
    public void download(GridFutureAdapter<?> fut){
        this.finishFut = fut;

        final ServerSocketChannel ch = serverChannel;

        fut.listen(new IgniteInClosureX<IgniteInternalFuture<?>>() {
            @Override public void applyx(IgniteInternalFuture<?> future) throws IgniteCheckedException {
                try {

                    if (log != null && log.isInfoEnabled())
                        log.info("Server socket closed " + ch.getLocalAddress());

                    ch.close();
                }
                catch (Exception ex) {
                    U.error(log, "Fail close socket.", ex);

                    throw new IgniteCheckedException(ex);
                }
            }
        });

        FileChannel writeChannel = null;
        SocketChannel readChannel = null;

        try {
            File f = new File(path.toUri().getPath());

            if (f.exists())
                f.delete();

            File cacheWorkDir = f.getParentFile();

            if (!cacheWorkDir.exists())
                cacheWorkDir.mkdir();

            writeChannel = FileChannel.open(path, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);

            initFut.onDone();

            readChannel = serverChannel.accept();

            long pos = 0;

            long size = this.size.get();

            while (size == -1 || pos < size) {
                pos += writeChannel.transferFrom(readChannel, pos, CHUNK_SIZE);

                if (size == -1)
                    size = this.size.get();
            }
        }
        catch (IOException ex) {
            initFut.onDone(ex);

            fut.onDone(ex);
        }
        finally {
            try {
                if (writeChannel != null)
                    writeChannel.close();
            }
            catch (IOException ex) {
                throw new IgniteException("Could not close file: " + path);
            }

            try {
                if (readChannel != null)
                    readChannel.close();
            }
            catch (IOException ex) {
                throw new IgniteException("Could not close socket");
            }
        }
    }

    /**
     *
     */
    public void download(long size, Throwable th) {
        try {
            initFut.get();

            if (th != null)
                finishFut.onDone(th);
            else {
                if (!this.size.compareAndSet(-1, size))
                    finishFut.onDone(new IgniteException("Size mismatch: " + this.size.get() + " != " + size));
                else
                    finishFut.onDone();
            }

        }
        catch (IgniteCheckedException e) {
            finishFut.onDone(e);
        }
    }
}
