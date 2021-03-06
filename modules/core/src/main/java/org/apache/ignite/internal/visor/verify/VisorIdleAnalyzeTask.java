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

package org.apache.ignite.internal.visor.verify;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.apache.ignite.IgniteException;
import org.apache.ignite.compute.ComputeJobContext;
import org.apache.ignite.compute.ComputeTaskFuture;
import org.apache.ignite.internal.processors.cache.verify.CollectConflictPartitionKeysTask;
import org.apache.ignite.internal.processors.cache.verify.PartitionEntryHashRecord;
import org.apache.ignite.internal.processors.cache.verify.PartitionHashRecord;
import org.apache.ignite.internal.processors.cache.verify.RetrieveConflictPartitionValuesTask;
import org.apache.ignite.internal.processors.task.GridInternal;
import org.apache.ignite.internal.util.typedef.internal.S;
import org.apache.ignite.internal.visor.VisorJob;
import org.apache.ignite.internal.visor.VisorOneNodeTask;
import org.apache.ignite.lang.IgniteFuture;
import org.apache.ignite.lang.IgniteInClosure;
import org.apache.ignite.resources.JobContextResource;

/**
 * Task to find diverged keys of conflict partition.
 */
@GridInternal
public class VisorIdleAnalyzeTask extends VisorOneNodeTask<VisorIdleAnalyzeTaskArg, VisorIdleAnalyzeTaskResult> {
    /** */
    private static final long serialVersionUID = 0L;

    /** {@inheritDoc} */
    @Override protected VisorJob<VisorIdleAnalyzeTaskArg, VisorIdleAnalyzeTaskResult> job(VisorIdleAnalyzeTaskArg arg) {
        return new VisorIdleVerifyJob(arg, debug);
    }

    /**
     *
     */
    private static class VisorIdleVerifyJob extends VisorJob<VisorIdleAnalyzeTaskArg, VisorIdleAnalyzeTaskResult> {
        /** */
        private static final long serialVersionUID = 0L;

        /** */
        private ComputeTaskFuture<Map<PartitionHashRecord, List<PartitionEntryHashRecord>>> conflictKeysFut;

        /** */
        private ComputeTaskFuture<Map<PartitionHashRecord, List<PartitionEntryHashRecord>>> conflictValsFut;

        /** Auto-inject job context. */
        @JobContextResource
        protected transient ComputeJobContext jobCtx;

        /**
         * @param arg Argument.
         * @param debug Debug.
         */
        private VisorIdleVerifyJob(VisorIdleAnalyzeTaskArg arg, boolean debug) {
            super(arg, debug);
        }

        /** {@inheritDoc} */
        @Override protected VisorIdleAnalyzeTaskResult run(VisorIdleAnalyzeTaskArg arg) throws IgniteException {
            if (conflictKeysFut == null) {
                conflictKeysFut = ignite.compute()
                    .executeAsync(CollectConflictPartitionKeysTask.class, arg.getPartitionKey());

                if (!conflictKeysFut.isDone()) {
                    jobCtx.holdcc();

                    conflictKeysFut.listen(new IgniteInClosure<IgniteFuture<Map<PartitionHashRecord, List<PartitionEntryHashRecord>>>>() {
                        @Override public void apply(IgniteFuture<Map<PartitionHashRecord, List<PartitionEntryHashRecord>>> f) {
                            jobCtx.callcc();
                        }
                    });

                    return null;
                }
            }

            Map<PartitionHashRecord, List<PartitionEntryHashRecord>> conflictKeys = conflictKeysFut.get();

            if (conflictKeys.isEmpty())
                return new VisorIdleAnalyzeTaskResult(Collections.emptyMap());

            if (conflictValsFut == null) {
                conflictValsFut = ignite.compute().executeAsync(RetrieveConflictPartitionValuesTask.class, conflictKeys);

                if (!conflictValsFut.isDone()) {
                    jobCtx.holdcc();

                    conflictKeysFut.listen(new IgniteInClosure<IgniteFuture<Map<PartitionHashRecord, List<PartitionEntryHashRecord>>>>() {
                        @Override public void apply(IgniteFuture<Map<PartitionHashRecord, List<PartitionEntryHashRecord>>> f) {
                            jobCtx.callcc();
                        }
                    });

                    return null;
                }
            }

            return new VisorIdleAnalyzeTaskResult(conflictValsFut.get());
        }

        /** {@inheritDoc} */
        @Override public String toString() {
            return S.toString(VisorIdleVerifyJob.class, this);
        }
    }
}
