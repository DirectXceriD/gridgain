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

package org.apache.ignite.internal.processors.cache.mvcc;

import java.util.concurrent.atomic.AtomicLong;
import org.apache.ignite.internal.IgniteInternalFuture;
import org.apache.ignite.internal.processors.affinity.AffinityTopologyVersion;
import org.apache.ignite.internal.processors.cache.GridCacheContext;
import org.apache.ignite.internal.processors.cache.distributed.near.GridNearTxLocal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Mvcc tracker.
 */
public interface MvccQueryTracker {
    /** */
    public static final AtomicLong ID_CNTR = new AtomicLong();

    /** */
    public static final long MVCC_TRACKER_ID_NA = -1;

    /**
     * @return Tracker id.
     */
    public long id();

    /**
     * @return Requested MVCC snapshot.
     */
    public MvccSnapshot snapshot();

    /**
     * @return Cache context.
     */
    public GridCacheContext context();

    /**
     * @return Topology version.
     */
    public AffinityTopologyVersion topologyVersion();

    /**
     * Requests version on coordinator.
     *
     * @return Future to wait for result.
     */
    public IgniteInternalFuture<MvccSnapshot> requestSnapshot();

    /**
     * Requests version on coordinator.
     *
     * @param topVer Topology version.
     * @return Future to wait for result.
     */
    public IgniteInternalFuture<MvccSnapshot> requestSnapshot(@NotNull AffinityTopologyVersion topVer);

    /**
     * Requests version on coordinator.
     *
     * @param topVer Topology version.
     * @param lsnr Response listener.
     */
    public void requestSnapshot(@NotNull AffinityTopologyVersion topVer, @NotNull MvccSnapshotResponseListener lsnr);

    /**
     * Marks tracker as done.
     */
    public void onDone();

    /**
     * Marks tracker as done.
     *
     * @param tx Transaction.
     * @param commit Commit flag.
     * @return Acknowledge future.
     */
    @Nullable public IgniteInternalFuture<Void> onDone(@NotNull GridNearTxLocal tx, boolean commit);

    /**
     * Mvcc coordinator change callback.
     *
     * @param newCrd New mvcc coordinator.
     * @return Query id if exists.
     */
    long onMvccCoordinatorChange(MvccCoordinator newCrd);
}
