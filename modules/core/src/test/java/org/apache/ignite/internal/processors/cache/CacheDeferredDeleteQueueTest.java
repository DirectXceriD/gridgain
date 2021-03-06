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
import org.apache.ignite.configuration.NearCacheConfiguration;
import org.apache.ignite.internal.IgniteKernal;
import org.apache.ignite.internal.processors.cache.distributed.dht.topology.GridDhtLocalPartition;
import org.apache.ignite.internal.processors.cache.distributed.dht.topology.GridDhtPartitionTopology;
import org.apache.ignite.internal.util.lang.GridAbsPredicate;
import org.apache.ignite.testframework.GridTestUtils;
import org.apache.ignite.testframework.junits.common.GridCommonAbstractTest;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.apache.ignite.IgniteSystemProperties.IGNITE_CACHE_REMOVED_ENTRIES_TTL;
import static org.apache.ignite.cache.CacheAtomicityMode.ATOMIC;
import static org.apache.ignite.cache.CacheAtomicityMode.TRANSACTIONAL;
import static org.apache.ignite.cache.CacheAtomicityMode.TRANSACTIONAL_SNAPSHOT;
import static org.apache.ignite.cache.CacheMode.PARTITIONED;
import static org.apache.ignite.cache.CacheWriteSynchronizationMode.FULL_SYNC;

/**
 *
 */
@RunWith(JUnit4.class)
public class CacheDeferredDeleteQueueTest extends GridCommonAbstractTest {
    /** */
    private static String ttlProp;

    /** */
    private static int NODES = 2;

    /** {@inheritDoc} */
    @Override protected void beforeTestsStarted() throws Exception {
        ttlProp = System.getProperty(IGNITE_CACHE_REMOVED_ENTRIES_TTL);

        System.setProperty(IGNITE_CACHE_REMOVED_ENTRIES_TTL, "1000");

        startGridsMultiThreaded(NODES);
    }

    /** {@inheritDoc} */
    @Override protected void afterTestsStopped() throws Exception {
        if (ttlProp != null)
            System.setProperty(IGNITE_CACHE_REMOVED_ENTRIES_TTL, ttlProp);
        else
            System.clearProperty(IGNITE_CACHE_REMOVED_ENTRIES_TTL);
    }

    /**
     * @throws Exception If failed.
     */
    @Test
    public void testDeferredDeleteQueue() throws Exception {
        testQueue(ATOMIC, false);

        testQueue(TRANSACTIONAL, false);

        testQueue(TRANSACTIONAL_SNAPSHOT, false);

        testQueue(ATOMIC, true);

        testQueue(TRANSACTIONAL, true);
    }

    /**
     * @throws Exception If failed.
     */
    @Ignore("https://issues.apache.org/jira/browse/IGNITE-7187")
    @Test
    public void testDeferredDeleteQueueMvcc() throws Exception {
        testQueue(TRANSACTIONAL_SNAPSHOT, true);
    }

    /**
     * @param atomicityMode Cache atomicity mode.
     * @param nearCache {@code True} if need create near cache.
     *
     * @throws Exception If failed.
     */
    private void testQueue(CacheAtomicityMode atomicityMode, boolean nearCache) throws Exception {
        CacheConfiguration<Integer, Integer> ccfg = new CacheConfiguration<>(DEFAULT_CACHE_NAME);

        ccfg.setCacheMode(PARTITIONED);
        ccfg.setAtomicityMode(atomicityMode);
        ccfg.setWriteSynchronizationMode(FULL_SYNC);
        ccfg.setBackups(1);

        if (nearCache)
            ccfg.setNearConfiguration(new NearCacheConfiguration<Integer, Integer>());

        IgniteCache<Integer, Integer> cache = ignite(0).createCache(ccfg);

        try {
            final int KEYS = cache.getConfiguration(CacheConfiguration.class).getAffinity().partitions() * 3;

            for (int i = 0; i < KEYS; i++)
                cache.put(i, i);

            for (int i = 0; i < KEYS; i++)
                cache.remove(i);

            boolean wait = GridTestUtils.waitForCondition(new GridAbsPredicate() {
                @Override public boolean apply() {
                    for (int i = 0; i < NODES; i++) {
                        final GridDhtPartitionTopology top =
                            ((IgniteKernal)ignite(i)).context().cache().cache(DEFAULT_CACHE_NAME).context().topology();

                        for (GridDhtLocalPartition p : top.currentLocalPartitions()) {
                            Collection<Object> rmvQueue = GridTestUtils.getFieldValue(p, "rmvQueue");

                            if (!rmvQueue.isEmpty() || p.dataStore().fullSize() != 0)
                                return false;
                        }
                    }

                    return true;
                }
            }, 5000);

            assertTrue("Failed to wait for rmvQueue cleanup.", wait);
        }
        finally {
            ignite(0).destroyCache(ccfg.getName());
        }
    }
}
