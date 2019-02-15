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

package org.apache.ignite.internal.processors.query.h2;

import org.apache.ignite.IgniteCheckedException;
import org.apache.ignite.internal.pagemem.PageIdUtils;
import org.apache.ignite.internal.pagemem.PageMemory;
import org.apache.ignite.internal.processors.cache.CacheGroupContext;
import org.apache.ignite.internal.processors.cache.GridCacheContext;
import org.apache.ignite.internal.processors.query.GridQueryRowCacheCleaner;
import org.apache.ignite.internal.processors.query.h2.opt.GridH2KeyValueRowOnheap;
import org.apache.ignite.internal.util.typedef.F;
import org.jsr166.ConcurrentLinkedHashMap;

import java.util.Iterator;
import java.util.Map;

import static org.jsr166.ConcurrentLinkedHashMap.DFLT_INIT_CAP;
import static org.jsr166.ConcurrentLinkedHashMap.DFLT_LOAD_FACTOR;

/**
 * H2 row cache.
 */
public class H2RowCache implements GridQueryRowCacheCleaner {
    /** Cached rows. */
    private final ConcurrentLinkedHashMap<Long, GridH2KeyValueRowOnheap> rows;

    /** Cache group ID. */
    private final CacheGroupContext grpCtx;

    /** Usage count. */
    private int usageCnt = 1;

    /**
     * @param grpCtx Cache group context.
     */
    public H2RowCache(CacheGroupContext grpCtx, int maxSize) {
        this.grpCtx = grpCtx;

        rows = new ConcurrentLinkedHashMap<Long, GridH2KeyValueRowOnheap>(
            DFLT_INIT_CAP,
            DFLT_LOAD_FACTOR,
            Runtime.getRuntime().availableProcessors(),
            maxSize
        );
    }

    /**
     * Get row by link.
     *
     * @param link Link.
     * @return Cached on-heap row.
     * @throws IgniteCheckedException On error.
     */
    public GridH2KeyValueRowOnheap get(long link) throws IgniteCheckedException {
        GridH2KeyValueRowOnheap row = rows.get(link);

        if (row != null)
            touch(link);

        return row;
    }

    /**
     * Put row by link.
     *
     * @param row Row.
     */
    public void put(GridH2KeyValueRowOnheap row) {
        rows.put(row.link(), row);
    }

    /** {@inheritDoc} */
    @Override public void remove(long link) {
        rows.remove(link);
    }

    /**
     * Cache registration callback.
     */
    public void onCacheRegistered() {
        usageCnt++;
    }

    /**
     * Cache un-registration callback.
     *
     * @param cctx Cache context.
     * @return {@code True} if there are no more usages for the given cache group.
     */
    public boolean onCacheUnregistered(GridCacheContext cctx) {
        boolean res = --usageCnt == 0;

        clearForCache(cctx);

        return res;
    }

    /**
     * @return Cached rows count.
     */
    public int size() {
        return rows.size();
    }

    /**
     * Clear entries belonging to the given cache.
     *
     * @param cctx Cache context.
     */
    private void clearForCache(GridCacheContext cctx) {
        int cacheId = cctx.cacheId();

        Iterator<Map.Entry<Long, GridH2KeyValueRowOnheap>> iter = rows.entrySet().iterator();

        while (iter.hasNext()) {
            GridH2KeyValueRowOnheap row = iter.next().getValue();

            if (F.eq(cacheId, row.cacheId()))
                iter.remove();
        }
    }

    /**
     * Update page
     *
     * @param link Link.
     * @throws IgniteCheckedException On error.
     */
    private void touch(long link) throws IgniteCheckedException {
        PageMemory mem = grpCtx.dataRegion().pageMemory();

        int grpId = grpCtx.groupId();

        final long pageId = PageIdUtils.pageId(link);

        final long page = mem.acquirePage(grpId, pageId);

        try {
            // Touch page timestamp
            mem.readLock(grpId, pageId, page);

            mem.readUnlock(grpId, pageId, page);
        }
        finally {
            mem.releasePage(grpId, pageId, page);
        }
    }
}
