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

import org.apache.ignite.IgniteCache;
import org.apache.ignite.IgniteCheckedException;
import org.apache.ignite.IgniteException;
import org.apache.ignite.cache.eviction.EvictableEntry;
import org.apache.ignite.internal.processors.cache.transactions.IgniteInternalTx;
import org.apache.ignite.internal.util.lang.GridMetadataAwareAdapter;
import org.apache.ignite.internal.util.lang.GridTuple;
import org.apache.ignite.internal.util.tostring.GridToStringInclude;
import org.apache.ignite.internal.util.typedef.internal.S;
import org.apache.ignite.internal.util.typedef.internal.U;
import org.jetbrains.annotations.Nullable;

/**
 * Entry wrapper that never obscures obsolete entries from user.
 */
public class CacheEvictableEntryImpl<K, V> implements EvictableEntry<K, V> {
    /** */
    private static final int META_KEY = GridMetadataAwareAdapter.EntryKey.CACHE_EVICTABLE_ENTRY_KEY.key();

    /** Cached entry. */
    @GridToStringInclude
    protected GridCacheEntryEx cached;

    /**
     * @param cached Cached entry.
     */
    @SuppressWarnings({"TypeMayBeWeakened"})
    protected CacheEvictableEntryImpl(GridCacheEntryEx cached) {
        this.cached = cached;
    }

    /** {@inheritDoc} */
    @Override public K getKey() throws IllegalStateException {
        return cached.key().value(cached.context().cacheObjectContext(), false);
    }

    /** {@inheritDoc} */
    @Override public boolean isCached() {
        return !cached.obsoleteOrDeleted();
    }

    /** {@inheritDoc} */
    @Override public boolean evict() {
        GridCacheContext<K, V> ctx = cached.context();

        try {
            assert ctx != null;

            CacheEvictionManager mgr = ctx.evicts();

            if (mgr == null) {
                assert ctx.kernalContext().isStopping();

                return false;
            }

            return mgr.evict(cached, null, false, null);
        }
        catch (IgniteCheckedException e) {
            U.error(ctx.grid().log(), "Failed to evict entry from cache: " + cached, e);

            return false;
        }
    }

    /**
     * @return Peeks value.
     */
    @Nullable public V peek() {
        try {
            CacheObject val = cached.peek();

            return val != null ? val.<V>value(cached.context().cacheObjectContext(), false) : null;
        }
        catch (GridCacheEntryRemovedException ignored) {
            return null;
        }
        catch (IgniteCheckedException e) {
            throw new IgniteException(e);
        }
    }

    /** {@inheritDoc} */
    @Override public int size() {
        try {
            GridCacheContext<Object, Object> cctx = cached.context();

            KeyCacheObject key = cached.key();

            byte[] keyBytes = key.valueBytes(cctx.cacheObjectContext());

            byte[] valBytes = null;

            CacheObject cacheObj = cached.valueBytes();

            if (cacheObj != null)
                valBytes = cacheObj.valueBytes(cctx.cacheObjectContext());

            return valBytes == null ? keyBytes.length : keyBytes.length + valBytes.length;
        }
        catch (GridCacheEntryRemovedException ignored) {
            return 0;
        }
        catch (IgniteCheckedException e) {
            throw new IgniteException(e);
        }
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override public V getValue() {
        try {
            IgniteInternalTx tx = cached.context().tm().userTx();

            if (tx != null) {
                GridTuple<CacheObject> peek = tx.peek(cached.context(), false, cached.key());

                if (peek != null)
                    return peek.get().value(cached.context().cacheObjectContext(), false);
            }

            if (cached.detached()) {
                CacheObject val = cached.rawGet();

                return val != null ? val.<V>value(cached.context().cacheObjectContext(), false) : null;
            }

            for (;;) {
                GridCacheEntryEx e = cached.context().cache().peekEx(cached.key());

                if (e == null)
                    return null;

                try {
                    CacheObject val = e.peek();

                    return val != null ? val.<V>value(cached.context().cacheObjectContext(), false) : null;
                }
                catch (GridCacheEntryRemovedException ignored) {
                    // No-op.
                }
                catch (IgniteCheckedException ex) {
                    throw new IgniteException(ex);
                }
            }
        }
        catch (GridCacheFilterFailedException ignored) {
            throw new IgniteException("Should never happen.");
        }
    }

    /** {@inheritDoc} */
    @Nullable @Override public <T> T addMeta(T val) {
        return cached.addMeta(META_KEY, val);
    }

    /** {@inheritDoc} */
    @Nullable @Override public <T> T meta() {
        return cached.meta(META_KEY);
    }

    /** {@inheritDoc} */
    @Nullable @Override public <T> T removeMeta() {
        return cached.removeMeta(META_KEY);
    }

    /** {@inheritDoc} */
    @Override public <T> boolean removeMeta(T val) {
        return cached.removeMeta(META_KEY, val);
    }

    /** {@inheritDoc} */
    @Nullable @Override public <T> T putMetaIfAbsent(T val) {
        return cached.putMetaIfAbsent(META_KEY, val);
    }

    /** {@inheritDoc} */
    @Override public <T> boolean replaceMeta(T curVal, T newVal) {
        return cached.replaceMeta(META_KEY,curVal, newVal);
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override public <T> T unwrap(Class<T> clazz) {
        if (clazz.isAssignableFrom(IgniteCache.class))
            return (T)cached.context().grid().cache(cached.context().name());

        if(clazz.isAssignableFrom(getClass()))
            return clazz.cast(this);

        throw new IllegalArgumentException();
    }

    /** {@inheritDoc} */
    @Override public int hashCode() {
        return cached.key().hashCode();
    }

    /** {@inheritDoc} */
    @SuppressWarnings({"unchecked"})
    @Override public boolean equals(Object obj) {
        if (obj == this)
            return true;

        if (obj instanceof CacheEvictableEntryImpl) {
            CacheEvictableEntryImpl<K, V> other = (CacheEvictableEntryImpl<K, V>)obj;

            return cached.key().equals(other.cached.key());
        }

        return false;
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(CacheEvictableEntryImpl.class, this);
    }
}
