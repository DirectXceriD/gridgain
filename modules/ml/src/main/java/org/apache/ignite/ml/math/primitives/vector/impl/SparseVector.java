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

package org.apache.ignite.ml.math.primitives.vector.impl;

import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.Map;
import java.util.Set;
import java.util.Spliterator;
import java.util.function.Consumer;
import org.apache.ignite.ml.math.StorageConstants;
import org.apache.ignite.ml.math.primitives.matrix.Matrix;
import org.apache.ignite.ml.math.primitives.matrix.impl.SparseMatrix;
import org.apache.ignite.ml.math.primitives.vector.AbstractVector;
import org.apache.ignite.ml.math.primitives.vector.Vector;
import org.apache.ignite.ml.math.primitives.vector.storage.SparseVectorStorage;

/**
 * Local on-heap sparse vector based on hash map storage.
 */
public class SparseVector extends AbstractVector implements StorageConstants {
    /**
     *
     */
    public SparseVector() {
        // No-op.
    }

    /**
     * @param map Underlying map.
     * @param cp Should given map be copied.
     */
    public SparseVector(Map<Integer, Double> map, boolean cp) {
        setStorage(new SparseVectorStorage(map, cp));
    }

    /**
     * @param size Vector size.
     * @param acsMode Vector elements access mode.
     */
    public SparseVector(int size, int acsMode) {
        assertAccessMode(acsMode);

        setStorage(new SparseVectorStorage(size, acsMode));
    }

    /** */
    private SparseVectorStorage storage() {
        return (SparseVectorStorage)getStorage();
    }

    /** {@inheritDoc} */
    @Override public Vector like(int crd) {
        SparseVectorStorage sto = storage();

        return new SparseVector(crd, sto.getAccessMode());
    }

    /** {@inheritDoc} */
    @Override public Matrix likeMatrix(int rows, int cols) {
        return new SparseMatrix(rows, cols);
    }

    /** {@inheritDoc} */
    @Override public Vector times(double x) {
        if (x == 0.0)
            return assign(0);
        else
            return super.times(x);
    }

    /** Indexes of non-default elements. */
    public IntSet indexes() {
        return storage().indexes();
    }

    /** {@inheritDoc} */
    @Override public Spliterator<Double> nonZeroSpliterator() {
        return new Spliterator<Double>() {
            /** {@inheritDoc} */
            @Override public boolean tryAdvance(Consumer<? super Double> act) {
                Set<Integer> indexes = storage().indexes();

                for (Integer index : indexes)
                    act.accept(storageGet(index));

                return true;
            }

            /** {@inheritDoc} */
            @Override public Spliterator<Double> trySplit() {
                return null; // No Splitting.
            }

            /** {@inheritDoc} */
            @Override public long estimateSize() {
                return storage().indexes().size();
            }

            /** {@inheritDoc} */
            @Override public int characteristics() {
                return ORDERED | SIZED;
            }
        };
    }
}
