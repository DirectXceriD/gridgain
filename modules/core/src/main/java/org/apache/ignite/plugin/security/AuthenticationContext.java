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

package org.apache.ignite.plugin.security;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import org.apache.ignite.internal.processors.authentication.AuthorizationContext;

/**
 * Authentication context.
 */
public class AuthenticationContext {
    /** Subject type. */
    private SecuritySubjectType subjType;

    /** Subject ID.w */
    private UUID subjId;

    /** Credentials. */
    private SecurityCredentials creds;

    /** Subject address. */
    private InetSocketAddress addr;

    /** */
    private Map<String, Object> nodeAttrs;

    /** Authorization context. */
    private AuthorizationContext athrCtx;

    /** True if this is a client node context. */
    private boolean client;

    /**
     * Gets subject type.
     *
     * @return Subject type.
     */
    public SecuritySubjectType subjectType() {
        return subjType;
    }

    /**
     * Sets subject type.
     *
     * @param subjType Subject type.
     */
    public void subjectType(SecuritySubjectType subjType) {
        this.subjType = subjType;
    }

    /**
     * Gets subject ID.
     *
     * @return Subject ID.
     */
    public UUID subjectId() {
        return subjId;
    }

    /**
     * Sets subject ID.
     *
     * @param subjId Subject ID.
     */
    public void subjectId(UUID subjId) {
        this.subjId = subjId;
    }

    /**
     * Gets security credentials.
     *
     * @return Security credentials.
     */
    public SecurityCredentials credentials() {
        return creds;
    }

    /**
     * Sets security credentials.
     *
     * @param creds Security credentials.
     */
    public void credentials(SecurityCredentials creds) {
        this.creds = creds;
    }

    /**
     * Gets subject network address.
     *
     * @return Subject network address.
     */
    public InetSocketAddress address() {
        return addr;
    }

    /**
     * Sets subject network address.
     *
     * @param addr Subject network address.
     */
    public void address(InetSocketAddress addr) {
        this.addr = addr;
    }

    /**
     * Gets node attributes.
     *
     * @return Node attributes or empty map for {@link SecuritySubjectType#REMOTE_CLIENT}.
     */
    public Map<String, Object> nodeAttributes() {
        return nodeAttrs != null ? nodeAttrs : Collections.<String, Object>emptyMap();
    }

    /**
     * Sets node attributes.
     *
     * @param nodeAttrs Node attributes.
     */
    public void nodeAttributes(Map<String, Object> nodeAttrs) {
        this.nodeAttrs = nodeAttrs;
    }

    /**
     * @return Native Apache Ignite authorization context acquired after authentication or {@code null} if native
     * Ignite authentication is not used.
     */
    public AuthorizationContext authorizationContext(){
        return athrCtx;
    }

    /**
     * Set authorization context acquired after native Apache Ignite authentication.
     */
    public AuthenticationContext authorizationContext(AuthorizationContext newVal) {
        athrCtx = newVal;

        return this;
    }

    /**
     * @return {@code true} if this is a client node context.
     */
    public boolean isClient() {
        return client;
    }

    /**
     * Sets flag indicating if this is client node context.
     */
    public AuthenticationContext setClient(boolean newVal) {
        client = newVal;

        return this;
    }
}