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

package org.apache.ignite.internal.processors.cache;

import java.io.Serializable;
import javax.cache.processor.EntryProcessor;
import javax.cache.processor.EntryProcessorException;
import javax.cache.processor.MutableEntry;
import org.apache.ignite.IgniteCheckedException;
import org.apache.ignite.IgniteException;
import org.apache.ignite.internal.GridKernalContext;
import org.apache.ignite.internal.processors.resource.GridResourceIoc;
import org.apache.ignite.internal.processors.resource.GridResourceProcessor;
import org.jetbrains.annotations.Nullable;

/**
 * Entry processor wrapper injecting Ignite resources into target processor before execution.
 */
public class EntryProcessorResourceInjectorProxy<K, V, T> implements EntryProcessor<K, V, T>, Serializable {
    /** */
    private static final long serialVersionUID = 0L;

    /** Delegate. */
    private EntryProcessor<K, V, T> delegate;

    /** Injected flag. */
    private transient boolean injected;

    /**
     * @param delegate Delegate.
     */
    private EntryProcessorResourceInjectorProxy(EntryProcessor<K, V, T> delegate) {
        this.delegate = delegate;
    }

    /** {@inheritDoc} */
    @Override public T process(MutableEntry<K, V> entry, Object... arguments) throws EntryProcessorException {
        if (!injected) {
            GridCacheContext cctx = entry.unwrap(GridCacheContext.class);

            GridResourceProcessor rsrc = cctx.kernalContext().resource();

            try {
                rsrc.inject(delegate, GridResourceIoc.AnnotationSet.ENTRY_PROCESSOR, cctx.name());
            }
            catch (IgniteCheckedException e) {
                throw new IgniteException(e);
            }

            injected = true;
        }

        return delegate.process(entry, arguments);
    }

    /**
     * @return Delegate entry processor.
     */
    public EntryProcessor<K, V, T> delegate() {
        return delegate;
    }

    /**
     * Wraps EntryProcessor if needed.
     *
     * @param ctx Context.
     * @param proc Entry proc.
     * @return Wrapped entry proc if wrapping is needed.
     */
    public static <K, V, T> EntryProcessor<K, V, T> wrap(GridKernalContext ctx,
        @Nullable EntryProcessor<K, V, T> proc) {
        if (proc == null || proc instanceof EntryProcessorResourceInjectorProxy)
            return proc;

        GridResourceProcessor rsrcProcessor = ctx.resource();

        return rsrcProcessor.isAnnotationsPresent(null, proc, GridResourceIoc.AnnotationSet.ENTRY_PROCESSOR) ?
            new EntryProcessorResourceInjectorProxy<>(proc) : proc;
    }

    /**
     * Unwraps EntryProcessor as Object if needed.
     *
     * @param obj Entry processor.
     * @return Unwrapped entry processor.
     */
    static Object unwrap(Object obj) {
        return (obj instanceof EntryProcessorResourceInjectorProxy) ? ((EntryProcessorResourceInjectorProxy)obj).delegate() : obj;
    }
}
