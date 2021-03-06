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

namespace Apache.Ignite.Core.Tests.Compute
{
    using System;
    using System.Collections.Generic;
    using System.Linq;
    using System.Threading;
    using Apache.Ignite.Core.Cluster;
    using Apache.Ignite.Core.Compute;
    using NUnit.Framework;

    /// <summary>
    /// Cancellation tests.
    /// </summary>
    public class CancellationTest : SpringTestBase
    {
        public CancellationTest() 
            : base("config\\compute\\compute-grid1.xml", "config\\compute\\compute-grid2.xml")
        {
            // No-op.
        }

        [Test]
        public void TestTask()
        {
            TestTask((c, t) => c.ExecuteAsync(new Task(), t));
            TestTask((c, t) => c.ExecuteAsync(new Task(), 1, t));
            TestTask((c, t) => c.ExecuteAsync<int, IList<IComputeJobResult<int>>>(typeof(Task), t));
            TestTask((c, t) => c.ExecuteAsync<object, int, IList<IComputeJobResult<int>>>(typeof(Task), 1, t));
        }

        [Test]
        public void TestJavaTask()
        {
            using (var cts = new CancellationTokenSource())
            {
                var task = Compute.ExecuteJavaTaskAsync<object>(ComputeApiTest.BroadcastTask, null, cts.Token);

                Assert.IsFalse(task.IsCanceled);

                cts.Cancel();

                Assert.IsTrue(task.IsCanceled);

                // Pass cancelled token
                Assert.IsTrue(
                    Compute.ExecuteJavaTaskAsync<object>(ComputeApiTest.BroadcastTask, null, cts.Token).IsCanceled);
            }
        }

        [Test]
        public void TestClosures()
        {
            TestClosure((c, t) => c.BroadcastAsync(new ComputeAction(), t));
            TestClosure((c, t) => c.BroadcastAsync(new ComputeFunc(), t));
            TestClosure((c, t) => c.BroadcastAsync(new ComputeBiFunc(), 10, t));

            TestClosure((c, t) => c.AffinityRunAsync("default", 0, new ComputeAction(), t));

            TestClosure((c, t) => c.RunAsync(new ComputeAction(), t));
            TestClosure((c, t) => c.RunAsync(Enumerable.Range(1, 10).Select(x => new ComputeAction()), t));

            TestClosure((c, t) => c.CallAsync(new ComputeFunc(), t));
            TestClosure((c, t) => c.CallAsync(Enumerable.Range(1, 10).Select(x => new ComputeFunc()), t));
            TestClosure((c, t) => c.CallAsync(Enumerable.Range(1, 10).Select(x => new ComputeFunc()), 
                new ComputeReducer(), t));

            TestClosure((c, t) => c.AffinityCallAsync("default", 0, new ComputeFunc(), t));

            TestClosure((c, t) => c.ApplyAsync(new ComputeBiFunc(), 10, t));
            TestClosure((c, t) => c.ApplyAsync(new ComputeBiFunc(), Enumerable.Range(1, 100), t));
            TestClosure((c, t) => c.ApplyAsync(new ComputeBiFunc(), Enumerable.Range(1, 100), new ComputeReducer(), t));
        }

        private void TestTask(Func<ICompute, CancellationToken, System.Threading.Tasks.Task> runner)
        {
            Job.CancelCount = 0;

            TestClosure(runner);

            Assert.IsTrue(TestUtils.WaitForCondition(() => Job.CancelCount > 0, 5000));
        }

        private void TestClosure(Func<ICompute, CancellationToken, System.Threading.Tasks.Task> runner)
        {
            using (var cts = new CancellationTokenSource())
            {
                var task = runner(Compute, cts.Token);

                Assert.IsFalse(task.IsCanceled);

                cts.Cancel();

                Assert.IsTrue(task.IsCanceled);

                // Pass cancelled token
                Assert.IsTrue(runner(Compute, cts.Token).IsCanceled);
            }
        }

        private class Task : IComputeTask<int, IList<IComputeJobResult<int>>>
        {
            public IDictionary<IComputeJob<int>, IClusterNode> Map(IList<IClusterNode> subgrid, object arg)
            {
                return Enumerable.Range(1, 100)
                    .SelectMany(x => subgrid)
                    .ToDictionary(x => (IComputeJob<int>)new Job(), x => x);
            }

            public ComputeJobResultPolicy OnResult(IComputeJobResult<int> res, IList<IComputeJobResult<int>> rcvd)
            {
                return ComputeJobResultPolicy.Wait;
            }

            public IList<IComputeJobResult<int>> Reduce(IList<IComputeJobResult<int>> results)
            {
                Assert.Fail("Reduce should not be called on a cancelled task.");
                return results;
            }
        }

        [Serializable]
        private class Job : IComputeJob<int>
        {
            private static int _cancelCount;

            public static int CancelCount
            {
                get { return Thread.VolatileRead(ref _cancelCount); }
                set { Thread.VolatileWrite(ref _cancelCount, value); }
            }

            public int Execute()
            {
                Thread.Sleep(50);
                return 1;
            }

            public void Cancel()
            {
                Interlocked.Increment(ref _cancelCount);
            }
        }

        [Serializable]
        private class ComputeBiFunc : IComputeFunc<int, int>
        {
            public int Invoke(int arg)
            {
                Thread.Sleep(50);
                return arg;
            }
        }

        private class ComputeReducer : IComputeReducer<int, int>
        {
            public bool Collect(int res)
            {
                return true;
            }

            public int Reduce()
            {
                return 0;
            }
        }
    }
}
