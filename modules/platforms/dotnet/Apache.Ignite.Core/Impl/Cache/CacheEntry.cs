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

namespace Apache.Ignite.Core.Impl.Cache
{
    using System;
    using System.Collections.Generic;
    using System.Globalization;
    using Apache.Ignite.Core.Cache;

    /// <summary>
    /// Represents a cache entry.
    /// </summary>
    public struct CacheEntry<TK, TV> : ICacheEntry<TK, TV>, IEquatable<CacheEntry<TK, TV>>
    {
        /** Key. */
        private readonly TK _key;

        /** Value. */
        private readonly TV _val;

        /// <summary>
        /// Initializes a new instance of the <see cref="CacheEntry{K,V}"/> struct.
        /// </summary>
        /// <param name="key">The key.</param>
        /// <param name="val">The value.</param>
        public CacheEntry(TK key, TV val)
        {
            _key = key;
            _val = val;
        }

        /// <summary>
        /// Gets the key.
        /// </summary>
        public TK Key
        {
            get { return _key; }
        }

        /// <summary>
        /// Gets the value.
        /// </summary>
        public TV Value
        {
            get { return _val; }
        }

        /// <summary>
        /// Determines whether the specified <see cref="CacheEntry{K,V}"/>, is equal to this instance.
        /// </summary>
        /// <param name="other">The <see cref="CacheEntry{K,V}"/> to compare with this instance.</param>
        /// <returns>
        ///   <c>true</c> if the specified <see cref="CacheEntry{K,V}"/> is equal to this instance; 
        ///   otherwise, <c>false</c>.
        /// </returns>
        public bool Equals(CacheEntry<TK, TV> other)
        {
            return EqualityComparer<TK>.Default.Equals(_key, other._key) &&
                EqualityComparer<TV>.Default.Equals(_val, other._val);
        }

        /// <summary>
        /// Determines whether the specified <see cref="object" /> is equal to this instance.
        /// </summary>
        /// <param name="obj">The <see cref="object" /> to compare with this instance.</param>
        /// <returns>
        ///   <c>true</c> if the specified <see cref="object" /> is equal to this instance;
        /// otherwise, <c>false</c>.
        /// </returns>
        public override bool Equals(object obj)
        {
            if (ReferenceEquals(null, obj)) 
                return false;

            return obj is CacheEntry<TK, TV> && Equals((CacheEntry<TK, TV>) obj);
        }

        /// <summary>
        /// Returns a hash code for this instance.
        /// </summary>
        /// <returns>
        /// A hash code for this instance, suitable for use in hashing algorithms
        /// and data structures like a hash table. 
        /// </returns>
        public override int GetHashCode()
        {
            unchecked
            {
                return (EqualityComparer<TK>.Default.GetHashCode(_key) * 397) ^
                    EqualityComparer<TV>.Default.GetHashCode(_val);
            }
        }

        /// <summary>
        /// Returns a <see cref="string" /> that represents this instance.
        /// </summary>
        /// <returns>
        /// A <see cref="string" /> that represents this instance.
        /// </returns>
        public override string ToString()
        {
            return string.Format(CultureInfo.CurrentCulture, "CacheEntry [Key={0}, Value={1}]", _key, _val);
        }

        /// <summary>
        /// Implements the operator ==.
        /// </summary>
        /// <param name="a">First item.</param>
        /// <param name="b">Second item.</param>
        /// <returns>
        /// The result of the operator.
        /// </returns>
        public static bool operator ==(CacheEntry<TK, TV> a, CacheEntry<TK, TV> b)
        {
            return a.Equals(b);
        }

        /// <summary>
        /// Implements the operator !=.
        /// </summary>
        /// <param name="a">First item.</param>
        /// <param name="b">Second item.</param>
        /// <returns>
        /// The result of the operator.
        /// </returns>
        public static bool operator !=(CacheEntry<TK, TV> a, CacheEntry<TK, TV> b)
        {
            return !(a == b);
        }
    }
}