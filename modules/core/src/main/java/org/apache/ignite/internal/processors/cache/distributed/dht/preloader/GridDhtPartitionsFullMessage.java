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

package org.apache.ignite.internal.processors.cache.distributed.dht.preloader;

import java.io.Externalizable;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.apache.ignite.IgniteCheckedException;
import org.apache.ignite.cluster.ClusterNode;
import org.apache.ignite.internal.GridDirectMap;
import org.apache.ignite.internal.GridDirectTransient;
import org.apache.ignite.internal.managers.communication.GridIoPolicy;
import org.apache.ignite.internal.managers.discovery.GridDiscoveryManager;
import org.apache.ignite.internal.processors.affinity.AffinityTopologyVersion;
import org.apache.ignite.internal.processors.cache.GridCacheSharedContext;
import org.apache.ignite.internal.processors.cache.distributed.dht.topology.GridDhtPartitionState;
import org.apache.ignite.internal.processors.cache.version.GridCacheVersion;
import org.apache.ignite.internal.util.lang.IgniteThrowableFunction;
import org.apache.ignite.internal.util.tostring.GridToStringInclude;
import org.apache.ignite.internal.util.typedef.F;
import org.apache.ignite.internal.util.typedef.T2;
import org.apache.ignite.internal.util.typedef.internal.S;
import org.apache.ignite.internal.util.typedef.internal.U;
import org.apache.ignite.plugin.extensions.communication.MessageCollectionItemType;
import org.apache.ignite.plugin.extensions.communication.MessageReader;
import org.apache.ignite.plugin.extensions.communication.MessageWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Information about partitions of all nodes in topology.
 */
public class GridDhtPartitionsFullMessage extends GridDhtPartitionsAbstractMessage {
    /** */
    private static final long serialVersionUID = 0L;

    /** grpId -> FullMap */
    @GridToStringInclude
    @GridDirectTransient
    private Map<Integer, GridDhtPartitionFullMap> parts;

    /** */
    @GridDirectMap(keyType = Integer.class, valueType = Integer.class)
    private Map<Integer, Integer> dupPartsData;

    /** */
    private byte[] partsBytes;

    /** Partitions update counters. */
    @GridToStringInclude
    @GridDirectTransient
    private IgniteDhtPartitionCountersMap partCntrs;

    /** Serialized partitions counters. */
    private byte[] partCntrsBytes;

    /** Partitions update counters. */
    @GridToStringInclude
    @GridDirectTransient
    private IgniteDhtPartitionCountersMap2 partCntrs2;

    /** Serialized partitions counters. */
    private byte[] partCntrsBytes2;

    /** Partitions history suppliers. */
    @GridToStringInclude
    @GridDirectTransient
    private IgniteDhtPartitionHistorySuppliersMap partHistSuppliers;

    /** Serialized partitions history suppliers. */
    private byte[] partHistSuppliersBytes;

    /** Partitions that must be cleared and re-loaded. */
    @GridToStringInclude
    @GridDirectTransient
    private IgniteDhtPartitionsToReloadMap partsToReload;

    /** Serialized partitions that must be cleared and re-loaded. */
    private byte[] partsToReloadBytes;

    /** Partitions sizes. */
    @GridToStringInclude
    @GridDirectTransient
    private Map<Integer, Map<Integer, Long>> partsSizes;

    /** Serialized partitions sizes. */
    private byte[] partsSizesBytes;

    /** Topology version. */
    private AffinityTopologyVersion topVer;

    /** Exceptions. */
    @GridToStringInclude
    @GridDirectTransient
    private Map<UUID, Exception> errs;

    /**  */
    private byte[] errsBytes;

    /** */
    @GridDirectTransient
    private transient boolean compress;

    /** */
    private AffinityTopologyVersion resTopVer;

    /** */
    @GridDirectMap(keyType = Integer.class, valueType = CacheGroupAffinityMessage.class)
    private Map<Integer, CacheGroupAffinityMessage> joinedNodeAff;

    /** */
    @GridDirectMap(keyType = Integer.class, valueType = CacheGroupAffinityMessage.class)
    private Map<Integer, CacheGroupAffinityMessage> idealAffDiff;

    /**
     * Required by {@link Externalizable}.
     */
    public GridDhtPartitionsFullMessage() {
        // No-op.
    }

    /**
     * @param id Exchange ID.
     * @param lastVer Last version.
     * @param topVer Topology version. For messages not related to exchange may be {@link AffinityTopologyVersion#NONE}.
     * @param partHistSuppliers
     * @param partsToReload
     */
    public GridDhtPartitionsFullMessage(@Nullable GridDhtPartitionExchangeId id,
        @Nullable GridCacheVersion lastVer,
        @NotNull AffinityTopologyVersion topVer,
        @Nullable IgniteDhtPartitionHistorySuppliersMap partHistSuppliers,
        @Nullable IgniteDhtPartitionsToReloadMap partsToReload) {
        super(id, lastVer);

        assert id == null || topVer.equals(id.topologyVersion());

        this.topVer = topVer;
        this.partHistSuppliers = partHistSuppliers;
        this.partsToReload = partsToReload;
    }

    /** {@inheritDoc} */
    @Override void copyStateTo(GridDhtPartitionsAbstractMessage msg) {
        super.copyStateTo(msg);

        GridDhtPartitionsFullMessage cp = (GridDhtPartitionsFullMessage)msg;

        cp.parts = parts;
        cp.dupPartsData = dupPartsData;
        cp.partsBytes = partsBytes;
        cp.partCntrs = partCntrs;
        cp.partCntrsBytes = partCntrsBytes;
        cp.partCntrs2 = partCntrs2;
        cp.partCntrsBytes2 = partCntrsBytes2;
        cp.partHistSuppliers = partHistSuppliers;
        cp.partHistSuppliersBytes = partHistSuppliersBytes;
        cp.partsToReload = partsToReload;
        cp.partsToReloadBytes = partsToReloadBytes;
        cp.partsSizes = partsSizes;
        cp.partsSizesBytes = partsSizesBytes;
        cp.topVer = topVer;
        cp.errs = errs;
        cp.errsBytes = errsBytes;
        cp.compress = compress;
        cp.resTopVer = resTopVer;
        cp.joinedNodeAff = joinedNodeAff;
        cp.idealAffDiff = idealAffDiff;
    }

    /**
     * @return Message copy.
     */
    GridDhtPartitionsFullMessage copy() {
        GridDhtPartitionsFullMessage cp = new GridDhtPartitionsFullMessage();

        copyStateTo(cp);

        return cp;
    }

    /**
     * @param resTopVer Result topology version.
     */
    public void resultTopologyVersion(AffinityTopologyVersion resTopVer) {
        this.resTopVer = resTopVer;
    }

    /**
     * @return Result topology version.
     */
    public AffinityTopologyVersion resultTopologyVersion() {
        return resTopVer;
    }

    /**
     * @return Caches affinity for joining nodes.
     */
    @Nullable public Map<Integer, CacheGroupAffinityMessage> joinedNodeAffinity() {
        return joinedNodeAff;
    }

    /**
     * @param joinedNodeAff Caches affinity for joining nodes.
     */
    GridDhtPartitionsFullMessage joinedNodeAffinity(Map<Integer, CacheGroupAffinityMessage> joinedNodeAff) {
        this.joinedNodeAff = joinedNodeAff;

        return this;
    }

    /**
     * @return Difference with ideal affinity.
     */
    @Nullable public Map<Integer, CacheGroupAffinityMessage> idealAffinityDiff() {
        return idealAffDiff;
    }

    /**
     * @param idealAffDiff Difference with ideal affinity.
     */
    void idealAffinityDiff(Map<Integer, CacheGroupAffinityMessage> idealAffDiff) {
        this.idealAffDiff = idealAffDiff;
    }

    /** {@inheritDoc} */
    @Override public int handlerId() {
        return 0;
    }

    /**
     * @param compress {@code True} if it is possible to use compression for message.
     */
    public void compress(boolean compress) {
        this.compress = compress;
    }

    /**
     * @return Local partitions.
     */
    public Map<Integer, GridDhtPartitionFullMap> partitions() {
        if (parts == null)
            parts = new HashMap<>();

        return parts;
    }

    /**
     * @param grpId Cache group ID.
     * @return {@code True} if message contains full map for given cache.
     */
    public boolean containsGroup(int grpId) {
        return parts != null && parts.containsKey(grpId);
    }

    /**
     * @param grpId Cache group ID.
     * @param fullMap Full partitions map.
     * @param dupDataCache Optional ID of cache with the same partition state map.
     */
    public void addFullPartitionsMap(int grpId, GridDhtPartitionFullMap fullMap, @Nullable Integer dupDataCache) {
        assert fullMap != null;

        if (parts == null)
            parts = new HashMap<>();

        if (!parts.containsKey(grpId)) {
            parts.put(grpId, fullMap);

            if (dupDataCache != null) {
                assert compress;
                assert parts.containsKey(dupDataCache);

                if (dupPartsData == null)
                    dupPartsData = new HashMap<>();

                dupPartsData.put(grpId, dupDataCache);
            }
        }
    }

    /**
     * @param grpId Cache group ID.
     * @param cntrMap Partition update counters.
     */
    public void addPartitionUpdateCounters(int grpId, Map<Integer, T2<Long, Long>> cntrMap) {
        if (partCntrs == null)
            partCntrs = new IgniteDhtPartitionCountersMap();

        partCntrs.putIfAbsent(grpId, cntrMap);
    }

    /**
     * @param grpId Cache group ID.
     * @param cntrMap Partition update counters.
     */
    public void addPartitionUpdateCounters(int grpId, CachePartitionFullCountersMap cntrMap) {
        if (partCntrs2 == null)
            partCntrs2 = new IgniteDhtPartitionCountersMap2();

        partCntrs2.putIfAbsent(grpId, cntrMap);
    }

    /**
     * @param grpId Cache group ID.
     * @param partsCnt Total cache partitions.
     * @return Partition update counters.
     */
    public CachePartitionFullCountersMap partitionUpdateCounters(int grpId, int partsCnt) {
        if (partCntrs2 != null)
            return partCntrs2.get(grpId);

        if (partCntrs == null)
            return null;

        Map<Integer, T2<Long, Long>> map = partCntrs.get(grpId);

        return map != null ? CachePartitionFullCountersMap.fromCountersMap(map, partsCnt) : null;
    }

    /**
     *
     */
    public IgniteDhtPartitionHistorySuppliersMap partitionHistorySuppliers() {
        if (partHistSuppliers == null)
            return IgniteDhtPartitionHistorySuppliersMap.empty();

        return partHistSuppliers;
    }

    /**
     *
     */
    public Set<Integer> partsToReload(UUID nodeId, int grpId) {
        if (partsToReload == null)
            return Collections.emptySet();

        return partsToReload.get(nodeId, grpId);
    }

    /**
     * Adds partition sizes map for specified {@code grpId} to the current message.
     *
     * @param grpId Group id.
     * @param partSizesMap Partition sizes map.
     */
    public void addPartitionSizes(int grpId, Map<Integer, Long> partSizesMap) {
        if (partSizesMap.isEmpty())
            return;

        if (partsSizes == null)
            partsSizes = new HashMap<>();

        partsSizes.put(grpId, partSizesMap);
    }

    /**
     * Returns partition sizes map for specified {@code grpId}.
     *
     * @param grpId Group id.
     * @return Partition sizes map (partId, partSize).
     */
    public Map<Integer, Long> partitionSizes(int grpId) {
        if (partsSizes == null)
            return Collections.emptyMap();

        return partsSizes.getOrDefault(grpId, Collections.emptyMap());
    }

    /**
     * @return Errors map.
     */
    @Nullable Map<UUID, Exception> getErrorsMap() {
        return errs;
    }

    /**
     * @param errs Errors map.
     */
    void setErrorsMap(Map<UUID, Exception> errs) {
        this.errs = new HashMap<>(errs);
    }

    /** {@inheritDoc} */
    @Override public void prepareMarshal(GridCacheSharedContext ctx) throws IgniteCheckedException {
        super.prepareMarshal(ctx);

        boolean marshal = (!F.isEmpty(parts) && partsBytes == null) ||
            (partCntrs != null && !partCntrs.empty() && partCntrsBytes == null) ||
            (partCntrs2 != null && !partCntrs2.empty() && partCntrsBytes2 == null) ||
            (partHistSuppliers != null && partHistSuppliersBytes == null) ||
            (partsToReload != null && partsToReloadBytes == null) ||
            (!F.isEmpty(errs) && errsBytes == null);

        if (marshal) {
            // Reserve at least 2 threads for system operations.
            int parallelismLvl = U.availableThreadCount(ctx.kernalContext(), GridIoPolicy.SYSTEM_POOL, 2);

            Collection<Object> objectsToMarshall = new ArrayList<>();

            if (!F.isEmpty(parts) && partsBytes == null)
                objectsToMarshall.add(parts);

            if (partCntrs != null && !partCntrs.empty() && partCntrsBytes == null)
                objectsToMarshall.add(partCntrs);

            if (partCntrs2 != null && !partCntrs2.empty() && partCntrsBytes2 == null)
                objectsToMarshall.add(partCntrs2);

            if (partHistSuppliers != null && partHistSuppliersBytes == null)
                objectsToMarshall.add(partHistSuppliers);

            if (partsToReload != null && partsToReloadBytes == null)
                objectsToMarshall.add(partsToReload);

            if (partsSizes != null && partsSizesBytes == null)
                objectsToMarshall.add(partsSizes);

            if (!F.isEmpty(errs) && errsBytes == null)
                objectsToMarshall.add(errs);

            Collection<byte[]> marshalled = U.doInParallel(
                parallelismLvl,
                ctx.kernalContext().getSystemExecutorService(),
                objectsToMarshall,
                new IgniteThrowableFunction<Object, byte[]>() {
                    @Override public byte[] apply(Object payload) throws IgniteCheckedException {
                        byte[] marshalled = U.marshal(ctx, payload);

                        if(compress)
                            marshalled = U.zip(marshalled, ctx.gridConfig().getNetworkCompressionLevel());

                        return marshalled;
                    }
            });

            Iterator<byte[]> iterator = marshalled.iterator();

            if (!F.isEmpty(parts) && partsBytes == null)
                partsBytes = iterator.next();

            if (partCntrs != null && !partCntrs.empty() && partCntrsBytes == null)
                partCntrsBytes = iterator.next();

            if (partCntrs2 != null && !partCntrs2.empty() && partCntrsBytes2 == null)
                partCntrsBytes2 = iterator.next();

            if (partHistSuppliers != null && partHistSuppliersBytes == null)
                partHistSuppliersBytes = iterator.next();

            if (partsToReload != null && partsToReloadBytes == null)
                partsToReloadBytes = iterator.next();

            if (partsSizes != null && partsSizesBytes == null)
                partsSizesBytes = iterator.next();

            if (!F.isEmpty(errs) && errsBytes == null)
                errsBytes = iterator.next();

            if (compress) {
                assert !compressed() : "Unexpected compressed state";

                compressed(true);
            }
        }
    }

    /**
     * @return Topology version.
     */
    @Override public AffinityTopologyVersion topologyVersion() {
        return topVer;
    }

    /**
     * @param topVer Topology version.
     */
    public void topologyVersion(AffinityTopologyVersion topVer) {
        this.topVer = topVer;
    }

    /** {@inheritDoc} */
    @Override public void finishUnmarshal(GridCacheSharedContext ctx, ClassLoader ldr) throws IgniteCheckedException {
        super.finishUnmarshal(ctx, ldr);

        ClassLoader classLoader = U.resolveClassLoader(ldr, ctx.gridConfig());

        Collection<byte[]> objectsToUnmarshall = new ArrayList<>();

        // Reserve at least 2 threads for system operations.
        int parallelismLvl = U.availableThreadCount(ctx.kernalContext(), GridIoPolicy.SYSTEM_POOL, 2);

        if (partsBytes != null && parts == null)
            objectsToUnmarshall.add(partsBytes);

        if (partCntrsBytes != null && partCntrs == null)
            objectsToUnmarshall.add(partCntrsBytes);

        if (partCntrsBytes2 != null && partCntrs2 == null)
            objectsToUnmarshall.add(partCntrsBytes2);

        if (partHistSuppliersBytes != null && partHistSuppliers == null)
            objectsToUnmarshall.add(partHistSuppliersBytes);

        if (partsToReloadBytes != null && partsToReload == null)
            objectsToUnmarshall.add(partsToReloadBytes);

        if (partsSizesBytes != null && partsSizes == null)
            objectsToUnmarshall.add(partsSizesBytes);

        if (errsBytes != null && errs == null)
            objectsToUnmarshall.add(errsBytes);

        Collection<Object> unmarshalled = U.doInParallel(
            parallelismLvl,
            ctx.kernalContext().getSystemExecutorService(),
            objectsToUnmarshall,
            new IgniteThrowableFunction<byte[], Object>() {
                @Override public Object apply(byte[] binary) throws IgniteCheckedException {
                    return compressed()
                        ? U.unmarshalZip(ctx.marshaller(), binary, classLoader)
                        : U.unmarshal(ctx, binary, classLoader);
                }
            }
        );

        Iterator<Object> iterator = unmarshalled.iterator();

        if (partsBytes != null && parts == null) {
            parts = (Map<Integer, GridDhtPartitionFullMap>)iterator.next();

            if (dupPartsData != null) {
                assert parts != null;

                for (Map.Entry<Integer, Integer> e : dupPartsData.entrySet()) {
                    GridDhtPartitionFullMap map1 = parts.get(e.getKey());
                    GridDhtPartitionFullMap map2 = parts.get(e.getValue());

                    assert map1 != null : e.getKey();
                    assert map2 != null : e.getValue();
                    assert map1.size() == map2.size();

                    for (Map.Entry<UUID, GridDhtPartitionMap> e0 : map2.entrySet()) {
                        GridDhtPartitionMap partMap1 = map1.get(e0.getKey());

                        assert partMap1 != null && partMap1.map().isEmpty() : partMap1;
                        assert !partMap1.hasMovingPartitions() : partMap1;

                        GridDhtPartitionMap partMap2 = e0.getValue();

                        assert partMap2 != null;

                        for (Map.Entry<Integer, GridDhtPartitionState> stateEntry : partMap2.entrySet())
                            partMap1.put(stateEntry.getKey(), stateEntry.getValue());
                    }
                }
            }
        }

        if (partCntrsBytes != null && partCntrs == null)
            partCntrs = (IgniteDhtPartitionCountersMap)iterator.next();

        if (partCntrsBytes2 != null && partCntrs2 == null)
            partCntrs2 = (IgniteDhtPartitionCountersMap2)iterator.next();

        if (partHistSuppliersBytes != null && partHistSuppliers == null)
            partHistSuppliers = (IgniteDhtPartitionHistorySuppliersMap)iterator.next();

        if (partsToReloadBytes != null && partsToReload == null)
            partsToReload = (IgniteDhtPartitionsToReloadMap)iterator.next();

        if (partsSizesBytes != null && partsSizes == null)
            partsSizes = (Map<Integer, Map<Integer, Long>>)iterator.next();

        if (errsBytes != null && errs == null)
            errs = (Map<UUID, Exception>)iterator.next();

        if (parts == null)
            parts = new HashMap<>();

        if (partCntrs == null)
            partCntrs = new IgniteDhtPartitionCountersMap();

        if (partCntrs2 == null)
            partCntrs2 = new IgniteDhtPartitionCountersMap2();

        if(partHistSuppliers == null)
            partHistSuppliers = new IgniteDhtPartitionHistorySuppliersMap();

        if(partsToReload == null)
            partsToReload = new IgniteDhtPartitionsToReloadMap();

        if(partsSizes == null)
            partsSizes = new HashMap<>();

        if (errs == null)
            errs = new HashMap<>();
    }

    /** {@inheritDoc} */
    @Override public boolean writeTo(ByteBuffer buf, MessageWriter writer) {
        writer.setBuffer(buf);

        if (!super.writeTo(buf, writer))
            return false;

        if (!writer.isHeaderWritten()) {
            if (!writer.writeHeader(directType(), fieldsCount()))
                return false;

            writer.onHeaderWritten();
        }

        switch (writer.state()) {
            case 6:
                if (!writer.writeMap("dupPartsData", dupPartsData, MessageCollectionItemType.INT, MessageCollectionItemType.INT))
                    return false;

                writer.incrementState();

            case 7:
                if (!writer.writeByteArray("errsBytes", errsBytes))
                    return false;

                writer.incrementState();

            case 8:
                if (!writer.writeMap("idealAffDiff", idealAffDiff, MessageCollectionItemType.INT, MessageCollectionItemType.MSG))
                    return false;

                writer.incrementState();

            case 9:
                if (!writer.writeMap("joinedNodeAff", joinedNodeAff, MessageCollectionItemType.INT, MessageCollectionItemType.MSG))
                    return false;

                writer.incrementState();

            case 10:
                if (!writer.writeByteArray("partCntrsBytes", partCntrsBytes))
                    return false;

                writer.incrementState();

            case 11:
                if (!writer.writeByteArray("partCntrsBytes2", partCntrsBytes2))
                    return false;

                writer.incrementState();

            case 12:
                if (!writer.writeByteArray("partHistSuppliersBytes", partHistSuppliersBytes))
                    return false;

                writer.incrementState();

            case 13:
                if (!writer.writeByteArray("partsBytes", partsBytes))
                    return false;

                writer.incrementState();

            case 14:
                if (!writer.writeByteArray("partsSizesBytes", partsSizesBytes))
                    return false;

                writer.incrementState();

            case 15:
                if (!writer.writeByteArray("partsToReloadBytes", partsToReloadBytes))
                    return false;

                writer.incrementState();

            case 16:
                if (!writer.writeAffinityTopologyVersion("resTopVer", resTopVer))
                    return false;

                writer.incrementState();

            case 17:
                if (!writer.writeAffinityTopologyVersion("topVer", topVer))
                    return false;

                writer.incrementState();

        }

        return true;
    }

    /** {@inheritDoc} */
    @Override public boolean readFrom(ByteBuffer buf, MessageReader reader) {
        reader.setBuffer(buf);

        if (!reader.beforeMessageRead())
            return false;

        if (!super.readFrom(buf, reader))
            return false;

        switch (reader.state()) {
            case 6:
                dupPartsData = reader.readMap("dupPartsData", MessageCollectionItemType.INT, MessageCollectionItemType.INT, false);

                if (!reader.isLastRead())
                    return false;

                reader.incrementState();

            case 7:
                errsBytes = reader.readByteArray("errsBytes");

                if (!reader.isLastRead())
                    return false;

                reader.incrementState();

            case 8:
                idealAffDiff = reader.readMap("idealAffDiff", MessageCollectionItemType.INT, MessageCollectionItemType.MSG, false);

                if (!reader.isLastRead())
                    return false;

                reader.incrementState();

            case 9:
                joinedNodeAff = reader.readMap("joinedNodeAff", MessageCollectionItemType.INT, MessageCollectionItemType.MSG, false);

                if (!reader.isLastRead())
                    return false;

                reader.incrementState();

            case 10:
                partCntrsBytes = reader.readByteArray("partCntrsBytes");

                if (!reader.isLastRead())
                    return false;

                reader.incrementState();

            case 11:
                partCntrsBytes2 = reader.readByteArray("partCntrsBytes2");

                if (!reader.isLastRead())
                    return false;

                reader.incrementState();

            case 12:
                partHistSuppliersBytes = reader.readByteArray("partHistSuppliersBytes");

                if (!reader.isLastRead())
                    return false;

                reader.incrementState();

            case 13:
                partsBytes = reader.readByteArray("partsBytes");

                if (!reader.isLastRead())
                    return false;

                reader.incrementState();

            case 14:
                partsSizesBytes = reader.readByteArray("partsSizesBytes");

                if (!reader.isLastRead())
                    return false;

                reader.incrementState();

            case 15:
                partsToReloadBytes = reader.readByteArray("partsToReloadBytes");

                if (!reader.isLastRead())
                    return false;

                reader.incrementState();

            case 16:
                resTopVer = reader.readAffinityTopologyVersion("resTopVer");

                if (!reader.isLastRead())
                    return false;

                reader.incrementState();

            case 17:
                topVer = reader.readAffinityTopologyVersion("topVer");

                if (!reader.isLastRead())
                    return false;

                reader.incrementState();

        }

        return reader.afterMessageRead(GridDhtPartitionsFullMessage.class);
    }

    /** {@inheritDoc} */
    @Override public short directType() {
        return 46;
    }

    /** {@inheritDoc} */
    @Override public byte fieldsCount() {
        return 18;
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(GridDhtPartitionsFullMessage.class, this, "partCnt", parts != null ? parts.size() : 0,
            "super", super.toString());
    }

    /**
     * Merges (replaces with newer) partitions map from given {@code other} full message.
     *
     * @param other Other full message.
     */
    public void merge(GridDhtPartitionsFullMessage other, GridDiscoveryManager discovery) {
        assert other.exchangeId() == null && exchangeId() == null :
            "Both current and merge full message must have exchangeId == null"
             + other.exchangeId() + "," + exchangeId();

        for (Map.Entry<Integer, GridDhtPartitionFullMap> groupAndMap : other.partitions().entrySet()) {
            int grpId = groupAndMap.getKey();
            GridDhtPartitionFullMap updMap = groupAndMap.getValue();

            GridDhtPartitionFullMap currMap = partitions().get(grpId);

            if (currMap == null)
                partitions().put(grpId, updMap);
            else {
                ClusterNode currentMapSentBy = discovery.node(currMap.nodeId());
                ClusterNode newMapSentBy = discovery.node(updMap.nodeId());

                if (newMapSentBy == null)
                    return;

                if (currentMapSentBy == null || newMapSentBy.order() > currentMapSentBy.order() || updMap.compareTo(currMap) >= 0)
                    partitions().put(grpId, updMap);
            }
        }
    }
}
