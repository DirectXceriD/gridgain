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

package org.apache.ignite.internal.client.router.impl;

import java.io.IOException;
import java.nio.ByteBuffer;
import org.apache.ignite.IgniteCheckedException;
import org.apache.ignite.internal.client.marshaller.GridClientMarshaller;
import org.apache.ignite.internal.processors.rest.client.message.GridClientHandshakeResponse;
import org.apache.ignite.internal.processors.rest.client.message.GridClientMessage;
import org.apache.ignite.internal.processors.rest.client.message.GridClientPingPacket;
import org.apache.ignite.internal.processors.rest.client.message.GridClientResponse;
import org.apache.ignite.internal.processors.rest.client.message.GridRouterRequest;
import org.apache.ignite.internal.processors.rest.client.message.GridRouterResponse;
import org.apache.ignite.internal.processors.rest.protocols.tcp.GridTcpRestParser;
import org.apache.ignite.internal.util.nio.GridNioSession;
import org.apache.ignite.internal.util.typedef.internal.U;

import static org.apache.ignite.internal.processors.rest.protocols.tcp.GridMemcachedMessage.IGNITE_REQ_FLAG;

/**
 *
 */
class GridTcpRouterNioParser extends GridTcpRestParser {
    /** Number of received messages. */
    private volatile long rcvCnt;

    /** Number of sent messages. */
    private volatile long sndCnt;

    /**
     */
    public GridTcpRouterNioParser() {
        super(false);
    }

    /** {@inheritDoc} */
    @Override protected GridClientMessage parseClientMessage(GridNioSession ses, ParserState state) {
        rcvCnt++;

        return new GridRouterRequest(
            state.buffer().toByteArray(),
            state.header().reqId(),
            state.header().clientId(),
            state.header().destinationId());
    }

    /** {@inheritDoc} */
    @Override public ByteBuffer encode(GridNioSession ses, Object msg) throws IOException, IgniteCheckedException {
        sndCnt++;

        if (msg instanceof GridRouterResponse) {
            GridRouterResponse resp = (GridRouterResponse)msg;

            ByteBuffer res = ByteBuffer.allocate(resp.body().length + 45);

            res.put(IGNITE_REQ_FLAG);
            res.putInt(resp.body().length + 40);
            res.putLong(resp.requestId());
            res.put(U.uuidToBytes(resp.clientId()));
            res.put(U.uuidToBytes(resp.destinationId()));
            res.put(resp.body());

            res.flip();

            return res;
        }
        else if (msg instanceof GridClientResponse) {
            GridClientMarshaller marsh = marshaller(ses);

            GridClientMessage clientMsg = (GridClientMessage)msg;

            ByteBuffer res = marsh.marshal(msg, 45);

            ByteBuffer slice = res.slice();

            slice.put(IGNITE_REQ_FLAG);
            slice.putInt(res.remaining() - 5);
            slice.putLong(clientMsg.requestId());
            slice.put(U.uuidToBytes(clientMsg.clientId()));
            slice.put(U.uuidToBytes(clientMsg.destinationId()));

            return res;
        }
        else if (msg instanceof GridClientPingPacket || msg instanceof GridClientHandshakeResponse)
            return super.encode(ses, msg);
        else
            throw new IgniteCheckedException("Unsupported message: " + msg);
    }

    /**
     * @return Number of received messages.
     */
    public long getReceivedCount() {
        return rcvCnt;
    }

    /**
     * @return Number of sent messages.
     */
    public long getSendCount() {
        return sndCnt;
    }
}