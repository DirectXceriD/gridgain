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

package org.apache.ignite.internal.processors.service;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;

import org.apache.ignite.Ignite;
import org.apache.ignite.Ignition;
import org.apache.ignite.cluster.ClusterGroup;
import org.apache.ignite.cluster.ClusterNode;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.internal.IgniteInternalFuture;
import org.apache.ignite.internal.binary.BinaryMarshaller;
import org.apache.ignite.lang.IgnitePredicate;
import org.apache.ignite.services.Service;
import org.apache.ignite.services.ServiceContext;
import org.apache.ignite.testframework.GridTestUtils;
import org.apache.ignite.testframework.junits.common.GridCommonAbstractTest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.apache.ignite.cache.CacheAtomicityMode.ATOMIC;
import static org.apache.ignite.cache.CacheMode.REPLICATED;
import static org.apache.ignite.cache.CacheWriteSynchronizationMode.FULL_SYNC;

/**
 *
 */
@RunWith(JUnit4.class)
public class ServicePredicateAccessCacheTest extends GridCommonAbstractTest {
    /** */
    private static CountDownLatch latch;

    /** {@inheritDoc} */
    @Override protected IgniteConfiguration getConfiguration(String igniteInstanceName) throws Exception {
        IgniteConfiguration cfg = super.getConfiguration(igniteInstanceName);

        cfg.setMarshaller(new BinaryMarshaller());

        cfg.setPeerClassLoadingEnabled(false);

        return cfg;
    }

    /** {@inheritDoc} */
    @Override protected long getTestTimeout() {
        return 60_000;
    }

    /**
     * @throws Exception If failed.
     */
    @Test
    public void testPredicateAccessCache() throws Exception {
        final Ignite ignite0 = startGrid(0);

        ignite0.getOrCreateCache(new CacheConfiguration<>(DEFAULT_CACHE_NAME)
            .setName("testCache")
            .setAtomicityMode(ATOMIC)
            .setCacheMode(REPLICATED)
            .setWriteSynchronizationMode(FULL_SYNC));

        latch = new CountDownLatch(1);

        final ClusterGroup grp = ignite0.cluster().forPredicate(new IgnitePredicate<ClusterNode>() {
            @Override public boolean apply(ClusterNode node) {
                System.out.println("Predicated started [thread=" + Thread.currentThread().getName() + ']');

                latch.countDown();

                try {
                    Thread.sleep(3000);
                }
                catch (InterruptedException ignore) {
                    // No-op.
                }

                System.out.println("Call contains key [thread=" + Thread.currentThread().getName() + ']');

                boolean ret = Ignition.localIgnite().cache("testCache").containsKey(node.id().toString());

                System.out.println("After contains key [ret=" + ret +
                    ", thread=" + Thread.currentThread().getName() + ']');

                return ret;
            }
        });

        IgniteInternalFuture<?> fut = GridTestUtils.runAsync(new Callable<Void>() {
            @Override public Void call() throws Exception {
                info("Start deploy service.");

                ignite0.services(grp).deployNodeSingleton("testService", new TestService());

                info("Service deployed.");

                return null;
            }
        }, "deploy-thread");

        latch.await();

        startGrid(1);

        fut.get();
    }

    /**
     *
     */
    public static class TestService implements Service {
        /** {@inheritDoc} */
        @Override public void execute(ServiceContext ctx) {
            // No-op.
        }

        /** {@inheritDoc} */
        @Override public void init(ServiceContext ctx) {
            // No-op.
        }

        /** {@inheritDoc} */
        @Override public void cancel(ServiceContext ctx) {
            // No-op.
        }
    }
}
