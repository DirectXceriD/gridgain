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

package org.apache.ignite.internal.util.future;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.ignite.IgniteCheckedException;
import org.jetbrains.annotations.Nullable;

/**
 *
 */
public class CountDownFuture extends GridFutureAdapter<Void> {
    /** */
    private AtomicInteger remaining;

    /** */
    private AtomicReference<Exception> errCollector;

    /**
     * @param cnt Number of completing parties.
     */
    public CountDownFuture(int cnt) {
        remaining = new AtomicInteger(cnt);
        errCollector = new AtomicReference<>();
    }

    /** {@inheritDoc} */
    @Override public boolean onDone(@Nullable Void res, @Nullable Throwable err) {
        if (err != null)
            addError(err);

        int left = remaining.decrementAndGet();

        boolean done = left == 0 && super.onDone(res, errCollector.get());

        if (done)
            afterDone();

        return done;
    }

    /**
     *
     */
    protected void afterDone() {
        // No-op, to be overridden in subclasses.
    }

    /**
     * @param err Error.
     */
    private void addError(Throwable err) {
        Exception ex = errCollector.get();

        if (ex == null) {
            Exception compound = new IgniteCheckedException("Compound exception for CountDownFuture.");

            ex = errCollector.compareAndSet(null, compound) ? compound : errCollector.get();
        }

        assert ex != null;

        ex.addSuppressed(err);
    }
}
