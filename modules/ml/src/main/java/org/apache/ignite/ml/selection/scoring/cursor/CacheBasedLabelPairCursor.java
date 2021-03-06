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

package org.apache.ignite.ml.selection.scoring.cursor;

import java.util.Iterator;
import javax.cache.Cache;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.cache.query.QueryCursor;
import org.apache.ignite.cache.query.ScanQuery;
import org.apache.ignite.lang.IgniteBiPredicate;
import org.apache.ignite.ml.Model;
import org.apache.ignite.ml.math.functions.IgniteBiFunction;
import org.apache.ignite.ml.math.primitives.vector.Vector;
import org.apache.ignite.ml.selection.scoring.LabelPair;
import org.jetbrains.annotations.NotNull;

/**
 * Truth with prediction cursor based on a data stored in Ignite cache.
 *
 * @param <L> Type of a label (truth or prediction).
 * @param <K> Type of a key in {@code upstream} data.
 * @param <V> Type of a value in {@code upstream} data.
 */
public class CacheBasedLabelPairCursor<L, K, V> implements LabelPairCursor<L> {
    /** Query cursor. */
    private final QueryCursor<Cache.Entry<K, V>> cursor;

    /** Feature extractor. */
    private final IgniteBiFunction<K, V, Vector> featureExtractor;

    /** Label extractor. */
    private final IgniteBiFunction<K, V, L> lbExtractor;

    /** Model for inference. */
    private final Model<Vector, L> mdl;

    /**
     * Constructs a new instance of cache based truth with prediction cursor.
     *
     * @param upstreamCache Ignite cache with {@code upstream} data.
     * @param filter Filter for {@code upstream} data.
     * @param featureExtractor Feature extractor.
     * @param lbExtractor Label extractor.
     * @param mdl Model for inference.
     */
    public CacheBasedLabelPairCursor(IgniteCache<K, V> upstreamCache, IgniteBiPredicate<K, V> filter,
                                     IgniteBiFunction<K, V, Vector> featureExtractor, IgniteBiFunction<K, V, L> lbExtractor,
                                     Model<Vector, L> mdl) {
        this.cursor = query(upstreamCache, filter);
        this.featureExtractor = featureExtractor;
        this.lbExtractor = lbExtractor;
        this.mdl = mdl;
    }

    /**
     * Constructs a new instance of cache based truth with prediction cursor.
     *
     * @param upstreamCache Ignite cache with {@code upstream} data.
     * @param featureExtractor Feature extractor.
     * @param lbExtractor Label extractor.
     * @param mdl Model for inference.
     */
    public CacheBasedLabelPairCursor(IgniteCache<K, V> upstreamCache,
        IgniteBiFunction<K, V, Vector> featureExtractor, IgniteBiFunction<K, V, L> lbExtractor,
        Model<Vector, L> mdl) {
        this.cursor = query(upstreamCache);
        this.featureExtractor = featureExtractor;
        this.lbExtractor = lbExtractor;
        this.mdl = mdl;
    }

    /** {@inheritDoc} */
    @Override public void close() {
        cursor.close();
    }

    /** {@inheritDoc} */
    @NotNull @Override public Iterator<LabelPair<L>> iterator() {
        return new TruthWithPredictionIterator(cursor.iterator());
    }

    /**
     * Queries the specified cache using the specified filter.
     *
     * @param upstreamCache Ignite cache with {@code upstream} data.
     * @param filter Filter for {@code upstream} data.
     * @return Query cursor.
     */
    private QueryCursor<Cache.Entry<K, V>> query(IgniteCache<K, V> upstreamCache, IgniteBiPredicate<K, V> filter) {
        ScanQuery<K, V> qry = new ScanQuery<>();
        qry.setFilter(filter);

        return upstreamCache.query(qry);
    }

    /**
     * Queries the specified cache using the specified filter.
     *
     * @param upstreamCache Ignite cache with {@code upstream} data.
     * @return Query cursor.
     */
    private QueryCursor<Cache.Entry<K, V>> query(IgniteCache<K, V> upstreamCache) {
        ScanQuery<K, V> qry = new ScanQuery<>();

        return upstreamCache.query(qry);
    }

    /**
     * Util iterator that makes predictions using the model.
     */
    private class TruthWithPredictionIterator implements Iterator<LabelPair<L>> {
        /** Base iterator. */
        private final Iterator<Cache.Entry<K, V>> iter;

        /**
         * Constructs a new instance of truth with prediction iterator.
         *
         * @param iter Base iterator.
         */
        public TruthWithPredictionIterator(Iterator<Cache.Entry<K, V>> iter) {
            this.iter = iter;
        }

        /** {@inheritDoc} */
        @Override public boolean hasNext() {
            return iter.hasNext();
        }

        /** {@inheritDoc} */
        @Override public LabelPair<L> next() {
            Cache.Entry<K, V> entry = iter.next();

            Vector features = featureExtractor.apply(entry.getKey(), entry.getValue());
            L lb = lbExtractor.apply(entry.getKey(), entry.getValue());

            return new LabelPair<>(lb, mdl.apply(features));
        }
    }
}
