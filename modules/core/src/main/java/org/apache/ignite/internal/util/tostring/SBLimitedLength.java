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

package org.apache.ignite.internal.util.tostring;

import org.apache.ignite.internal.util.GridStringBuilder;
import java.util.Arrays;

/**
 *
 */
public class SBLimitedLength extends GridStringBuilder {
    /** */
    private static final long serialVersionUID = 0L;

    /** */
    private SBLengthLimit lenLimit;

    /** Additional string builder to get tail of message. */
    private CircularStringBuilder tail;

    /**
     * @param cap Capacity.
     */
    SBLimitedLength(int cap) {
        super(cap);
    }

    /**
     * @param lenLimit Length limit.
     */
    void initLimit(SBLengthLimit lenLimit) {
        this.lenLimit = lenLimit;

        if (tail != null)
            tail.reset();
    }

    /**
     * @return tail string builder.
     */
    public CircularStringBuilder getTail() {
        return tail;
    }

    /**
     * @param tail tail CircularStringBuilder to set.
     */
    public void setTail(CircularStringBuilder tail) {
        this.tail = tail;
    }

    /**
     * @param lenBeforeWrite Length before write.
     * @return This builder.
     */
    private GridStringBuilder onWrite(int lenBeforeWrite) {
        assert lenLimit != null;

        lenLimit.onWrite(this, length() - lenBeforeWrite);

        return this;
    }

    /** {@inheritDoc} */
    @Override public GridStringBuilder a(Object obj) {
        if (lenLimit.overflowed(this)) {
            tail.append(obj);
            return this;
        }

        int curLen = length();

        super.a(obj);

        return onWrite(curLen);
    }

    /** {@inheritDoc} */
    @Override public GridStringBuilder a(String str) {
        if (lenLimit.overflowed(this)) {
            tail.append(str);
            return this;
        }

        int curLen = length();

        super.a(str);

        return onWrite(curLen);
    }

    /** {@inheritDoc} */
    @Override public GridStringBuilder a(StringBuffer sb) {
        if (lenLimit.overflowed(this)) {
            tail.append(sb);
            return this;
        }

        int curLen = length();

        super.a(sb);

        return onWrite(curLen);
    }

    /** {@inheritDoc} */
    @Override public GridStringBuilder a(CharSequence s) {
        if (lenLimit.overflowed(this)) {
            tail.append(s);
            return this;
        }

        int curLen = length();

        super.a(s);

        return onWrite(curLen);
    }

    /** {@inheritDoc} */
    @Override public GridStringBuilder a(CharSequence s, int start, int end) {
        if (lenLimit.overflowed(this)) {
            tail.append(s.subSequence(start, end));
            return this;
        }

        int curLen = length();

        super.a(s, start, end);

        return onWrite(curLen);
    }

    /** {@inheritDoc} */
    @Override public GridStringBuilder a(char[] str) {
        if (lenLimit.overflowed(this)) {
            tail.append(str);
            return this;
        }

        int curLen = length();

        super.a(str);

        return onWrite(curLen);
    }

    /** {@inheritDoc} */
    @Override public GridStringBuilder a(char[] str, int offset, int len) {
        if (lenLimit.overflowed(this)) {
            tail.append(Arrays.copyOfRange(str, offset, len));
            return this;
        }

        int curLen = length();

        super.a(str, offset, len);

        return onWrite(curLen);
    }

    /** {@inheritDoc} */
    @Override public GridStringBuilder a(boolean b) {
        if (lenLimit.overflowed(this)) {
            tail.append(b);
            return this;
        }

        int curLen = length();

        super.a(b);

        return onWrite(curLen);
    }

    /** {@inheritDoc} */
    @Override public GridStringBuilder a(char c) {
        if (lenLimit.overflowed(this)) {
            tail.append(c);
            return this;
        }

        int curLen = length();

        super.a(c);

        return onWrite(curLen);
    }

    /** {@inheritDoc} */
    @Override public GridStringBuilder a(int i) {
        if (lenLimit.overflowed(this)) {
            tail.append(i);
            return this;
        }

        int curLen = length();

        super.a(i);

        return onWrite(curLen);
    }

    /** {@inheritDoc} */
    @Override public GridStringBuilder a(long lng) {
        if (lenLimit.overflowed(this)) {
            tail.append(lng);
            return this;
        }

        int curLen = length();

        super.a(lng);

        return onWrite(curLen);
    }

    /** {@inheritDoc} */
    @Override public GridStringBuilder a(float f) {
        if (lenLimit.overflowed(this)) {
            tail.append(f);
            return this;
        }

        int curLen = length();

        super.a(f);

        return onWrite(curLen);
    }

    /** {@inheritDoc} */
    @Override public GridStringBuilder a(double d) {
        if (lenLimit.overflowed(this)) {
            tail.append(d);
            return this;
        }

        int curLen = length();

        super.a(d);

        return onWrite(curLen);
    }

    /** {@inheritDoc} */
    @Override public GridStringBuilder appendCodePoint(int codePoint) {
        if (lenLimit.overflowed(this)) {
            tail.append(codePoint);
            return this;
        }

        int curLen = length();

        super.appendCodePoint(codePoint);

        return onWrite(curLen);
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        if (tail == null)
            return super.toString();
        else {
            int tailLen = tail.length();
            StringBuilder res = new StringBuilder(impl().capacity() + tailLen + 100);

            res.append(impl());

            if (tail.getSkipped() > 0) {
                res.append("... and ").append(String.valueOf(tail.getSkipped() + tailLen))
                    .append(" skipped ...");
            }

            res.append(tail.toString());

            return res.toString();
        }
    }
}
