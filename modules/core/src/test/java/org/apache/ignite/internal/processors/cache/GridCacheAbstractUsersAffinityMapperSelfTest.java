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

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.cache.CacheAtomicityMode;
import org.apache.ignite.cache.CacheMode;
import org.apache.ignite.cache.CacheWriteSynchronizationMode;
import org.apache.ignite.cache.affinity.AffinityKeyMapped;
import org.apache.ignite.cache.affinity.AffinityKeyMapper;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.configuration.NearCacheConfiguration;
import org.apache.ignite.internal.util.GridArgumentCheck;
import org.apache.ignite.lang.IgniteRunnable;
import org.apache.ignite.testframework.junits.common.GridCommonAbstractTest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.apache.ignite.cache.CacheRebalanceMode.SYNC;

/**
 * Test affinity mapper.
 */
@RunWith(JUnit4.class)
public abstract class GridCacheAbstractUsersAffinityMapperSelfTest extends GridCommonAbstractTest {
    /** */
    private static final int KEY_CNT = 1000;

    /** */
    public static final AffinityKeyMapper AFFINITY_MAPPER = new UsersAffinityKeyMapper();

    /** */
    protected GridCacheAbstractUsersAffinityMapperSelfTest() {
        super(false /* doesn't start grid */);
    }

    /** {@inheritDoc} */
    @Override protected void afterTest() throws Exception {
        super.afterTest();

        stopAllGrids();
    }

    /** {@inheritDoc} */
    @Override protected IgniteConfiguration getConfiguration(String igniteInstanceName) throws Exception {
        IgniteConfiguration cfg = super.getConfiguration(igniteInstanceName);

        CacheConfiguration cacheCfg = new CacheConfiguration(DEFAULT_CACHE_NAME);

        cacheCfg.setCacheMode(getCacheMode());
        cacheCfg.setAtomicityMode(getAtomicMode());
        cacheCfg.setNearConfiguration(nearConfiguration());
        cacheCfg.setWriteSynchronizationMode(CacheWriteSynchronizationMode.FULL_SYNC);
        cacheCfg.setRebalanceMode(SYNC);
        cacheCfg.setAffinityMapper(AFFINITY_MAPPER);

        cfg.setCacheConfiguration(cacheCfg);

        return cfg;
    }

    /**
     * @return Distribution mode.
     */
    protected abstract NearCacheConfiguration nearConfiguration();

    /**
     * @return Cache atomicity mode.
     */
    protected abstract CacheAtomicityMode getAtomicMode();

    /**
     * @return Cache mode.
     */
    protected abstract CacheMode getCacheMode();

    /**
     * @throws Exception If failed.
     */
    @Test
    public void testAffinityMapper() throws Exception {
        IgniteCache<Object, Object> cache = startGrid(0).cache(DEFAULT_CACHE_NAME);

        for (int i = 0; i < KEY_CNT; i++) {
            cache.put(String.valueOf(i), String.valueOf(i));

            cache.put(new TestAffinityKey(i, String.valueOf(i)), i);
        }

        assertEquals(1, cache.get(new TestAffinityKey(1, "1")));

        startGrid(1);

        for (int i = 0; i < KEY_CNT; i++)
            grid(i % 2).compute().affinityRun(DEFAULT_CACHE_NAME, new TestAffinityKey(1, "1"), new NoopClosure());
    }

    /**
     * Test key for field annotation.
     */
    private static class TestAffinityKey implements Externalizable {
        /** Key. */
        private int key;

        /** Affinity key. */
        @AffinityKeyMapped
        private String affKey;

        /**
         * Constructor.
         */
        public TestAffinityKey() {
        }

        /**
         * Constructor.
         *
         * @param key Key.
         * @param affKey Affinity key.
         */
        TestAffinityKey(int key, String affKey) {
            this.key = key;
            this.affKey = affKey;
        }

        /**
         * @return Key.
         */
        public int key() {
            return key;
        }

        /**
         * @return Affinity key.
         */
        public String affinityKey() {
            return affKey;
        }

        /** {@inheritDoc} */
        @Override public boolean equals(Object o) {
            return o instanceof TestAffinityKey && key == ((TestAffinityKey)o).key();
        }

        /** {@inheritDoc} */
        @Override public int hashCode() {
            return key + affKey.hashCode();
        }

        /** {@inheritDoc} */
        @Override public void writeExternal(ObjectOutput out) throws IOException {
            out.writeInt(key);
            out.writeUTF(affKey);
        }

        /** {@inheritDoc} */
        @Override public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
            key = in.readInt();
            affKey = in.readUTF();
        }
    }

    /**
     * Users affinity mapper.
     */
    private static class UsersAffinityKeyMapper extends GridCacheDefaultAffinityKeyMapper{
        /** {@inheritDoc} */
        @Override public Object affinityKey(Object key) {
            GridArgumentCheck.notNull(key, "key");

            assertFalse("GridCacheInternal entry mustn't be passed in user's key mapper.",
                key instanceof GridCacheInternal);

            return super.affinityKey(key);
        }
    }

    /**
     * Noop closure.
     */
    private static class NoopClosure implements IgniteRunnable {
        /** {@inheritDoc} */
        @Override public void run() {
            // No-op.
        }
    }
}
