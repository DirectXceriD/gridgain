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

import java.util.UUID;
import org.apache.ignite.internal.processors.affinity.AffinityTopologyVersion;
import org.apache.ignite.internal.processors.cluster.BaselineTopology;
import org.apache.ignite.internal.processors.cluster.BaselineTopologyHistoryItem;
import org.apache.ignite.internal.processors.cluster.ChangeGlobalStateMessage;
import org.apache.ignite.internal.util.typedef.internal.S;
import org.apache.ignite.lang.IgniteUuid;
import org.jetbrains.annotations.Nullable;

/**
 *
 */
public class StateChangeRequest {
    /** */
    private final ChangeGlobalStateMessage msg;

    /** */
    private final BaselineTopologyHistoryItem prevBltHistItem;

    /** */
    private final boolean activeChanged;

    /** */
    private final AffinityTopologyVersion topVer;

    /**
     * @param msg Message.
     * @param topVer State change topology versoin.
     */
    public StateChangeRequest(ChangeGlobalStateMessage msg,
        BaselineTopologyHistoryItem bltHistItem,
        boolean activeChanged,
        AffinityTopologyVersion topVer) {
        this.msg = msg;
        prevBltHistItem = bltHistItem;
        this.activeChanged = activeChanged;
        this.topVer = topVer;
    }

    /**
     * @return State change exchange version.
     */
    public AffinityTopologyVersion topologyVersion() {
        return topVer;
    }

    /**
     * @return State change message ID.
     */
    public IgniteUuid id() {
        return msg.id();
    }

    /**
     * @return State change request ID.
     */
    public UUID requestId() {
        return msg.requestId();
    }

    /**
     * @return New state.
     */
    public boolean activate() {
        return msg.activate();
    }

    /**
     * @return {@code True} if active state was changed.
     */
    public boolean activeChanged() {
        return activeChanged;
    }

    /**
     * @return Previous baseline topology.
     */
    @Nullable public BaselineTopologyHistoryItem prevBaselineTopologyHistoryItem() {
        return prevBltHistItem;
    }

    /**
     * @return Baseline topology.
     */
    @Nullable public BaselineTopology baselineTopology() {
        return msg.baselineTopology();
    }

    /**
     * @return Node initiated state change process.
     */
    public UUID initiatorNodeId() {
        return msg.initiatorNodeId();
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(StateChangeRequest.class, this);
    }
}
