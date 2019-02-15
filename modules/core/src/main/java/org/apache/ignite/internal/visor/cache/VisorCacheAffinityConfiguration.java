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

package org.apache.ignite.internal.visor.cache;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.apache.ignite.cache.affinity.AffinityFunction;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.internal.util.typedef.internal.S;
import org.apache.ignite.internal.util.typedef.internal.U;
import org.apache.ignite.internal.visor.VisorDataTransferObject;
import org.jetbrains.annotations.Nullable;

import static org.apache.ignite.internal.util.IgniteUtils.findNonPublicMethod;
import static org.apache.ignite.internal.visor.util.VisorTaskUtils.compactClass;

/**
 * Data transfer object for affinity configuration properties.
 */
public class VisorCacheAffinityConfiguration extends VisorDataTransferObject {
    /** */
    private static final long serialVersionUID = 0L;

    /** Cache affinity function. */
    private String function;

    /** Cache affinity mapper. */
    private String mapper;

    /** Number of backup nodes for one partition. */
    private int partitionedBackups;

    /** Total partition count. */
    private int partitions;

    /** Cache partitioned affinity exclude neighbors. */
    private Boolean exclNeighbors;

    /**
     * Default constructor
     */
    public VisorCacheAffinityConfiguration() {
        // No-op.
    }

    /**
     * Create data transfer object for affinity configuration properties.
     *
     * @param ccfg Cache configuration.
     */
    public VisorCacheAffinityConfiguration(CacheConfiguration ccfg) {
        AffinityFunction aff = ccfg.getAffinity();

        function = compactClass(aff);
        mapper = compactClass(ccfg.getAffinityMapper());
        partitions = aff.partitions();
        partitionedBackups = ccfg.getBackups();

        Method mthd = findNonPublicMethod(aff.getClass(), "isExcludeNeighbors");

        if (mthd != null) {
            try {
                exclNeighbors = (Boolean)mthd.invoke(aff);
            }
            catch (InvocationTargetException | IllegalAccessException ignored) {
                //  No-op.
            }
        }
    }

    /**
     * @return Cache affinity.
     */
    public String getFunction() {
        return function;
    }

    /**
     * @return Cache affinity mapper.
     */
    public String getMapper() {
        return mapper;
    }

    /**
     * @return Number of backup nodes for one partition.
     */
    public int getPartitionedBackups() {
        return partitionedBackups;
    }

    /**
     * @return Total partition count.
     */
    public int getPartitions() {
        return partitions;
    }

    /**
     * @return Cache partitioned affinity exclude neighbors.
     */
    @Nullable public Boolean isExcludeNeighbors() {
        return exclNeighbors;
    }

    /** {@inheritDoc} */
    @Override protected void writeExternalData(ObjectOutput out) throws IOException {
        U.writeString(out, function);
        U.writeString(out, mapper);
        out.writeInt(partitionedBackups);
        out.writeInt(partitions);
        out.writeObject(exclNeighbors);
    }

    /** {@inheritDoc} */
    @Override protected void readExternalData(byte protoVer, ObjectInput in) throws IOException, ClassNotFoundException {
        function = U.readString(in);
        mapper = U.readString(in);
        partitionedBackups = in.readInt();
        partitions = in.readInt();
        exclNeighbors = (Boolean)in.readObject();
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(VisorCacheAffinityConfiguration.class, this);
    }
}
