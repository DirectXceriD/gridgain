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

package org.apache.ignite.p2p;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.ignite.Ignite;
import org.apache.ignite.cluster.ClusterNode;
import org.apache.ignite.compute.ComputeJob;
import org.apache.ignite.compute.ComputeJobAdapter;
import org.apache.ignite.compute.ComputeJobResult;
import org.apache.ignite.compute.ComputeTaskAdapter;
import org.apache.ignite.configuration.DeploymentMode;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.resources.IgniteInstanceResource;
import org.apache.ignite.testframework.junits.common.GridCommonAbstractTest;
import org.apache.ignite.testframework.junits.common.GridCommonTest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 *
 */
@SuppressWarnings({"ProhibitedExceptionDeclared"})
@GridCommonTest(group = "P2P")
@RunWith(JUnit4.class)
public class GridP2PRecursionTaskSelfTest extends GridCommonAbstractTest {
    /**
     * Current deployment mode. Used in {@link #getConfiguration(String)}.
     */
    private DeploymentMode depMode;

    /** {@inheritDoc} */
    @Override protected IgniteConfiguration getConfiguration(String igniteInstanceName) throws Exception {
        IgniteConfiguration cfg = super.getConfiguration(igniteInstanceName);

        cfg.setDeploymentMode(depMode);

        return cfg;
    }

    /**
     * Process one test.
     *
     * @param depMode deployment mode.
     * @throws Exception if error occur.
     */
    private void processTest(DeploymentMode depMode) throws Exception {
        this.depMode = depMode;

        try {
            Ignite ignite1 = startGrid(1);
            startGrid(2);

            long res = ignite1.compute().execute(FactorialTask.class, 3L);

            assert res == factorial(3);

            res = ignite1.compute().execute(FactorialTask.class, 3L);

            assert res == factorial(3);
        }
        finally {
            stopGrid(2);
            stopGrid(1);
        }
    }

    /**
     * Test GridDeploymentMode.ISOLATED mode.
     *
     * @throws Exception if error occur.
     */
    @Test
    public void testPrivateMode() throws Exception {
        processTest(DeploymentMode.PRIVATE);
    }

    /**
     * Test GridDeploymentMode.ISOLATED mode.
     *
     * @throws Exception if error occur.
     */
    @Test
    public void testIsolatedMode() throws Exception {
        processTest(DeploymentMode.ISOLATED);
    }

    /**
     * Test GridDeploymentMode.CONTINOUS mode.
     *
     * @throws Exception if error occur.
     */
    @Test
    public void testContinuousMode() throws Exception {
        processTest(DeploymentMode.CONTINUOUS);
    }

    /**
     * Test GridDeploymentMode.SHARED mode.
     *
     * @throws Exception if error occur.
     */
    @Test
    public void testSharedMode() throws Exception {
        processTest(DeploymentMode.SHARED);
    }

    /**
     * Calculates factorial.
     *
     * @param num Factorial to calculate.
     * @return Factorial for the number passed in.
     */
    private long factorial(long num) {
        assert num > 0;

        if (num == 1)
            return num;

        return num * factorial(num - 1);
    }

    /**
     * Task that will always fail due to non-transient resource injection.
     */
    public static class FactorialTask extends ComputeTaskAdapter<Long, Long> {
        /** {@inheritDoc} */
        @Override public Map<? extends ComputeJob, ClusterNode> map(List<ClusterNode> subgrid, Long arg) {
            assert arg > 1;

            Map<FactorialJob, ClusterNode> map = new HashMap<>();

            Iterator<ClusterNode> iter = subgrid.iterator();

            for (int i = 0; i < arg; i++) {
                // Recycle iterator.
                if (iter.hasNext() == false)
                    iter = subgrid.iterator();

                ClusterNode node = iter.next();

                map.put(new FactorialJob(arg - 1), node);
            }

            return map;
        }

        /** {@inheritDoc} */
        @Override public Long reduce(List<ComputeJobResult> results) {
            long retVal = 0;

            for (ComputeJobResult res : results)
                retVal += (Long)res.getData();

            return retVal;
        }
    }

    /** */
    private static class FactorialJob extends ComputeJobAdapter {
        /** */
        @IgniteInstanceResource
        private Ignite ignite;

        /**
         * Constructor.
         * @param arg Argument.
         */
        FactorialJob(Long arg) {
            super(arg);
        }

        /** {@inheritDoc} */
        @Override public Long execute() {
            Long arg = argument(0);

            assert arg != null;
            assert arg > 0;

            if (arg == 1)
                return 1L;

            ignite.compute().localDeployTask(FactorialTask.class, FactorialTask.class.getClassLoader());

            return ignite.compute().execute(FactorialTask.class, arg);
        }
    }
}
