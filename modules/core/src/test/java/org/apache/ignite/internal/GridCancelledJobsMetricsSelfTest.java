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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCheckedException;
import org.apache.ignite.IgniteException;
import org.apache.ignite.IgniteLogger;
import org.apache.ignite.compute.ComputeJob;
import org.apache.ignite.compute.ComputeJobAdapter;
import org.apache.ignite.compute.ComputeJobResult;
import org.apache.ignite.compute.ComputeTaskFuture;
import org.apache.ignite.compute.ComputeTaskSplitAdapter;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.internal.util.lang.GridAbsPredicate;
import org.apache.ignite.internal.util.typedef.G;
import org.apache.ignite.internal.util.typedef.X;
import org.apache.ignite.resources.LoggerResource;
import org.apache.ignite.spi.IgniteSpiAdapter;
import org.apache.ignite.spi.IgniteSpiException;
import org.apache.ignite.spi.IgniteSpiMultipleInstancesSupport;
import org.apache.ignite.spi.collision.CollisionContext;
import org.apache.ignite.spi.collision.CollisionExternalListener;
import org.apache.ignite.spi.collision.CollisionJobContext;
import org.apache.ignite.spi.collision.CollisionSpi;
import org.apache.ignite.spi.discovery.DiscoverySpi;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.apache.ignite.testframework.GridTestUtils;
import org.apache.ignite.testframework.junits.common.GridCommonAbstractTest;
import org.apache.ignite.testframework.junits.common.GridCommonTest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Cancelled jobs metrics self test.
 */
@GridCommonTest(group = "Kernal Self")
@RunWith(JUnit4.class)
public class GridCancelledJobsMetricsSelfTest extends GridCommonAbstractTest {
    /** */
    private static GridCancelCollisionSpi colSpi = new GridCancelCollisionSpi();

    /** */
    public GridCancelledJobsMetricsSelfTest() {
        super(true);
    }


    /** {@inheritDoc} */
    @Override protected IgniteConfiguration getConfiguration() throws Exception {
        IgniteConfiguration cfg = super.getConfiguration();

        cfg.setCollisionSpi(colSpi);

        DiscoverySpi discoSpi = cfg.getDiscoverySpi();

        assert discoSpi instanceof TcpDiscoverySpi;

        cfg.setMetricsUpdateFrequency(500);

        return cfg;
    }

    /**
     * @throws Exception If failed.
     */
    @Test
    public void testCancelledJobs() throws Exception {
        final Ignite ignite = G.ignite(getTestIgniteInstanceName());

        Collection<ComputeTaskFuture<?>> futs = new ArrayList<>();

        for (int i = 1; i <= 10; i++)
            futs.add(ignite.compute().executeAsync(CancelledTask.class, null));

        // Wait to be sure that metrics were updated.
        GridTestUtils.waitForCondition(new GridAbsPredicate() {
            @Override public boolean apply() {
                return ignite.cluster().localNode().metrics().getTotalCancelledJobs() > 0;
            }
        }, 5000);

        colSpi.externalCollision();

        for (ComputeTaskFuture<?> fut : futs) {
            try {
                fut.get();

                assert false : "Job was not interrupted.";
            }
            catch (IgniteException e) {
                if (e.hasCause(InterruptedException.class))
                    throw new IgniteCheckedException("Test run has been interrupted.", e);

                info("Caught expected exception: " + e.getMessage());
            }
        }

        // Job was cancelled and now we need to calculate metrics.
        int totalCancelledJobs = ignite.cluster().localNode().metrics().getTotalCancelledJobs();

        assert totalCancelledJobs == 10 : "Metrics were not updated. Expected 10 got " + totalCancelledJobs;
    }

    /**
     *
     */
    private static final class CancelledTask extends ComputeTaskSplitAdapter<String, Object> {
        /** {@inheritDoc} */
        @Override protected Collection<? extends ComputeJob> split(int gridSize, String arg) {
            return Arrays.asList(new GridCancelledJob());
        }

        /** {@inheritDoc} */
        @Override public Object reduce(List<ComputeJobResult> results) {
            assert results.get(0).isCancelled() : "Wrong job result status.";

            return null;
        }
    }

    /**
     *
     */
    private static final class GridCancelledJob extends ComputeJobAdapter {
        /** {@inheritDoc} */
        @Override public String execute() {
            X.println("Executing job.");

            try {
                Thread.sleep(Long.MAX_VALUE);
            }
            catch (InterruptedException ignored) {
                try {
                    Thread.sleep(1000);
                }
                catch (InterruptedException e1) {
                    throw new IgniteException("Unexpected exception: ", e1);
                }

                throw new IgniteException("Job got interrupted while waiting for cancellation.");
            }
            finally {
                X.println("Finished job.");
            }

            return null;
        }
    }

    /**
     *
     */
    @IgniteSpiMultipleInstancesSupport(true)
    private static class GridCancelCollisionSpi extends IgniteSpiAdapter
        implements CollisionSpi {
        /** */
        @LoggerResource
        private IgniteLogger log;

        /** */
        private CollisionExternalListener lsnr;

        /** {@inheritDoc} */
        @Override public void onCollision(CollisionContext ctx) {
            Collection<CollisionJobContext> activeJobs = ctx.activeJobs();
            Collection<CollisionJobContext> waitJobs = ctx.waitingJobs();

            for (CollisionJobContext job : waitJobs)
                job.activate();

            for (CollisionJobContext job : activeJobs) {
                log.info("Cancelling job : " + job.getJob());

                job.cancel();
            }
        }

        /** {@inheritDoc} */
        @Override public void spiStart(String igniteInstanceName) throws IgniteSpiException {
            // Start SPI start stopwatch.
            startStopwatch();

            // Ack start.
            if (log.isInfoEnabled())
                log.info(startInfo());
        }

        /** {@inheritDoc} */
        @Override public void spiStop() throws IgniteSpiException {
            // Ack stop.
            if (log.isInfoEnabled())
                log.info(stopInfo());
        }

        /** {@inheritDoc} */
        @Override public void setExternalCollisionListener(CollisionExternalListener lsnr) {
            this.lsnr = lsnr;
        }

        /** */
        public void externalCollision() {
            CollisionExternalListener tmp = lsnr;

            if (tmp != null)
                tmp.onExternalCollision();
        }
    }
}
