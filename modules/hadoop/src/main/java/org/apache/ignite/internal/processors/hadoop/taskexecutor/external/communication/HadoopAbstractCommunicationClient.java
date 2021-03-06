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

package org.apache.ignite.internal.processors.hadoop.taskexecutor.external.communication;

import java.util.concurrent.atomic.AtomicInteger;
import org.apache.ignite.internal.util.typedef.internal.S;
import org.apache.ignite.internal.util.typedef.internal.U;

/**
 * Implements basic lifecycle for communication clients.
 */
public abstract class HadoopAbstractCommunicationClient implements HadoopCommunicationClient {
    /** Time when this client was last used. */
    private volatile long lastUsed = U.currentTimeMillis();

    /** Reservations. */
    private final AtomicInteger reserves = new AtomicInteger();

    /** {@inheritDoc} */
    @Override public boolean close() {
        return reserves.compareAndSet(0, -1);
    }

    /** {@inheritDoc} */
    @Override public void forceClose() {
        reserves.set(-1);
    }

    /** {@inheritDoc} */
    @Override public boolean closed() {
        return reserves.get() == -1;
    }

    /** {@inheritDoc} */
    @Override public boolean reserve() {
        while (true) {
            int r = reserves.get();

            if (r == -1)
                return false;

            if (reserves.compareAndSet(r, r + 1))
                return true;
        }
    }

    /** {@inheritDoc} */
    @Override public void release() {
        while (true) {
            int r = reserves.get();

            if (r == -1)
                return;

            if (reserves.compareAndSet(r, r - 1))
                return;
        }
    }

    /** {@inheritDoc} */
    @Override public boolean reserved() {
        return reserves.get() > 0;
    }

    /** {@inheritDoc} */
    @Override public long getIdleTime() {
        return U.currentTimeMillis() - lastUsed;
    }

    /**
     * Updates used time.
     */
    protected void markUsed() {
        lastUsed = U.currentTimeMillis();
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(HadoopAbstractCommunicationClient.class, this);
    }
}