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

package org.apache.ignite.internal.processors.cache;

import java.util.Collection;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.cache.CacheAtomicityMode;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.configuration.NearCacheConfiguration;
import org.apache.ignite.internal.IgniteKernal;
import org.apache.ignite.internal.TestRecordingCommunicationSpi;
import org.apache.ignite.internal.processors.cache.distributed.near.GridNearCacheEntry;
import org.apache.ignite.internal.processors.cache.distributed.near.GridNearLockRequest;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.apache.ignite.testframework.MvccFeatureChecker;
import org.apache.ignite.testframework.junits.common.GridCommonAbstractTest;
import org.apache.ignite.transactions.Transaction;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.apache.ignite.transactions.TransactionConcurrency.PESSIMISTIC;
import static org.apache.ignite.transactions.TransactionIsolation.REPEATABLE_READ;

/**
 *
 */
@RunWith(JUnit4.class)
public class IgniteCacheNearLockValueSelfTest extends GridCommonAbstractTest {
    /** {@inheritDoc} */
    @Override protected void beforeTestsStarted() throws Exception {
        MvccFeatureChecker.failIfNotSupported(MvccFeatureChecker.Feature.NEAR_CACHE);

        startGrid(1);

        startGrid(0);
    }

    /** {@inheritDoc} */
    @Override protected IgniteConfiguration getConfiguration(String igniteInstanceName) throws Exception {
        IgniteConfiguration cfg = super.getConfiguration(igniteInstanceName);

        ((TcpDiscoverySpi)cfg.getDiscoverySpi()).setForceServerMode(true);

        if (getTestIgniteInstanceName(0).equals(igniteInstanceName))
            cfg.setClientMode(true);

        TestRecordingCommunicationSpi commSpi = new TestRecordingCommunicationSpi();

        commSpi.record(GridNearLockRequest.class);

        cfg.setCommunicationSpi(commSpi);

        return cfg;
    }

    /**
     * @throws Exception If failed.
     */
    @Test
    public void testDhtVersion() throws Exception {
        CacheConfiguration<Object, Object> pCfg = new CacheConfiguration<>("partitioned");

        pCfg.setAtomicityMode(CacheAtomicityMode.TRANSACTIONAL);

        try (IgniteCache<Object, Object> cache = ignite(0).getOrCreateCache(pCfg, new NearCacheConfiguration<>())) {
            cache.put("key1", "val1");

            for (int i = 0; i < 3; i++) {
                try (Transaction tx = ignite(0).transactions().txStart(PESSIMISTIC, REPEATABLE_READ)) {
                    cache.get("key1");

                    tx.commit();
                }

                TestRecordingCommunicationSpi comm =
                    (TestRecordingCommunicationSpi)ignite(0).configuration().getCommunicationSpi();

                Collection<GridNearLockRequest> reqs = (Collection)comm.recordedMessages(false);

                assertEquals(1, reqs.size());

                GridCacheAdapter<Object, Object> primary = ((IgniteKernal)grid(1)).internalCache("partitioned");

                GridCacheEntryEx dhtEntry = primary.peekEx(primary.context().toCacheKeyObject("key1"));

                assertNotNull(dhtEntry);

                GridNearLockRequest req = reqs.iterator().next();

                assertEquals(dhtEntry.version(), req.dhtVersion(0));

                // Check entry version in near cache after commit.
                GridCacheAdapter<Object, Object> near = ((IgniteKernal)grid(0)).internalCache("partitioned");

                GridNearCacheEntry nearEntry = (GridNearCacheEntry)near.peekEx(near.context().toCacheKeyObject("key1"));

                assertNotNull(nearEntry);

                assertEquals(dhtEntry.version(), nearEntry.dhtVersion());
            }
        }
    }
}
