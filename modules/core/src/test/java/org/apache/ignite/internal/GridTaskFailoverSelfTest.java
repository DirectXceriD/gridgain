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

package org.apache.ignite.internal;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteLogger;
import org.apache.ignite.cluster.ClusterTopologyException;
import org.apache.ignite.compute.ComputeJobAdapter;
import org.apache.ignite.compute.ComputeJobResult;
import org.apache.ignite.compute.ComputeJobResultPolicy;
import org.apache.ignite.compute.ComputeTaskFuture;
import org.apache.ignite.compute.ComputeTaskSplitAdapter;
import org.apache.ignite.resources.LoggerResource;
import org.apache.ignite.testframework.junits.common.GridCommonAbstractTest;
import org.apache.ignite.testframework.junits.common.GridCommonTest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Test for task failover.
 */
@GridCommonTest(group = "Kernal Self")
@RunWith(JUnit4.class)
public class GridTaskFailoverSelfTest extends GridCommonAbstractTest {
    /** Don't change it value. */
    public static final int SPLIT_COUNT = 2;

    /** */
    public GridTaskFailoverSelfTest() {
        super(false);
    }

    /**
     * @throws Exception If test failed.
     */
    @Test
    public void testFailover() throws Exception {
        Ignite ignite = startGrid();

        try {
            ignite.compute().localDeployTask(GridFailoverTestTask.class, GridFailoverTestTask.class.getClassLoader());

            ComputeTaskFuture<?> fut = ignite.compute().execute(GridFailoverTestTask.class.getName(), null);

            assert fut != null;

            fut.get();

            assert false : "Should never be reached due to exception thrown.";
        }
        catch (ClusterTopologyException e) {
            info("Received correct exception: " + e);
        }
        finally {
            stopGrid();
        }
    }

    /** */
    @SuppressWarnings({"PublicInnerClass"})
    public static class GridFailoverTestTask extends ComputeTaskSplitAdapter<Serializable, Integer> {
        /** */
        @LoggerResource
        private IgniteLogger log;

        /** {@inheritDoc} */
        @Override public Collection<ComputeJobAdapter> split(int gridSize, Serializable arg) {
            if (log.isInfoEnabled())
                log.info("Splitting job [job=" + this + ", gridSize=" + gridSize + ", arg=" + arg + ']');

            Collection<ComputeJobAdapter> jobs = new ArrayList<>(SPLIT_COUNT);

            for (int i = 0; i < SPLIT_COUNT; i++)
                jobs.add(new ComputeJobAdapter() {
                    @Override public Serializable execute() {
                        if (log.isInfoEnabled())
                            log.info("Computing job [job=" + this + ']');

                        return null;
                    }
                });

            return jobs;
        }

        /** {@inheritDoc} */
        @Override public ComputeJobResultPolicy result(ComputeJobResult res, List<ComputeJobResult> received) {
            if (res.getException() != null)
                throw res.getException();

            return ComputeJobResultPolicy.FAILOVER;
        }

        /** {@inheritDoc} */
        @Override public Integer reduce(List<ComputeJobResult> results) {
            if (log.isInfoEnabled())
                log.info("Reducing job [job=" + this + ", results=" + results + ']');

            int res = 0;

            for (ComputeJobResult result : results)
                res += (Integer)result.getData();

            return res;
        }
    }
}
