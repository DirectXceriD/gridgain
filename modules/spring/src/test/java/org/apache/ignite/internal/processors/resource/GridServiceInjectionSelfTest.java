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

package org.apache.ignite.internal.processors.resource;

import java.io.Serializable;
import java.util.concurrent.Callable;
import org.apache.ignite.IgniteCheckedException;
import org.apache.ignite.lang.IgniteCallable;
import org.apache.ignite.resources.ServiceResource;
import org.apache.ignite.services.Service;
import org.apache.ignite.services.ServiceContext;
import org.apache.ignite.testframework.GridTestUtils;
import org.apache.ignite.testframework.junits.common.GridCommonAbstractTest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests for injected service.
 */
@RunWith(JUnit4.class)
public class GridServiceInjectionSelfTest extends GridCommonAbstractTest implements Serializable {
    /** */
    private static final long serialVersionUID = 0L;

    /** Service name. */
    private static final String SERVICE_NAME1 = "testService1";

    /** Service name. */
    private static final String SERVICE_NAME2 = "testService2";

    /** {@inheritDoc} */
    @Override protected void beforeTestsStarted() throws Exception {
        super.beforeTestsStarted();

        startGrid(0);
        startGrid(1);

        grid(0).services().deployNodeSingleton(SERVICE_NAME1, new DummyServiceImpl());
        grid(0).services(grid(0).cluster().forLocal()).deployClusterSingleton(SERVICE_NAME2, new DummyServiceImpl());

        assertEquals(2, grid(0).cluster().nodes().size());
        assertEquals(2, grid(1).cluster().nodes().size());
    }

    /**
     * @throws Exception If failed.
     */
    @Test
    public void testClosureField() throws Exception {
        grid(0).compute().call(new IgniteCallable<Object>() {
            @ServiceResource(serviceName = SERVICE_NAME1)
            private DummyService svc;

            @Override public Object call() throws Exception {
                assertNotNull(svc);
                assertTrue(svc instanceof Service);

                svc.noop();

                return null;
            }
        });
    }

    /**
     * @throws Exception If failed.
     */
    @Test
    public void testClosureFieldProxy() throws Exception {
        grid(0).compute(grid(0).cluster().forRemotes()).call(new IgniteCallable<Object>() {
            @ServiceResource(serviceName = SERVICE_NAME2, proxyInterface = DummyService.class)
            private DummyService svc;

            @Override public Object call() throws Exception {
                assertNotNull(svc);

                // Ensure proxy instance.
                assertFalse(svc instanceof Service);

                svc.noop();

                return null;
            }
        });
    }

    /**
     * @throws Exception If failed.
     */
    @Test
    public void testClosureFieldLocalProxy() throws Exception {
        grid(0).compute(grid(0).cluster().forRemotes()).call(new IgniteCallable<Object>() {
            @ServiceResource(serviceName = SERVICE_NAME1, proxyInterface = DummyService.class)
            private DummyService svc;

            @Override public Object call() throws Exception {
                assertNotNull(svc);

                // Ensure proxy instance.
                assertTrue(svc instanceof Service);

                svc.noop();

                return null;
            }
        });
    }

    /**
     * @throws Exception If failed.
     */
    @Test
    public void testClosureFieldWithIncorrectType() throws Exception {
        GridTestUtils.assertThrowsAnyCause(log, new Callable<Void>() {
            @Override public Void call() {
                grid(0).compute().call(new IgniteCallable<Object>() {
                    @ServiceResource(serviceName = SERVICE_NAME1)
                    private String svcName;

                    @Override public Object call() throws Exception {
                        fail();

                        return null;
                    }
                });

                return null;
            }
        }, IgniteCheckedException.class, "Resource field is not assignable from the resource");
    }

    /**
     * @throws Exception If failed.
     */
    @Test
    public void testClosureMethod() throws Exception {
        grid(0).compute().call(new IgniteCallable<Object>() {
            private DummyService svc;

            @ServiceResource(serviceName = SERVICE_NAME1)
            private void service(DummyService svc) {
                assertNotNull(svc);

                assertTrue(svc instanceof Service);

                this.svc = svc;
            }

            @Override public Object call() throws Exception {
                svc.noop();

                return null;
            }
        });
    }

    /**
     * @throws Exception If failed.
     */
    @Test
    public void testClosureMethodProxy() throws Exception {
        grid(0).compute(grid(0).cluster().forRemotes()).call(new IgniteCallable<Object>() {
            private DummyService svc;

            @ServiceResource(serviceName = SERVICE_NAME2, proxyInterface = DummyService.class)
            private void service(DummyService svc) {
                assertNotNull(svc);

                // Ensure proxy instance.
                assertFalse(svc instanceof Service);

                this.svc = svc;
            }

            @Override public Object call() throws Exception {
                svc.noop();

                return null;
            }
        });
    }

    /**
     * @throws Exception If failed.
     */
    @Test
    public void testClosureMethodLocalProxy() throws Exception {
        grid(0).compute(grid(0).cluster().forRemotes()).call(new IgniteCallable<Object>() {
            private DummyService svc;

            @ServiceResource(serviceName = SERVICE_NAME1, proxyInterface = DummyService.class)
            private void service(DummyService svc) {
                assertNotNull(svc);

                // Ensure proxy instance.
                assertTrue(svc instanceof Service);

                this.svc = svc;
            }

            @Override public Object call() throws Exception {
                svc.noop();

                return null;
            }
        });
    }

    /**
     * @throws Exception If failed.
     */
    @Test
    public void testClosureMethodWithIncorrectType() throws Exception {
        GridTestUtils.assertThrowsAnyCause(log, new Callable<Void>() {
            @Override public Void call() {
                grid(0).compute().call(new IgniteCallable<Object>() {
                    @ServiceResource(serviceName = SERVICE_NAME1)
                    private void service(String svcs) {
                        fail();
                    }

                    @Override public Object call() throws Exception {
                        return null;
                    }
                });

                return null;
            }
        }, IgniteCheckedException.class, "Setter does not have single parameter of required type");
    }

    /**
     * @throws Exception If failed.
     */
    @Test
    public void testClosureFieldWithNonExistentService() throws Exception {
        grid(0).compute().call(new IgniteCallable<Object>() {
            @ServiceResource(serviceName = "nonExistentService")
            private DummyService svc;

            @Override public Object call() throws Exception {
                assertNull(svc);

                return null;
            }
        });
    }

    /**
     * @throws Exception If failed.
     */
    @Test
    public void testClosureMethodWithNonExistentService() throws Exception {
        grid(0).compute().call(new IgniteCallable<Object>() {
            @ServiceResource(serviceName = "nonExistentService")
            private void service(DummyService svc) {
                assertNull(svc);
            }

            @Override public Object call() throws Exception {
                return null;
            }
        });
    }

    /**
     * Dummy Service.
     */
    public interface DummyService {
        public void noop();
    }

    /**
     * No-op test service.
     */
    public static class DummyServiceImpl implements DummyService, Service {
        /** */
        private static final long serialVersionUID = 0L;

        /** {@inheritDoc} */
        @Override public void noop() {
            // No-op.
        }

        /** {@inheritDoc} */
        @Override public void cancel(ServiceContext ctx) {
            System.out.println("Cancelling service: " + ctx.name());
        }

        /** {@inheritDoc} */
        @Override public void init(ServiceContext ctx) throws Exception {
            System.out.println("Initializing service: " + ctx.name());
        }

        /** {@inheritDoc} */
        @Override public void execute(ServiceContext ctx) {
            System.out.println("Executing service: " + ctx.name());
        }
    }
}
