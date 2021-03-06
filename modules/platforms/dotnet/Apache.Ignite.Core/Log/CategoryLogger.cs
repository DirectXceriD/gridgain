﻿/*
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

namespace Apache.Ignite.Core.Log
{
    using System;
    using Apache.Ignite.Core.Impl.Common;

    /// <summary>
    /// Wrapping logger with a predefined category.
    /// <para />
    /// When <see cref="Log"/> method is called, and <c>category</c> parameter is null, predefined category 
    /// will be used.
    /// </summary>
    public class CategoryLogger : ILogger
    {
        /** Wrapped logger. */
        private readonly ILogger _logger;

        /** Category to use. */
        private readonly string _category;

        /// <summary>
        /// Initializes a new instance of the <see cref="CategoryLogger"/> class.
        /// </summary>
        /// <param name="logger">The logger to wrap.</param>
        /// <param name="category">The category.</param>
        public CategoryLogger(ILogger logger, string category)
        {
            IgniteArgumentCheck.NotNull(logger, "log");

            // If logger is already a CategoryLogger, get underlying logger instead to avoid unnecessary nesting.
            var catLogger = logger as CategoryLogger;
            _logger = catLogger != null ? catLogger._logger : logger;

            _category = category;
        }

        /// <summary>
        /// Logs the specified message.
        /// </summary>
        /// <param name="level">The level.</param>
        /// <param name="message">The message.</param>
        /// <param name="args">The arguments to format <paramref name="message" />.
        /// Can be null (formatting will not occur).</param>
        /// <param name="formatProvider">The format provider. Can be null if <paramref name="args" /> is null.</param>
        /// <param name="category">The logging category name.</param>
        /// <param name="nativeErrorInfo">The native error information.</param>
        /// <param name="ex">The exception. Can be null.</param>
        public void Log(LogLevel level, string message, object[] args, IFormatProvider formatProvider, string category,
            string nativeErrorInfo, Exception ex)
        {
            _logger.Log(level, message, args, formatProvider, category ?? _category, nativeErrorInfo, ex);
        }

        /// <summary>
        /// Determines whether the specified log level is enabled.
        /// </summary>
        /// <param name="level">The level.</param>
        /// <returns>
        /// Value indicating whether the specified log level is enabled
        /// </returns>
        public bool IsEnabled(LogLevel level)
        {
            return _logger.IsEnabled(level);
        }
    }
}
