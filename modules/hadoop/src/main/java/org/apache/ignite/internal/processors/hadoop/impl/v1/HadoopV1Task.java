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

package org.apache.ignite.internal.processors.hadoop.impl.v1;

import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.TaskAttemptID;
import org.apache.ignite.internal.processors.hadoop.HadoopTask;
import org.apache.ignite.internal.processors.hadoop.HadoopTaskCancelledException;
import org.apache.ignite.internal.processors.hadoop.HadoopTaskInfo;
import org.apache.ignite.internal.processors.hadoop.impl.v2.HadoopV2TaskContext;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.text.NumberFormat;

/**
 * Extended Hadoop v1 task.
 */
public abstract class HadoopV1Task extends HadoopTask {
    /** Indicates that this task is to be cancelled. */
    private volatile boolean cancelled;

    /**
     * Constructor.
     *
     * @param taskInfo Task info.
     */
    protected HadoopV1Task(HadoopTaskInfo taskInfo) {
        super(taskInfo);
    }

    /**
     * Gets file name for that task result.
     *
     * @return File name.
     */
    public String fileName() {
        NumberFormat numFormat = NumberFormat.getInstance();

        numFormat.setMinimumIntegerDigits(5);
        numFormat.setGroupingUsed(false);

        return "part-" + numFormat.format(info().taskNumber());
    }

    /**
     *
     * @param jobConf Job configuration.
     * @param taskCtx Task context.
     * @param directWrite Direct write flag.
     * @param fileName File name.
     * @param attempt Attempt of task.
     * @return Collector.
     * @throws IOException In case of IO exception.
     */
    protected HadoopV1OutputCollector collector(JobConf jobConf, HadoopV2TaskContext taskCtx,
        boolean directWrite, @Nullable String fileName, TaskAttemptID attempt) throws IOException {
        HadoopV1OutputCollector collector = new HadoopV1OutputCollector(jobConf, taskCtx, directWrite,
            fileName, attempt) {
            /** {@inheritDoc} */
            @Override public void collect(Object key, Object val) throws IOException {
                if (cancelled)
                    throw new HadoopTaskCancelledException("Task cancelled.");

                super.collect(key, val);
            }
        };

        collector.setup();

        return collector;
    }

    /** {@inheritDoc} */
    @Override public void cancel() {
        cancelled = true;
    }

    /** Returns true if task is cancelled. */
    public boolean isCancelled() {
        return cancelled;
    }
}