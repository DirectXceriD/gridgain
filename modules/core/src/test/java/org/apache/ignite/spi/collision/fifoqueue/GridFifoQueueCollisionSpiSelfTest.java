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

package org.apache.ignite.spi.collision.fifoqueue;

import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.apache.ignite.internal.util.typedef.CI1;
import org.apache.ignite.spi.collision.CollisionJobContext;
import org.apache.ignite.spi.collision.GridCollisionTestContext;
import org.apache.ignite.spi.collision.GridTestCollisionJobContext;
import org.apache.ignite.spi.collision.GridTestCollisionTaskSession;
import org.apache.ignite.testframework.junits.spi.GridSpiAbstractTest;
import org.apache.ignite.testframework.junits.spi.GridSpiTest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Unit tests for {@link FifoQueueCollisionSpi}.
 */
@GridSpiTest(spi = FifoQueueCollisionSpi.class, group = "Collision SPI")
@RunWith(JUnit4.class)
public class GridFifoQueueCollisionSpiSelfTest extends GridSpiAbstractTest<FifoQueueCollisionSpi> {
    /**
     * @throws Exception If failed.
     */
    @Test
    public void testCollision0() throws Exception {
        int activeCnt = 2;

        assert getSpi().getParallelJobsNumber() > activeCnt;

        GridCollisionTestContext cntx = createContext(activeCnt, getSpi().getParallelJobsNumber() - activeCnt);

        Collection<CollisionJobContext> activeJobs = cntx.activeJobs();
        Collection<CollisionJobContext> passiveJobs = cntx.waitingJobs();

        getSpi().onCollision(new GridCollisionTestContext(activeJobs, passiveJobs));

        for (CollisionJobContext ctx : passiveJobs) {
            assert ((GridTestCollisionJobContext)ctx).isActivated();
            assert !((GridTestCollisionJobContext)ctx).isCanceled();
        }

        int i = 0;

        for (CollisionJobContext ctx : activeJobs) {
            if (i++ < activeCnt)
                assert !((GridTestCollisionJobContext)ctx).isActivated() : i;
            else
                assert ((GridTestCollisionJobContext)ctx).isActivated() : i;

            assert !((GridTestCollisionJobContext)ctx).isCanceled();
        }
    }

    /**
     * @throws Exception If failed.
     */
    @Test
    public void testCollision1() throws Exception {

        getSpi().setParallelJobsNumber(32);

        GridCollisionTestContext cntx = createContext(0, 33);

        Collection<CollisionJobContext> activeJobs = cntx.activeJobs();
        Collection<CollisionJobContext> passiveJobs = cntx.waitingJobs();

        getSpi().onCollision(cntx);

        for (CollisionJobContext ctx : passiveJobs) {
            if (((GridTestCollisionJobContext)ctx).getIndex() == 32) {
                assert !((GridTestCollisionJobContext)ctx).isActivated();
                assert !((GridTestCollisionJobContext)ctx).isCanceled();
            }
            else {
                assert ((GridTestCollisionJobContext)ctx).isActivated();
                assert !((GridTestCollisionJobContext)ctx).isCanceled();
            }
        }

        assert activeJobs.size() == 32;
    }

    /**
     * @throws Exception If failed.
     */
    @Test
    public void testCollision2() throws Exception {
        getSpi().setParallelJobsNumber(3);

        GridCollisionTestContext cntx = createContext(11, 0);

        Collection<CollisionJobContext> activeJobs = cntx.activeJobs();
        Collection<CollisionJobContext> passiveJobs = cntx.waitingJobs();

        getSpi().onCollision(new GridCollisionTestContext(activeJobs, passiveJobs));

        for (CollisionJobContext ctx : activeJobs) {
            assert !((GridTestCollisionJobContext)ctx).isActivated();
            assert !((GridTestCollisionJobContext)ctx).isCanceled();
        }

        assert passiveJobs.isEmpty();
    }

    /**
     * @throws Exception If failed.
     */
    @Test
    public void testCollision3() throws Exception {
        getSpi().setParallelJobsNumber(15);

        GridCollisionTestContext cntx = createContext(10, 10);

        Collection<CollisionJobContext> activeJobs = cntx.activeJobs();
        Collection<CollisionJobContext> passiveJobs = cntx.waitingJobs();

        getSpi().onCollision(new GridCollisionTestContext(activeJobs, passiveJobs));

        int i = 0;

        for (CollisionJobContext ctx : activeJobs) {
            if (i++ < 10)
                assert !((GridTestCollisionJobContext)ctx).isActivated();
            else
                assert ((GridTestCollisionJobContext)ctx).isActivated();

            assert !((GridTestCollisionJobContext)ctx).isCanceled();
        }

        for (CollisionJobContext ctx : passiveJobs) {
            if (((GridTestCollisionJobContext)ctx).getIndex() < 5) {
                assert ((GridTestCollisionJobContext)ctx).isActivated();
                assert !((GridTestCollisionJobContext)ctx).isCanceled();
            }
            else {
                assert !((GridTestCollisionJobContext)ctx).isActivated();
                assert !((GridTestCollisionJobContext)ctx).isCanceled();
            }
        }
    }

    /**
     * Prepare test context.
     *
     * @param activeNum Number of active jobs in context.
     * @param passiveNum Number of passive jobs in context.
     * @return New context with the given numbers of jobs, which will update when one of jobs get activated.
     */
    private GridCollisionTestContext createContext(int activeNum, int passiveNum) {
        final Collection<CollisionJobContext> activeJobs = new ConcurrentLinkedQueue<>();

        for (int i = 0; i < activeNum; i++)
            activeJobs.add(new GridTestCollisionJobContext(new GridTestCollisionTaskSession(), i));

        final Collection<CollisionJobContext> passiveJobs = new ConcurrentLinkedQueue<>();

        for (int i = 0; i < passiveNum; i++)
            passiveJobs.add(new GridTestCollisionJobContext(new GridTestCollisionTaskSession(), i,
                new CI1<GridTestCollisionJobContext>() {
                    @Override public void apply(GridTestCollisionJobContext c) {
                        passiveJobs.remove(c);
                        activeJobs.add(c);
                    }
                }));

        return new GridCollisionTestContext(activeJobs, passiveJobs);
    }
}
