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

package org.apache.ignite.spi;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import org.apache.ignite.IgniteException;
import org.apache.ignite.IgniteLogger;
import org.apache.ignite.internal.util.typedef.internal.U;

/**
 * Class implements forwarding between two addresses.
 */
public final class GridTcpForwarder implements AutoCloseable {
    /** */
    private final IgniteLogger log;

    /** */
    private final Thread mainThread;

    /** */
    private final ServerSocket inputSock;

    /**
     * @param fromAddr Source address.
     * @param fromPort Source port.
     * @param toAddr Destination address.
     * @param toPort Destination port.
     * @param log Logger.
     * @throws IOException if an I/O error occurs when opening the socket.
     */
    public GridTcpForwarder(
        final InetAddress fromAddr,
        final int fromPort,
        final InetAddress toAddr,
        final int toPort,
        final IgniteLogger log
    ) throws IOException {
        inputSock = new ServerSocket(fromPort, 0, fromAddr);

        this.log = log;

        mainThread = new Thread(new Runnable() {
            @Override public void run() {
                try {
                    boolean closed = false;

                    while (!closed) {
                        Socket inputCon = null;
                        Socket outputCon = null;

                        Thread forwardThread1 = null;
                        Thread forwardThread2 = null;

                        try {
                            inputCon = inputSock.accept();

                            outputCon = new Socket(toAddr, toPort);

                            forwardThread1 = new ForwardThread(
                                "ForwardThread [" + fromAddr + ":" + fromPort + "->" + toAddr + ":" + toPort + "]",
                                inputCon.getInputStream(), outputCon.getOutputStream()
                            );

                            forwardThread2 = new ForwardThread(
                                "ForwardThread [" + toAddr + ":" + toPort + "->" + fromAddr + ":" + fromPort + "]",
                                outputCon.getInputStream(), inputCon.getOutputStream()
                            );

                            //Force closing sibling if one of thread failed.
                            forwardThread1.setUncaughtExceptionHandler(new ForwarderExceptionHandler(forwardThread2));
                            forwardThread2.setUncaughtExceptionHandler(new ForwarderExceptionHandler(forwardThread1));

                            forwardThread1.start();
                            forwardThread2.start();

                            U.join(forwardThread1, log);
                            U.join(forwardThread2, log);
                        }
                        catch (IOException ignore) {
                            if (inputSock.isClosed())
                                closed = true;
                        }
                        catch (Throwable ignored) {
                            closed = true;
                        }
                        finally {
                            U.closeQuiet(outputCon);
                            U.closeQuiet(inputCon);

                            U.interrupt(forwardThread1);
                            U.interrupt(forwardThread2);

                            U.join(forwardThread1, log);
                            U.join(forwardThread2, log);
                        }
                    }
                }
                finally {
                    U.closeQuiet(inputSock);
                }
            }
        }, "GridTcpForwarder [" + fromAddr + ":" + fromPort + "->" + toAddr + ":" + toPort + "]");

        mainThread.start();
    }

    /** {@inheritDoc} */
    @Override public void close() throws Exception {
        U.closeQuiet(inputSock);

        U.interrupt(mainThread);
        U.join(mainThread, log);
    }

    /**
     *
     */
    private static class ForwarderExceptionHandler implements Thread.UncaughtExceptionHandler {
        /** */
        private Thread siblingThread;

        /** */
        public ForwarderExceptionHandler(Thread siblingThread) {

            this.siblingThread = siblingThread;
        }

        /** */
        @Override public void uncaughtException(Thread t, Throwable e) {
            siblingThread.interrupt();
        }
    }

    /**
     * Thread reads data from input stream and write to output stream.
     */
    private class ForwardThread extends Thread {
        /** */
        private final InputStream inputStream;

        /** */
        private final OutputStream outputStream;

        /**
         * @param name Thread name.
         * @param inputStream Input stream.
         * @param outputStream Output stream.
         */
        private ForwardThread(String name, InputStream inputStream, OutputStream outputStream) {
            super(name);

            this.inputStream = inputStream;
            this.outputStream = outputStream;
        }

        /** {@inheritDoc} */
        @Override public void run() {
            try {
                byte[] buf = new byte[1 << 17];

                while (true) {
                    int bytesRead = inputStream.read(buf);

                    if (bytesRead == -1)
                        break;

                    outputStream.write(buf, 0, bytesRead);
                    outputStream.flush();
                }
            }
            catch (IOException e) {
                log.error("IOException while forwarding data [threadName=" + getName() + "]", e);

                throw new IgniteException(e);
            }
        }
    }
}