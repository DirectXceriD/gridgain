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

package org.apache.ignite.ml.regressions.logistic.binomial;

import java.io.Serializable;
import java.util.Arrays;
import org.apache.ignite.ml.dataset.Dataset;
import org.apache.ignite.ml.dataset.DatasetBuilder;
import org.apache.ignite.ml.dataset.primitive.context.EmptyContext;
import org.apache.ignite.ml.dataset.primitive.data.SimpleLabeledDatasetData;
import org.apache.ignite.ml.math.functions.IgniteBiFunction;
import org.apache.ignite.ml.math.functions.IgniteFunction;
import org.apache.ignite.ml.math.primitives.vector.Vector;
import org.apache.ignite.ml.math.primitives.vector.impl.DenseVector;
import org.apache.ignite.ml.nn.Activators;
import org.apache.ignite.ml.nn.MLPTrainer;
import org.apache.ignite.ml.nn.MultilayerPerceptron;
import org.apache.ignite.ml.nn.UpdatesStrategy;
import org.apache.ignite.ml.nn.architecture.MLPArchitecture;
import org.apache.ignite.ml.optimization.LossFunctions;
import org.apache.ignite.ml.optimization.updatecalculators.SimpleGDParameterUpdate;
import org.apache.ignite.ml.optimization.updatecalculators.SimpleGDUpdateCalculator;
import org.apache.ignite.ml.trainers.SingleLabelDatasetTrainer;
import org.jetbrains.annotations.NotNull;

/**
 * Trainer of the logistic regression model based on stochastic gradient descent algorithm.
 */
public class LogisticRegressionSGDTrainer<P extends Serializable> extends SingleLabelDatasetTrainer<LogisticRegressionModel> {
    /** Update strategy. */
    private UpdatesStrategy updatesStgy = new UpdatesStrategy<>(
        new SimpleGDUpdateCalculator(0.2),
        SimpleGDParameterUpdate::sumLocal,
        SimpleGDParameterUpdate::avg
    );

    /** Max number of iteration. */
    private int maxIterations = 100;

    /** Batch size. */
    private int batchSize = 100;

    /** Number of local iterations. */
    private int locIterations = 100;

    /** Seed for random generator. */
    private long seed = 1234L;

    /** {@inheritDoc} */
    @Override public <K, V> LogisticRegressionModel fit(DatasetBuilder<K, V> datasetBuilder,
        IgniteBiFunction<K, V, Vector> featureExtractor, IgniteBiFunction<K, V, Double> lbExtractor) {

        return updateModel(null, datasetBuilder, featureExtractor, lbExtractor);
    }

    /** {@inheritDoc} */
    @Override protected <K, V> LogisticRegressionModel updateModel(LogisticRegressionModel mdl,
        DatasetBuilder<K, V> datasetBuilder, IgniteBiFunction<K, V, Vector> featureExtractor,
        IgniteBiFunction<K, V, Double> lbExtractor) {

        IgniteFunction<Dataset<EmptyContext, SimpleLabeledDatasetData>, MLPArchitecture> archSupplier = dataset -> {
            int cols = dataset.compute(data -> {
                if (data.getFeatures() == null)
                    return null;
                return data.getFeatures().length / data.getRows();
            }, (a, b) -> {
                if (a == null)
                    return b == null ? 0 : b;
                if (b == null)
                    return a;
                return b;
            });

            MLPArchitecture architecture = new MLPArchitecture(cols);
            architecture = architecture.withAddedLayer(1, true, Activators.SIGMOID);

            return architecture;
        };

        MLPTrainer<?> trainer = new MLPTrainer<>(
            archSupplier,
            LossFunctions.L2,
            updatesStgy,
            maxIterations,
            batchSize,
            locIterations,
            seed
        );

        IgniteBiFunction<K, V, double[]> lbExtractorWrapper = (k, v) -> new double[] {lbExtractor.apply(k, v)};
        MultilayerPerceptron mlp;
        if (mdl != null) {
            mlp = restoreMLPState(mdl);
            mlp = trainer.update(mlp, datasetBuilder, featureExtractor, lbExtractorWrapper);
        }
        else
            mlp = trainer.fit(datasetBuilder, featureExtractor, lbExtractorWrapper);

        double[] params = mlp.parameters().getStorage().data();

        return new LogisticRegressionModel(new DenseVector(Arrays.copyOf(params, params.length - 1)),
            params[params.length - 1]
        );
    }

    /**
     * @param mdl Model.
     * @return state of MLP from last learning.
     */
    @NotNull private MultilayerPerceptron restoreMLPState(LogisticRegressionModel mdl) {
        Vector weights = mdl.weights();
        double intercept = mdl.intercept();

        MLPArchitecture architecture1 = new MLPArchitecture(weights.size());
        architecture1 = architecture1.withAddedLayer(1, true, Activators.SIGMOID);

        MLPArchitecture architecture = architecture1;
        MultilayerPerceptron perceptron = new MultilayerPerceptron(architecture);

        Vector mlpState = weights.like(weights.size() + 1);
        weights.nonZeroes().forEach(ith -> mlpState.set(ith.index(), ith.get()));
        mlpState.set(mlpState.size() - 1, intercept);
        perceptron.setParameters(mlpState);

        return perceptron;
    }

    /** {@inheritDoc} */
    @Override protected boolean checkState(LogisticRegressionModel mdl) {
        return true;
    }

    /**
     * Set up the max amount of iterations before convergence.
     *
     * @param maxIterations The parameter value.
     * @return Model with new max number of iterations before convergence parameter value.
     */
    public LogisticRegressionSGDTrainer<P> withMaxIterations(int maxIterations) {
        this.maxIterations = maxIterations;
        return this;
    }

    /**
     * Set up the batchSize parameter.
     *
     * @param batchSize The size of learning batch.
     * @return Trainer with new batch size parameter value.
     */
    public LogisticRegressionSGDTrainer<P> withBatchSize(int batchSize) {
        this.batchSize = batchSize;
        return this;
    }

    /**
     * Set up the amount of local iterations of SGD algorithm.
     *
     * @param amountOfLocIterations The parameter value.
     * @return Trainer with new locIterations parameter value.
     */
    public LogisticRegressionSGDTrainer<P> withLocIterations(int amountOfLocIterations) {
        this.locIterations = amountOfLocIterations;
        return this;
    }

    /**
     * Set up the random seed parameter.
     *
     * @param seed Seed for random generator.
     * @return Trainer with new seed parameter value.
     */
    public LogisticRegressionSGDTrainer<P> withSeed(long seed) {
        this.seed = seed;
        return this;
    }

    /**
     * Set up the regularization parameter.
     *
     * @param updatesStgy Update strategy.
     * @return Trainer with new update strategy parameter value.
     */
    public LogisticRegressionSGDTrainer withUpdatesStgy(UpdatesStrategy updatesStgy) {
        this.updatesStgy = updatesStgy;
        return this;
    }

    /**
     * Get the update strategy.
     *
     * @return The property value.
     */
    public UpdatesStrategy getUpdatesStgy() {
        return updatesStgy;
    }

    /**
     * Get the max amount of iterations.
     *
     * @return The property value.
     */
    public int getMaxIterations() {
        return maxIterations;
    }

    /**
     * Get the batch size.
     *
     * @return The property value.
     */
    public int getBatchSize() {
        return batchSize;
    }

    /**
     * Get the amount of local iterations.
     *
     * @return The property value.
     */
    public int getLocIterations() {
        return locIterations;
    }

    /**
     * Get the seed for random generator.
     *
     * @return The property value.
     */
    public long getSeed() {
        return seed;
    }
}
