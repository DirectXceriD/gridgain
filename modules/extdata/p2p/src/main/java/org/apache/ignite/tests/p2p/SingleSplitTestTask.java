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

package org.apache.ignite.tests.p2p;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.apache.ignite.compute.ComputeJob;
import org.apache.ignite.compute.ComputeJobAdapter;
import org.apache.ignite.compute.ComputeJobResult;
import org.apache.ignite.compute.ComputeTaskSplitAdapter;

/**
 * Test task for P2P deployment tests.
 */
public class SingleSplitTestTask extends ComputeTaskSplitAdapter<Integer, Integer> {
    /**
     * {@inheritDoc}
     */
    @Override protected Collection<? extends ComputeJob> split(int gridSize, Integer arg) {
        assert gridSize > 0 : "Subgrid cannot be empty.";

        Collection<ComputeJobAdapter> jobs = new ArrayList<>(gridSize);

        for (int i = 0; i < arg; i++)
            jobs.add(new SingleSplitTestJob(1));

        return jobs;
    }

    /**
     * {@inheritDoc}
     */
    @Override public Integer reduce(List<ComputeJobResult> results) {
        int retVal = 0;

        for (ComputeJobResult res : results) {
            assert res.getException() == null : "Load test jobs can never fail: " + res;

            retVal += (Integer)res.getData();
        }

        return retVal;
    }

    /**
     * Test job for P2P deployment tests.
     */
    @SuppressWarnings("PublicInnerClass")
    public static final class SingleSplitTestJob extends ComputeJobAdapter {
        /**
         * @param args Job arguments.
         */
        public SingleSplitTestJob(Integer args) {
            super(args);
        }

        /** {@inheritDoc} */
        @Override public Serializable execute() {
            return new GridSingleSplitTestJobTarget().executeLoadTestJob((Integer)argument(0));
        }
    }
}