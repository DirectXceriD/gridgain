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

package org.apache.ignite.ml.preprocessing.encoding;

import java.util.HashMap;
import java.util.Map;
import org.apache.ignite.ml.common.TrainerTest;
import org.apache.ignite.ml.dataset.DatasetBuilder;
import org.apache.ignite.ml.dataset.impl.local.LocalDatasetBuilder;
import org.apache.ignite.ml.math.exceptions.preprocessing.UnknownCategorialFeatureValue;
import org.junit.Test;

import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertArrayEquals;

/**
 * Tests for {@link EncoderTrainer}.
 */
public class EncoderTrainerTest extends TrainerTest {
    /** Tests {@code fit()} method. */
    @Test
    public void testFitOnStringCategorialFeatures() {
        Map<Integer, String[]> data = new HashMap<>();
        data.put(1, new String[]{"Monday", "September"});
        data.put(2, new String[]{"Monday", "August"});
        data.put(3, new String[]{"Monday", "August"});
        data.put(4, new String[]{"Friday", "June"});
        data.put(5, new String[]{"Friday", "June"});
        data.put(6, new String[]{"Sunday", "August"});

        DatasetBuilder<Integer, String[]> datasetBuilder = new LocalDatasetBuilder<>(data, parts);

        EncoderTrainer<Integer, String[]> strEncoderTrainer = new EncoderTrainer<Integer, String[]>()
            .withEncoderType(EncoderType.STRING_ENCODER)
            .withEncodedFeature(0)
            .withEncodedFeature(1);

        EncoderPreprocessor<Integer, String[]> preprocessor = strEncoderTrainer.fit(
            datasetBuilder,
            (k, v) -> v
        );

        assertArrayEquals(new double[]{0.0, 2.0}, preprocessor.apply(7, new String[]{"Monday", "September"}).asArray(), 1e-8);
    }

    /** Tests {@code fit()} method. */
    @Test
    public void testFitOnIntegerCategorialFeatures() {
        Map<Integer, Object[]> data = new HashMap<>();
        data.put(1, new Object[]{3.0, 0.0});
        data.put(2, new Object[]{3.0, 12.0});
        data.put(3, new Object[]{3.0, 12.0});
        data.put(4, new Object[]{2.0, 45.0});
        data.put(5, new Object[]{2.0, 45.0});
        data.put(6, new Object[]{14.0, 12.0});

        DatasetBuilder<Integer, Object[]> datasetBuilder = new LocalDatasetBuilder<>(data, parts);

        EncoderTrainer<Integer, Object[]> strEncoderTrainer = new EncoderTrainer<Integer, Object[]>()
            .withEncoderType(EncoderType.ONE_HOT_ENCODER)
            .withEncodedFeature(0)
            .withEncodedFeature(1);

        EncoderPreprocessor<Integer, Object[]> preprocessor = strEncoderTrainer.fit(
            datasetBuilder,
            (k, v) -> v
        );
        assertArrayEquals(new double[]{0.0, 2.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0}, preprocessor.apply(7, new Double[]{3.0, 0.0}).asArray(), 1e-8);
        assertArrayEquals(new double[]{1.0, 0.0, 0.0, 1.0, 0.0, 1.0, 0.0, 0.0}, preprocessor.apply(8, new Double[]{2.0, 12.0}).asArray(), 1e-8);
    }

    /** Tests {@code fit()} method. */
    @Test
    public void testFitWithUnknownStringValueInTheGivenData() {
        Map<Integer, Object[]> data = new HashMap<>();
        data.put(1, new Object[]{3.0, 0.0});
        data.put(2, new Object[]{3.0, 12.0});
        data.put(3, new Object[]{3.0, 12.0});
        data.put(4, new Object[]{2.0, 45.0});
        data.put(5, new Object[]{2.0, 45.0});
        data.put(6, new Object[]{14.0, 12.0});

        DatasetBuilder<Integer, Object[]> datasetBuilder = new LocalDatasetBuilder<>(data, parts);

        EncoderTrainer<Integer, Object[]> strEncoderTrainer = new EncoderTrainer<Integer, Object[]>()
            .withEncoderType(EncoderType.STRING_ENCODER)
            .withEncodedFeature(0)
            .withEncodedFeature(1);

        EncoderPreprocessor<Integer, Object[]> preprocessor = strEncoderTrainer.fit(
            datasetBuilder,
            (k, v) -> v
        );

        try {
            preprocessor.apply(7, new String[]{"Monday", "September"}).asArray();
            fail("UnknownCategorialFeatureValue");
        } catch (UnknownCategorialFeatureValue e) {
            return;
        }
        fail("UnknownCategorialFeatureValue");
    }
}
