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

namespace Apache.Ignite.Core.Tests
{
    using System;
    using System.Collections.Generic;
    using System.Diagnostics;
    using System.Linq;
    using System.Reflection;
    using Apache.Ignite.Core.Tests.Binary.Serializable;
    using Apache.Ignite.Core.Tests.Cache;
    using Apache.Ignite.Core.Tests.Cache.Query.Linq;
    using Apache.Ignite.Core.Tests.Client.Cache;
    using Apache.Ignite.Core.Tests.Compute;
    using Apache.Ignite.Core.Tests.Memory;
    using NUnit.ConsoleRunner;

    /// <summary>
    /// Console test runner.
    /// </summary>
    public static class TestRunner
    {
        [STAThread]
        static void Main(string[] args)
        {
            Debug.Listeners.Add(new TextWriterTraceListener(Console.Out));
            Debug.AutoFlush = true;

            if (args.Length == 1 && args[0] == "-basicTests")
            {
                RunBasicTests();
                
                return;
            }

            if (args.Length == 2)
            {
                //Debugger.Launch();
                var testClass = Type.GetType(args[0]);
                var method = args[1];

                if (testClass == null || testClass.GetMethods().All(x => x.Name != method))
                    throw new InvalidOperationException("Failed to find method: " + testClass + "." + method);

                Environment.ExitCode = TestOne(testClass, method);
                return;
            }

            Environment.ExitCode = TestAllInAssembly();
        }

        /// <summary>
        /// Runs some basic tests.
        /// </summary>
        private static void RunBasicTests()
        {
            Console.WriteLine(">>> Starting basic tests...");

            var basicTests = new[]
            {
                typeof(ComputeApiTest),
                typeof(CacheLinqTest),
                typeof(SqlDmlTest),
                typeof(LinqTest),
                typeof(PersistenceTest)
            };

            Environment.ExitCode = TestAll(basicTests, true);

            Console.WriteLine(">>> Test run finished.");
        }

        /// <summary>
        /// Runs specified test method.
        /// </summary>
        private static int TestOne(Type testClass, string method, bool sameDomain = false)
        {
            string[] args =
            {
                "-noshadow",
                "-domain:" + (sameDomain ? "None" : "Single"),
                "-run:" + testClass.FullName + "." + method,
                Assembly.GetAssembly(testClass).Location
            };

            return Runner.Main(args);
        }

        /// <summary>
        /// Runs all tests in specified class.
        /// </summary>
        private static int TestAll(IEnumerable<Type> testClass, bool sameDomain = false)
        {
            var args = new List<string>
            {
                "-noshadow",
                "-domain:" + (sameDomain ? "None" : "Single"),
                "-run:" + string.Join(",", testClass.Select(x => x.FullName)),
                Assembly.GetAssembly(typeof(TestRunner)).Location
            };
            
            return Runner.Main(args.ToArray());
        }

        /// <summary>
        /// Runs all tests in assembly.
        /// </summary>
        private static int TestAllInAssembly(bool sameDomain = false)
        {
            string[] args =
            {
                "-noshadow",
                "-domain:" + (sameDomain ? "None" : "Single"),
                Assembly.GetAssembly(typeof(InteropMemoryTest)).Location
            };

            return Runner.Main(args);
        }
    }
}