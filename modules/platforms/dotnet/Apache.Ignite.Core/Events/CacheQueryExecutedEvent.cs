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

namespace Apache.Ignite.Core.Events
{
    using System;
    using System.Globalization;
    using Apache.Ignite.Core.Binary;

    /// <summary>
    /// Cache query execution event.
    /// </summary>
    public sealed class CacheQueryExecutedEvent : EventBase
	{
        /** */
        private readonly string _queryType;

        /** */
        private readonly string _cacheName;

        /** */
        private readonly string _className;

        /** */
        private readonly string _clause;

        /** */
        private readonly Guid? _subjectId;

        /** */
        private readonly string _taskName;

        /// <summary>
        /// Constructor.
        /// </summary>
        /// <param name="r">The reader to read data from.</param>
        internal CacheQueryExecutedEvent(IBinaryRawReader r) : base(r)
        {
            _queryType = r.ReadString();
            _cacheName = r.ReadString();
            _className = r.ReadString();
            _clause = r.ReadString();
            _subjectId = r.ReadGuid();
            _taskName = r.ReadString();
        }
		
        /// <summary>
        /// Gets query type. 
        /// </summary>
        public string QueryType { get { return _queryType; } }

        /// <summary>
        /// Gets cache name on which query was executed. 
        /// </summary>
        public string CacheName { get { return _cacheName; } }

        /// <summary>
        /// Gets queried class name. Applicable for SQL and full text queries. 
        /// </summary>
        public string ClassName { get { return _className; } }

        /// <summary>
        /// Gets query clause. Applicable for SQL, SQL fields and full text queries. 
        /// </summary>
        public string Clause { get { return _clause; } }

        /// <summary>
        /// Gets security subject ID. 
        /// </summary>
        public Guid? SubjectId { get { return _subjectId; } }

        /// <summary>
        /// Gets the name of the task that executed the query (if any). 
        /// </summary>
        public string TaskName { get { return _taskName; } }

        /// <summary>
        /// Gets shortened version of ToString result.
        /// </summary>
	    public override string ToShortString()
	    {
	        return string.Format(CultureInfo.InvariantCulture,
	            "{0}: QueryType={1}, CacheName={2}, ClassName={3}, Clause={4}, SubjectId={5}, " +
	            "TaskName={6}", Name, QueryType, CacheName, ClassName, Clause, SubjectId, TaskName);
	    }
    }
}
