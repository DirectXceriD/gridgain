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

package org.apache.ignite.spi.discovery.tcp.ipfinder;

import java.net.InetSocketAddress;
import java.util.Collection;
import org.apache.ignite.Ignite;
import org.apache.ignite.internal.util.tostring.GridToStringExclude;
import org.apache.ignite.internal.util.typedef.internal.S;
import org.apache.ignite.resources.IgniteInstanceResource;
import org.apache.ignite.spi.IgniteSpiConfiguration;
import org.apache.ignite.spi.IgniteSpiContext;
import org.apache.ignite.spi.IgniteSpiException;
import org.apache.ignite.spi.discovery.DiscoverySpi;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;

/**
 * IP finder interface implementation adapter.
 */
public abstract class TcpDiscoveryIpFinderAdapter implements TcpDiscoveryIpFinder {
    /** Shared flag. */
    private boolean shared;

    /** SPI context. */
    @GridToStringExclude
    private volatile IgniteSpiContext spiCtx;

    /** Ignite instance . */
    @IgniteInstanceResource
    @GridToStringExclude
    protected Ignite ignite;

    /** {@inheritDoc} */
    @Override public void onSpiContextInitialized(IgniteSpiContext spiCtx) throws IgniteSpiException {
        this.spiCtx = spiCtx;
    }

    /** {@inheritDoc} */
    @Override public void onSpiContextDestroyed() {
        spiCtx = null;
    }

    /** {@inheritDoc} */
    @Override public void initializeLocalAddresses(Collection<InetSocketAddress> addrs) throws IgniteSpiException {
        if (!discoveryClientMode())
            registerAddresses(addrs);
    }

    /** {@inheritDoc} */
    @Override public boolean isShared() {
        return shared;
    }

    /**
     * Sets shared flag. If {@code true} then it is expected that IP addresses registered
     * with IP finder will be seen by IP finders on all other nodes.
     *
     * @param shared {@code true} if this IP finder is shared.
     * @return {@code this} for chaining.
     */
    @IgniteSpiConfiguration(optional = true)
    public TcpDiscoveryIpFinderAdapter setShared(boolean shared) {
        this.shared = shared;

        return this;
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(TcpDiscoveryIpFinderAdapter.class, this);
    }

    /** {@inheritDoc} */
    @Override public void close() {
        // No-op.
    }

    /**
     * @return {@code True} if TCP discovery works in client mode.
     */
    protected boolean discoveryClientMode() {
        boolean clientMode;

        Ignite ignite0 = ignite;

        if (ignite0 != null) { // Can be null if used in tests without starting Ignite.
            DiscoverySpi discoSpi = ignite0.configuration().getDiscoverySpi();

            if (!(discoSpi instanceof TcpDiscoverySpi))
                throw new IgniteSpiException("TcpDiscoveryIpFinder should be used with TcpDiscoverySpi: " + discoSpi);

            clientMode = ignite0.configuration().isClientMode() && !((TcpDiscoverySpi)discoSpi).isForceServerMode();
        }
        else
            clientMode = false;

        return clientMode;
    }

    /**
     * @return SPI context.
     */
    protected IgniteSpiContext spiContext() {
        return spiCtx;
    }
}