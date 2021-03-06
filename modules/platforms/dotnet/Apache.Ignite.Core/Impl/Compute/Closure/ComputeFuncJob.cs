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

namespace Apache.Ignite.Core.Impl.Compute.Closure
{
    using System;
    using Apache.Ignite.Core.Binary;
    using Apache.Ignite.Core.Impl.Binary;
    using Apache.Ignite.Core.Impl.Deployment;
    using Apache.Ignite.Core.Impl.Resource;

    /// <summary>
    /// System job which wraps over <c>Func</c>.
    /// </summary>
    internal class ComputeFuncJob : IComputeJob, IComputeResourceInjector, IBinaryWriteAware
    {
        /** Closure. */
        private readonly IComputeFunc _clo;

        /** Argument. */
        private readonly object _arg;

        /// <summary>
        /// Constructor.
        /// </summary>
        /// <param name="clo">Closure.</param>
        /// <param name="arg">Argument.</param>
        public ComputeFuncJob(IComputeFunc clo, object arg)
        {
            _clo = clo;
            _arg = arg;
        }

        /** <inheritDoc /> */
        public object Execute()
        {
            return _clo.Invoke(_arg);
        }

        /** <inheritDoc /> */
        public void Cancel()
        {
            throw new NotSupportedException("Func job cannot be cancelled.");
        }

        /** <inheritDoc /> */
        public void Inject(IIgniteInternal grid)
        {
            ResourceProcessor.Inject(_clo, grid);
        }

        /** <inheritDoc /> */
        public void WriteBinary(IBinaryWriter writer)
        {
            BinaryWriter writer0 = (BinaryWriter) writer.GetRawWriter();

            writer0.WriteWithPeerDeployment(_clo);
            writer0.WriteWithPeerDeployment(_arg);
        }

        /// <summary>
        /// Initializes a new instance of the <see cref="ComputeFuncJob"/> class.
        /// </summary>
        /// <param name="reader">The reader.</param>
        public ComputeFuncJob(IBinaryRawReader reader)
        {
            _clo = reader.ReadObject<IComputeFunc>();
            _arg = reader.ReadObject<object>();
        }
    }
}
