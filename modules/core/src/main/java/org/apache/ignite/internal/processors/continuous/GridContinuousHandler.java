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

package org.apache.ignite.internal.processors.continuous;

import java.io.Externalizable;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import org.apache.ignite.IgniteCheckedException;
import org.apache.ignite.internal.GridKernalContext;
import org.apache.ignite.internal.processors.affinity.AffinityTopologyVersion;
import org.apache.ignite.internal.util.typedef.T2;
import org.jetbrains.annotations.Nullable;

/**
 * Continuous routine handler.
 */
@SuppressWarnings("PublicInnerClass")
public interface GridContinuousHandler extends Externalizable, Cloneable {
    /**
     * Listener registration status.
     */
    public enum RegisterStatus {
        /** */
        REGISTERED,

        /** */
        NOT_REGISTERED,

        /** */
        DELAYED
    }

    /**
     * Registers listener.
     *
     * @param nodeId ID of the node that started routine.
     * @param routineId Routine ID.
     * @param ctx Kernal context.
     * @return Whether listener was actually registered.
     * @throws IgniteCheckedException In case of error.
     */
    public RegisterStatus register(UUID nodeId, UUID routineId, GridKernalContext ctx) throws IgniteCheckedException;

    /**
     * Unregisters listener.
     *
     * @param routineId Routine ID.
     * @param ctx Kernal context.
     */
    public void unregister(UUID routineId, GridKernalContext ctx);

    /**
     * Notifies local callback.
     *
     * @param nodeId ID of the node where notification came from.
     * @param routineId Routine ID.
     * @param objs Notification objects.
     * @param ctx Kernal context.
     */
    public void notifyCallback(UUID nodeId, UUID routineId, Collection<?> objs, GridKernalContext ctx);

    /**
     * Deploys and marshals inner objects (called only if peer deployment is enabled).
     *
     * @param ctx Kernal context.
     * @throws IgniteCheckedException In case of error.
     */
    public void p2pMarshal(GridKernalContext ctx) throws IgniteCheckedException;

    /**
     * Unmarshals inner objects (called only if peer deployment is enabled).
     *
     * @param nodeId Sender node ID.
     * @param ctx Kernal context.
     * @throws IgniteCheckedException In case of error.
     */
    public void p2pUnmarshal(UUID nodeId, GridKernalContext ctx) throws IgniteCheckedException;

    /**
     * Creates new batch.
     *
     * @return New batch.
     */
    public GridContinuousBatch createBatch();

    /**
     * Client node disconnected callback.
     */
    public void onClientDisconnected();

    /**
     * Called when ack for a batch is received from client.
     *
     * @param routineId Routine ID.
     * @param batch Acknowledged batch.
     * @param ctx Kernal context.
     */
    public void onBatchAcknowledged(UUID routineId, GridContinuousBatch batch, GridKernalContext ctx);

    /**
     * Node which started routine leave topology.
     */
    public void onNodeLeft();

    /**
     * @return Topic for ordered notifications. If {@code null}, notifications
     * will be sent in non-ordered messages.
     */
    @Nullable public Object orderedTopic();

    /**
     * Clones this handler.
     *
     * @return Clone of this handler.
     */
    public GridContinuousHandler clone();

    /**
     * @return {@code True} if for events.
     */
    public boolean isEvents();

    /**
     * @return {@code True} if for messaging.
     */
    public boolean isMessaging();

    /**
     * @return {@code True} if for continuous queries.
     */
    public boolean isQuery();

    /**
     * @return {@code True} if Ignite Binary objects should be passed to the listener and filter.
     */
    public boolean keepBinary();

    /**
     * @return Cache name if this is a continuous query handler.
     */
    public String cacheName();

    /**
     * @param cntrsPerNode Init state partition counters for node.
     * @param cntrs Init state for partition counters.
     * @param topVer Topology version.
     */
    public void updateCounters(AffinityTopologyVersion topVer, Map<UUID, Map<Integer, T2<Long, Long>>> cntrsPerNode,
        Map<Integer, T2<Long, Long>> cntrs);
}