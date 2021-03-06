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

package org.apache.ignite.internal.util;

import java.util.Collections;
import java.util.List;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteLogger;
import org.apache.ignite.IgniteSystemProperties;
import org.apache.ignite.lang.IgniteRunnable;
import org.apache.ignite.resources.IgniteInstanceResource;
import org.apache.ignite.testframework.GridStringLogger;
import org.apache.ignite.testframework.junits.common.GridCommonAbstractTest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Testing logging via {@link IgniteUtils#warnDevOnly(IgniteLogger, Object)}.
 */
@RunWith(JUnit4.class)
public class IgniteDevOnlyLogTest extends GridCommonAbstractTest {
    /** */
    private List<String> additionalArgs;

    /** {@inheritDoc} */
    @Override protected void beforeTest() throws Exception {
        startGrid(0);
    }

    /** {@inheritDoc} */
    @Override protected void afterTest() throws Exception {
        stopAllGrids();
    }

    /** {@inheritDoc} */
    @Override protected boolean isMultiJvm() {
        return true;
    }

    /** {@inheritDoc} */
    @Override protected List<String> additionalRemoteJvmArgs() {
        return additionalArgs;
    }

    /** Check that dev-only messages appear in the log. */
    @Test
    public void testDevOnlyQuietMessage() throws Exception {
        fail("https://issues.apache.org/jira/browse/IGNITE-9328");

        additionalArgs = Collections.singletonList("-D" + IgniteSystemProperties.IGNITE_QUIET + "=true");

        log = new GridStringLogger(false, grid(0).log());

        Ignite ignite = startGrid(1);

        String msg = getMessage(ignite);

        warnDevOnly(msg);

        assertTrue(log.toString().contains(msg));
    }

    /** Check that dev-only messages appear in the log. */
    @Test
    public void testDevOnlyVerboseMessage() throws Exception {
        additionalArgs = Collections.singletonList("-D" + IgniteSystemProperties.IGNITE_QUIET + "=false");

        log = new GridStringLogger(false, grid(0).log());

        Ignite ignite = startGrid(1);

        String msg = getMessage(ignite);

        warnDevOnly(msg);

        assertTrue(log.toString().contains(msg));
    }

    /**
     * Check that {@link IgniteUtils#warnDevOnly(IgniteLogger, Object)}
     * doesn't print anything if {@link org.apache.ignite.IgniteSystemProperties#IGNITE_DEV_ONLY_LOGGING_DISABLED}
     * is set to {@code true}.
     */
    @Test
    public void testDevOnlyDisabledProperty() throws Exception {
        additionalArgs = Collections.singletonList("-D" +
            IgniteSystemProperties.IGNITE_DEV_ONLY_LOGGING_DISABLED + "=true");

        log = new GridStringLogger(false, grid(0).log());

        Ignite ignite = startGrid(1);

        String msg = getMessage(ignite);

        warnDevOnly(msg);

        assertFalse(log.toString().contains(msg));
    }

    /** */
    private void warnDevOnly(final String msg) {
        grid(0).compute(grid(0).cluster().forRemotes()).broadcast(new IgniteRunnable() {
            @IgniteInstanceResource
            private Ignite ignite;

            @Override public void run() {
                IgniteUtils.warnDevOnly(ignite.log(), msg);
            }
        });
    }

    /** Returns a test message. */
    private String getMessage(Ignite ignite) {
        // use node id in the message to avoid interference with other tests
        return "My id is " + ignite.cluster().localNode().id();
    }
}
