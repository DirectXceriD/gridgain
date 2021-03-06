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
package org.apache.ignite.internal.processors.hadoop;

import org.apache.ignite.IgniteCheckedException;
import org.apache.ignite.IgniteException;
import org.apache.ignite.internal.GridKernalContext;
import org.apache.ignite.internal.util.typedef.internal.U;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.commons.RemappingClassAdapter;

import java.io.IOException;
import java.io.InputStream;

/**
 * Utility methods for Hadoop classloader required to avoid direct 3rd-party dependencies in class loader.
 */
public class HadoopHelperImpl implements HadoopHelper {
    /** Kernal context. */
    private GridKernalContext ctx;

    /** Common class loader. */
    private volatile HadoopClassLoader ldr;

    /**
     * Default constructor.
     */
    public HadoopHelperImpl() {
        this(null);
    }

    /**
     * Constructor.
     *
     * @param ctx Kernal context.
     */
    public HadoopHelperImpl(GridKernalContext ctx) {
        this.ctx = ctx;
    }

    /** {@inheritDoc} */
    @Override public boolean isNoOp() {
        return false;
    }

    /** {@inheritDoc} */
    @Override public HadoopClassLoader commonClassLoader() {
        HadoopClassLoader res = ldr;

        if (res == null) {
            synchronized (this) {
                res = ldr;

                if (res == null) {
                    String[] libNames = null;

                    if (ctx != null && ctx.config().getHadoopConfiguration() != null)
                        libNames = ctx.config().getHadoopConfiguration().getNativeLibraryNames();

                    res = new HadoopClassLoader(null, "hadoop-common", libNames, this);

                    ldr = res;
                }
            }
        }

        return res;
    }

    /** {@inheritDoc} */
    @Override public byte[] loadReplace(InputStream in, final String originalName, final String replaceName) {
        ClassReader rdr;

        try {
            rdr = new ClassReader(in);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }

        ClassWriter w = new ClassWriter(Opcodes.ASM4);

        rdr.accept(new RemappingClassAdapter(w, new Remapper() {
            /** */
            String replaceType = replaceName.replace('.', '/');

            /** */
            String nameType = originalName.replace('.', '/');

            @Override public String map(String type) {
                if (type.equals(replaceType))
                    return nameType;

                return type;
            }
        }), ClassReader.EXPAND_FRAMES);

        return w.toByteArray();
    }

    /** {@inheritDoc} */
    @Override @Nullable public InputStream loadClassBytes(ClassLoader ldr, String clsName) {
        return ldr.getResourceAsStream(clsName.replace('.', '/') + ".class");
    }

    /** {@inheritDoc} */
    @Override public String workDirectory() {
        try {
            return ctx != null ? ctx.config().getWorkDirectory() : U.defaultWorkDirectory();
        }
        catch (IgniteCheckedException e) {
            throw new IgniteException("Failed to resolve Ignite work directory.", e);
        }
    }

    /** {@inheritDoc} */
    @Override public void close() {
        // Force drop KernalContext link, because HadoopHelper leaks in some tests.
        ctx = null;
    }
}
