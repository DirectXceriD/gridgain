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

package org.apache.ignite.internal.util.lang;

import java.util.Iterator;
import java.util.NoSuchElementException;
import org.apache.ignite.IgniteCheckedException;
import org.apache.ignite.internal.util.GridSerializableIterator;

/**
 * Defines "rich" iterator interface that is also acts like lambda function and iterable.
 * @see GridIterable
 */
public interface GridIterator<T> extends Iterable<T>, GridSerializableIterator<T> {
    /**
     * This method is the same as {@link #hasNext()}, but allows for failure
     * with exception. Often iterators are used to iterate through values
     * that have not or have partially been received from remote nodes,
     * and need to account for possible network failures, rather than
     * just returning {@code false} out of {@link #hasNext()} method.
     *
     * @return {@code True} if iterator contains more elements.
     * @throws IgniteCheckedException If no more elements can be returned due
     *      to some failure, like a network error for example.
     * @see Iterator#hasNext()
     */
    public boolean hasNextX() throws IgniteCheckedException;

    /**
     * This method is the same as {@link #next()}, but allows for failure
     * with exception. Often iterators are used to iterate through values
     * that have not or have partially been received from remote nodes,
     * and need to account for possible network failures, rather than
     * throwing {@link NoSuchElementException} runtime exception.s
     *
     * @return {@code True} if iterator contains more elements.
     * @throws NoSuchElementException If there are no more elements to
     *      return.
     * @throws IgniteCheckedException If no more elements can be returned due
     *      to some failure, like a network error for example.
     * @see Iterator#next()
     */
    public T nextX() throws IgniteCheckedException;

    /**
     * This method is the same as {@link #remove()}, but allows for failure
     * with exception.
     *
     * @throws IgniteCheckedException If failed.
     */
    public void removeX() throws IgniteCheckedException;
}