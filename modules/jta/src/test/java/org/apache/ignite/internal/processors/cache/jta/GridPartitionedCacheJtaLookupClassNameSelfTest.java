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

package org.apache.ignite.internal.processors.cache.jta;

import java.util.concurrent.Callable;
import javax.transaction.TransactionManager;
import org.apache.ignite.IgniteException;
import org.apache.ignite.cache.CacheAtomicityMode;
import org.apache.ignite.cache.jta.CacheTmLookup;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.internal.IgniteEx;
import org.apache.ignite.testframework.GridTestUtils;
import org.apache.ignite.testsuites.IgniteIgnore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Lookup class name based JTA integration test using PARTITIONED cache.
 */
@RunWith(JUnit4.class)
public class GridPartitionedCacheJtaLookupClassNameSelfTest extends AbstractCacheJtaSelfTest {
    /** {@inheritDoc} */
    @Override protected void configureJta(IgniteConfiguration cfg) {
        cfg.getTransactionConfiguration().setTxManagerLookupClassName(TestTmLookup.class.getName());
    }

    /**
     *
     */
    @IgniteIgnore(value = "https://issues.apache.org/jira/browse/IGNITE-10723", forceFailure = true)
    @Test
    public void testIncompatibleTmLookup() {
        final IgniteEx ignite = grid(0);

        final CacheConfiguration cacheCfg = new CacheConfiguration(DEFAULT_CACHE_NAME);

        cacheCfg.setName("Foo");
        cacheCfg.setAtomicityMode(CacheAtomicityMode.TRANSACTIONAL);
        cacheCfg.setTransactionManagerLookupClassName(TestTmLookup2.class.getName());

        GridTestUtils.assertThrows(log, new Callable<Object>() {
            @Override public Object call() throws IgniteException {
                ignite.createCache(cacheCfg);

                return null;
            }
        }, IgniteException.class, null);
    }

    /**
     *
     */
    @SuppressWarnings("PublicInnerClass")
    public static class TestTmLookup implements CacheTmLookup {
        /** {@inheritDoc} */
        @Override public TransactionManager getTm() {
            return jotm.getTransactionManager();
        }
    }

    /**
     *
     */
    @SuppressWarnings("PublicInnerClass")
    public static class TestTmLookup2 implements CacheTmLookup {
        /** {@inheritDoc} */
        @Override public TransactionManager getTm() {
            return null;
        }
    }
}
