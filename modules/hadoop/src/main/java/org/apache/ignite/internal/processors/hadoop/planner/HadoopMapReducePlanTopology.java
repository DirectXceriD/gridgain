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

package org.apache.ignite.internal.processors.hadoop.planner;

import org.apache.ignite.internal.util.typedef.internal.S;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Map-reduce plan topology.
 */
public class HadoopMapReducePlanTopology {
    /** All groups. */
    private final List<HadoopMapReducePlanGroup> grps;

    /** Node ID to group map. */
    private final Map<UUID, HadoopMapReducePlanGroup> idToGrp;

    /** Host to group map. */
    private final Map<String, HadoopMapReducePlanGroup> hostToGrp;

    /**
     * Constructor.
     *
     * @param grps All groups.
     * @param idToGrp ID to group map.
     * @param hostToGrp Host to group map.
     */
    public HadoopMapReducePlanTopology(List<HadoopMapReducePlanGroup> grps,
        Map<UUID, HadoopMapReducePlanGroup> idToGrp, Map<String, HadoopMapReducePlanGroup> hostToGrp) {
        assert grps != null;
        assert idToGrp != null;
        assert hostToGrp != null;

        this.grps = grps;
        this.idToGrp = idToGrp;
        this.hostToGrp = hostToGrp;
    }

    /**
     * @return All groups.
     */
    public List<HadoopMapReducePlanGroup> groups() {
        return grps;
    }

    /**
     * Get group for node ID.
     *
     * @param id Node ID.
     * @return Group.
     */
    public HadoopMapReducePlanGroup groupForId(UUID id) {
        return idToGrp.get(id);
    }

    /**
     * Get group for host.
     *
     * @param host Host.
     * @return Group.
     */
    @Nullable public HadoopMapReducePlanGroup groupForHost(String host) {
        return hostToGrp.get(host);
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(HadoopMapReducePlanTopology.class, this);
    }
}
