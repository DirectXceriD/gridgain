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

namespace Apache.Ignite.Core.Impl.Client
{
    using System;
    using System.Collections.Generic;
    using System.ComponentModel;
    using Apache.Ignite.Core.Client;
    using Apache.Ignite.Core.Impl.Common;

    /// <summary>
    /// Internal representation of client endpoint.
    /// </summary>
    internal class Endpoint
    {
        /** */
        private static readonly string[] HostSeparators = {":"};

        /** */
        private static readonly string[] PortsSeparators = {".."};

        /// <summary>
        /// Initializes a new instance of the <see cref="Endpoint"/> class.
        /// </summary>
        private Endpoint(string host, int port = IgniteClientConfiguration.DefaultPort, int portRange = 0)
        {
            Host = IgniteArgumentCheck.NotNullOrEmpty(host, "host");
            Port = port;
            PortRange = portRange;
        }

        /// <summary>
        /// Gets or sets the host.
        /// </summary>
        public string Host { get; private set; }

        /// <summary>
        /// Gets or sets the port.
        /// </summary>
        [DefaultValue(IgniteClientConfiguration.DefaultPort)]
        public int Port { get; private set; }

        /// <summary>
        /// Size of the port range. Default is 0, meaning only one port is used, defined by <see cref="Port"/>.
        /// </summary>
        public int PortRange { get; private set; }

        /// <summary>
        /// Gets the client endpoints from given configuration.
        /// </summary>
        public static IEnumerable<Endpoint> GetEndpoints(IgniteClientConfiguration cfg)
        {
#pragma warning disable 618 // Type or member is obsolete
            if (cfg.Host != null)
            {
                yield return new Endpoint(cfg.Host, cfg.Port);
            }
#pragma warning restore 618

            if (cfg.Endpoints != null)
            {
                foreach (var endpoint in cfg.Endpoints)
                {
                    yield return ParseEndpoint(endpoint);
                }
            }
        }

        /// <summary>
        /// Parses the endpoint string.
        /// </summary>
        private static Endpoint ParseEndpoint(string endpoint)
        {
            if (string.IsNullOrWhiteSpace(endpoint))
            {
                throw new IgniteClientException(
                    "IgniteClientConfiguration.Endpoints[...] can't be null or whitespace.");
            }

            var parts = endpoint.Split(HostSeparators, StringSplitOptions.None);

            if (parts.Length == 1)
            {
                return new Endpoint(endpoint);
            }

            if (parts.Length == 2)
            {
                var host = parts[0];
                var port = parts[1];

                var ports = port.Split(PortsSeparators, StringSplitOptions.None);

                if (ports.Length == 1)
                {
                    return new Endpoint(host, ParsePort(endpoint, port));
                }

                if (ports.Length == 2)
                {
                    var minPort = ParsePort(endpoint, ports[0]);
                    var maxPort = ParsePort(endpoint, ports[1]);

                    if (maxPort < minPort)
                    {
                        throw new IgniteClientException(
                            "Invalid format of IgniteClientConfiguration.Endpoint, port range is empty: " + endpoint);
                    }

                    return new Endpoint(host, minPort, maxPort - minPort);
                }
            }

            throw new IgniteClientException("Unrecognized format of IgniteClientConfiguration.Endpoint: " + endpoint);
        }

        /// <summary>
        /// Parses the port string.
        /// </summary>
        private static int ParsePort(string endpoint, string portString)
        {
            int port;

            if (int.TryParse(portString, out port))
            {
                return port;
            }

            throw new IgniteClientException(
                "Unrecognized format of IgniteClientConfiguration.Endpoint, failed to parse port: " + endpoint);
        }
    }
}
