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

package org.apache.ignite.loadtests.job;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.ignite.cluster.ClusterNode;
import org.apache.ignite.compute.ComputeJob;
import org.apache.ignite.compute.ComputeJobResult;
import org.apache.ignite.compute.ComputeJobResultPolicy;
import org.apache.ignite.compute.ComputeTaskAdapter;
import org.jetbrains.annotations.Nullable;

import static org.apache.ignite.compute.ComputeJobResultPolicy.FAILOVER;
import static org.apache.ignite.compute.ComputeJobResultPolicy.REDUCE;
import static org.apache.ignite.compute.ComputeJobResultPolicy.WAIT;

/**
 * Test task for {@link GridJobLoadTest}
 */
public class GridJobLoadTestTask extends ComputeTaskAdapter<GridJobLoadTestParams, Integer> {
    /**{@inheritDoc} */
    @Override public Map<? extends ComputeJob, ClusterNode> map(List<ClusterNode> subgrid, @Nullable GridJobLoadTestParams arg) {
        assert !subgrid.isEmpty();

        Map<ComputeJob, ClusterNode> jobs = new HashMap<>();

        for (int i = 0; i < arg.getJobsCount(); i++)
            jobs.put(
                new GridJobLoadTestJob(
                    /*only on the first step*/i == 0,
                    arg.getJobFailureProbability(),
                    arg.getExecutionDuration(),
                    arg.getCompletionDelay()),
                subgrid.get(0));

        return jobs;
    }

    /**
     * Always trying to failover job, except failed assertions.
     *
     * {@inheritDoc}
     */
    @Override public ComputeJobResultPolicy result(ComputeJobResult res, List<ComputeJobResult> rcvd) {
        return res.getException() == null ? WAIT :
            res.getException().getCause() instanceof AssertionError ? REDUCE : FAILOVER;
    }

    /**{@inheritDoc} */
    @Override public Integer reduce(List<ComputeJobResult> results) {
        int sum = 0;

        for (ComputeJobResult r: results) {
            if (!r.isCancelled() && r.getException() == null)
                sum += r.<Integer>getData();
        }

        return sum;
    }
}