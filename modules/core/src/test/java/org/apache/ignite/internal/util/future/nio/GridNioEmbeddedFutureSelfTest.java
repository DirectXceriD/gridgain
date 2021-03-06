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

package org.apache.ignite.internal.util.future.nio;

import org.apache.ignite.internal.util.nio.GridNioEmbeddedFuture;
import org.apache.ignite.internal.util.nio.GridNioFutureImpl;
import org.apache.ignite.testframework.junits.common.GridCommonAbstractTest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * Test for NIO embedded future.
 */
@RunWith(JUnit4.class)
public class GridNioEmbeddedFutureSelfTest extends GridCommonAbstractTest {
    /**
     * @throws Exception If failed.
     */
    @Test
    public void testNioEmbeddedFuture() throws Exception {
        // Original future.
        final GridNioFutureImpl<Integer> origFut = new GridNioFutureImpl<>(null);

        // Embedded future to test.
        final GridNioEmbeddedFuture<Integer> embFut = new GridNioEmbeddedFuture<>();

        embFut.onDone(origFut, null);

        assertFalse("Expect original future is not complete.", origFut.isDone());

        // Finish original future in separate thread.
        Thread t = new Thread() {
            @Override public void run() {
                origFut.onDone(100);
            }
        };

        t.start();
        t.join();

        assertTrue("Expect original future is complete.", origFut.isDone());
        assertTrue("Expect embedded future is complete.", embFut.isDone());

        // Wait for embedded future completes.
        assertEquals(new Integer(100), embFut.get(1, SECONDS));
    }
}
