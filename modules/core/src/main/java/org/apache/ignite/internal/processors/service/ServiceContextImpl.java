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

package org.apache.ignite.internal.processors.service;

import java.lang.reflect.Method;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import org.apache.ignite.internal.util.tostring.GridToStringExclude;
import org.apache.ignite.internal.util.tostring.GridToStringInclude;
import org.apache.ignite.internal.util.typedef.internal.S;
import org.apache.ignite.services.Service;
import org.apache.ignite.services.ServiceContext;
import org.jetbrains.annotations.Nullable;

/**
 * Service context implementation.
 */
public class ServiceContextImpl implements ServiceContext {
    /** */
    private static final long serialVersionUID = 0L;

    /** Null method. */
    private static final Method NULL_METHOD = ServiceContextImpl.class.getMethods()[0];

    /** Service name. */
    private final String name;

    /** Execution ID. */
    private final UUID execId;

    /** Cache name. */
    private final String cacheName;

    /** Affinity key. */
    @GridToStringInclude
    private final Object affKey;

    /** Executor service. */
    @GridToStringExclude
    private final ExecutorService exe;

    /** Methods reflection cache. */
    private final ConcurrentMap<GridServiceMethodReflectKey, Method> mtds = new ConcurrentHashMap<>();

    /** Service. */
    @GridToStringExclude
    private volatile Service svc;

    /** Cancelled flag. */
    private volatile boolean isCancelled;

    /**
     * @param name Service name.
     * @param execId Execution ID.
     * @param cacheName Cache name.
     * @param affKey Affinity key.
     * @param exe Executor service.
     */
    ServiceContextImpl(String name,
        UUID execId,
        String cacheName,
        Object affKey,
        ExecutorService exe) {
        this.name = name;
        this.execId = execId;
        this.cacheName = cacheName;
        this.affKey = affKey;
        this.exe = exe;
    }

    /** {@inheritDoc} */
    @Override public String name() {
        return name;
    }

    /** {@inheritDoc} */
    @Override public UUID executionId() {
        return execId;
    }

    /** {@inheritDoc} */
    @Override public boolean isCancelled() {
        return isCancelled;
    }

    /** {@inheritDoc} */
    @Nullable @Override public String cacheName() {
        return cacheName;
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Nullable @Override public <K> K affinityKey() {
        return (K)affKey;
    }

    /**
     * @param svc Service instance.
     */
    void service(Service svc) {
        this.svc = svc;
    }

    /**
     * @return Service instance or {@code null} if service initialization is not finished yet.
     */
    @Nullable Service service() {
        return svc;
    }

    /**
     * @return Executor service.
     */
    ExecutorService executor() {
        return exe;
    }

    /**
     * @param key Method key.
     * @return Method.
     */
    @Nullable Method method(GridServiceMethodReflectKey key) {
        Method mtd = mtds.get(key);

        if (mtd == null) {
            try {
                mtd = svc.getClass().getMethod(key.methodName(), key.argTypes());

                mtd.setAccessible(true);
            }
            catch (NoSuchMethodException ignored) {
                mtd = NULL_METHOD;
            }

            mtds.put(key, mtd);
        }

        return mtd == NULL_METHOD ? null : mtd;
    }

    /**
     * @param isCancelled Cancelled flag.
     */
    public void setCancelled(boolean isCancelled) {
        this.isCancelled = isCancelled;
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(ServiceContextImpl.class, this);
    }
}
