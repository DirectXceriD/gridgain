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

import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.NearCacheConfiguration;
import org.apache.ignite.internal.processors.affinity.AffinityTopologyVersion;
import org.apache.ignite.internal.util.typedef.internal.S;
import org.jetbrains.annotations.Nullable;

/**
 * Specific cache information for start.
 */
public class StartCacheInfo {
    /** Cache configuration for start. */
    private final CacheConfiguration startedConf;

    /** Cache descriptor for start. */
    private final DynamicCacheDescriptor desc;

    /** Near cache configuration for start. */
    private final @Nullable NearCacheConfiguration reqNearCfg;

    /** Exchange topology version in which starting happened. */
    private final AffinityTopologyVersion exchTopVer;

    /** Disable started cache after start or not. */
    private final boolean disabledAfterStart;

    /**
     * @param desc Cache configuration for start.
     * @param reqNearCfg Near cache configuration for start.
     * @param exchTopVer Exchange topology version in which starting happened.
     * @param disabledAfterStart Disable started cache after start or not.
     */
    public StartCacheInfo(DynamicCacheDescriptor desc,
        NearCacheConfiguration reqNearCfg,
        AffinityTopologyVersion exchTopVer, boolean disabledAfterStart) {
        this(desc.cacheConfiguration(), desc, reqNearCfg, exchTopVer, disabledAfterStart);
    }

    /**
     * @param conf Cache configuration for start.
     * @param desc Cache descriptor for start.
     * @param reqNearCfg Near cache configuration for start.
     * @param exchTopVer Exchange topology version in which starting happened.
     * @param disabledAfterStart Disable started cache after start or not.
     */
    public StartCacheInfo(CacheConfiguration conf, DynamicCacheDescriptor desc,
        NearCacheConfiguration reqNearCfg,
        AffinityTopologyVersion exchTopVer, boolean disabledAfterStart) {
        startedConf = conf;
        this.desc = desc;
        this.reqNearCfg = reqNearCfg;
        this.exchTopVer = exchTopVer;
        this.disabledAfterStart = disabledAfterStart;
    }

    /**
     * @return Cache configuration for start.
     */
    public CacheConfiguration getStartedConfiguration() {
        return startedConf;
    }

    /**
     * @return Cache descriptor for start.
     */
    public DynamicCacheDescriptor getCacheDescriptor() {
        return desc;
    }

    /**
     * @return Near cache configuration for start.
     */
    @Nullable public NearCacheConfiguration getReqNearCfg() {
        return reqNearCfg;
    }

    /**
     * @return Exchange topology version in which starting happened.
     */
    public AffinityTopologyVersion getExchangeTopVer() {
        return exchTopVer;
    }

    /**
     * @return Disable started cache after start or not.
     */
    public boolean isDisabledAfterStart() {
        return disabledAfterStart;
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(StartCacheInfo.class, this);
    }
}
