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

package org.apache.ignite.internal.processors.cache.distributed;

import java.util.Objects;
import java.util.Random;
import org.apache.ignite.cache.query.ScanQuery;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.apache.ignite.cache.CacheAtomicityMode.ATOMIC;
import static org.apache.ignite.cache.CacheAtomicityMode.TRANSACTIONAL;
import static org.apache.ignite.cache.CacheMode.PARTITIONED;
import static org.apache.ignite.cache.CacheMode.REPLICATED;

/**
 *
 */
@RunWith(JUnit4.class)
public class CacheBlockOnScanTest extends CacheBlockOnReadAbstractTest {

    /** {@inheritDoc} */
    @Override @NotNull protected CacheReadBackgroundOperation<?, ?> getReadOperation() {
        return new IntCacheReadBackgroundOperation() {
            /** Random. */
            private Random random = new Random();

            /** {@inheritDoc} */
            @Override public void doRead() {
                int idx = random.nextInt(entriesCount());

                cache().query(new ScanQuery<>((k, v) -> Objects.equals(k, idx))).getAll();
            }
        };
    }

    /** {@inheritDoc} */
    @Params(baseline = 9, atomicityMode = ATOMIC, cacheMode = PARTITIONED, allowException = true)
    @Test
    @Override public void testStopBaselineAtomicPartitioned() throws Exception {
        super.testStopBaselineAtomicPartitioned();
    }

    /** {@inheritDoc} */
    @Params(baseline = 9, atomicityMode = ATOMIC, cacheMode = REPLICATED, allowException = true)
    @Test
    @Override public void testStopBaselineAtomicReplicated() throws Exception {
        super.testStopBaselineAtomicReplicated();
    }

    /** {@inheritDoc} */
    @Params(baseline = 9, atomicityMode = TRANSACTIONAL, cacheMode = PARTITIONED, allowException = true)
    @Test
    @Override public void testStopBaselineTransactionalPartitioned() throws Exception {
        super.testStopBaselineTransactionalPartitioned();
    }

    /** {@inheritDoc} */
    @Params(baseline = 9, atomicityMode = TRANSACTIONAL, cacheMode = REPLICATED, allowException = true)
    @Test
    @Override public void testStopBaselineTransactionalReplicated() throws Exception {
        super.testStopBaselineTransactionalReplicated();
    }

    /** {@inheritDoc} */
    @Params(baseline = 1, atomicityMode = ATOMIC, cacheMode = REPLICATED)
    @Test
    @Override public void testStartClientAtomicReplicated() {
        fail("https://issues.apache.org/jira/browse/IGNITE-9987");
    }

    /** {@inheritDoc} */
    @Params(baseline = 1, atomicityMode = TRANSACTIONAL, cacheMode = REPLICATED)
    @Test
    @Override public void testStartClientTransactionalReplicated() {
        fail("https://issues.apache.org/jira/browse/IGNITE-9987");
    }

    /** {@inheritDoc} */
    @Params(baseline = 1, atomicityMode = ATOMIC, cacheMode = REPLICATED)
    @Test
    @Override public void testStopClientAtomicReplicated() {
        fail("https://issues.apache.org/jira/browse/IGNITE-9987");
    }

    /** {@inheritDoc} */
    @Params(baseline = 1, atomicityMode = TRANSACTIONAL, cacheMode = REPLICATED)
    @Test
    @Override public void testStopClientTransactionalReplicated() {
        fail("https://issues.apache.org/jira/browse/IGNITE-9987");
    }

    /** {@inheritDoc} */
    @Params(atomicityMode = ATOMIC, cacheMode = PARTITIONED)
    @Test
    @Override public void testStartClientAtomicPartitioned() throws Exception {
        super.testStartClientTransactionalReplicated();
    }

    /** {@inheritDoc} */
    @Params(atomicityMode = TRANSACTIONAL, cacheMode = PARTITIONED)
    @Test
    @Override public void testStartClientTransactionalPartitioned() throws Exception {
        super.testStartClientTransactionalReplicated();
    }

    /** {@inheritDoc} */
    @Params(atomicityMode = ATOMIC, cacheMode = PARTITIONED)
    @Test
    @Override public void testStopClientAtomicPartitioned() throws Exception {
        super.testStopClientTransactionalReplicated();
    }

    /** {@inheritDoc} */
    @Params(atomicityMode = TRANSACTIONAL, cacheMode = PARTITIONED)
    @Test
    @Override public void testStopClientTransactionalPartitioned() throws Exception {
        super.testStopClientTransactionalReplicated();
    }
}
