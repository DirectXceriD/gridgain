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

package org.apache.ignite.examples.misc.lifecycle;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteException;
import org.apache.ignite.Ignition;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.lifecycle.LifecycleBean;
import org.apache.ignite.lifecycle.LifecycleEventType;
import org.apache.ignite.resources.IgniteInstanceResource;

import static org.apache.ignite.lifecycle.LifecycleEventType.AFTER_NODE_START;
import static org.apache.ignite.lifecycle.LifecycleEventType.AFTER_NODE_STOP;

/**
 * This example shows how to provide your own {@link LifecycleBean} implementation
 * to be able to hook into Ignite lifecycle. The {@link LifecycleExampleBean} bean
 * will output occurred lifecycle events to the console.
 * <p>
 * This example does not require remote nodes to be started.
 */
public final class LifecycleExample {
    /**
     * Executes example.
     *
     * @param args Command line arguments, none required.
     * @throws IgniteException If example execution failed.
     */
    public static void main(String[] args) throws IgniteException {
        System.out.println();
        System.out.println(">>> Lifecycle example started.");

        // Create new configuration.
        IgniteConfiguration cfg = new IgniteConfiguration();

        LifecycleExampleBean bean = new LifecycleExampleBean();

        // Provide lifecycle bean to configuration.
        cfg.setLifecycleBeans(bean);

        try (Ignite ignite  = Ignition.start(cfg)) {
            // Make sure that lifecycle bean was notified about ignite startup.
            assert bean.isStarted();
        }

        // Make sure that lifecycle bean was notified about ignite stop.
        assert !bean.isStarted();
    }

    /**
     * Simple {@link LifecycleBean} implementation that outputs event type when it is occurred.
     */
    public static class LifecycleExampleBean implements LifecycleBean {
        /** Auto-inject ignite instance. */
        @IgniteInstanceResource
        private Ignite ignite;

        /** Started flag. */
        private boolean isStarted;

        /** {@inheritDoc} */
        @Override public void onLifecycleEvent(LifecycleEventType evt) {
            System.out.println();
            System.out.println(">>> Lifecycle event occurred: " + evt);
            System.out.println(">>> Ignite name: " + ignite.name());

            if (evt == AFTER_NODE_START)
                isStarted = true;
            else if (evt == AFTER_NODE_STOP)
                isStarted = false;
        }

        /**
         * @return {@code True} if ignite has been started.
         */
        public boolean isStarted() {
            return isStarted;
        }
    }
}