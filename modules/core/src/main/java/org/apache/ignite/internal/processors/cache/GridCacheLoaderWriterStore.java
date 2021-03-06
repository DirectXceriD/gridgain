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

import java.io.Closeable;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import javax.cache.Cache;
import javax.cache.integration.CacheLoader;
import javax.cache.integration.CacheWriter;
import org.apache.ignite.cache.store.CacheStore;
import org.apache.ignite.internal.util.typedef.internal.S;
import org.apache.ignite.internal.util.typedef.internal.U;
import org.apache.ignite.lang.IgniteBiInClosure;
import org.apache.ignite.lifecycle.LifecycleAware;
import org.jetbrains.annotations.Nullable;

/**
 * Store implementation wrapping {@link CacheLoader} and {@link CacheWriter}.
 */
class GridCacheLoaderWriterStore<K, V> implements CacheStore<K, V>, LifecycleAware, Serializable {
    /** */
    private static final long serialVersionUID = 0L;

    /** */
    private final CacheLoader<K, V> ldr;

    /** */
    private final CacheWriter<K, V> writer;

    /**
     * @param ldr Loader.
     * @param writer Writer.
     */
    GridCacheLoaderWriterStore(@Nullable CacheLoader<K, V> ldr, @Nullable CacheWriter<K, V> writer) {
        assert ldr != null || writer != null;

        this.ldr = ldr;
        this.writer = writer;
    }

    /**
     * @return Cache loader.
     */
    CacheLoader<K, V> loader() {
        return ldr;
    }

    /**
     * @return Cache writer.
     */
    CacheWriter<K, V> writer() {
        return writer;
    }

    /** {@inheritDoc} */
    @Override public void start() {
        if (ldr instanceof LifecycleAware)
            ((LifecycleAware)ldr).start();

        if (writer instanceof LifecycleAware)
            ((LifecycleAware)writer).start();
    }

    /** {@inheritDoc} */
    @Override public void stop() {
        if (ldr instanceof LifecycleAware)
            ((LifecycleAware)ldr).stop();
        else if (ldr instanceof Closeable)
            U.closeQuiet((Closeable)ldr);

        if (writer instanceof LifecycleAware)
            ((LifecycleAware)writer).stop();
        else if (writer instanceof Closeable)
            U.closeQuiet((Closeable)writer);
    }

    /** {@inheritDoc} */
    @Override public void loadCache(IgniteBiInClosure<K, V> clo, @Nullable Object... args) {
        // No-op.
    }

    /** {@inheritDoc} */
    @Nullable @Override public V load(K key) {
        if (ldr == null)
            return null;

        return ldr.load(key);
    }

    /** {@inheritDoc} */
    @Override public Map<K, V> loadAll(Iterable<? extends K> keys) {
        if (ldr == null)
            return Collections.emptyMap();

        return ldr.loadAll(keys);
    }

    /** {@inheritDoc} */
    @Override public void write(Cache.Entry<? extends K, ? extends V> entry) {
        if (writer == null)
            return;

        writer.write(entry);
    }

    /** {@inheritDoc} */
    @Override public void writeAll(Collection<Cache.Entry<? extends K, ? extends V>> entries) {
        if (writer == null)
            return;

        writer.writeAll(entries);
    }

    /** {@inheritDoc} */
    @Override public void delete(Object key) {
        if (writer == null)
            return;

        writer.delete(key);
    }

    /** {@inheritDoc} */
    @Override public void deleteAll(Collection<?> keys) {
        if (writer == null)
            return;

        writer.deleteAll(keys);
    }

    /** {@inheritDoc} */
    @Override public void sessionEnd(boolean commit) {
        // No-op.
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(GridCacheLoaderWriterStore.class, this);
    }
}