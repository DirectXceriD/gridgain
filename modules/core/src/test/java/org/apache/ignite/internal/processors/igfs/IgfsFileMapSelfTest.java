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

package org.apache.ignite.internal.processors.igfs;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import org.apache.ignite.internal.util.typedef.F;
import org.apache.ignite.lang.IgniteUuid;
import org.apache.ignite.testframework.GridTestUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.apache.ignite.internal.processors.igfs.IgfsFileAffinityRange.RANGE_STATUS_INITIAL;
import static org.apache.ignite.internal.processors.igfs.IgfsFileAffinityRange.RANGE_STATUS_MOVED;
import static org.apache.ignite.internal.processors.igfs.IgfsFileAffinityRange.RANGE_STATUS_MOVING;

/**
 * File map self test.
 */
@RunWith(JUnit4.class)
public class IgfsFileMapSelfTest extends IgfsCommonAbstractTest {
    /**
     * @throws Exception If failed.
     */
    @Test
    public void testRanges() throws Exception {
        IgfsFileMap map = new IgfsFileMap();

        IgniteUuid[] affKeys = new IgniteUuid[20];

        for (int i = 0; i < affKeys.length; i++)
            affKeys[i] = IgniteUuid.randomUuid();

        int numOfRanges = 0;

        do {
            for (int i = 0; i < 2 * numOfRanges + 1; i++) {
                long off1 = i * 10;
                long off2 = i * 10 + 5;
                long off3 = i * 10 + 8;

                IgniteUuid affKey = i % 2 == 0 ? null : affKeys[i / 2];

                assertEquals("For i: " + i, affKey, map.affinityKey(off1, false));
                assertEquals("For i: " + i, affKey, map.affinityKey(off2, false));
                assertEquals("For i: " + i, affKey, map.affinityKey(off3, false));
            }

            map.addRange(new IgfsFileAffinityRange(10 + 20 * numOfRanges, 19 + 20 * numOfRanges,
                affKeys[numOfRanges]));

            numOfRanges++;
        } while (numOfRanges < 20);
    }

    /**
     * @throws Exception If failed.
     */
    @Test
    public void testAddUpdateAdd() throws Exception {
        IgfsFileMap map = new IgfsFileMap();

        IgniteUuid affKey = IgniteUuid.randomUuid();

        map.addRange(new IgfsFileAffinityRange(0, 9, affKey));

        map.updateRangeStatus(new IgfsFileAffinityRange(0, 9, affKey), RANGE_STATUS_MOVING);

        map.addRange(new IgfsFileAffinityRange(10, 19, affKey));

        List<IgfsFileAffinityRange> ranges = map.ranges();

        assertEquals(2, ranges.size());

        assertEquals(RANGE_STATUS_MOVING, ranges.get(0).status());
        assertTrue(ranges.get(0).regionEqual(new IgfsFileAffinityRange(0, 9, affKey)));

        assertEquals(RANGE_STATUS_INITIAL, ranges.get(1).status());
        assertTrue(ranges.get(1).regionEqual(new IgfsFileAffinityRange(10, 19, affKey)));
    }

    /**
     * @throws Exception If failed.
     */
    @Test
    public void testRangeUpdate1() throws Exception {
        IgfsFileMap map = new IgfsFileMap();

        IgniteUuid affKey = IgniteUuid.randomUuid();

        for (int i = 0; i < 4; i++)
            map.addRange(new IgfsFileAffinityRange(i * 20 + 10, i * 20 + 19, affKey));

        // Middle, first, last.
        map.updateRangeStatus(new IgfsFileAffinityRange(30, 39, affKey), RANGE_STATUS_MOVING);
        map.updateRangeStatus(new IgfsFileAffinityRange(10, 19, affKey), RANGE_STATUS_MOVING);
        map.updateRangeStatus(new IgfsFileAffinityRange(70, 79, affKey), RANGE_STATUS_MOVING);

        List<IgfsFileAffinityRange> ranges = map.ranges();

        assertEquals(RANGE_STATUS_MOVING, ranges.get(0).status());
        assertEquals(RANGE_STATUS_MOVING, ranges.get(1).status());
        assertEquals(RANGE_STATUS_INITIAL, ranges.get(2).status());
        assertEquals(RANGE_STATUS_MOVING, ranges.get(3).status());

        // Middle, first, last.
        map.updateRangeStatus(new IgfsFileAffinityRange(30, 39, affKey), RANGE_STATUS_MOVED);
        map.updateRangeStatus(new IgfsFileAffinityRange(10, 19, affKey), RANGE_STATUS_MOVED);
        map.updateRangeStatus(new IgfsFileAffinityRange(70, 79, affKey), RANGE_STATUS_MOVED);

        ranges = map.ranges();

        assertEquals(RANGE_STATUS_MOVED, ranges.get(0).status());
        assertEquals(RANGE_STATUS_MOVED, ranges.get(1).status());
        assertEquals(RANGE_STATUS_INITIAL, ranges.get(2).status());
        assertEquals(RANGE_STATUS_MOVED, ranges.get(3).status());

        // Middle, first, last.
        map.deleteRange(new IgfsFileAffinityRange(30, 39, affKey));
        map.deleteRange(new IgfsFileAffinityRange(10, 19, affKey));
        map.deleteRange(new IgfsFileAffinityRange(70, 79, affKey));

        ranges = map.ranges();

        assertEquals(1, ranges.size());
        assertEquals(RANGE_STATUS_INITIAL, ranges.get(0).status());
        assertTrue(ranges.get(0).regionEqual(new IgfsFileAffinityRange(50, 59, affKey)));
    }

    /**
     * @throws Exception If failed.
     */
    @Test
    public void testRangeUpdate2() throws Exception {
        IgfsFileMap map = new IgfsFileMap();

        IgniteUuid affKey = IgniteUuid.randomUuid();

        for (int i = 0; i < 4; i++)
            map.addRange(new IgfsFileAffinityRange(i * 20 + 10, i * 20 + 19, affKey));

        // Middle, first, last.
        map.updateRangeStatus(new IgfsFileAffinityRange(30, 35, affKey), RANGE_STATUS_MOVING);
        map.updateRangeStatus(new IgfsFileAffinityRange(10, 15, affKey), RANGE_STATUS_MOVING);
        map.updateRangeStatus(new IgfsFileAffinityRange(70, 75, affKey), RANGE_STATUS_MOVING);

        List<IgfsFileAffinityRange> ranges = map.ranges();

        assertEquals(7, ranges.size());

        int idx = 0;
        assertTrue(ranges.get(idx).regionEqual(new IgfsFileAffinityRange(10, 15, affKey)));
        assertEquals(RANGE_STATUS_MOVING, ranges.get(idx).status());
        idx++;

        assertTrue(ranges.get(idx).regionEqual(new IgfsFileAffinityRange(16, 19, affKey)));
        assertEquals(RANGE_STATUS_INITIAL, ranges.get(idx).status());
        idx++;

        assertTrue(ranges.get(idx).regionEqual(new IgfsFileAffinityRange(30, 35, affKey)));
        assertEquals(RANGE_STATUS_MOVING, ranges.get(idx).status());
        idx++;

        assertTrue(ranges.get(idx).regionEqual(new IgfsFileAffinityRange(36, 39, affKey)));
        assertEquals(RANGE_STATUS_INITIAL, ranges.get(idx).status());
        idx++;

        assertTrue(ranges.get(idx).regionEqual(new IgfsFileAffinityRange(50, 59, affKey)));
        assertEquals(RANGE_STATUS_INITIAL, ranges.get(idx).status());
        idx++;

        assertTrue(ranges.get(idx).regionEqual(new IgfsFileAffinityRange(70, 75, affKey)));
        assertEquals(RANGE_STATUS_MOVING, ranges.get(idx).status());
        idx++;

        assertTrue(ranges.get(idx).regionEqual(new IgfsFileAffinityRange(76, 79, affKey)));
        assertEquals(RANGE_STATUS_INITIAL, ranges.get(idx).status());

        // Middle, first, last.
        map.updateRangeStatus(new IgfsFileAffinityRange(30, 35, affKey), RANGE_STATUS_MOVED);
        map.updateRangeStatus(new IgfsFileAffinityRange(10, 15, affKey), RANGE_STATUS_MOVED);
        map.updateRangeStatus(new IgfsFileAffinityRange(70, 75, affKey), RANGE_STATUS_MOVED);

        idx = 0;
        assertTrue(ranges.get(idx).regionEqual(new IgfsFileAffinityRange(10, 15, affKey)));
        assertEquals(RANGE_STATUS_MOVED, ranges.get(idx).status());
        idx++;

        assertTrue(ranges.get(idx).regionEqual(new IgfsFileAffinityRange(16, 19, affKey)));
        assertEquals(RANGE_STATUS_INITIAL, ranges.get(idx).status());
        idx++;

        assertTrue(ranges.get(idx).regionEqual(new IgfsFileAffinityRange(30, 35, affKey)));
        assertEquals(RANGE_STATUS_MOVED, ranges.get(idx).status());
        idx++;

        assertTrue(ranges.get(idx).regionEqual(new IgfsFileAffinityRange(36, 39, affKey)));
        assertEquals(RANGE_STATUS_INITIAL, ranges.get(idx).status());
        idx++;

        assertTrue(ranges.get(idx).regionEqual(new IgfsFileAffinityRange(50, 59, affKey)));
        assertEquals(RANGE_STATUS_INITIAL, ranges.get(idx).status());
        idx++;

        assertTrue(ranges.get(idx).regionEqual(new IgfsFileAffinityRange(70, 75, affKey)));
        assertEquals(RANGE_STATUS_MOVED, ranges.get(idx).status());
        idx++;

        assertTrue(ranges.get(idx).regionEqual(new IgfsFileAffinityRange(76, 79, affKey)));
        assertEquals(RANGE_STATUS_INITIAL, ranges.get(idx).status());

        // Middle, first, last.
        map.deleteRange(new IgfsFileAffinityRange(30, 35, affKey));
        map.deleteRange(new IgfsFileAffinityRange(10, 15, affKey));
        map.deleteRange(new IgfsFileAffinityRange(70, 75, affKey));

        ranges = map.ranges();

        assertEquals(4, ranges.size());

        assertEquals(RANGE_STATUS_INITIAL, ranges.get(0).status());
        assertTrue(ranges.get(0).regionEqual(new IgfsFileAffinityRange(16, 19, affKey)));

        assertEquals(RANGE_STATUS_INITIAL, ranges.get(1).status());
        assertTrue(ranges.get(1).regionEqual(new IgfsFileAffinityRange(36, 39, affKey)));

        assertEquals(RANGE_STATUS_INITIAL, ranges.get(2).status());
        assertTrue(ranges.get(2).regionEqual(new IgfsFileAffinityRange(50, 59, affKey)));

        assertEquals(RANGE_STATUS_INITIAL, ranges.get(3).status());
        assertTrue(ranges.get(3).regionEqual(new IgfsFileAffinityRange(76, 79, affKey)));
    }

    /**
     * @throws Exception If failed.
     */
    @Test
    public void testInvalidRangeUpdates() throws Exception {
        final IgfsFileMap map = new IgfsFileMap();

        final IgniteUuid affKey1 = IgniteUuid.randomUuid();
        final IgniteUuid affKey2 = IgniteUuid.randomUuid();

        map.addRange(new IgfsFileAffinityRange(10, 19, affKey1));
        map.addRange(new IgfsFileAffinityRange(30, 39, affKey1));

        GridTestUtils.assertThrows(log, new Callable<Object>() {
            @Override public Object call() throws Exception {
                map.updateRangeStatus(new IgfsFileAffinityRange(0, 5, affKey1), RANGE_STATUS_MOVING);

                return null;
            }
        }, IgfsInvalidRangeException.class, null);

        GridTestUtils.assertThrows(log, new Callable<Object>() {
            @Override public Object call() throws Exception {
                map.updateRangeStatus(new IgfsFileAffinityRange(15, 19, affKey1), RANGE_STATUS_MOVING);

                return null;
            }
        }, IgfsInvalidRangeException.class, null);

        GridTestUtils.assertThrows(log, new Callable<Object>() {
            @Override public Object call() throws Exception {
                map.updateRangeStatus(new IgfsFileAffinityRange(10, 19, affKey2), RANGE_STATUS_MOVING);

                return null;
            }
        }, AssertionError.class, null);

        GridTestUtils.assertThrows(log, new Callable<Object>() {
            @Override public Object call() throws Exception {
                map.updateRangeStatus(new IgfsFileAffinityRange(10, 22, affKey1), RANGE_STATUS_MOVING);

                return null;
            }
        }, AssertionError.class, null);

        assertEquals(2, map.ranges().size());
    }

    /**
     * @throws Exception If failed.
     */
    @Test
    public void testRangeSplit() throws Exception {
        IgniteUuid affKey = IgniteUuid.randomUuid();

        IgfsFileAffinityRange range = new IgfsFileAffinityRange(0, 9999, affKey);

        Collection<IgfsFileAffinityRange> split = range.split(10000);

        assertEquals(1, split.size());
        assertTrue(range.regionEqual(F.first(split)));

        split = range.split(5000);

        assertEquals(2, split.size());

        Iterator<IgfsFileAffinityRange> it = split.iterator();

        IgfsFileAffinityRange part = it.next();

        assertTrue(part.regionEqual(new IgfsFileAffinityRange(0, 4999, affKey)));

        part = it.next();

        assertTrue(part.regionEqual(new IgfsFileAffinityRange(5000, 9999, affKey)));

        split = range.split(3000);

        assertEquals(4, split.size());

        it = split.iterator();

        part = it.next();

        assertTrue(part.regionEqual(new IgfsFileAffinityRange(0, 2999, affKey)));

        part = it.next();

        assertTrue(part.regionEqual(new IgfsFileAffinityRange(3000, 5999, affKey)));

        part = it.next();

        assertTrue(part.regionEqual(new IgfsFileAffinityRange(6000, 8999, affKey)));

        part = it.next();

        assertTrue(part.regionEqual(new IgfsFileAffinityRange(9000, 9999, affKey)));
    }
}
