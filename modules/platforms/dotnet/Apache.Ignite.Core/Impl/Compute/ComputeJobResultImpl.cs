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

namespace Apache.Ignite.Core.Impl.Compute
{
    using System;
    using Apache.Ignite.Core.Compute;

    /// <summary>
    /// Job result implementation.
    /// </summary>
    internal class ComputeJobResultImpl : IComputeJobResult<object>
    {
        /** Data. */
        private readonly object _data;

        /** Exception. */
        private readonly Exception _err;

        /** Backing job. */
        private readonly IComputeJob _job;

        /** Node ID. */
        private readonly Guid _nodeId;

        /** Cancel flag. */
        private readonly bool _cancelled;

        /// <summary>
        /// Constructor.
        /// </summary>
        /// <param name="data">Data.</param>
        /// <param name="err">Exception.</param>
        /// <param name="job">Backing job.</param>
        /// <param name="nodeId">Node ID.</param>
        /// <param name="cancelled">Cancel flag.</param>
        public ComputeJobResultImpl(object data, Exception err, IComputeJob job, Guid nodeId, bool cancelled)
        {
            _data = data;
            _err = err;
            _job = job;
            _nodeId = nodeId;
            _cancelled = cancelled;
        }

        /** <inheritDoc /> */

        public object Data
        {
            get { return _data; }
        }

        /** <inheritDoc /> */

        public Exception Exception
        {
            get { return _err; }
        }

        /** <inheritDoc /> */

        public IComputeJob<object> Job
        {
            get { return _job; }
        }

        /** <inheritDoc /> */
        public Guid NodeId
        {
            get
            {
                return _nodeId;
            }
        }

        /** <inheritDoc /> */
        public bool Cancelled
        {
            get 
            { 
                return _cancelled; 
            }
        }
    }
}
