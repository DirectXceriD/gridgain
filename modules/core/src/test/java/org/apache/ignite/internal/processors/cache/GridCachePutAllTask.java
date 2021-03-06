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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.cache.CacheException;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.IgniteException;
import org.apache.ignite.IgniteLogger;
import org.apache.ignite.cluster.ClusterNode;
import org.apache.ignite.compute.ComputeJob;
import org.apache.ignite.compute.ComputeJobAdapter;
import org.apache.ignite.compute.ComputeJobResult;
import org.apache.ignite.compute.ComputeJobResultPolicy;
import org.apache.ignite.compute.ComputeTaskAdapter;
import org.apache.ignite.internal.util.typedef.F;
import org.apache.ignite.internal.util.typedef.internal.U;
import org.apache.ignite.lang.IgnitePredicate;
import org.apache.ignite.resources.IgniteInstanceResource;
import org.apache.ignite.resources.LoggerResource;
import org.jetbrains.annotations.Nullable;

/**
 * Puts all the passed data into partitioned cache in small chunks.
 */
class GridCachePutAllTask extends ComputeTaskAdapter<Collection<Integer>, Void> {
    /** */
    private static final boolean DEBUG_DATA = false;

    /** Number of entries per put. */
    private static final int TX_BOUND = 30;

    /** Preferred node. */
    private final UUID preferredNode;

    /** Cache name. */
    private final String cacheName;

    /**
     *
     * @param preferredNode A node that we'd prefer to take from grid.
     * @param cacheName A name of the cache to work with.
     */
    GridCachePutAllTask(UUID preferredNode, String cacheName) {
        this.preferredNode = preferredNode;
        this.cacheName = cacheName;
    }

    /** {@inheritDoc} */
    @Override public Map<? extends ComputeJob, ClusterNode> map(List<ClusterNode> subgrid,
        @Nullable final Collection<Integer> data) {
        assert !subgrid.isEmpty();

        // Give preference to wanted node. Otherwise, take the first one.
        ClusterNode targetNode = F.find(subgrid, subgrid.get(0), new IgnitePredicate<ClusterNode>() {
            /** {@inheritDoc} */
            @Override public boolean apply(ClusterNode e) {
                return preferredNode.equals(e.id());
            }
        });

        return Collections.singletonMap(
            new ComputeJobAdapter() {
                @LoggerResource
                private IgniteLogger log;

                @IgniteInstanceResource
                private Ignite ignite;

                @Override public Object execute() {
                    if (DEBUG_DATA)
                        log.info("Going to put data: " + data);
                    else
                        log.info("Going to put data [size=" + data.size() + ']');

                    IgniteCache<Object, Object> cache = ignite.cache(cacheName);

                    assert cache != null;

                    HashMap<Integer, Integer> putMap = U.newLinkedHashMap(TX_BOUND);

                    Iterator<Integer> it = data.iterator();

                    int cnt = 0;

                    final int RETRIES = 5;

                    while (it.hasNext()) {
                        Integer val = it.next();

                        putMap.put(val, val);

                        if (++cnt == TX_BOUND) {
                            if (DEBUG_DATA)
                                log.info("Putting keys to cache: " + putMap.keySet());
                            else
                                log.info("Putting keys to cache [size=" + putMap.size() + ']');

                            for (int i = 0; i < RETRIES; i++) {
                                try {
                                    cache.putAll(putMap);

                                    break;
                                }
                                catch (CacheException e) {
                                    if (i < RETRIES - 1)
                                        log.info("Put error, will retry: " + e);
                                    else
                                        throw new IgniteException(e);
                                }
                            }

                            cnt = 0;

                            putMap = U.newLinkedHashMap(TX_BOUND);
                        }
                    }

                    assert cnt < TX_BOUND;
                    assert putMap.size() == (data.size() % TX_BOUND) : "putMap.size() = " + putMap.size();

                    if (DEBUG_DATA)
                        log.info("Putting keys to cache: " + putMap.keySet());
                    else
                        log.info("Putting keys to cache [size=" + putMap.size() + ']');

                    for (int i = 0; i < RETRIES; i++) {
                        try {
                            cache.putAll(putMap);

                            break;
                        }
                        catch (CacheException e) {
                            if (i < RETRIES - 1)
                                log.info("Put error, will retry: " + e);
                            else
                                throw new IgniteException(e);
                        }
                    }

                    if (DEBUG_DATA)
                        log.info("Finished putting data: " + data);
                    else
                        log.info("Finished putting data [size=" + data.size() + ']');

                    return data;
                }
            },
            targetNode);
    }

    /** {@inheritDoc} */
    @Override public ComputeJobResultPolicy result(ComputeJobResult res, List<ComputeJobResult> rcvd) {
        if (res.getException() != null)
            return ComputeJobResultPolicy.FAILOVER;

        return ComputeJobResultPolicy.WAIT;
    }

    /** {@inheritDoc} */
    @Nullable @Override public Void reduce(List<ComputeJobResult> results) {
        return null;
    }
}