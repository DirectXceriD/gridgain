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

package org.apache.ignite.examples.misc.schedule;

import java.util.concurrent.Callable;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteException;
import org.apache.ignite.Ignition;
import org.apache.ignite.examples.ExampleNodeStartup;
import org.apache.ignite.lang.IgniteRunnable;
import org.apache.ignite.scheduler.SchedulerFuture;

/**
 * Demonstrates a cron-based {@link Runnable} execution scheduling.
 * Test runnable object broadcasts a phrase to all cluster nodes every minute
 * three times with initial scheduling delay equal to five seconds.
 * <p>
 * Remote nodes should always be started with special configuration file which
 * enables P2P class loading: {@code 'ignite.{sh|bat} examples/config/example-ignite.xml'}.
 * <p>
 * Alternatively you can run {@link ExampleNodeStartup} in another JVM which will start node
 * with {@code examples/config/example-ignite.xml} configuration.
 */
public class ComputeScheduleExample {
    /**
     * Executes example.
     *
     * @param args Command line arguments, none required.
     * @throws IgniteException If example execution failed.
     */
    public static void main(String[] args) throws IgniteException {
        try (Ignite ignite = Ignition.start("examples/config/example-ignite.xml")) {
            System.out.println();
            System.out.println("Compute schedule example started.");

            // Schedule output message every minute.
            SchedulerFuture<?> fut = ignite.scheduler().scheduleLocal(
                new Callable<Integer>() {
                    private int invocations;

                    @Override public Integer call() {
                        invocations++;

                        ignite.compute().broadcast(
                            new IgniteRunnable() {
                                @Override public void run() {
                                    System.out.println();
                                    System.out.println("Howdy! :)");
                                }
                            }
                        );

                        return invocations;
                    }
                },
                "{5, 3} * * * * *" // Cron expression.
            );

            while (!fut.isDone())
                System.out.println(">>> Invocation #: " + fut.get());

            System.out.println();
            System.out.println(">>> Schedule future is done and has been unscheduled.");
            System.out.println(">>> Check all nodes for hello message output.");
        }
    }
}