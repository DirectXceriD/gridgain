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

package org.apache.ignite.compatibility.persistence;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import org.apache.ignite.compatibility.testframework.junits.IgniteCompatibilityAbstractTest;
import org.apache.ignite.compatibility.testframework.util.CompatibilityTestsUtils;
import org.apache.ignite.internal.util.typedef.internal.U;

import static org.apache.ignite.internal.processors.cache.persistence.file.FilePageStoreManager.DFLT_STORE_DIR;
import static org.apache.ignite.internal.processors.cache.persistence.wal.reader.StandaloneGridKernalContext.BINARY_META_FOLDER;

/**
 * Super class for all persistence compatibility tests.
 */
public abstract class IgnitePersistenceCompatibilityAbstractTest extends IgniteCompatibilityAbstractTest {
    /** Persistence directories. */
    private static final List<String> PERSISTENCE_DIRS = Arrays.asList(DFLT_STORE_DIR, BINARY_META_FOLDER, "cp", "marshaller");

    /** {@inheritDoc} */
    @Override protected void beforeTest() throws Exception {
        super.beforeTest();

        for (String dirName : PERSISTENCE_DIRS) {
            File dir = U.resolveWorkDirectory(U.defaultWorkDirectory(), dirName, true);

            if (!CompatibilityTestsUtils.isDirectoryEmpty(dir)) {
                throw new IllegalStateException("Directory is not empty and can't be cleaned: " +
                    "[" + dir.getAbsolutePath() + "]. It may be locked by another system process.");
            }
        }
    }

    /** {@inheritDoc} */
    @Override protected void afterTest() throws Exception {
        super.afterTest();

        //protection if test failed to finish, e.g. by error
        stopAllGrids();

        for (String dir : PERSISTENCE_DIRS)
            U.delete(U.resolveWorkDirectory(U.defaultWorkDirectory(), dir, false));
    }
}
