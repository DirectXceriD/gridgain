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

package org.apache.ignite.internal.visor.node;

import java.util.Collection;
import java.util.List;
import org.apache.ignite.cache.CacheMetrics;
import org.apache.ignite.cluster.BaselineNode;
import org.apache.ignite.compute.ComputeJobResult;
import org.apache.ignite.internal.cluster.IgniteClusterEx;
import org.apache.ignite.internal.processors.cache.CacheGroupContext;
import org.apache.ignite.internal.processors.cache.GridCacheAdapter;
import org.apache.ignite.internal.processors.cache.GridCacheProcessor;
import org.apache.ignite.internal.processors.cache.GridCacheUtils;
import org.apache.ignite.internal.processors.task.GridInternal;
import org.apache.ignite.internal.util.typedef.internal.S;
import org.apache.ignite.internal.util.typedef.internal.U;
import org.apache.ignite.internal.visor.VisorJob;
import org.apache.ignite.internal.visor.VisorMultiNodeTask;
import org.jetbrains.annotations.Nullable;

import static org.apache.ignite.internal.visor.node.VisorNodeBaselineStatus.BASELINE_NOT_AVAILABLE;
import static org.apache.ignite.internal.visor.node.VisorNodeBaselineStatus.NODE_IN_BASELINE;
import static org.apache.ignite.internal.visor.node.VisorNodeBaselineStatus.NODE_NOT_IN_BASELINE;
import static org.apache.ignite.internal.visor.util.VisorTaskUtils.MINIMAL_REBALANCE;
import static org.apache.ignite.internal.visor.util.VisorTaskUtils.NOTHING_TO_REBALANCE;
import static org.apache.ignite.internal.visor.util.VisorTaskUtils.REBALANCE_COMPLETE;
import static org.apache.ignite.internal.visor.util.VisorTaskUtils.REBALANCE_NOT_AVAILABLE;
import static org.apache.ignite.internal.visor.util.VisorTaskUtils.isProxyCache;
import static org.apache.ignite.internal.visor.util.VisorTaskUtils.isRestartingCache;
import static org.apache.ignite.internal.visor.util.VisorTaskUtils.log;

/**
 * Collects topology rebalance metrics.
 */
@GridInternal
public class VisorCacheRebalanceCollectorTask extends VisorMultiNodeTask<VisorCacheRebalanceCollectorTaskArg,
    VisorCacheRebalanceCollectorTaskResult, VisorCacheRebalanceCollectorJobResult> {
    /** */
    private static final long serialVersionUID = 0L;

    /** {@inheritDoc} */
    @Override protected VisorCacheRebalanceCollectorJob job(VisorCacheRebalanceCollectorTaskArg arg) {
        return new VisorCacheRebalanceCollectorJob(arg, debug);
    }

    /** {@inheritDoc} */
    @Nullable @Override protected VisorCacheRebalanceCollectorTaskResult reduce0(List<ComputeJobResult> results) {
        return reduce(new VisorCacheRebalanceCollectorTaskResult(), results);
    }

    /**
     * @param taskRes Task result.
     * @param results Results.
     * @return Topology rebalance metrics collector task result.
     */
    protected VisorCacheRebalanceCollectorTaskResult reduce(
        VisorCacheRebalanceCollectorTaskResult taskRes,
        List<ComputeJobResult> results
    ) {
        for (ComputeJobResult res : results) {
            VisorCacheRebalanceCollectorJobResult jobRes = res.getData();

            if (jobRes != null) {
                if (res.getException() == null)
                    taskRes.getRebalance().put(res.getNode().id(), jobRes.getRebalance());

                taskRes.getBaseline().put(res.getNode().id(), jobRes.getBaseline());
            }
        }

        return taskRes;
    }

    /**
     * Job that collects rebalance metrics.
     */
    private static class VisorCacheRebalanceCollectorJob extends VisorJob<VisorCacheRebalanceCollectorTaskArg, VisorCacheRebalanceCollectorJobResult> {
        /** */
        private static final long serialVersionUID = 0L;

        /**
         * Create job with given argument.
         *
         * @param arg Job argument.
         * @param debug Debug flag.
         */
        private VisorCacheRebalanceCollectorJob(VisorCacheRebalanceCollectorTaskArg arg, boolean debug) {
            super(arg, debug);
        }

        /** {@inheritDoc} */
        @Override protected VisorCacheRebalanceCollectorJobResult run(VisorCacheRebalanceCollectorTaskArg arg) {
            VisorCacheRebalanceCollectorJobResult res = new VisorCacheRebalanceCollectorJobResult();

            long start0 = U.currentTimeMillis();

            try {
                int partitions = 0;
                double total = 0;
                double ready = 0;

                GridCacheProcessor cacheProc = ignite.context().cache();

                boolean rebalanceInProgress = false;

                for (CacheGroupContext grp: cacheProc.cacheGroups()) {
                    String cacheName = grp.config().getName();

                    if (isProxyCache(ignite, cacheName) || isRestartingCache(ignite, cacheName))
                        continue;

                    try {
                        GridCacheAdapter ca = cacheProc.internalCache(cacheName);

                        if (ca == null || !ca.context().started())
                            continue;

                        CacheMetrics cm = ca.localMetrics();

                        partitions += cm.getTotalPartitionsCount();

                        long keysTotal = cm.getEstimatedRebalancingKeys();
                        long keysReady = cm.getRebalancedKeys();

                        if (keysReady >= keysTotal)
                            keysReady = Math.max(keysTotal - 1, 0);

                        total += keysTotal;
                        ready += keysReady;

                        if (cm.getRebalancingPartitionsCount() > 0)
                            rebalanceInProgress = true;
                    }
                    catch(IllegalStateException | IllegalArgumentException e) {
                        if (debug && ignite.log() != null)
                            ignite.log().error("Ignored cache group: " + grp.cacheOrGroupName(), e);
                    }
                }

                if (partitions == 0)
                    res.setRebalance(NOTHING_TO_REBALANCE);
                else if (total == 0 && rebalanceInProgress)
                    res.setRebalance(MINIMAL_REBALANCE);
                else
                    res.setRebalance(total > 0 && rebalanceInProgress ? Math.max(ready / total, MINIMAL_REBALANCE) : REBALANCE_COMPLETE);
            }
            catch (Exception e) {
                res.setRebalance(REBALANCE_NOT_AVAILABLE);

                ignite.log().error("Failed to collect rebalance metrics", e);
            }

            if (GridCacheUtils.isPersistenceEnabled(ignite.configuration())) {
                IgniteClusterEx cluster = ignite.cluster();

                Object consistentId = ignite.localNode().consistentId();

                Collection<? extends BaselineNode> baseline = cluster.currentBaselineTopology();

                boolean inBaseline = baseline.stream().anyMatch(n -> consistentId.equals(n.consistentId()));

                res.setBaseline(inBaseline ? NODE_IN_BASELINE : NODE_NOT_IN_BASELINE);
            }
            else
                res.setBaseline(BASELINE_NOT_AVAILABLE);

            if (debug)
                log(ignite.log(), "Collected rebalance metrics", getClass(), start0);

            return res;
        }

        /** {@inheritDoc} */
        @Override public String toString() {
            return S.toString(VisorCacheRebalanceCollectorJob.class, this);
        }
    }
}
