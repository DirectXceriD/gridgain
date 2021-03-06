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

package org.apache.ignite.ml.dataset.impl.local;

import java.io.Serializable;
import java.util.List;
import org.apache.ignite.ml.dataset.Dataset;
import org.apache.ignite.ml.math.functions.IgniteBiFunction;
import org.apache.ignite.ml.math.functions.IgniteBinaryOperator;
import org.apache.ignite.ml.math.functions.IgniteTriFunction;

/**
 * An implementation of dataset based on local data structures such as {@code Map} and {@code List} and doesn't requires
 * Ignite environment. Introduces for testing purposes mostly, but can be used for simple local computations as well.
 *
 * @param <C> Type of a partition {@code context}.
 * @param <D> Type of a partition {@code data}.
 */
public class LocalDataset<C extends Serializable, D extends AutoCloseable> implements Dataset<C, D> {
    /** Partition {@code context} storage. */
    private final List<C> ctx;

    /** Partition {@code data} storage. */
    private final List<D> data;

    /**
     * Constructs a new instance of dataset based on local data structures such as {@code Map} and {@code List} and
     * doesn't requires Ignite environment.
     *
     * @param ctx Partition {@code context} storage.
     * @param data Partition {@code data} storage.
     */
    LocalDataset(List<C> ctx, List<D> data) {
        this.ctx = ctx;
        this.data = data;
    }

    /** {@inheritDoc} */
    @Override public <R> R computeWithCtx(IgniteTriFunction<C, D, Integer, R> map, IgniteBinaryOperator<R> reduce,
        R identity) {
        R res = identity;

        for (int part = 0; part < ctx.size(); part++) {
            D partData = data.get(part);

            if (partData != null)
                res = reduce.apply(res, map.apply(ctx.get(part), partData, part));
        }

        return res;
    }

    /** {@inheritDoc} */
    @Override public <R> R compute(IgniteBiFunction<D, Integer, R> map, IgniteBinaryOperator<R> reduce, R identity) {
        R res = identity;

        for (int part = 0; part < data.size(); part++) {
            D partData = data.get(part);

            if (partData != null)
                res = reduce.apply(res, map.apply(partData, part));
        }

        return res;
    }

    /** {@inheritDoc} */
    @Override public void close() {
        // Do nothing, GC will clean up.
    }

    /** */
    public List<C> getCtx() {
        return ctx;
    }

    /** */
    public List<D> getData() {
        return data;
    }
}
