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

package org.apache.ignite.internal.processors.cache.distributed.near;

import org.apache.ignite.IgniteCheckedException;
import org.apache.ignite.internal.IgniteInternalFuture;
import org.apache.ignite.internal.processors.cache.mvcc.MvccQueryTracker;
import org.apache.ignite.internal.processors.cache.mvcc.MvccSnapshot;
import org.apache.ignite.internal.processors.cache.transactions.IgniteInternalTx;
import org.apache.ignite.internal.util.future.GridFutureAdapter;
import org.apache.ignite.internal.util.typedef.internal.S;
import org.apache.ignite.lang.IgniteInClosure;

/**
 *
 */
public class GridNearTxFinishAndAckFuture extends GridFutureAdapter<IgniteInternalTx> implements NearTxFinishFuture {
    /** */
    private final NearTxFinishFuture finishFut;

    /**
     * @param finishFut Finish future.
     */
    GridNearTxFinishAndAckFuture(NearTxFinishFuture finishFut) {
        this.finishFut = finishFut;
    }

    /** {@inheritDoc} */
    @Override public boolean commit() {
        return finishFut.commit();
    }

    /** {@inheritDoc} */
    @Override public GridNearTxLocal tx() {
        return finishFut.tx();
    }

    /** {@inheritDoc} */
    @Override @SuppressWarnings("unchecked")
    public void finish(boolean commit, boolean clearThreadMap, boolean onTimeout) {
        finishFut.finish(commit, clearThreadMap, onTimeout);

        if (finishFut.commit()) {
            finishFut.listen((IgniteInClosure)new IgniteInClosure<NearTxFinishFuture>() {
                @Override public void apply(final NearTxFinishFuture fut) {
                    GridNearTxLocal tx = fut.tx();

                    IgniteInternalFuture<Void> ackFut = null;

                    MvccQueryTracker tracker = tx.mvccQueryTracker();

                    MvccSnapshot mvccSnapshot = tx.mvccSnapshot();

                    if (tracker != null)
                        ackFut = tracker.onDone(tx, commit);
                    else if (mvccSnapshot != null) {
                        if (commit)
                            ackFut = tx.context().coordinators().ackTxCommit(mvccSnapshot);
                        else
                            tx.context().coordinators().ackTxRollback(mvccSnapshot);
                    }

                    if (ackFut != null) {
                        ackFut.listen(new IgniteInClosure<IgniteInternalFuture<Void>>() {
                            @Override public void apply(IgniteInternalFuture<Void> ackFut) {
                                Exception err = null;

                                try {
                                    fut.get();

                                    ackFut.get();
                                }
                                catch (Exception e) {
                                    err = e;
                                }
                                catch (Error e) {
                                    onDone(e);

                                    throw e;
                                }

                                if (err != null)
                                    onDone(err);
                                else
                                    onDone(fut.tx());
                            }
                        });
                    }
                    else
                        finishWithFutureResult(fut);
                }
            });
        }
        else {
            finishFut.listen(new IgniteInClosure<IgniteInternalFuture>() {
                @Override public void apply(IgniteInternalFuture fut) {
                    finishWithFutureResult(fut);
                }
            });
        }
    }

    /** {@inheritDoc} */
    @Override public void onNodeStop(IgniteCheckedException e) {
        super.onDone(finishFut.tx(), e);
    }

    /**
     * @param fut Future.
     */
    private void finishWithFutureResult(IgniteInternalFuture<IgniteInternalTx> fut) {
        try {
            onDone(fut.get());
        }
        catch (IgniteCheckedException | RuntimeException e) {
            onDone(e);
        }
        catch (Error e) {
            onDone(e);

            throw e;
        }
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(GridNearTxFinishAndAckFuture.class, this);
    }
}
