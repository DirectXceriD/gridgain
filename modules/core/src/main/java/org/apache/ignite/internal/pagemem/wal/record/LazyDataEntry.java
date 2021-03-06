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

package org.apache.ignite.internal.pagemem.wal.record;

import org.apache.ignite.IgniteCheckedException;
import org.apache.ignite.IgniteException;
import org.apache.ignite.internal.processors.cache.CacheObject;
import org.apache.ignite.internal.processors.cache.GridCacheContext;
import org.apache.ignite.internal.processors.cache.GridCacheOperation;
import org.apache.ignite.internal.processors.cache.GridCacheSharedContext;
import org.apache.ignite.internal.processors.cache.KeyCacheObject;
import org.apache.ignite.internal.processors.cache.version.GridCacheVersion;
import org.apache.ignite.internal.processors.cacheobject.IgniteCacheObjectProcessor;

/**
 * Represents Data Entry ({@link #key}, {@link #val value}) pair update {@link #op operation}. <br>
 * This Data entry was not converted to key, value pair during record deserialization.
 */
public class LazyDataEntry extends DataEntry implements MarshalledDataEntry {
    /** */
    private GridCacheSharedContext cctx;

    /** Data Entry key type code. See {@link CacheObject} for built-in value type codes */
    private byte keyType;

    /** Key value bytes. */
    private byte[] keyBytes;

    /** Data Entry Value type code. See {@link CacheObject} for built-in value type codes */
    private byte valType;

    /** Value value bytes. */
    private byte[] valBytes;

    /**
     * @param cctx Shared context.
     * @param cacheId Cache ID.
     * @param keyType Object type code for Key.
     * @param keyBytes Data Entry Key value bytes.
     * @param valType Object type code for Value.
     * @param valBytes Data Entry Value value bytes.
     * @param op Operation.
     * @param nearXidVer Near transaction version.
     * @param writeVer Write version.
     * @param expireTime Expire time.
     * @param partId Partition ID.
     * @param partCnt Partition counter.
     */
    public LazyDataEntry(
        GridCacheSharedContext cctx,
        int cacheId,
        byte keyType,
        byte[] keyBytes,
        byte valType,
        byte[] valBytes,
        GridCacheOperation op,
        GridCacheVersion nearXidVer,
        GridCacheVersion writeVer,
        long expireTime,
        int partId,
        long partCnt
    ) {
        super(cacheId, null, null, op, nearXidVer, writeVer, expireTime, partId, partCnt);

        this.cctx = cctx;
        this.keyType = keyType;
        this.keyBytes = keyBytes;
        this.valType = valType;
        this.valBytes = valBytes;
    }

    /** {@inheritDoc} */
    @Override public KeyCacheObject key() {
        try {
            if (key == null) {
                GridCacheContext cacheCtx = cctx.cacheContext(cacheId);

                if (cacheCtx == null)
                    throw new IgniteException("Failed to find cache context for the given cache ID: " + cacheId);

                IgniteCacheObjectProcessor co = cctx.kernalContext().cacheObjects();

                key = co.toKeyCacheObject(cacheCtx.cacheObjectContext(), keyType, keyBytes);

                if (key.partition() == -1)
                    key.partition(partId);
            }

            return key;
        }
        catch (IgniteCheckedException e) {
            throw new IgniteException(e);
        }
    }

    /** {@inheritDoc} */
    @Override public CacheObject value() {
        if (val == null && valBytes != null) {
            GridCacheContext cacheCtx = cctx.cacheContext(cacheId);

            if (cacheCtx == null)
                throw new IgniteException("Failed to find cache context for the given cache ID: " + cacheId);

            IgniteCacheObjectProcessor co = cctx.kernalContext().cacheObjects();

            val = co.toCacheObject(cacheCtx.cacheObjectContext(), valType, valBytes);
        }

        return val;
    }

    /** {@inheritDoc} */
    @Override public byte getKeyType() {
        return keyType;
    }

    /** {@inheritDoc} */
    @Override public byte[] getKeyBytes() {
        return keyBytes;
    }

    /** {@inheritDoc} */
    @Override public byte getValType() {
        return valType;
    }

    /** {@inheritDoc} */
    @Override public byte[] getValBytes() {
        return valBytes;
    }
}
