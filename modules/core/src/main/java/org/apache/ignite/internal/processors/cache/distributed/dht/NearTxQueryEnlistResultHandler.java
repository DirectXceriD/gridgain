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

package org.apache.ignite.internal.processors.cache.distributed.dht;

import java.util.UUID;
import org.apache.ignite.IgniteCheckedException;
import org.apache.ignite.internal.IgniteInternalFuture;
import org.apache.ignite.internal.processors.cache.GridCacheContext;
import org.apache.ignite.internal.processors.cache.distributed.near.GridNearTxQueryEnlistResponse;
import org.apache.ignite.internal.processors.cache.distributed.near.GridNearTxQueryResultsEnlistResponse;
import org.apache.ignite.internal.processors.cache.transactions.IgniteInternalTx;
import org.apache.ignite.internal.processors.cache.version.GridCacheVersion;
import org.apache.ignite.internal.util.lang.GridClosureException;
import org.apache.ignite.internal.util.typedef.CI1;
import org.apache.ignite.internal.util.typedef.internal.CU;
import org.apache.ignite.internal.util.typedef.internal.U;
import org.apache.ignite.lang.IgniteUuid;
import org.jetbrains.annotations.NotNull;

/**
 *
 */
public final class NearTxQueryEnlistResultHandler implements CI1<IgniteInternalFuture<Long>> {
    /** */
    private static final long serialVersionUID = 5189735824793607906L;

    /** */
    private static final NearTxQueryEnlistResultHandler INSTANCE = new NearTxQueryEnlistResultHandler();

    /** */
    private NearTxQueryEnlistResultHandler() {}

    /**
     * @return Handler instance.
     */
    public static NearTxQueryEnlistResultHandler instance() {
        return INSTANCE;
    }

    /**
     * @param future Enlist future.
     * @return Enlist response.
     */
    @SuppressWarnings("unchecked")
    public static <T extends GridNearTxQueryEnlistResponse> T createResponse(IgniteInternalFuture<?> future) {
        assert future != null;

        Class<?> clazz = future.getClass();

        if (clazz == GridDhtTxQueryResultsEnlistFuture.class)
            return (T)createResponse((GridDhtTxQueryResultsEnlistFuture)future);
        else if (clazz == GridDhtTxQueryEnlistFuture.class)
            return (T)createResponse((GridDhtTxQueryEnlistFuture)future);
        else
            throw new IllegalStateException();
    }

    /**
     * @param future Enlist future.
     * @return Enlist response.
     */
    @NotNull private static GridNearTxQueryEnlistResponse createResponse(GridDhtTxQueryEnlistFuture future) {
        try {
            future.get();

            assert future.tx.queryEnlisted() || future.cnt == 0;

            return new GridNearTxQueryEnlistResponse(future.cctx.cacheId(), future.nearFutId, future.nearMiniId,
                future.nearLockVer, future.cnt, future.tx.empty() && !future.tx.queryEnlisted(), future.newDhtNodes);
        }
        catch (IgniteCheckedException e) {
            return new GridNearTxQueryEnlistResponse(future.cctx.cacheId(), future.nearFutId, future.nearMiniId, future.nearLockVer, e);
        }
    }

    /**
     * @param fut Enlist future.
     * @return Enlist response.
     */
    @NotNull private static GridNearTxQueryResultsEnlistResponse createResponse(GridDhtTxQueryResultsEnlistFuture fut) {
        try {
            fut.get();

            GridCacheVersion ver = null;
            IgniteUuid id = null;

            if (fut.hasNearNodeUpdates) {
                ver = fut.cctx.tm().mappedVersion(fut.nearLockVer);

                id = fut.futId;
            }

            return new GridNearTxQueryResultsEnlistResponse(fut.cctx.cacheId(), fut.nearFutId, fut.nearMiniId,
                fut.nearLockVer, fut.cnt, ver, id, fut.newDhtNodes);
        }
        catch (IgniteCheckedException e) {
            return new GridNearTxQueryResultsEnlistResponse(fut.cctx.cacheId(), fut.nearFutId, fut.nearMiniId,
                fut.nearLockVer, e);
        }
    }

    /** {@inheritDoc} */
    @Override public void apply(IgniteInternalFuture<Long> fut0) {
        GridDhtTxAbstractEnlistFuture fut = (GridDhtTxAbstractEnlistFuture)fut0;

        GridCacheContext<?, ?> cctx = fut.cctx;
        GridDhtTxLocal tx = (GridDhtTxLocal)fut.tx;
        UUID nearNodeId = fut.nearNodeId;

        GridNearTxQueryEnlistResponse res = createResponse(fut);

        if (res.removeMapping()) {
            tx.forceSkipCompletedVersions();

            tx.rollbackDhtLocalAsync().listen(new CI1<IgniteInternalFuture<IgniteInternalTx>>() {
                @Override public void apply(IgniteInternalFuture<IgniteInternalTx> fut0) {
                    try {
                        cctx.io().send(nearNodeId, res, cctx.ioPolicy());
                    }
                    catch (IgniteCheckedException e) {
                        U.error(fut.log, "Failed to send near enlist response [" +
                            "tx=" + CU.txString(tx) +
                            ", node=" + nearNodeId +
                            ", res=" + res + ']', e);

                        throw new GridClosureException(e);
                    }
                }
            });

            return;
        }

        try {
            cctx.io().send(nearNodeId, res, cctx.ioPolicy());
        }
        catch (IgniteCheckedException e) {
            U.error(fut.log, "Failed to send near enlist response (will rollback transaction) [" +
                "tx=" + CU.txString(tx) +
                ", node=" + nearNodeId +
                ", res=" + res + ']', e);

            try {
                tx.rollbackDhtLocalAsync();
            }
            catch (Throwable e1) {
                e.addSuppressed(e1);
            }

            throw new GridClosureException(e);
        }
    }
}
