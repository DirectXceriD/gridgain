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

package org.apache.ignite.internal.processors.igfs.split;

import java.nio.charset.Charset;
import org.apache.ignite.igfs.IgfsInputStream;
import org.apache.ignite.igfs.mapreduce.IgfsFileRange;
import org.apache.ignite.igfs.mapreduce.records.IgfsByteDelimiterRecordResolver;
import org.apache.ignite.igfs.mapreduce.records.IgfsStringDelimiterRecordResolver;
import org.apache.ignite.internal.util.typedef.F;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * String delimiter split resolver self-test.
 */
@RunWith(JUnit4.class)
public class IgfsStringDelimiterRecordResolverSelfTest extends IgfsAbstractRecordResolverSelfTest {
    /** Charset used in tests. */
    private static final Charset UTF8 = Charset.forName("UTF-8");

    /**
     * Test split resolver.
     *
     * @throws Exception If failed.
     */
    @Test
    public void testResolver() throws Exception {
        String delim = "aaaaaaaa";

        byte[] delimBytes = delim.getBytes(UTF8);
        byte[] data = array(F.t(wrap(1), 8), F.t(delimBytes, 1), F.t(wrap(1), 8));

        assertSplit(0, 4, 0, 16, data, delim);
        assertSplit(0, 8, 0, 16, data, delim);
        assertSplit(0, 12, 0, 16, data, delim);
        assertSplit(0, 16, 0, 16, data, delim);
        assertSplit(0, 20, 0, 24, data, delim);
        assertSplit(0, 24, 0, 24, data, delim);

        assertSplitNull(2, 2, data, delim);
        assertSplitNull(2, 6, data, delim);
        assertSplitNull(2, 10, data, delim);
        assertSplitNull(2, 14, data, delim);
        assertSplit(2, 18, 16, 8, data, delim);
        assertSplit(2, 22, 16, 8, data, delim);

        assertSplitNull(8, 4, data, delim);
        assertSplitNull(8, 8, data, delim);
        assertSplit(8, 12, 16, 8, data, delim);
        assertSplit(8, 16, 16, 8, data, delim);

        assertSplitNull(10, 2, data, delim);
        assertSplitNull(10, 6, data, delim);
        assertSplit(10, 10, 16, 8, data, delim);
        assertSplit(10, 14, 16, 8, data, delim);

        assertSplit(16, 4, 16, 8, data, delim);
        assertSplit(16, 8, 16, 8, data, delim);

        assertSplitNull(18, 2, data, delim);
        assertSplitNull(18, 6, data, delim);
    }

    /**
     * Check split resolution.
     *
     * @param suggestedStart Suggested start.
     * @param suggestedLen Suggested length.
     * @param expStart Expected start.
     * @param expLen Expected length.
     * @param data File data.
     * @param delims Delimiters.
     * @throws Exception If failed.
     */
    public void assertSplit(long suggestedStart, long suggestedLen, long expStart, long expLen, byte[] data,
        String... delims) throws Exception {
        write(data);

        IgfsByteDelimiterRecordResolver rslvr = resolver(delims);

        IgfsFileRange split;

        try (IgfsInputStream is = read()) {
            split = rslvr.resolveRecords(igfs, is, split(suggestedStart, suggestedLen));
        }

        assert split != null : "Split is null.";
        assert split.start() == expStart : "Incorrect start [expected=" + expStart + ", actual=" + split.start() + ']';
        assert split.length() == expLen : "Incorrect length [expected=" + expLen + ", actual=" + split.length() + ']';
    }

    /**
     * Check the split resolution resulted in {@code null}.
     *
     * @param suggestedStart Suggested start.
     * @param suggestedLen Suggested length.
     * @param data File data.
     * @param delims Delimiters.
     * @throws Exception If failed.
     */
    public void assertSplitNull(long suggestedStart, long suggestedLen, byte[] data, String... delims)
        throws Exception {
        write(data);

        IgfsStringDelimiterRecordResolver rslvr = resolver(delims);

        IgfsFileRange split;

        try (IgfsInputStream is = read()) {
            split = rslvr.resolveRecords(igfs, is, split(suggestedStart, suggestedLen));
        }

        assert split == null : "Split is not null.";
    }

    /**
     * Create resolver.
     *
     * @param delims Delimiters.
     * @return Resolver.
     */
    private IgfsStringDelimiterRecordResolver resolver(String... delims) {
        return new IgfsStringDelimiterRecordResolver(UTF8, delims);
    }
}
