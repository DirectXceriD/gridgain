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

package org.apache.ignite.internal.util.lang.gridfunc;

import java.util.Iterator;
import java.util.Set;
import org.apache.ignite.internal.util.GridSerializableMap;
import org.apache.ignite.internal.util.GridSerializableSet;
import org.apache.ignite.internal.util.lang.GridFunc;
import org.apache.ignite.internal.util.typedef.F;
import org.apache.ignite.lang.IgniteClosure;
import org.apache.ignite.lang.IgnitePredicate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Light-weight view on given map with provided preds and mapping.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 */
public class PredicateSetView<K, V> extends GridSerializableMap<K, V> {
    /** */
    private static final long serialVersionUID = 0L;

    /** */
    private final Set<K> set;

    /** */
    private final IgniteClosure<? super K, V> clo;

    /** */
    private final IgnitePredicate<? super K>[] preds;

    /** Entry predicate. */
    private IgnitePredicate<K> entryPred;

    /**
     * @param set Input collection.
     * @param clo Mapping closure, that maps key to value.
     * @param preds Optional predicates to filter input collection. If predicates are not provided - all elements
     * will be in
     */
    @SuppressWarnings({"unchecked"})
    public PredicateSetView(Set<K> set, IgniteClosure<? super K, V> clo,
        IgnitePredicate<? super K>... preds) {
        this.set = set;
        this.clo = clo;
        this.preds = preds;
        this.entryPred = new IsAllPredicate(preds);
    }

    /** {@inheritDoc} */
    @NotNull @Override public Set<Entry<K, V>> entrySet() {
        return new GridSerializableSet<Entry<K, V>>() {
            @NotNull @Override public Iterator<Entry<K, V>> iterator() {
                return new Iterator<Entry<K, V>>() {

                    private Iterator<K> it = GridFunc.iterator0(set, true, entryPred);

                    @Override public boolean hasNext() {
                        return it.hasNext();
                    }

                    @Override public Entry<K, V> next() {
                        final K e = it.next();

                        return new Entry<K, V>() {
                            @Override public K getKey() {
                                return e;
                            }

                            @Override public V getValue() {
                                return clo.apply(e);
                            }

                            @Override public V setValue(V val) {
                                throw new UnsupportedOperationException(
                                    "Put is not supported for readonly collection view.");
                            }
                        };
                    }

                    @Override public void remove() {
                        throw new UnsupportedOperationException(
                            "Remove is not support for readonly collection view.");
                    }
                };
            }

            @Override public int size() {
                return F.size(set, preds);
            }

            @Override public boolean remove(Object o) {
                throw new UnsupportedOperationException("Remove is not support for readonly collection view.");
            }

            @Override public boolean isEmpty() {
                return !iterator().hasNext();
            }
        };
    }

    /** {@inheritDoc} */
    @Override public boolean isEmpty() {
        return entrySet().isEmpty();
    }

    /** {@inheritDoc} */
    @Nullable @Override public V get(Object key) {
        if (containsKey(key))
            return clo.apply((K)key);

        return null;
    }

    /** {@inheritDoc} */
    @Nullable @Override public V put(K key, V val) {
        throw new UnsupportedOperationException("Put is not supported for readonly collection view.");
    }

    /** {@inheritDoc} */
    @Override public V remove(Object key) {
        throw new UnsupportedOperationException("Remove is not supported for readonly collection view.");
    }

    /** {@inheritDoc} */
    @Override public boolean containsKey(Object key) {
        return GridFunc.isAll((K)key, preds) && set.contains(key);
    }
}
