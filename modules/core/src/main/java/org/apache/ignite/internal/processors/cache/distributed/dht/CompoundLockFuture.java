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

import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import org.apache.ignite.internal.IgniteInternalFuture;
import org.apache.ignite.internal.processors.cache.distributed.near.GridNearTxAbstractEnlistFuture;
import org.apache.ignite.internal.processors.cache.distributed.near.GridNearTxSelectForUpdateFuture;
import org.apache.ignite.internal.util.future.GridFutureAdapter;
import org.apache.ignite.lang.IgniteInClosure;

/**
 *
 */
public class CompoundLockFuture extends GridFutureAdapter<Void> implements DhtLockFuture<Void>, IgniteInClosure<IgniteInternalFuture<?>> {
    /** */
    private static final long serialVersionUID = 4644646033267042131L;
    /** */
    private static final AtomicIntegerFieldUpdater<CompoundLockFuture> CNT_UPD =
        AtomicIntegerFieldUpdater.newUpdater(CompoundLockFuture.class, "cnt");
    /** */
    private volatile int cnt;
    /** */
    private final GridDhtTxLocalAdapter tx;

    /**
     * @param cnt ResultSet futures count.
     * @param tx Transaction.
     */
    public CompoundLockFuture(int cnt, GridDhtTxLocalAdapter tx) {
        this.cnt = cnt;
        this.tx = tx;
    }

    /**
     * @param fut ResultSet future.
     */
    public void register(IgniteInternalFuture<?> fut) {
        fut.listen(this);
    }

    /**
     *  Init method.
     */
    public void init() {
        while(true) {
            IgniteInternalFuture<?> fut = tx.lockFuture();

            if (fut == GridDhtTxLocalAdapter.ROLLBACK_FUT) {
                onDone(tx.timedOut() ? tx.timeoutException() : tx.rollbackException());

                break;
            }
            else if (fut != null) {
                // Wait for previous future.
                assert fut instanceof GridNearTxAbstractEnlistFuture
                    || fut instanceof GridDhtTxAbstractEnlistFuture
                    || fut instanceof CompoundLockFuture
                    || fut instanceof GridNearTxSelectForUpdateFuture : fut;

                // Terminate this future if parent future is terminated by rollback.
                if (!fut.isDone()) {
                    fut.listen(new IgniteInClosure<IgniteInternalFuture>() {
                        @Override public void apply(IgniteInternalFuture fut) {
                            if (fut.error() != null)
                                onDone(fut.error());
                        }
                    });
                }
                else if (fut.error() != null)
                    onDone(fut.error());

                break;
            }
            else if (tx.updateLockFuture(null, this))
                break;
        }
    }

    @Override public void apply(IgniteInternalFuture<?> future) {
        if (!isDone() && (future.error() != null || CNT_UPD.decrementAndGet(this) == 0)) {
            Throwable err = future.error();

            if (err == null)
                tx.clearLockFuture(this);

            onDone(err);
        }
    }

    /** {@inheritDoc} */
    @Override public void onError(Throwable error) {
        assert error != null;

        onDone(error);
    }
}
