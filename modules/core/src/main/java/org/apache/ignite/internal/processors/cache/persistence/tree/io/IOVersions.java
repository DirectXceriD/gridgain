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

package org.apache.ignite.internal.processors.cache.persistence.tree.io;

import org.apache.ignite.internal.util.typedef.internal.S;

/**
 * Registry for IO versions.
 */
public final class IOVersions<V extends PageIO> {
    /** */
    private final V[] vers;

    /** */
    private final int type;

    /** */
    private final V latest;

    /**
     * @param vers Versions.
     */
    @SafeVarargs
    public IOVersions(V... vers) {
        assert vers != null;
        assert vers.length > 0;

        this.vers = vers;
        this.type = vers[0].getType();

        latest = vers[vers.length - 1];

        assert checkVersions();
    }

    /**
     * @return Type.
     */
    public int getType() {
        return type;
    }

    /**
     * @return {@code true} If versions are correct.
     */
    private boolean checkVersions() {
        for (int i = 0; i < vers.length; i++) {
            V v = vers[i];

            if (v.getType() != type || v.getVersion() != i + 1)
                return false;
        }

        return true;
    }

    /**
     * @return Latest IO version.
     */
    public V latest() {
        return latest;
    }

    /**
     * @param ver Version.
     * @return IO.
     */
    public V forVersion(int ver) {
        if (ver == 0)
            throw new IllegalStateException("Failed to get page IO instance (page content is corrupted)");

        return vers[ver - 1];
    }

    /**
     * @param pageAddr Page address.
     * @return IO.
     */
    public V forPage(long pageAddr) {
        int ver = PageIO.getVersion(pageAddr);

        V res = forVersion(ver);

        assert res.getType() == PageIO.getType(pageAddr) : "resType=" + res.getType() +
            ", pageType=" + PageIO.getType(pageAddr);

        return res;
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(IOVersions.class, this);
    }
}
