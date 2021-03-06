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

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.locks.ReadWriteLock;
import org.apache.ignite.IgniteLogger;
import org.apache.ignite.IgniteSystemProperties;
import static org.apache.ignite.IgniteSystemProperties.IGNITE_TO_STRING_COLLECTION_LIMIT;
import static org.apache.ignite.IgniteSystemProperties.IGNITE_TO_STRING_MAX_LENGTH;
import org.apache.ignite.internal.util.typedef.internal.S;
import org.apache.ignite.testframework.junits.common.GridCommonAbstractTest;
import org.apache.ignite.testframework.junits.common.GridCommonTest;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests for {@link GridToStringBuilder}.
 */
@GridCommonTest(group = "Utils")
@RunWith(JUnit4.class)
public class GridToStringBuilderSelfTest extends GridCommonAbstractTest {
    /**
     * @throws Exception If failed.
     */
    @Test
    public void testToString() throws Exception {
        TestClass1 obj = new TestClass1();

        IgniteLogger log = log();

        log.info(obj.toStringManual());
        log.info(obj.toStringAutomatic());

        assertEquals (obj.toStringManual(), obj.toStringAutomatic());
    }

    /**
     * @throws Exception If failed.
     */
    @Test
    public void testToStringWithAdditions() throws Exception {
        TestClass1 obj = new TestClass1();

        IgniteLogger log = log();

        String manual = obj.toStringWithAdditionalManual();
        log.info(manual);

        String automatic = obj.toStringWithAdditionalAutomatic();
        log.info(automatic);

        assertEquals(manual, automatic);
    }

    /**
     * @throws Exception If failed.
     */
    @Test
    public void testToStringCheckSimpleListRecursionPrevention() throws Exception {
        ArrayList<Object> list1 = new ArrayList<>();
        ArrayList<Object> list2 = new ArrayList<>();

        list2.add(list1);
        list1.add(list2);


        GridToStringBuilder.toString(ArrayList.class, list1);
        GridToStringBuilder.toString(ArrayList.class, list2);
    }

    /**
     * @throws Exception If failed.
     */
    @Ignore("https://issues.apache.org/jira/browse/IGNITE-602")
    @Test
    public void testToStringCheckAdvancedRecursionPrevention() throws Exception {
        ArrayList<Object> list1 = new ArrayList<>();
        ArrayList<Object> list2 = new ArrayList<>();

        list2.add(list1);
        list1.add(list2);

        GridToStringBuilder.toString(ArrayList.class, list1, "name", list2);
        GridToStringBuilder.toString(ArrayList.class, list2, "name", list1);
    }

    /**
     * JUnit.
     */
    @Test
    public void testToStringPerformance() {
        TestClass1 obj = new TestClass1();

        IgniteLogger log = log();

        // Warm up.
        obj.toStringAutomatic();

        long start = System.currentTimeMillis();

        for (int i = 0; i < 100000; i++)
            obj.toStringManual();

        log.info("Manual toString() took: " + (System.currentTimeMillis() - start) + "ms");

        start = System.currentTimeMillis();

        for (int i = 0; i < 100000; i++)
            obj.toStringAutomatic();

        log.info("Automatic toString() took: " + (System.currentTimeMillis() - start) + "ms");
    }

    /**
     * Test array print.
     * @param v value to get array class and fill array.
     * @param limit value of IGNITE_TO_STRING_COLLECTION_LIMIT.
     * @throws Exception if failed.
     */
    private <T, V> void testArr(V v, int limit) throws Exception {
        T[] arrOf = (T[]) Array.newInstance(v.getClass(), limit + 1);
        Arrays.fill(arrOf, v);
        T[] arr = Arrays.copyOf(arrOf, limit);

        String arrStr = GridToStringBuilder.arrayToString(arr.getClass(), arr);
        String arrOfStr = GridToStringBuilder.arrayToString(arrOf.getClass(), arrOf);

        // Simulate overflow
        StringBuilder resultSB = new StringBuilder(arrStr);
            resultSB.deleteCharAt(resultSB.length()-1);
            resultSB.append("... and ").append(arrOf.length - limit).append(" more]");
            arrStr = resultSB.toString();

        assertTrue("Collection limit error in array of type " + arrOf.getClass().getName()
            + " error, normal arr: <" + arrStr + ">, overflowed arr: <" + arrOfStr + ">", arrStr.equals(arrOfStr));
    }

    /**
     * @throws Exception If failed.
     */
    @Test
    public void testToStringCollectionLimits() throws Exception {
        int limit = IgniteSystemProperties.getInteger(IGNITE_TO_STRING_COLLECTION_LIMIT, 100);

        Object vals[] = new Object[] {Byte.MIN_VALUE, Boolean.TRUE, Short.MIN_VALUE, Integer.MIN_VALUE, Long.MIN_VALUE,
            Float.MIN_VALUE, Double.MIN_VALUE, Character.MIN_VALUE, new TestClass1()};
        for (Object val : vals)
            testArr(val, limit);

        byte[] byteArr = new byte[1];
        byteArr[0] = 1;
        assertEquals(Arrays.toString(byteArr), GridToStringBuilder.arrayToString(byteArr.getClass(), byteArr));
        byteArr = Arrays.copyOf(byteArr, 101);
        assertTrue("Can't find \"... and 1 more\" in overflowed array string!",
            GridToStringBuilder.arrayToString(byteArr.getClass(), byteArr).contains("... and 1 more"));

        boolean[] boolArr = new boolean[1];
        boolArr[0] = true;
        assertEquals(Arrays.toString(boolArr), GridToStringBuilder.arrayToString(boolArr.getClass(), boolArr));
        boolArr = Arrays.copyOf(boolArr, 101);
        assertTrue("Can't find \"... and 1 more\" in overflowed array string!",
            GridToStringBuilder.arrayToString(boolArr.getClass(), boolArr).contains("... and 1 more"));

        short[] shortArr = new short[1];
        shortArr[0] = 100;
        assertEquals(Arrays.toString(shortArr), GridToStringBuilder.arrayToString(shortArr.getClass(), shortArr));
        shortArr = Arrays.copyOf(shortArr, 101);
        assertTrue("Can't find \"... and 1 more\" in overflowed array string!",
            GridToStringBuilder.arrayToString(shortArr.getClass(), shortArr).contains("... and 1 more"));

        int[] intArr = new int[1];
        intArr[0] = 10000;
        assertEquals(Arrays.toString(intArr), GridToStringBuilder.arrayToString(intArr.getClass(), intArr));
        intArr = Arrays.copyOf(intArr, 101);
        assertTrue("Can't find \"... and 1 more\" in overflowed array string!",
            GridToStringBuilder.arrayToString(intArr.getClass(), intArr).contains("... and 1 more"));

        long[] longArr = new long[1];
        longArr[0] = 10000000;
        assertEquals(Arrays.toString(longArr), GridToStringBuilder.arrayToString(longArr.getClass(), longArr));
        longArr = Arrays.copyOf(longArr, 101);
        assertTrue("Can't find \"... and 1 more\" in overflowed array string!",
            GridToStringBuilder.arrayToString(longArr.getClass(), longArr).contains("... and 1 more"));

        float[] floatArr = new float[1];
        floatArr[0] = 1.f;
        assertEquals(Arrays.toString(floatArr), GridToStringBuilder.arrayToString(floatArr.getClass(), floatArr));
        floatArr = Arrays.copyOf(floatArr, 101);
        assertTrue("Can't find \"... and 1 more\" in overflowed array string!",
            GridToStringBuilder.arrayToString(floatArr.getClass(), floatArr).contains("... and 1 more"));

        double[] doubleArr = new double[1];
        doubleArr[0] = 1.;
        assertEquals(Arrays.toString(doubleArr), GridToStringBuilder.arrayToString(doubleArr.getClass(), doubleArr));
        doubleArr = Arrays.copyOf(doubleArr, 101);
        assertTrue("Can't find \"... and 1 more\" in overflowed array string!",
            GridToStringBuilder.arrayToString(doubleArr.getClass(), doubleArr).contains("... and 1 more"));

        char[] charArr = new char[1];
        charArr[0] = 'a';
        assertEquals(Arrays.toString(charArr), GridToStringBuilder.arrayToString(charArr.getClass(), charArr));
        charArr = Arrays.copyOf(charArr, 101);
        assertTrue("Can't find \"... and 1 more\" in overflowed array string!",
            GridToStringBuilder.arrayToString(charArr.getClass(), charArr).contains("... and 1 more"));

        Map<String, String> strMap = new TreeMap<>();
        List<String> strList = new ArrayList<>(limit+1);

        TestClass1 testClass = new TestClass1();
        testClass.strMap = strMap;
        testClass.strListIncl = strList;

        for (int i = 0; i < limit; i++) {
            strMap.put("k" + i, "v");
            strList.add("e");
        }
        String testClassStr = GridToStringBuilder.toString(TestClass1.class, testClass);

        strMap.put("kz", "v"); // important to add last element in TreeMap here
        strList.add("e");

        String testClassStrOf = GridToStringBuilder.toString(TestClass1.class, testClass);

        String testClassStrOfR = testClassStrOf.replaceAll("... and 1 more","");

        assertTrue("Collection limit error in Map or List, normal: <" + testClassStr + ">, overflowed: <"
            +"testClassStrOf", testClassStr.length() == testClassStrOfR.length());

    }

    /**
     * @throws Exception If failed.
     */
    @Test
    public void testToStringSizeLimits() throws Exception {
        int limit = IgniteSystemProperties.getInteger(IGNITE_TO_STRING_MAX_LENGTH, 10_000);
        int tailLen = limit / 10 * 2;
        StringBuilder sb = new StringBuilder(limit + 10);
        for (int i = 0; i < limit - 100; i++) {
            sb.append('a');
        }
        String actual = GridToStringBuilder.toString(TestClass2.class, new TestClass2(sb.toString()));
        String expected = "TestClass2 [str=" + sb.toString() + ", nullArr=null]";
        assertEquals(expected, actual);

        for (int i = 0; i < 110; i++) {
            sb.append('b');
        }
        actual = GridToStringBuilder.toString(TestClass2.class, new TestClass2(sb.toString()));
        expected = "TestClass2 [str=" + sb.toString() + ", nullArr=null]";
        assertEquals(expected.substring(0, limit - tailLen), actual.substring(0, limit - tailLen));
        assertEquals(expected.substring(expected.length() - tailLen), actual.substring(actual.length() - tailLen));
        assertTrue(actual.contains("... and"));
        assertTrue(actual.contains("skipped ..."));
    }

    /**
     * Test class.
     */
    private static class TestClass1 {
        /** */
        @SuppressWarnings("unused")
        @GridToStringOrder(0)
        private String id = "1234567890";

        /** */
        @SuppressWarnings("unused")
        private int intVar;

        /** */
        @SuppressWarnings("unused")
        @GridToStringInclude(sensitive = true)
        private long longVar;

        /** */
        @SuppressWarnings("unused")
        @GridToStringOrder(1)
        private final UUID uuidVar = UUID.randomUUID();

        /** */
        @SuppressWarnings("unused")
        private boolean boolVar;

        /** */
        @SuppressWarnings("unused")
        private byte byteVar;

        /** */
        @SuppressWarnings("unused")
        private String name = "qwertyuiopasdfghjklzxcvbnm";

        /** */
        @SuppressWarnings("unused")
        private final Integer finalInt = 2;

        /** */
        @SuppressWarnings("unused")
        private List<String> strList;

        /** */
        @SuppressWarnings("unused")
        @GridToStringInclude
        private Map<String, String> strMap;

        /** */
        @SuppressWarnings("unused")
        @GridToStringInclude
        private List<String> strListIncl;


        /** */
        @SuppressWarnings("unused")
        private final Object obj = new Object();

        /** */
        @SuppressWarnings("unused")
        private ReadWriteLock lock;

        /**
         * @return Manual string.
         */
        String toStringManual() {
            StringBuilder buf = new StringBuilder();

            buf.append(getClass().getSimpleName()).append(" [");

            buf.append("id=").append(id).append(", ");
            buf.append("uuidVar=").append(uuidVar).append(", ");
            buf.append("intVar=").append(intVar).append(", ");
            if (S.INCLUDE_SENSITIVE)
                buf.append("longVar=").append(longVar).append(", ");
            buf.append("boolVar=").append(boolVar).append(", ");
            buf.append("byteVar=").append(byteVar).append(", ");
            buf.append("name=").append(name).append(", ");
            buf.append("finalInt=").append(finalInt).append(", ");
            buf.append("strMap=").append(strMap).append(", ");
            buf.append("strListIncl=").append(strListIncl);

            buf.append("]");

            return buf.toString();
        }

        /**
         * @return Automatic string.
         */
        String toStringAutomatic() {
            return S.toString(TestClass1.class, this);
        }

        /**
         * @return Automatic string with additional parameters.
         */
        String toStringWithAdditionalAutomatic() {
            return S.toString(TestClass1.class, this, "newParam1", 1, false, "newParam2", 2, true);
        }

        /**
         * @return Manual string with additional parameters.
         */
        String toStringWithAdditionalManual() {
            StringBuilder s = new StringBuilder(toStringManual());
            s.setLength(s.length() - 1);
            s.append(", newParam1=").append(1);
            if (S.INCLUDE_SENSITIVE)
                s.append(", newParam2=").append(2);
            s.append(']');
            return s.toString();
        }
    }

    /**
     *
     */
    private static class TestClass2{
        /** */
        @SuppressWarnings("unused")
        @GridToStringInclude
        private String str;

        /** */
        @GridToStringInclude
        private Object[] nullArr;

        /**
         * @param str String.
         */
        public TestClass2(String str) {
            this.str = str;
        }
    }
}
