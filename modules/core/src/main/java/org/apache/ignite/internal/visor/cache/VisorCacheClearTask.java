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

package org.apache.ignite.internal.visor.cache;

import org.apache.ignite.IgniteCache;
import org.apache.ignite.cache.CachePeekMode;
import org.apache.ignite.compute.ComputeJobContext;
import org.apache.ignite.internal.processors.task.GridInternal;
import org.apache.ignite.internal.processors.task.GridVisorManagementTask;
import org.apache.ignite.internal.util.typedef.internal.S;
import org.apache.ignite.internal.visor.VisorJob;
import org.apache.ignite.internal.visor.VisorOneNodeTask;
import org.apache.ignite.lang.IgniteFuture;
import org.apache.ignite.lang.IgniteInClosure;
import org.apache.ignite.resources.JobContextResource;

/**
 * Task that clears specified caches on specified node.
 */
@GridInternal
@GridVisorManagementTask
public class VisorCacheClearTask extends VisorOneNodeTask<VisorCacheClearTaskArg, VisorCacheClearTaskResult> {
    /** */
    private static final long serialVersionUID = 0L;

    /** {@inheritDoc} */
    @Override protected VisorCacheClearJob job(VisorCacheClearTaskArg arg) {
        return new VisorCacheClearJob(arg, debug);
    }

    /**
     * Job that clear specified caches.
     */
    private static class VisorCacheClearJob extends VisorJob<VisorCacheClearTaskArg, VisorCacheClearTaskResult> {
        /** */
        private static final long serialVersionUID = 0L;

        /** */
        private final IgniteInClosure<IgniteFuture> lsnr;

        /** */
        private IgniteFuture<Long>[] futs;

        /** */
        @JobContextResource
        private ComputeJobContext jobCtx;

        /**
         * Create job.
         *
         * @param arg Task argument.
         * @param debug Debug flag.
         */
        private VisorCacheClearJob(VisorCacheClearTaskArg arg, boolean debug) {
            super(arg, debug);

            lsnr = new IgniteInClosure<IgniteFuture>() {
                /** */
                private static final long serialVersionUID = 0L;

                @Override public void apply(IgniteFuture f) {
                    assert futs[0].isDone();
                    assert futs[1] == null || futs[1].isDone();
                    assert futs[2] == null || futs[2].isDone();

                    jobCtx.callcc();
                }
            };
        }

        /**
         * @param fut Future to listen.
         * @return {@code true} If future was not completed and this job should holdCC.
         */
        private boolean callAsync(IgniteFuture fut) {
            if (fut.isDone())
                return false;

            jobCtx.holdcc();

            fut.listen(lsnr);

            return true;
        }

        /** {@inheritDoc} */
        @Override protected VisorCacheClearTaskResult run(final VisorCacheClearTaskArg arg) {
            if (futs == null)
                futs = new IgniteFuture[3];

            if (futs[0] == null || futs[1] == null || futs[2] == null) {
                String cacheName = arg.getCacheName();

                IgniteCache cache = ignite.cache(cacheName);

                if (cache == null)
                    throw new IllegalStateException("Failed to find cache for name: " + cacheName);

                if (futs[0] == null) {
                    futs[0] = cache.sizeLongAsync(CachePeekMode.PRIMARY);

                    if (callAsync(futs[0]))
                        return null;
                }

                if (futs[1] == null) {
                    futs[1] = cache.clearAsync();

                    if (callAsync(futs[1]))
                        return null;
                }

                if (futs[2] == null) {
                    futs[2] = cache.sizeLongAsync(CachePeekMode.PRIMARY);

                    if (callAsync(futs[2]))
                        return null;
                }
            }

            assert futs[0].isDone() && futs[1].isDone() && futs[2].isDone();

            return new VisorCacheClearTaskResult(futs[0].get(), futs[2].get());
        }

        /** {@inheritDoc} */
        @Override public String toString() {
            return S.toString(VisorCacheClearJob.class, this);
        }
    }
}
