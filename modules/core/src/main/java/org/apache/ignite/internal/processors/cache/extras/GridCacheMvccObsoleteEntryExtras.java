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

package org.apache.ignite.internal.processors.cache.extras;

import org.apache.ignite.internal.processors.cache.GridCacheMvcc;
import org.apache.ignite.internal.processors.cache.version.GridCacheVersion;
import org.apache.ignite.internal.util.typedef.internal.CU;
import org.apache.ignite.internal.util.typedef.internal.S;

/**
 * Extras where MVCC and obsolete version are set.
 */
public class GridCacheMvccObsoleteEntryExtras extends GridCacheEntryExtrasAdapter {
    /** MVCC. */
    private GridCacheMvcc mvcc;

    /** Obsolete version. */
    private GridCacheVersion obsoleteVer;

    /**
     * Constructor.
     *
     * @param mvcc MVCC.
     * @param obsoleteVer Obsolete version.
     */
    GridCacheMvccObsoleteEntryExtras(GridCacheMvcc mvcc, GridCacheVersion obsoleteVer) {
        assert mvcc != null;
        assert obsoleteVer != null;

        this.mvcc = mvcc;
        this.obsoleteVer = obsoleteVer;
    }

    /** {@inheritDoc} */
    @Override public GridCacheMvcc mvcc() {
        return mvcc;
    }

    /** {@inheritDoc} */
    @Override public GridCacheEntryExtras mvcc(GridCacheMvcc mvcc) {
        if (mvcc != null) {
            this.mvcc = mvcc;

            return this;
        }
        else
            return new GridCacheObsoleteEntryExtras(obsoleteVer);
    }

    /** {@inheritDoc} */
    @Override public GridCacheVersion obsoleteVersion() {
        return obsoleteVer;
    }

    /** {@inheritDoc} */
    @Override public GridCacheEntryExtras obsoleteVersion(GridCacheVersion obsoleteVer) {
        if (obsoleteVer != null) {
            this.obsoleteVer = obsoleteVer;

            return this;
        }
        else
            return new GridCacheMvccEntryExtras(mvcc);
    }

    /** {@inheritDoc} */
    @Override public GridCacheEntryExtras ttlAndExpireTime(long ttl, long expireTime) {
        return expireTime != CU.EXPIRE_TIME_ETERNAL ? new GridCacheMvccObsoleteTtlEntryExtras(mvcc, obsoleteVer, ttl, expireTime) : this;
    }

    /** {@inheritDoc} */
    @Override public int size() {
        return 16;
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(GridCacheMvccObsoleteEntryExtras.class, this);
    }
}