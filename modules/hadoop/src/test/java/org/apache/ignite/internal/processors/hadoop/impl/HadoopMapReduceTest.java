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

package org.apache.ignite.internal.processors.hadoop.impl;

import org.apache.ignite.igfs.IgfsPath;
import org.apache.ignite.internal.processors.hadoop.impl.examples.HadoopWordCount2;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Test of whole cycle of map-reduce processing via Job tracker.
 */
@RunWith(JUnit4.class)
public class HadoopMapReduceTest extends HadoopAbstractMapReduceTest {
    /**
     * Tests whole job execution with all phases in all combination of new and old versions of API.
     * @throws Exception If fails.
     */
    @Test
    public void testWholeMapReduceExecution() throws Exception {
        IgfsPath inDir = new IgfsPath(PATH_INPUT);

        igfs.mkdirs(inDir);

        IgfsPath inFile = new IgfsPath(inDir, HadoopWordCount2.class.getSimpleName() + "-input");

        generateTestFile(inFile.toString(), "red", red, "blue", blue, "green", green, "yellow", yellow );

        for (boolean[] apiMode: getApiModes()) {
            assert apiMode.length == 3;

            boolean useNewMapper = apiMode[0];
            boolean useNewCombiner = apiMode[1];
            boolean useNewReducer = apiMode[2];

            doTest(inFile, useNewMapper, useNewCombiner, useNewReducer);
        }
    }

    /**
     * Gets API mode combinations to be tested.
     * Each boolean[] is { newMapper, newCombiner, newReducer } flag triplet.
     *
     * @return Arrays of booleans indicating API combinations to test.
     */
    protected boolean[][] getApiModes() {
        return new boolean[][] {
            { false, false, false },
            { false, false, true },
            { false, true,  false },
            { true,  false, false },
            { true,  true,  true },
        };
    }
}
