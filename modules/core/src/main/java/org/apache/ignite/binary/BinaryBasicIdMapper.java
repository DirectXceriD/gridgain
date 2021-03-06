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

package org.apache.ignite.binary;

import org.apache.ignite.internal.util.typedef.internal.A;

/**
 * Base binary ID mapper implementation.
 */
public class BinaryBasicIdMapper implements BinaryIdMapper {
    /** Default lower case flag setting. */
    public static final boolean DFLT_LOWER_CASE = true;

    /** Maximum lower-case character. */
    private static final char MAX_LOWER_CASE_CHAR = 0x7e;

    /** Cached lower-case characters. */
    private static final char[] LOWER_CASE_CHARS;

    /** */
    private boolean isLowerCase = DFLT_LOWER_CASE;

    /**
     * Static initializer.
     */
    static {
        LOWER_CASE_CHARS = new char[MAX_LOWER_CASE_CHAR + 1];

        for (char c = 0; c <= MAX_LOWER_CASE_CHAR; c++)
            LOWER_CASE_CHARS[c] = Character.toLowerCase(c);
    }

    /**
     * Default constructor.
     */
    public BinaryBasicIdMapper() {
    }

    /**
     * @param isLowerCase Whether to use strings in lower case or not.
     * <p>
     * Defaults to {@link #DFLT_LOWER_CASE}.
     */
    public BinaryBasicIdMapper(boolean isLowerCase) {
        this.isLowerCase = isLowerCase;
    }

    /**
     * Get type ID.
     *
     * @param typeName Type name.
     * @return Type ID.
     */
    @Override public int typeId(String typeName) {
        A.notNull(typeName, "typeName");

        int id = isLowerCase ? lowerCaseHashCode(typeName) : typeName.hashCode();

        if (id != 0)
            return id;
        else {
            throw new BinaryObjectException("Binary ID mapper resolved type ID to zero " +
                "(either change type's name or use custom ID mapper) " +
                "[name=" + typeName + ", isLowerCase=" + isLowerCase + "]");
        }
    }

    /**
     * Get field ID.
     *
     * @param typeId Type ID.
     * @param fieldName Field name.
     * @return Field ID.
     */
    @Override public int fieldId(int typeId, String fieldName) {
        A.notNull(fieldName, "fieldName");

        int id = isLowerCase ? lowerCaseHashCode(fieldName) : fieldName.hashCode();

        if (id != 0)
            return id;
        else {
            throw new BinaryObjectException("Binary ID mapper resolved field ID to zero " +
                "(either change filed's name or use custom ID mapper) " +
                "[name=" + fieldName + ", isLowerCase=" + isLowerCase + "]");
        }
    }

    /**
     * Gets whether to use strings in lower case or not.
     *
     * @return Whether to use strings in lower case or not.
     */
    public boolean isLowerCase() {
        return isLowerCase;
    }

    /**
     * Sets whether to use strings in lower case or not.
     *
     * @param isLowerCase Whether to use strings in lower case or not.
     * @return {@code this} for chaining.
     */
    public BinaryBasicIdMapper setLowerCase(boolean isLowerCase) {
        this.isLowerCase = isLowerCase;

        return this;
    }

    /**
     * Routine to calculate string hash code an
     *
     * @param str String.
     * @return Hash code for given string converted to lower case.
     */
    private static int lowerCaseHashCode(String str) {
        int len = str.length();

        int h = 0;

        for (int i = 0; i < len; i++) {
            int c = str.charAt(i);

            c = c <= MAX_LOWER_CASE_CHAR ? LOWER_CASE_CHARS[c] : Character.toLowerCase(c);

            h = 31 * h + c;
        }

        return h;
    }

    /** {@inheritDoc} */
    @Override public boolean equals(Object o) {
        if (this == o)
            return true;

        if (!(o instanceof BinaryBasicIdMapper))
            return false;

        BinaryBasicIdMapper mapper = (BinaryBasicIdMapper)o;

        return isLowerCase == mapper.isLowerCase;

    }

    /** {@inheritDoc} */
    @Override public int hashCode() {
        return (isLowerCase ? 1 : 0);
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return "BinaryBaseIdMapper [isLowerCase=" + isLowerCase + ']';
    }
}
