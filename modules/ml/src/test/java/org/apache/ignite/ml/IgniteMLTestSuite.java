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

package org.apache.ignite.ml;

import junit.framework.JUnit4TestAdapter;
import junit.framework.TestSuite;
import org.apache.ignite.ml.clustering.ClusteringTestSuite;
import org.apache.ignite.ml.common.CommonTestSuite;
import org.apache.ignite.ml.composition.CompositionTestSuite;
import org.apache.ignite.ml.dataset.DatasetTestSuite;
import org.apache.ignite.ml.environment.EnvironmentTestSuite;
import org.apache.ignite.ml.genetic.GAGridTestSuite;
import org.apache.ignite.ml.knn.KNNTestSuite;
import org.apache.ignite.ml.math.MathImplMainTestSuite;
import org.apache.ignite.ml.nn.MLPTestSuite;
import org.apache.ignite.ml.pipeline.PipelineTestSuite;
import org.apache.ignite.ml.preprocessing.PreprocessingTestSuite;
import org.apache.ignite.ml.regressions.RegressionsTestSuite;
import org.apache.ignite.ml.selection.SelectionTestSuite;
import org.apache.ignite.ml.structures.StructuresTestSuite;
import org.apache.ignite.ml.svm.SVMTestSuite;
import org.apache.ignite.ml.tree.DecisionTreeTestSuite;
import org.junit.runner.RunWith;
import org.junit.runners.AllTests;

/**
 * Test suite for all module tests. IMPL NOTE tests in {@code org.apache.ignite.ml.tree.performance} are not
 * included here because these are intended only for manual execution.
 */
@RunWith(AllTests.class)
public class IgniteMLTestSuite {
    /** */
    public static TestSuite suite() {
        TestSuite suite = new TestSuite(IgniteMLTestSuite.class.getSimpleName());

        /** JUnit 4 tests. */
        suite.addTest(new JUnit4TestAdapter(MathImplMainTestSuite.class));
        suite.addTest(new JUnit4TestAdapter(RegressionsTestSuite.class));
        suite.addTest(new JUnit4TestAdapter(SVMTestSuite.class));
        suite.addTest(new JUnit4TestAdapter(ClusteringTestSuite.class));
        suite.addTest(new JUnit4TestAdapter(KNNTestSuite.class));
        suite.addTest(new JUnit4TestAdapter(PipelineTestSuite.class));
        suite.addTest(new JUnit4TestAdapter(PreprocessingTestSuite.class));
        suite.addTest(new JUnit4TestAdapter(GAGridTestSuite.class));
        suite.addTest(new JUnit4TestAdapter(CompositionTestSuite.class));
        suite.addTest(new JUnit4TestAdapter(EnvironmentTestSuite.class));
        suite.addTest(new JUnit4TestAdapter(StructuresTestSuite.class));
        suite.addTest(new JUnit4TestAdapter(CommonTestSuite.class));

        /** JUnit 3 tests. */
        suite.addTest(DecisionTreeTestSuite.suite());
        suite.addTest(MLPTestSuite.suite());
        suite.addTest(DatasetTestSuite.suite());
        suite.addTest(SelectionTestSuite.suite());

        return suite;
    }
}
