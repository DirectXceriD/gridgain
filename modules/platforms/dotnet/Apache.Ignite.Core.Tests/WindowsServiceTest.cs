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
    using System.Diagnostics;
    using System.IO;
    using System.Linq;
    using System.ServiceProcess;
    using Apache.Ignite.Core.Cluster;
    using Apache.Ignite.Core.Impl.Unmanaged.Jni;
    using Apache.Ignite.Core.Log;
    using Apache.Ignite.Core.Tests.Process;
    using NUnit.Framework;

    /// <summary>
    /// Tests windows service deployment and lifecycle.
    /// <para />
    /// NOTE: This fixture requires administrative privileges.
    /// </summary>
    public class WindowsServiceTest
    {
        /// <summary>
        /// Test fixture set up.
        /// </summary>
        [TestFixtureSetUp]
        public void TestFixtureSetUp()
        {
            StopServiceAndUninstall();
        }

        /// <summary>
        /// Test fixture tear down.
        /// </summary>
        [TestFixtureTearDown]
        public void TestFixtureTearDown()
        {
            StopServiceAndUninstall();
        }

        /// <summary>
        /// Tests that service stops when Ignition stops.
        /// </summary>
        [Test]
        public void TestStopFromJava()
        {
            var startTime = DateTime.Now.AddSeconds(-1);

            var exePath = typeof(IgniteRunner).Assembly.Location;
            var springPath = Path.GetFullPath(@"config\compute\compute-grid1.xml");

            JvmDll.Load(null, new NoopLogger());
            var jvmDll = System.Diagnostics.Process.GetCurrentProcess().Modules
                .OfType<ProcessModule>()
                .Single(x => JvmDll.FileJvmDll.Equals(x.ModuleName, StringComparison.OrdinalIgnoreCase));

            IgniteProcess.Start(exePath, string.Empty, args: new[]
            {
                "/install",
                "ForceTestClasspath=true",
                "-springConfigUrl=" + springPath,
                "-jvmDll=" + jvmDll.FileName
            }).WaitForExit();

            var service = GetIgniteService();
            Assert.IsNotNull(service);

            service.Start();  // see IGNITE_HOME\work\log for service instance logs
            WaitForStatus(service, startTime, ServiceControllerStatus.Running);

            using (var ignite = Ignition.Start(new IgniteConfiguration(TestUtils.GetTestConfiguration())
            {
                SpringConfigUrl = springPath
            }))
            {
                Assert.IsTrue(ignite.WaitTopology(2), "Failed to join with service node");

                // Stop remote node via Java task
                // Doing so will fail the task execution
                Assert.Throws<ClusterGroupEmptyException>(() =>
                    ignite.GetCluster().ForRemotes().GetCompute().ExecuteJavaTask<object>(
                        "org.apache.ignite.platform.PlatformStopIgniteTask", ignite.Name));

                Assert.IsTrue(ignite.WaitTopology(1), "Failed to stop remote node");

                // Check that service has stopped.
                WaitForStatus(service, startTime, ServiceControllerStatus.Stopped);
            }
        }

        /// <summary>
        /// Waits for service status.
        /// </summary>
        private static void WaitForStatus(ServiceController service, DateTime startTime,
            ServiceControllerStatus status)
        {
            try
            {
                service.WaitForStatus(status, TimeSpan.FromSeconds(30));
            }
            catch (Exception ex)
            {
                // Check Windows log for more details.
                var log = EventLog.GetEventLogs().Single(x => x.Log == "Application");
                var entries = log.Entries;
                var recentEntries = Enumerable.Range(0, entries.Count)
                    .Select(x => entries[x])
                    .Where(x => x.TimeGenerated >= startTime)
                    .Select(x => x.Message);
                var msg = string.Join("\n===\n", recentEntries);

                throw new Exception("Failed to start service. Event entries:\n" + msg, ex);
            }
        }

        /// <summary>
        /// Stops the service and uninstalls it.
        /// </summary>
        private static void StopServiceAndUninstall()
        {
            var controller = GetIgniteService();

            if (controller != null)
            {
                if (controller.CanStop)
                    controller.Stop();

                controller.WaitForStatus(ServiceControllerStatus.Stopped, TimeSpan.FromSeconds(30));

                var exePath = typeof(IgniteRunner).Assembly.Location;
                IgniteProcess.Start(exePath, string.Empty, args: new[] {"/uninstall"}).WaitForExit();
            }
        }

        /// <summary>
        /// Gets the ignite service.
        /// </summary>
        private static ServiceController GetIgniteService()
        {
            return ServiceController.GetServices().FirstOrDefault(x => x.ServiceName.StartsWith("Apache Ignite.NET"));
        }

        private class NoopLogger : ILogger
        {
            public void Log(LogLevel level, string message, object[] args, IFormatProvider formatProvider, string category,
                string nativeErrorInfo, Exception ex)
            {
                // No-op.
            }

            public bool IsEnabled(LogLevel level)
            {
                return false;
            }
        }
    }
}
