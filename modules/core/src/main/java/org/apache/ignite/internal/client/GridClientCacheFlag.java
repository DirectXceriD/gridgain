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

package org.apache.ignite.internal.client;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;

/**
 * Cache projection flags that specify projection behaviour.
 */
public enum GridClientCacheFlag {
    /** Skips store, i.e. no read-through and no write-through behavior. */
    SKIP_STORE,

    /**
     * Disable deserialization of binary objects on get operations.
     * If set and binary marshaller is used, {@link GridClientData#get(Object)}
     * and {@link GridClientData#getAll(Collection)} methods will return
     * instances of {@code BinaryObject} class instead of user objects.
     * Use this flag if you don't have corresponding class on your client of
     * if you want to get access to some individual fields, but do not want to
     * fully deserialize the object.
     */
    KEEP_BINARIES;

    /** */
    public static final int SKIP_STORE_MASK = 0b1;

    /** */
    public static final int KEEP_BINARIES_MASK = 0b10;

    /** */
    private static final GridClientCacheFlag[] VALS = values();

    /**
     * Efficiently gets enumerated value from its ordinal.
     *
     * @param ord Ordinal value.
     * @return Enumerated value or {@code null} if ordinal out of range.
     */
    public static GridClientCacheFlag fromOrdinal(int ord) {
        return ord >= 0 && ord < VALS.length ? VALS[ord] : null;
    }

    /**
     * Encodes cache flags to bit map.
     *
     * @param flagSet Set of flags to be encoded.
     * @return Bit map.
     */
    public static int encodeCacheFlags(Collection<GridClientCacheFlag> flagSet) {
        int bits = 0;

        if (flagSet.contains(SKIP_STORE))
            bits |= SKIP_STORE_MASK;

        if (flagSet.contains(KEEP_BINARIES))
            bits |= KEEP_BINARIES_MASK;

        return bits;
    }

    /**
     * Retrieves cache flags from corresponding bits.
     *
     * @param cacheFlagsBits Integer representation of cache flags bit set.
     * @return Cache flags.
     */
    public static Set<GridClientCacheFlag> parseCacheFlags(int cacheFlagsBits) {
        boolean skipStore = (cacheFlagsBits & SKIP_STORE_MASK) != 0;
        boolean keepBinaries = (cacheFlagsBits & KEEP_BINARIES_MASK) != 0;

        if (skipStore & keepBinaries)
            return EnumSet.of(SKIP_STORE, KEEP_BINARIES);

        if (skipStore)
            return EnumSet.of(SKIP_STORE);

        if (keepBinaries)
            return EnumSet.of(KEEP_BINARIES);

        return EnumSet.noneOf(GridClientCacheFlag.class);
    }
}
