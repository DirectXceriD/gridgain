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

package org.apache.ignite.ml.nn;

import java.io.Serializable;
import java.util.List;
import org.apache.ignite.ml.math.functions.IgniteFunction;
import org.apache.ignite.ml.optimization.updatecalculators.ParameterUpdateCalculator;

/**
 * Class encapsulating update strategies for group trainers based on updates.
 *
 * @param <M> Type of model to be optimized.
 * @param <U> Type of update.
 */
public class UpdatesStrategy<M, U extends Serializable> {
    /**
     * {@link ParameterUpdateCalculator}.
     */
    private ParameterUpdateCalculator<M, U> updatesCalculator;

    /**
     * Function used to reduce updates in one training (for example, sum all sequential gradient updates to get one
     * gradient update).
     */
    private IgniteFunction<List<U>, U> locStepUpdatesReducer;

    /**
     * Function used to reduce updates from different trainings (for example, averaging of gradients of all parallel trainings).
     */
    private IgniteFunction<List<U>, U> allUpdatesReducer;

    /**
     * Construct instance of this class with given parameters.
     *
     * @param updatesCalculator Parameter update calculator.
     * @param locStepUpdatesReducer Function used to reduce updates in one training
     * (for example, sum all sequential gradient updates to get one gradient update).
     * @param allUpdatesReducer Function used to reduce updates from different trainings
     * (for example, averaging of gradients of all parallel trainings).
     */
    public UpdatesStrategy(
        ParameterUpdateCalculator<M, U> updatesCalculator,
        IgniteFunction<List<U>, U> locStepUpdatesReducer,
        IgniteFunction<List<U>, U> allUpdatesReducer) {
        this.updatesCalculator = updatesCalculator;
        this.locStepUpdatesReducer = locStepUpdatesReducer;
        this.allUpdatesReducer = allUpdatesReducer;
    }

    /**
     * Get parameter update calculator (see {@link ParameterUpdateCalculator}).
     *
     * @return Parameter update calculator.
     */
    public ParameterUpdateCalculator<M, U> getUpdatesCalculator() {
        return updatesCalculator;
    }

    /**
     * Get function used to reduce updates in one training
     * (for example, sum all sequential gradient updates to get one gradient update).
     *
     * @return Function used to reduce updates in one training
     * (for example, sum all sequential gradient updates to get on gradient update).
     */
    public IgniteFunction<List<U>, U> locStepUpdatesReducer() {
        return locStepUpdatesReducer;
    }

    /**
     * Get function used to reduce updates from different trainings
     * (for example, averaging of gradients of all parallel trainings).
     *
     * @return Function used to reduce updates from different trainings.
     */
    public IgniteFunction<List<U>, U> allUpdatesReducer() {
        return allUpdatesReducer;
    }
}
