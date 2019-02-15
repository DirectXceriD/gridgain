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

package org.apache.ignite.session;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteException;
import org.apache.ignite.cluster.ClusterNode;
import org.apache.ignite.compute.ComputeJob;
import org.apache.ignite.compute.ComputeJobAdapter;
import org.apache.ignite.compute.ComputeJobResult;
import org.apache.ignite.compute.ComputeTaskAdapter;
import org.apache.ignite.compute.ComputeTaskFuture;
import org.apache.ignite.compute.ComputeTaskSession;
import org.apache.ignite.compute.ComputeTaskSessionFullSupport;
import org.apache.ignite.resources.TaskSessionResource;
import org.apache.ignite.testframework.junits.common.GridCommonAbstractTest;
import org.apache.ignite.testframework.junits.common.GridCommonTest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Job attribute test.
 */
@GridCommonTest(group = "Task Session")
@RunWith(JUnit4.class)
public class GridSessionSetJobAttribute2SelfTest extends GridCommonAbstractTest {
    /** */
    private static final String TEST_ATTR_KEY = "grid.tasksession.test.attr";

    /** */
    public GridSessionSetJobAttribute2SelfTest() {
        super(/*start Grid*/false);
    }

    /**
     * @throws Exception If failed.
     */
    @Test
    public void testJobSetAttribute() throws Exception {
        try {
            Ignite ignite1 = startGrid(1);
            Ignite ignite2 = startGrid(2);

            ignite1.compute().localDeployTask(SessionTestTask.class, SessionTestTask.class.getClassLoader());

            ComputeTaskFuture<?> fut =
                executeAsync(ignite1.compute(), SessionTestTask.class.getName(), ignite2.cluster().localNode().id());

            fut.get();
        }
        finally {
            stopGrid(1);
            stopGrid(2);
        }
    }

    /**
     *
     */
    @ComputeTaskSessionFullSupport
    private static class SessionTestTask extends ComputeTaskAdapter<UUID, Object> {
        /** */
        @TaskSessionResource
        private ComputeTaskSession taskSes;

        /** */
        private UUID attrVal;

        /** {@inheritDoc} */
        @Override public Map<? extends ComputeJob, ClusterNode> map(List<ClusterNode> subgrid, UUID arg) {
            assert subgrid.size() == 2;
            assert arg != null;

            attrVal = UUID.randomUUID();

            for (ClusterNode node : subgrid) {
                if (node.id().equals(arg))
                    return Collections.singletonMap(new SessionTestJob(attrVal), node);
            }

            assert false;

            return null;
        }

        /** {@inheritDoc} */
        @Override public Object reduce(List<ComputeJobResult> results) {
            try {
                Thread.sleep(100);
            }
            catch (InterruptedException e) {
                throw new IgniteException("Got interrupted while while sleeping.", e);
            }

            Serializable ser = taskSes.getAttribute(TEST_ATTR_KEY);

            assert ser != null;

            assert attrVal.equals(ser);

            return null;
        }
    }

    /** */
    private static class SessionTestJob extends ComputeJobAdapter {
        /** */
        @TaskSessionResource
        private ComputeTaskSession taskSes;

        /**
         * @param arg Argument.
         */
        private SessionTestJob(UUID arg) {
            super(arg);
        }

        /** {@inheritDoc} */
        @Override public Serializable execute() {
            assert taskSes != null;
            assert argument(0) != null;

            taskSes.setAttribute(TEST_ATTR_KEY, argument(0));

            return argument(0);
        }
    }
}
