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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.ignite.Ignite;
import org.apache.ignite.compute.ComputeJob;
import org.apache.ignite.compute.ComputeJobAdapter;
import org.apache.ignite.compute.ComputeJobContext;
import org.apache.ignite.compute.ComputeJobResult;
import org.apache.ignite.compute.ComputeTaskSplitAdapter;
import org.apache.ignite.resources.IgniteInstanceResource;
import org.apache.ignite.resources.JobContextResource;
import org.apache.ignite.testframework.junits.common.GridCommonAbstractTest;
import org.apache.ignite.testframework.junits.common.GridCommonTest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Job context test.
 */
@GridCommonTest(group = "Kernal Self")
@RunWith(JUnit4.class)
public class GridJobContextSelfTest extends GridCommonAbstractTest {
    /**
     * @throws Exception If anything failed.
     */
    @Test
    public void testJobContext() throws Exception {
        Ignite ignite = startGrid(1);

        try {
            startGrid(2);

            try {
                ignite.compute().execute(JobContextTask.class, null);
            }
            finally {
                stopGrid(2);
            }
        }
        finally{
            stopGrid(1);
        }
    }

    /** */
    @SuppressWarnings("PublicInnerClass")
    public static class JobContextTask extends ComputeTaskSplitAdapter<Object, Object> {
        @Override protected Collection<? extends ComputeJob> split(int gridSize, Object arg) {
            Collection<ComputeJobAdapter> jobs = new ArrayList<>(gridSize);

            for (int i = 0; i < gridSize; i++) {
                jobs.add(new ComputeJobAdapter() {
                    /** */
                    @JobContextResource
                    private ComputeJobContext jobCtx;

                    /** Ignite instance. */
                    @IgniteInstanceResource
                    private Ignite ignite;

                    /** {@inheritDoc} */
                    @Override public Serializable execute() {
                        UUID locNodeId = ignite.configuration().getNodeId();

                        jobCtx.setAttribute("nodeId", locNodeId);
                        jobCtx.setAttribute("jobId", jobCtx.getJobId());

                        Map<String, String> attrs = new HashMap<>(10);

                        for (int i = 0; i < 10; i++) {
                            String s = jobCtx.getJobId().toString() + i;

                            attrs.put(s, s);
                        }

                        jobCtx.setAttributes(attrs);

                        assert jobCtx.getAttribute("nodeId").equals(locNodeId);
                        assert jobCtx.getAttributes().get("nodeId").equals(locNodeId);
                        assert jobCtx.getAttributes().keySet().containsAll(attrs.keySet());
                        assert jobCtx.getAttributes().values().containsAll(attrs.values());

                        return null;
                    }
                });
            }

            return jobs;
        }

        /** {@inheritDoc} */
        @Override public Object reduce(List<ComputeJobResult> results) {
            for (ComputeJobResult res : results) {
                ComputeJobContext jobCtx = res.getJobContext();

                assert jobCtx.getAttribute("nodeId").equals(res.getNode().id());
                assert jobCtx.getAttributes().get("nodeId").equals(res.getNode().id());

                assert jobCtx.getAttribute("jobId").equals(jobCtx.getJobId());

                for (int i = 0; i < 10; i++) {
                    String s = jobCtx.getJobId().toString() + i;

                    assert jobCtx.getAttribute(s).equals(s);
                    assert jobCtx.getAttributes().get(s).equals(s);
                }
            }

            return null;
        }
    }
}
