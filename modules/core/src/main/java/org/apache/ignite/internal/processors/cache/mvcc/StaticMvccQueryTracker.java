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

import org.apache.ignite.internal.IgniteInternalFuture;
import org.apache.ignite.internal.processors.affinity.AffinityTopologyVersion;
import org.apache.ignite.internal.processors.cache.GridCacheContext;
import org.apache.ignite.internal.processors.cache.distributed.near.GridNearTxLocal;
import org.apache.ignite.internal.util.future.GridFinishedFuture;
import org.jetbrains.annotations.NotNull;

/**
 * Simple MVCC tracker used only as an Mvcc snapshot holder.
 */
public class StaticMvccQueryTracker implements MvccQueryTracker {
    /** */
    private final MvccSnapshot snapshot;
    /** */
    private final GridCacheContext cctx;

    /**
     * @param cctx Cache context.
     * @param snapshot Mvcc snapshot.
     */
    public StaticMvccQueryTracker(GridCacheContext cctx, MvccSnapshot snapshot) {
        this.snapshot = snapshot;
        this.cctx = cctx;
    }

    /** {@inheritDoc} */
    @Override public MvccSnapshot snapshot() {
        assert snapshot != null : this;

        return snapshot;
    }

    /** {@inheritDoc} */
    @Override public GridCacheContext context() {
        return cctx;
    }

    /** {@inheritDoc} */
    @Override public AffinityTopologyVersion topologyVersion() {
        return AffinityTopologyVersion.NONE;
    }

    /** {@inheritDoc} */
    @Override public IgniteInternalFuture<MvccSnapshot> requestSnapshot() {
        return new GridFinishedFuture<>(snapshot);
    }

    /** {@inheritDoc} */
    @Override public IgniteInternalFuture<MvccSnapshot> requestSnapshot(@NotNull final AffinityTopologyVersion topVer) {
        return new GridFinishedFuture<>(snapshot);
    }

    /** {@inheritDoc} */
    @Override public void requestSnapshot(@NotNull AffinityTopologyVersion topVer, @NotNull MvccSnapshotResponseListener lsnr) {
        lsnr.onResponse(snapshot);
    }

    /** {@inheritDoc} */
    @Override public void onDone() {
        // No-op.
    }

    /** {@inheritDoc} */
    @Override public IgniteInternalFuture<Void> onDone(@NotNull GridNearTxLocal tx, boolean commit) {
        throw new UnsupportedOperationException("Operation is not supported.");
    }

    /** {@inheritDoc} */
    @Override public long onMvccCoordinatorChange(MvccCoordinator newCrd) {
        return MVCC_TRACKER_ID_NA;
    }

    /** {@inheritDoc} */
    @Override public long id() {
        return MVCC_TRACKER_ID_NA;
    }
}
