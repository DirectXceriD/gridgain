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

package org.apache.ignite.tensorflow.cluster.spec;

import java.io.Serializable;
import java.util.Collection;
import java.util.UUID;
import org.apache.ignite.Ignite;

/**
 * TensorFlow server address specification.
 */
public class TensorFlowServerAddressSpec implements Serializable {
    /** */
    private static final long serialVersionUID = 7883701602323727681L;

    /** Node identifier. */
    private final UUID nodeId;

    /** Port. */
    private final int port;

    /**
     * Constructs a new instance of TensorFlow server address specification.
     *
     * @param nodeId Node identifier.
     * @param port Port.
     */
    TensorFlowServerAddressSpec(UUID nodeId, int port) {
        assert nodeId != null : "Node identifier should not be null";
        assert port >= 0 && port <= 0xFFFF : "Port should be between 0 and 65535";

        this.nodeId = nodeId;
        this.port = port;
    }

    /**
     * Formats Server Address specification so that TensorFlow accepts it.
     *
     * @param ignite Ignite instance.
     * @return Formatted server address specification.
     */
    public String format(Ignite ignite) {
        Collection<String> names = ignite.cluster().forNodeId(nodeId).hostNames();

        return names.iterator().next() + ":" + port;
    }

    /** */
    public UUID getNodeId() {
        return nodeId;
    }

    /** */
    public int getPort() {
        return port;
    }
}
