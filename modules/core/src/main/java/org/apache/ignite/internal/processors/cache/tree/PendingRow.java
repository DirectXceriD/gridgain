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

package org.apache.ignite.internal.processors.cache.tree;

import org.apache.ignite.IgniteCheckedException;
import org.apache.ignite.internal.processors.cache.CacheGroupContext;
import org.apache.ignite.internal.processors.cache.KeyCacheObject;
import org.apache.ignite.internal.processors.cache.persistence.CacheDataRowAdapter;
import org.apache.ignite.internal.processors.cache.tree.mvcc.data.MvccDataRow;
import org.apache.ignite.internal.util.typedef.internal.S;

/**
 *
 */
public class PendingRow {
    /** Expire time. */
    public long expireTime;

    /** Link. */
    public long link;

    /** Cache ID. */
    public int cacheId;

    /** */
    public KeyCacheObject key;

    /**
     * Creates a new instance which represents an upper or lower bound
     * inside a logical cache.
     *
     * @param cacheId Cache ID.
     */
    public PendingRow(int cacheId) {
        this.cacheId = cacheId;
    }

    /**
     * @param cacheId Cache ID.
     * @param expireTime Expire time.
     * @param link Link
     */
    public PendingRow(int cacheId, long expireTime, long link) {
        assert expireTime != 0;

        this.cacheId = cacheId;
        this.expireTime = expireTime;
        this.link = link;
    }

    /**
     * @param grp Cache group.
     * @return Row.
     * @throws IgniteCheckedException If failed.
     */
    PendingRow initKey(CacheGroupContext grp) throws IgniteCheckedException {
        CacheDataRowAdapter rowData = grp.mvccEnabled() ? new MvccDataRow(link) : new CacheDataRowAdapter(link);
        rowData.initFromLink(grp, CacheDataRowAdapter.RowData.KEY_ONLY);

        key = rowData.key();

        return this;
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(PendingRow.class, this);
    }
}
