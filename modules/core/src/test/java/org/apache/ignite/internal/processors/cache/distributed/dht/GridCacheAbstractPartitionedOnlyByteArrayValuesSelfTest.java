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

package org.apache.ignite.internal.processors.cache.distributed.dht;

import java.util.Arrays;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.configuration.NearCacheConfiguration;
import org.apache.ignite.internal.processors.cache.distributed.GridCacheAbstractPartitionedByteArrayValuesSelfTest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.apache.ignite.cache.CacheAtomicityMode.ATOMIC;
import static org.junit.Assert.assertArrayEquals;

/**
 * Tests for byte array values in PARTITIONED-ONLY caches.
 */
@RunWith(JUnit4.class)
public abstract class GridCacheAbstractPartitionedOnlyByteArrayValuesSelfTest extends
    GridCacheAbstractPartitionedByteArrayValuesSelfTest {
    /** */
    public static final String ATOMIC_CACHE = "atomicCache";

    /** Atomic caches. */
    private static IgniteCache<Integer, Object>[] cachesAtomic;

    /** {@inheritDoc} */
    @Override protected IgniteConfiguration getConfiguration(String igniteInstanceName) throws Exception {
        IgniteConfiguration c = super.getConfiguration(igniteInstanceName);

        CacheConfiguration atomicCacheCfg = cacheConfiguration0();

        atomicCacheCfg.setName(ATOMIC_CACHE);
        atomicCacheCfg.setAtomicityMode(ATOMIC);

        int size = c.getCacheConfiguration().length;

        CacheConfiguration[] configs = Arrays.copyOf(c.getCacheConfiguration(), size + 1);

        configs[size] = atomicCacheCfg;

        c.setCacheConfiguration(configs);

        c.setPeerClassLoadingEnabled(peerClassLoading());

        return c;
    }

    /** {@inheritDoc} */
    @Override protected NearCacheConfiguration nearConfiguration() {
        return null;
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override protected void beforeTestsStarted() throws Exception {
        super.beforeTestsStarted();

        int gridCnt = gridCount();

        cachesAtomic = new IgniteCache[gridCnt];

        for (int i = 0; i < gridCount(); i++)
            cachesAtomic[i] = grid(i).cache(ATOMIC_CACHE);
    }

    /** {@inheritDoc} */
    @Override protected void afterTestsStopped() throws Exception {
        cachesAtomic = null;

        super.afterTestsStopped();
    }

    /**
     * Test atomic cache.
     *
     * @throws Exception If failed.
     */
    @Test
    public void testAtomic() throws Exception {
        testAtomic0(cachesAtomic);
    }

    /**
     * INternal routine for ATOMIC cache testing.
     *
     * @param caches Caches.
     * @throws Exception If failed.
     */
    private void testAtomic0(IgniteCache<Integer, Object>[] caches) throws Exception {
        byte[] val = wrap(1);

        for (IgniteCache<Integer, Object> cache : caches) {
            cache.put(KEY_1, val);

            for (IgniteCache<Integer, Object> cacheInner : caches)
                assertArrayEquals(val, (byte[])cacheInner.get(KEY_1));

            cache.remove(KEY_1);

            assertNull(cache.get(KEY_1));
        }
    }
}
