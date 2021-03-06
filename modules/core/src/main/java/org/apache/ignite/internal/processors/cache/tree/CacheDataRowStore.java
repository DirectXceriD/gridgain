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

import org.apache.ignite.internal.processors.cache.CacheGroupContext;
import org.apache.ignite.internal.processors.cache.persistence.CacheDataRow;
import org.apache.ignite.internal.processors.cache.persistence.CacheDataRowAdapter;
import org.apache.ignite.internal.processors.cache.persistence.CacheSearchRow;
import org.apache.ignite.internal.processors.cache.persistence.RowStore;
import org.apache.ignite.internal.processors.cache.persistence.freelist.FreeList;
import org.apache.ignite.internal.processors.cache.tree.mvcc.data.MvccDataRow;
import org.apache.ignite.internal.util.typedef.internal.CU;

/**
 *
 */
public class CacheDataRowStore extends RowStore {
    /** */
    private final int partId;

    /** */
    private final CacheGroupContext grp;

    /**
     * @param grp Cache group.
     * @param freeList Free list.
     * @param partId Partition number.
     */
    public CacheDataRowStore(CacheGroupContext grp, FreeList freeList, int partId) {
        super(grp, freeList);

        this.partId = partId;
        this.grp = grp;
    }

    /**
     * @param cacheId Cache ID.
     * @param hash Hash code.
     * @param link Link.
     * @return Search row.
     */
    CacheSearchRow keySearchRow(int cacheId, int hash, long link) {
        return initDataRow(new DataRow(grp, hash, link, partId, CacheDataRowAdapter.RowData.KEY_ONLY), cacheId);
    }

    /**
     * @param cacheId Cache ID.
     * @param hash Hash code.
     * @param link Link.
     * @param rowData Required row data.
     * @param crdVer Mvcc coordinator version.
     * @param mvccCntr Mvcc counter.
     * @param opCntr Mvcc operation counter.
     * @return Search row.
     */
    MvccDataRow mvccRow(int cacheId, int hash, long link, CacheDataRowAdapter.RowData rowData, long crdVer, long mvccCntr, int opCntr) {
        MvccDataRow dataRow = new MvccDataRow(grp,
            hash,
            link,
            partId,
            rowData,
            crdVer,
            mvccCntr,
            opCntr);

        return initDataRow(dataRow, cacheId);
    }

    /**
     * @param cacheId Cache ID.
     * @param hash Hash code.
     * @param link Link.
     * @param rowData Required row data.
     * @return Data row.
     */
    CacheDataRow dataRow(int cacheId, int hash, long link, CacheDataRowAdapter.RowData rowData) {
        return initDataRow(new DataRow(grp, hash, link, partId, rowData), cacheId);
    }

    /**
     * @param dataRow Data row.
     * @param cacheId Cache ID.
     */
    private <T extends DataRow> T initDataRow(T dataRow, int cacheId) {
        if (dataRow.cacheId() == CU.UNDEFINED_CACHE_ID && grp.sharedGroup())
            dataRow.cacheId(cacheId);

        return dataRow;
    }
}
