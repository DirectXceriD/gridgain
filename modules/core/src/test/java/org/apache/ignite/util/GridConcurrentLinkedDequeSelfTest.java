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

package org.apache.ignite.util;

import java.util.Iterator;
import org.apache.ignite.testframework.junits.common.GridCommonAbstractTest;
import org.jsr166.ConcurrentLinkedDeque8;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.jsr166.ConcurrentLinkedDeque8.Node;

/**
 * Tests for {@link org.jsr166.ConcurrentLinkedDeque8}.
 */
@RunWith(JUnit4.class)
public class GridConcurrentLinkedDequeSelfTest extends GridCommonAbstractTest {
    /**
     * @throws Exception If failed.
     */
    @Test
    public void testPoll() throws Exception {
        ConcurrentLinkedDeque8<Integer> deque = new ConcurrentLinkedDeque8<>();

        deque.add(1);
        deque.add(2);
        deque.add(3);

        assert !deque.isEmpty();

        checkSize(deque, 3);

        // Poll 1.
        assertEquals(Integer.valueOf(1), deque.poll());

        assert !deque.isEmpty();

        checkSize(deque, 2);

        // Poll 2.
        assertEquals(Integer.valueOf(2), deque.poll());

        assert !deque.isEmpty();

        checkSize(deque, 1);

        // Poll 3.
        assertEquals(Integer.valueOf(3), deque.poll());

        assert deque.isEmpty();

        checkSize(deque, 0);
    }

    /**
     *
     */
    @Test
    public void testUnlink() {
        ConcurrentLinkedDeque8<Integer> deque = new ConcurrentLinkedDeque8<>();

        Node<Integer> n1 = deque.addx(1);
        Node<Integer> n2 = deque.addx(2);
        Node<Integer> n3 = deque.addx(3);
        Node<Integer> n4 = deque.addx(4);
        Node<Integer> n5 = deque.addx(5);

        deque.unlinkx(n2);

        checkSize(deque, 4);

        // Double unlinkx() call.
        deque.unlinkx(n2);

        checkSize(deque, 4);

        Iterator<Integer> iter = deque.iterator();

        boolean hasNext = iter.hasNext();

        assert hasNext;

        Integer next = iter.next();

        assert next == 1;

        iter.remove();

        // Iterator should have set item to null.
        assert n1.item() == null;

        checkSize(deque, 3);

        // Double unlinkx() call.
        deque.unlinkx(n1);

        checkSize(deque, 3);

        deque.unlinkx(n3);
        deque.unlinkx(n4);
        deque.unlinkx(n5);

        checkSize(deque, 0);
    }

    /**
     *
     */
    @Test
    public void testEmptyDeque() {
        ConcurrentLinkedDeque8<Integer> deque = new ConcurrentLinkedDeque8<>();

        assert deque.poll() == null;
        assert deque.pollFirst() == null;
        assert deque.pollLast() == null;

        assert deque.peek() == null;
        assert deque.peekFirst() == null;
        assert deque.peekLast() == null;

        checkSize(deque, 0);
    }

    /**
     * @param q Deque.
     * @param expSize Expected size.
     */
    @SuppressWarnings({"ForLoopReplaceableByForEach"})
    private <T> void checkSize(ConcurrentLinkedDeque8<T> q, int expSize) {
        int actualSize = 0;

        for (Iterator<T> iter = q.iterator(); iter.hasNext();) {
            iter.next();

            actualSize++;
        }

        assertEquals(expSize, actualSize);

        actualSize = 0;

        for (Iterator<T> iter = q.iterator(); iter.hasNext();) {
            iter.next();

            actualSize++;
        }

        assertEquals(expSize, actualSize);

        assertEquals(expSize, q.size());

        assertEquals(expSize, q.sizex());

        if (expSize > 0) {
            assert !q.isEmpty();

            assert !q.isEmptyx();
        }
        else {
            assert q.isEmpty();

            assert q.isEmptyx();
        }
    }

    /**
     *
     */
    @Test
    public void testUnlinkWithIterator() {
        ConcurrentLinkedDeque8<Integer> q = new ConcurrentLinkedDeque8<>();

        q.add(1);
        Node<Integer> n2 = q.addx(2);
        Node<Integer> n3 = q.addx(3);
        Node<Integer> n4 = q.addx(4);
        Node<Integer> n5 = q.addx(5);
        q.add(6);

        Iterator<Integer> it = q.iterator();

        assertTrue(it.hasNext());
        assertEquals(1, it.next().intValue());

        assertTrue(it.hasNext());
        assertEquals(2, it.next().intValue());

        assertTrue(it.hasNext());
        assertEquals(3, it.next().intValue());

        q.unlinkx(n2);
        q.unlinkx(n3);
        q.unlinkx(n4);
        q.unlinkx(n5);

        assertTrue(it.hasNext());
        assertEquals(4, it.next().intValue());

        assertTrue(it.hasNext());
        assertEquals(6, it.next().intValue());
    }

    /**
     *
     */
    @Test
    public void testUnlinkLastWithIterator() {
        ConcurrentLinkedDeque8<Integer> q = new ConcurrentLinkedDeque8<>();

        q.add(1);
        q.addx(2);
        Node<Integer> n3 = q.addx(3);

        Iterator<Integer> it = q.iterator();

        assertTrue(it.hasNext());
        assertEquals(1, it.next().intValue());

        q.unlinkx(n3);

        assertTrue(it.hasNext());
        assertEquals(2, it.next().intValue());

        assertFalse(it.hasNext());
    }
}
