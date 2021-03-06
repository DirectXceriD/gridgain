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

namespace Apache.Ignite.Core.Messaging
{
    using System;
    using System.Collections;
    using System.Threading.Tasks;
    using Apache.Ignite.Core.Cluster;

    /// <summary>
    /// Provides functionality for topic-based message exchange among nodes defined by <see cref="IClusterGroup"/>.
    /// Users can send ordered and unordered messages to various topics. Note that same topic name
    /// cannot be reused between ordered and unordered messages.
    /// <para/>
    /// All members are thread-safe and may be used concurrently from multiple threads.
    /// </summary>
    public interface IMessaging
    {
        /// <summary>
        /// Gets the cluster group to which this instance belongs.
        /// </summary>
        IClusterGroup ClusterGroup { get; }

        /// <summary>
        /// Sends a message with specified topic to the nodes in the underlying cluster group.
        /// </summary>
        /// <param name="message">Message to send.</param>
        /// <param name="topic">Topic to send to, null for default topic.</param>
        void Send(object message, object topic = null);

        /// <summary>
        /// Sends messages with specified topic to the nodes in the underlying cluster group.
        /// </summary>
        /// <param name="messages">Messages to send.</param>
        /// <param name="topic">Topic to send to, null for default topic.</param>
        void SendAll(IEnumerable messages, object topic = null);

        /// <summary>
        /// Sends a message with specified topic to the nodes in the underlying cluster group.
        /// Messages sent with this method will arrive in the same order they were sent. Note that if a topic is used
        /// for ordered messages, then it cannot be reused for non-ordered messages.
        /// </summary>
        /// <param name="message">Message to send.</param>
        /// <param name="topic">Topic to send to, null for default topic.</param>
        /// <param name="timeout">
        /// Message timeout, null for for default value from configuration (IgniteConfiguration.getNetworkTimeout).
        /// </param>
        void SendOrdered(object message, object topic = null, TimeSpan? timeout = null);

        /// <summary>
        /// Adds local listener for given topic on local node only. This listener will be notified whenever any
        /// node within the cluster group will send a message for a given topic to this node. Local listen
        /// subscription will happen regardless of whether local node belongs to this cluster group or not.
        /// </summary>
        /// <param name="listener">
        /// Predicate that is called on each received message. If predicate returns false,
        /// then it will be unsubscribed from any further notifications.
        /// </param>
        /// <param name="topic">Topic to subscribe to.</param>
        void LocalListen<T>(IMessageListener<T> listener, object topic = null);

        /// <summary>
        /// Unregisters local listener for given topic on local node only.
        /// </summary>
        /// <param name="listener">Listener predicate.</param>
        /// <param name="topic">Topic to unsubscribe from.</param>
        void StopLocalListen<T>(IMessageListener<T> listener, object topic = null);

        /// <summary>
        /// Adds a message listener for a given topic to all nodes in the cluster group (possibly including
        /// this node if it belongs to the cluster group as well). This means that any node within this cluster
        /// group can send a message for a given topic and all nodes within the cluster group will receive
        /// listener notifications.
        /// </summary>
        /// <param name="listener">
        /// Predicate that is called on each received message. If predicate returns false,
        /// then it will be unsubscribed from any further notifications.
        /// </param>
        /// <param name="topic">Topic to unsubscribe from.</param>
        /// <returns>
        /// Operation ID that can be passed to <see cref="StopRemoteListen"/> method to stop listening.
        /// </returns>
        Guid RemoteListen<T>(IMessageListener<T> listener, object topic = null);

        /// <summary>
        /// Adds a message listener for a given topic to all nodes in the cluster group (possibly including
        /// this node if it belongs to the cluster group as well). This means that any node within this cluster
        /// group can send a message for a given topic and all nodes within the cluster group will receive
        /// listener notifications.
        /// </summary>
        /// <param name="listener">
        /// Predicate that is called on each received message. If predicate returns false,
        /// then it will be unsubscribed from any further notifications.
        /// </param>
        /// <param name="topic">Topic to unsubscribe from.</param>
        /// <returns>
        /// Operation ID that can be passed to <see cref="StopRemoteListen"/> method to stop listening.
        /// </returns>
        Task<Guid> RemoteListenAsync<T>(IMessageListener<T> listener, object topic = null);

        /// <summary>
        /// Unregisters all listeners identified with provided operation ID on all nodes in the cluster group.
        /// </summary>
        /// <param name="opId">Operation ID that was returned from <see cref="RemoteListen{T}"/> method.</param>
        void StopRemoteListen(Guid opId);

        /// <summary>
        /// Unregisters all listeners identified with provided operation ID on all nodes in the cluster group.
        /// </summary>
        /// <param name="opId">Operation ID that was returned from <see cref="RemoteListen{T}"/> method.</param>
        Task StopRemoteListenAsync(Guid opId);
    }
}