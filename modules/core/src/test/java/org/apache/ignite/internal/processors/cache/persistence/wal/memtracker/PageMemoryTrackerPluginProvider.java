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

package org.apache.ignite.internal.processors.cache.persistence.wal.memtracker;

import java.io.Serializable;
import java.util.UUID;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCheckedException;
import org.apache.ignite.IgniteLogger;
import org.apache.ignite.cluster.ClusterNode;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.internal.GridKernalContext;
import org.apache.ignite.internal.IgniteEx;
import org.apache.ignite.internal.pagemem.store.IgnitePageStoreManager;
import org.apache.ignite.internal.pagemem.wal.IgniteWriteAheadLogManager;
import org.apache.ignite.internal.processors.cache.persistence.DatabaseLifecycleListener;
import org.apache.ignite.internal.processors.cache.persistence.IgniteCacheDatabaseSharedManager;
import org.apache.ignite.internal.processors.cluster.IgniteChangeGlobalStateSupport;
import org.apache.ignite.internal.util.typedef.internal.CU;
import org.apache.ignite.plugin.CachePluginContext;
import org.apache.ignite.plugin.CachePluginProvider;
import org.apache.ignite.plugin.ExtensionRegistry;
import org.apache.ignite.plugin.IgnitePlugin;
import org.apache.ignite.plugin.PluginConfiguration;
import org.apache.ignite.plugin.PluginContext;
import org.apache.ignite.plugin.PluginNotFoundException;
import org.apache.ignite.plugin.PluginProvider;
import org.apache.ignite.plugin.PluginValidationException;
import org.jetbrains.annotations.Nullable;

/**
 * PageMemory tracker plugin provider.
 */
public class PageMemoryTrackerPluginProvider implements PluginProvider<PageMemoryTrackerConfiguration>,
    IgniteChangeGlobalStateSupport, DatabaseLifecycleListener {
    /** System property name to implicitly enable page memory tracker . */
    public static final String IGNITE_ENABLE_PAGE_MEMORY_TRACKER = "IGNITE_ENABLE_PAGE_MEMORY_TRACKER";

    /** Plugin name. */
    private static final String PLUGIN_NAME = "PageMemory tracker plugin";

    /** Plugin instance */
    private PageMemoryTracker plugin;

    /** Logger. */
    private IgniteLogger log;

    /** {@inheritDoc} */
    @Override public String name() {
        return PLUGIN_NAME;
    }

    /** {@inheritDoc} */
    @Override public String version() {
        return "1.0";
    }

    /** {@inheritDoc} */
    @Override public String copyright() {
        return "";
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override public <T extends IgnitePlugin> T plugin() {
        return (T)plugin;
    }

    /** {@inheritDoc} */
    @Override public void initExtensions(PluginContext ctx, ExtensionRegistry registry) {
        IgniteConfiguration igniteCfg = ctx.igniteConfiguration();

        log = ctx.log(getClass());

        if (igniteCfg.getPluginConfigurations() != null) {
            for (PluginConfiguration pluginCfg : igniteCfg.getPluginConfigurations()) {
                if (pluginCfg instanceof PageMemoryTrackerConfiguration) {
                    PageMemoryTrackerConfiguration cfg = (PageMemoryTrackerConfiguration)pluginCfg;

                    plugin = new PageMemoryTracker(ctx, cfg);

                    if (cfg.isEnabled() && !CU.isPersistenceEnabled(igniteCfg)) {
                        log.warning("Page memory tracker plugin enabled, " +
                            "but there are no persistable data regions in configuration. Tracker will be disabled.");
                    }

                    return;
                }
            }
        }

        if (Boolean.getBoolean(IGNITE_ENABLE_PAGE_MEMORY_TRACKER) && CU.isPersistenceEnabled(igniteCfg)) {
            plugin = new PageMemoryTracker(ctx, new PageMemoryTrackerConfiguration()
                .setEnabled(true)
                .setCheckPagesOnCheckpoint(true)
            );

            log.info("PageMemory tracking enabled by system property.");
        }
        else
            plugin = new PageMemoryTracker(ctx, null);
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Nullable @Override public <T> T createComponent(PluginContext ctx, Class<T> cls) {
        if (plugin != null) {
            if (IgniteWriteAheadLogManager.class.equals(cls))
                return (T)plugin.createWalManager();
            else if (IgnitePageStoreManager.class.equals(cls))
                return (T)plugin.createPageStoreManager();
        }

        return null;
    }

    /** {@inheritDoc} */
    @Override public CachePluginProvider createCacheProvider(CachePluginContext ctx) {
        return null;
    }

    /** {@inheritDoc} */
    @Override public void start(PluginContext ctx) {
        ((IgniteEx)ctx.grid()).context().internalSubscriptionProcessor().registerDatabaseListener(this);
    }

    /** {@inheritDoc} */
    @Override public void stop(boolean cancel) {
        // No-op
    }

    /** {@inheritDoc} */
    @Override public void onIgniteStart() {
        // No-op
    }

    /** {@inheritDoc} */
    @Override public void onIgniteStop(boolean cancel) {
        if (plugin != null)
            plugin.stop();
    }

    /** {@inheritDoc} */
    @Nullable @Override public Serializable provideDiscoveryData(UUID nodeId) {
        return null;
    }

    /** {@inheritDoc} */
    @Override public void receiveDiscoveryData(UUID nodeId, Serializable data) {
        // No-op
    }

    /** {@inheritDoc} */
    @Override public void validateNewNode(ClusterNode node) throws PluginValidationException {
        // No-op
    }

    /** {@inheritDoc} */
    @Override public void onActivate(GridKernalContext kctx) {
        if (plugin != null) {
            try {
                plugin.start();
            }
            catch (Exception e) {
                log.error("Can't start plugin", e);
            }
        }
    }

    /** {@inheritDoc} */
    @Override public void onDeActivate(GridKernalContext kctx) {
        if (plugin != null)
            plugin.stop();
    }

    /**
     * Gets PageMemory tracker for ignite instance or null if it's not enabled.
     *
     * @param ignite Ignite.
     */
    public static PageMemoryTracker tracker(Ignite ignite) {
        try {
            return ignite.plugin(PLUGIN_NAME);
        }
        catch (PluginNotFoundException ignore) {
            return null;
        }
    }

    @Override public void beforeBinaryMemoryRestore(IgniteCacheDatabaseSharedManager mgr) throws IgniteCheckedException {
        if (plugin != null) {
            try {
                plugin.start();
            }
            catch (Exception e) {
                log.error("Can't start plugin", e);
            }
        }
    }
}
