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
import org.apache.ignite.Ignite;
import org.apache.ignite.cache.CacheMode;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.testframework.junits.common.GridCommonAbstractTest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Test that validates {@link Ignite#cacheNames()} implementation.
 */
@RunWith(JUnit4.class)
public class CacheNamesSelfTest extends GridCommonAbstractTest {
    /** */
    private boolean client;

    /** {@inheritDoc} */
    @Override protected IgniteConfiguration getConfiguration(String igniteInstanceName) throws Exception {
        IgniteConfiguration cfg = super.getConfiguration(igniteInstanceName);

        CacheConfiguration cacheCfg1 = new CacheConfiguration(DEFAULT_CACHE_NAME);
        cacheCfg1.setCacheMode(CacheMode.REPLICATED);
        cacheCfg1.setName("replicated");

        CacheConfiguration cacheCfg2 = new CacheConfiguration(DEFAULT_CACHE_NAME);
        cacheCfg2.setCacheMode(CacheMode.PARTITIONED);
        cacheCfg2.setName("partitioned");

        CacheConfiguration cacheCfg3 = new CacheConfiguration(DEFAULT_CACHE_NAME);
        cacheCfg3.setCacheMode(CacheMode.LOCAL);

        if (client)
            cfg.setClientMode(true);
        else
            cfg.setCacheConfiguration(cacheCfg1, cacheCfg2, cacheCfg3);

        return cfg;
    }

    /**
     * @throws Exception In case of failure.
     */
    @Test
    public void testCacheNames() throws Exception {
        try {
            startGridsMultiThreaded(2);

            Collection<String> names = grid(0).cacheNames();

            assertEquals(3, names.size());

            for (String name : names)
                assertTrue(DEFAULT_CACHE_NAME.equals(name) || name.equals("replicated") || name.equals("partitioned"));

            client = true;

            Ignite client = startGrid(2);

            names = client.cacheNames();

            assertEquals(3, names.size());
        }
        finally {
            stopAllGrids();
        }
    }
}
