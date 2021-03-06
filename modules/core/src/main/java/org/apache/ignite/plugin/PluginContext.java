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

package org.apache.ignite.plugin;

import java.util.Collection;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteLogger;
import org.apache.ignite.cluster.ClusterNode;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.marshaller.MarshallerContext;
import org.apache.ignite.spi.IgnitePortProtocol;
import org.apache.ignite.spi.discovery.DiscoverySpi;

/**
 * Plugin context.
 */
public interface PluginContext {
    /**
     * @return Ignite configuration.
     */
    public IgniteConfiguration igniteConfiguration();

    /**
     * @return Grid.
     */
    public Ignite grid();

    /**
     * @return Marshaller context.
     */
    public MarshallerContext marshallerContext();

    /**
     * Gets a collection of all grid nodes. Remote nodes are discovered via underlying
     * {@link DiscoverySpi} implementation used.
     *
     * @return Collection of grid nodes.
     * @see #localNode()
     * @see DiscoverySpi
     */
    public Collection<ClusterNode> nodes();

    /**
     * Gets local grid node. Instance of local node is provided by underlying {@link DiscoverySpi}
     * implementation used.
     *
     * @return Local grid node.
     * @see DiscoverySpi
     */
    public ClusterNode localNode();

    /**
     * Gets logger for given class.
     *
     * @param cls Class to get logger for.
     * @return Logger.
     */
    public IgniteLogger log(Class<?> cls);

    /**
     * Registers open port.
     *
     * @param port Port.
     * @param proto Protocol.
     * @param cls Class.
     */
    public void registerPort(int port, IgnitePortProtocol proto, Class<?> cls);

    /**
     * Deregisters closed port.
     *
     * @param port Port.
     * @param proto Protocol.
     * @param cls Class.
     */
    public void deregisterPort(int port, IgnitePortProtocol proto, Class<?> cls);

    /**
     * Deregisters all closed ports.
     *
     * @param cls Class.
     */
    public void deregisterPorts(Class<?> cls);
}