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

import java.util.Collection;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCompute;
import org.apache.ignite.cluster.ClusterNode;
import org.apache.ignite.cluster.ClusterTopologyException;
import org.apache.ignite.internal.cluster.ClusterTopologyCheckedException;
import org.apache.ignite.internal.util.typedef.F;
import org.apache.ignite.lang.IgniteCallable;
import org.apache.ignite.testframework.junits.common.GridCommonAbstractTest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.apache.ignite.internal.GridClosureCallMode.BALANCE;

/**
 *
 */
@RunWith(JUnit4.class)
public class IgniteComputeTopologyExceptionTest extends GridCommonAbstractTest {
    /** {@inheritDoc} */
    @Override protected void beforeTest() throws Exception {
        startGrids(2);
    }

    /** {@inheritDoc} */
    @Override protected void afterTest() throws Exception {
        stopAllGrids();
    }

    /**
     * @throws Exception If failed.
     */
    @Test
    public void testCorrectException() throws Exception {
        Ignite ignite = ignite(0);

        IgniteCompute comp = ignite.compute(ignite.cluster().forRemotes()).withNoFailover();

        stopGrid(1);

        try {
            comp.call(new IgniteCallable<Object>() {
                @Override public Object call() throws Exception {
                    fail("Should not be called.");

                    return null;
                }
            });

            fail();
        }
        catch (ClusterTopologyException e) {
            log.info("Expected exception: " + e);
        }
    }

    /**
     * @throws Exception If failed.
     */
    @Test
    public void testCorrectCheckedException() throws Exception {
        IgniteKernal ignite0 = (IgniteKernal)ignite(0);

        Collection<ClusterNode> nodes = F.asList(ignite(1).cluster().localNode());

        stopGrid(1);

        IgniteInternalFuture<?> fut = ignite0.context().closure().callAsyncNoFailover(BALANCE,
            new IgniteCallable<Object>() {
                @Override public Object call() throws Exception {
                    fail("Should not be called.");

                    return null;
                }
            },
            nodes,
            false,
            0, false);

        try {
            fut.get();

            fail();
        }
        catch (ClusterTopologyCheckedException e) {
            log.info("Expected exception: " + e);
        }
    }
}
