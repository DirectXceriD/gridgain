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

package org.apache.ignite.internal.processors.cache.persistence.pagemem;

import org.apache.ignite.internal.pagemem.FullPageId;
import org.apache.ignite.internal.util.tostring.GridToStringExclude;
import org.apache.ignite.internal.util.tostring.GridToStringInclude;
import org.apache.ignite.internal.util.typedef.internal.S;
import org.apache.ignite.internal.util.typedef.internal.U;

/**
 * Replacement removal candidate. Class represents some page from loaded pages table. Usually candidate is found during
 * random {@link LoadedPagesMap} touch.
 */
public class ReplaceCandidate {
    /** Partition generation saved in map, too old value means page may be safely cleared. */
    private int gen;

    /** */
    @GridToStringExclude
    private long relPtr;

    /** */
    @GridToStringInclude
    private FullPageId fullId;

    /**
     * @param gen Partition generation.
     * @param relPtr Relative pointer to page.
     * @param fullId Full page ID.
     */
    public ReplaceCandidate(int gen, long relPtr, FullPageId fullId) {
        this.gen = gen;
        this.relPtr = relPtr;
        this.fullId = fullId;
    }

    /**
     * @return Partition generation saved in map, too old value means page may be safely cleared.
     */
    public int generation() {
        return gen;
    }

    /**
     * @return Relative pointer to page.
     */
    public long relativePointer() {
        return relPtr;
    }

    /**
     * @return Index.
     */
    public FullPageId fullId() {
        return fullId;
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(ReplaceCandidate.class, this, "relPtr", U.hexLong(relPtr));
    }
}
