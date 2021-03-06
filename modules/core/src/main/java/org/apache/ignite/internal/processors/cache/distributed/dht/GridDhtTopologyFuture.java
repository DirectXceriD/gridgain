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

package org.apache.ignite.internal.processors.cache.distributed.dht;

import java.util.Collection;
import org.apache.ignite.cache.affinity.Affinity;
import org.apache.ignite.internal.IgniteInternalFuture;
import org.apache.ignite.internal.processors.affinity.AffinityTopologyVersion;
import org.apache.ignite.internal.processors.cache.GridCacheContext;
import org.apache.ignite.internal.processors.cache.GridCachePartitionExchangeManager;
import org.apache.ignite.internal.processors.cache.distributed.dht.preloader.GridDhtPartitionsExchangeFuture;
import org.apache.ignite.internal.util.future.GridFutureAdapter;
import org.jetbrains.annotations.Nullable;

/**
 * Future that implements a barrier after which dht topology is safe to use. Topology is considered to be
 * safe to use when all transactions that involve moving primary partitions are completed and partition map
 * exchange is also completed.
 * <p/>
 * When new cache operation is started, it will wait for this future before acquiring new locks on particular
 * topology version.
 */
public interface GridDhtTopologyFuture extends IgniteInternalFuture<AffinityTopologyVersion> {
    /**
     * Returns topology version when exchange started. It can differ from result topology version if exchanges for
     * multiple discovery events are merged. Initial version should not be used as version for cache operation
     * since it is possible affinity for this version is never calculated.
     *
     * @return Topology version when exchange started.
     */
    public AffinityTopologyVersion initialVersion();

    /**
     * Gets result topology version of this future. Result version can differ from initial exchange version
     * if exchanges for multiple discovery events are merged, in this case result version is version of last
     * discovery event.
     * <p>
     * This method should be called only for finished topology future
     * since result version is not known before exchange finished.
     *
     * @return Result topology version.
     */
    public AffinityTopologyVersion topologyVersion();

    /**
     * Ready affinity future ({@link GridCachePartitionExchangeManager#affinityReadyFuture(AffinityTopologyVersion)}
     * is completed before {@link GridFutureAdapter#onDone(Object, Throwable)} is called on
     * {@link GridDhtPartitionsExchangeFuture}, it is guaranteed that this method will return {@code true}
     * if affinity ready future is finished.
     * <p>
     * Also this method returns {@code false} for merged exchange futures.
     *
     * @return {@code True} if exchange is finished and result topology version can be used.
     */
    public boolean exchangeDone();

    /**
     * Returns error is cache topology is not valid.
     *
     * @param cctx Cache context.
     * @param recovery {@code True} if cache operation is done in recovery mode. Then it will only check
     *      for cache active state and topology validator result.
     * @param read {@code True} if validating read operation, {@code false} if validating write.
     * @param key Key (optimization to avoid collection creation).
     * @param keys Keys involved in a cache operation.
     * @return valid ot not.
     */
    @Nullable public Throwable validateCache(
        GridCacheContext cctx,
        boolean recovery,
        boolean read,
        @Nullable Object key,
        @Nullable Collection<?> keys);

    /**
     *
     * @return {@code True} if this exchange changed affinity.
     */
    public boolean changedAffinity();
}