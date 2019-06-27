/*
 ***************************************************************************************
 *  Copyright (C) 2006 EsperTech, Inc. All rights reserved.                            *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 ***************************************************************************************
 */
package com.espertech.esper.common.internal.util;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.internal.event.map.MapEventBean;
import junit.framework.TestCase;

import java.util.*;

import static com.espertech.esper.common.internal.util.CollectionUtil.*;

public class TestCollectionUtil extends TestCase {

    public void testArrayAllNull() {
        assertTrue(isArrayAllNull(null));
        assertTrue(isArrayAllNull(new Object[0]));
        assertTrue(isArrayAllNull(new Object[] {null}));
        assertTrue(isArrayAllNull(new Object[] {null, null}));

        assertFalse(isArrayAllNull(new Object[] {"a", null}));
        assertFalse(isArrayAllNull(new Object[] {null, "b"}));
    }

    public void testArraySameReferences() {
        String a = "a";
        String b = "b";

        assertTrue(isArraySameReferences(new Object[0], new Object[0]));
        assertTrue(isArraySameReferences(new Object[] {a}, new Object[] {a}));
        assertTrue(isArraySameReferences(new Object[] {a, b}, new Object[] {a, b}));

        assertFalse(isArraySameReferences(new Object[] {}, new Object[] {b}));
        assertFalse(isArraySameReferences(new Object[] {a}, new Object[] {}));
        assertFalse(isArraySameReferences(new Object[] {a}, new Object[] {b}));
        assertFalse(isArraySameReferences(new Object[] {a}, new Object[] {b, a}));
        assertFalse(isArraySameReferences(new Object[] {a, b}, new Object[] {a}));
        assertFalse(isArraySameReferences(new Object[] {a, b}, new Object[] {b, a}));
        assertFalse(isArraySameReferences(new Object[] {new String(new char[] {'a'})}, new Object[] {new String(new char[] {'a'})}));
    }

    public void testGetMapValueChecked() {
        assertNull(getMapValueChecked(null, "x"));
        assertNull(getMapValueChecked("b", "x"));
        assertNull(getMapValueChecked(Collections.emptyMap(), "x"));
        assertEquals("y", getMapValueChecked(Collections.singletonMap("x", "y"), "x"));
    }

    public void testGetMapKeyExistsChecked() {
        assertFalse(getMapKeyExistsChecked(null, "x"));
        assertFalse(getMapKeyExistsChecked("b", "x"));
        assertFalse(getMapKeyExistsChecked(Collections.emptyMap(), "x"));
        assertTrue(getMapKeyExistsChecked(Collections.singletonMap("x", "y"), "x"));
    }

    public void testSubdivide() {
        runAssertionSubdivide3("", "");
        runAssertionSubdivide3("a", "a");
        runAssertionSubdivide3("a,b", "a,b");
        runAssertionSubdivide3("a,b,c", "a,b,c");
        runAssertionSubdivide3("a,b,c,d", "a,b,c|d");
        runAssertionSubdivide3("a,b,c,d,e", "a,b,c|d,e");
        runAssertionSubdivide3("a,b,c,d,e,f", "a,b,c|d,e,f");
        runAssertionSubdivide3("a,b,c,d,e,f,g", "a,b,c|d,e,f|g");
        runAssertionSubdivide3("a,b,c,d,e,f,g,h", "a,b,c|d,e,f|g,h");
        runAssertionSubdivide3("a,b,c,d,e,f,g,h,i", "a,b,c|d,e,f|g,h,i");
        runAssertionSubdivide3("a,b,c,d,e,f,g,h,i,j", "a,b,c|d,e,f|g,h,i|j");

        runAssertionSubdivide("", "", 2);
        runAssertionSubdivide("a", "a", 2);
        runAssertionSubdivide("a,b", "a,b", 2);
        runAssertionSubdivide("a,b,c", "a,b|c", 2);
        runAssertionSubdivide("a,b,c,d", "a,b|c,d", 2);
        runAssertionSubdivide("a,b,c,d,e", "a,b|c,d|e", 2);

        runAssertionSubdivide("", "", 1);
        runAssertionSubdivide("a", "a", 1);
        runAssertionSubdivide("a,b", "a|b", 1);
        runAssertionSubdivide("a,b,c", "a|b|c", 1);
    }

    public void testArrayExpandSingle() {
        runAssertionExpandSingle("a", "", "a");
        runAssertionExpandSingle("a,b", "a", "b");
        runAssertionExpandSingle("a,b,c", "a,b", "c");
        runAssertionExpandSingle("a,b,c,d", "a,b,c", "d");
    }

    public void testArrayExpandCollectionAndArray() {
        runAssertionExpandColl("", "", "");
        runAssertionExpandColl("a,b", "a", "b");
        runAssertionExpandColl("a,b", "", "a,b");
        runAssertionExpandColl("b", "", "b");
        runAssertionExpandColl("a,b,c", "a,b", "c");
        runAssertionExpandColl("a,b,c", "", "a,b,c");
        runAssertionExpandColl("a,b,c", "a", "b,c");
        runAssertionExpandColl("a,b,c,d", "a,b,c", "d");
    }

    public void testArrayShrink() {
        runAssertionShrink("a,c", "a,b,c", 1);
        runAssertionShrink("b,c", "a,b,c", 0);
        runAssertionShrink("a,b", "a,b,c", 2);
        runAssertionShrink("a", "a,b", 1);
        runAssertionShrink("b", "a,b", 0);
        runAssertionShrink("", "a", 0);
    }

    private void runAssertionShrink(String expected, String existing, int index) {
        String[] expectedArr = expected.length() == 0 ? new String[0] : expected.split(",");
        String[] existingArr = existing.length() == 0 ? new String[0] : existing.split(",");
        String[] resultAddColl = (String[]) CollectionUtil.arrayShrinkRemoveSingle(existingArr, index);
        EPAssertionUtil.assertEqualsExactOrder(expectedArr, resultAddColl);
    }

    private void runAssertionExpandColl(String expected, String existing, String coll) {
        String[] expectedArr = expected.length() == 0 ? new String[0] : expected.split(",");
        String[] existingArr = existing.length() == 0 ? new String[0] : existing.split(",");
        Collection<String> addCollection = Arrays.asList(coll.length() == 0 ? new String[0] : coll.split(","));
        String[] resultAddColl = (String[]) CollectionUtil.arrayExpandAddElements(existingArr, addCollection);
        EPAssertionUtil.assertEqualsExactOrder(expectedArr, resultAddColl);

        String[] resultAddArr = (String[]) CollectionUtil.arrayExpandAddElements(existingArr, addCollection.toArray());
        EPAssertionUtil.assertEqualsExactOrder(expectedArr, resultAddArr);
    }

    private void runAssertionExpandSingle(String expected, String existing, String single) {
        String[] expectedArr = expected.length() == 0 ? new String[0] : expected.split(",");
        String[] existingArr = existing.length() == 0 ? new String[0] : existing.split(",");
        String[] result = (String[]) CollectionUtil.arrayExpandAddSingle(existingArr, single);
        EPAssertionUtil.assertEqualsExactOrder(expectedArr, result);
    }

    public void testAddArraySetSemantics() {

        EventBean[] e = new EventBean[10];
        for (int i = 0; i < e.length; i++) {
            e[i] = new MapEventBean(null);
        }
        assertFalse(e[0].equals(e[1]));

        Object[][] testData = new Object[][]{
                {new EventBean[]{}, new EventBean[]{}, "p2"},
                {new EventBean[]{}, new EventBean[]{e[0], e[1]}, "p2"},
                {new EventBean[]{e[0]}, new EventBean[]{}, "p1"},
                {new EventBean[]{e[0]}, new EventBean[]{e[0]}, "p1"},
                {new EventBean[]{e[0]}, new EventBean[]{e[1]}, new EventBean[]{e[0], e[1]}},
                {new EventBean[]{e[0], e[1]}, new EventBean[]{e[1]}, "p1"},
                {new EventBean[]{e[0], e[1]}, new EventBean[]{e[0]}, "p1"},
                {new EventBean[]{e[0]}, new EventBean[]{e[0], e[1]}, "p2"},
                {new EventBean[]{e[1]}, new EventBean[]{e[0], e[1]}, "p2"},
                {new EventBean[]{e[2]}, new EventBean[]{e[0], e[1]}, new EventBean[]{e[0], e[1], e[2]}},
                {new EventBean[]{e[2], e[0]}, new EventBean[]{e[0], e[1]}, new EventBean[]{e[0], e[1], e[2]}},
                {new EventBean[]{e[2], e[0]}, new EventBean[]{e[0], e[1], e[2]}, new EventBean[]{e[0], e[1], e[2]}}
        };

        for (int i = 0; i < testData.length; i++) {
            EventBean[] p1 = (EventBean[]) testData[i][0];
            EventBean[] p2 = (EventBean[]) testData[i][1];
            Object expectedObj = testData[i][2];

            Object result = CollectionUtil.addArrayWithSetSemantics(p1, p2);

            if (expectedObj.equals("p1")) {
                assertTrue(result == p1);
            } else if (expectedObj.equals("p2")) {
                assertTrue(result == p2);
            } else {
                EventBean[] resultArray = (EventBean[]) result;
                EventBean[] expectedArray = (EventBean[]) result;
                EPAssertionUtil.assertEqualsAnyOrder(resultArray, expectedArray);
            }
        }
    }

    public void testAddArray() {
        tryAddStringArr("b,a".split(","), CollectionUtil.addArrays(new String[]{"b"}, new String[]{"a"}));
        tryAddStringArr("a".split(","), CollectionUtil.addArrays(null, new String[]{"a"}));
        tryAddStringArr("b".split(","), CollectionUtil.addArrays(new String[]{"b"}, null));
        tryAddStringArr("a,b,c,d".split(","), CollectionUtil.addArrays(new String[]{"a", "b"}, new String[]{"c", "d"}));
        assertEquals(null, CollectionUtil.addArrays(null, null));

        Object result = CollectionUtil.addArrays(new int[]{1, 2}, new int[]{3, 4});
        EPAssertionUtil.assertEqualsExactOrder(new int[]{1, 2, 3, 4}, (int[]) result);

        try {
            CollectionUtil.addArrays("a", null);
            fail();
        } catch (IllegalArgumentException ex) {
            assertEquals("Parameter is not an array: a", ex.getMessage());
        }

        try {
            CollectionUtil.addArrays(null, "b");
            fail();
        } catch (IllegalArgumentException ex) {
            assertEquals("Parameter is not an array: b", ex.getMessage());
        }
    }

    private void tryAddStringArr(String[] expected, Object result) {
        assertTrue(result.getClass().isArray());
        assertEquals(String.class, result.getClass().getComponentType());
        EPAssertionUtil.assertEqualsExactOrder(expected, (String[]) result);
    }

    public void testCopySort() {
        Object[][] testdata = new Object[][]{
                {new String[]{"a", "b"}, new String[]{"a", "b"}},
                {new String[]{"b", "a"}, new String[]{"a", "b"}},
                {new String[]{"a"}, new String[]{"a"}},
                {new String[]{"c", "b", "a"}, new String[]{"a", "b", "c"}},
                {new String[0], new String[0]},
        };

        for (int i = 0; i < testdata.length; i++) {
            String[] expected = (String[]) testdata[i][1];
            String[] input = (String[]) testdata[i][0];
            String[] received = CollectionUtil.copySortArray(input);
            if (!Arrays.equals(expected, received)) {
                fail("Failed for input " + Arrays.toString(input) + " expected " + Arrays.toString(expected) + " received " + Arrays.toString(received));
            }
            assertNotSame(input, expected);
        }
    }

    public void testCompare() {
        Object[][] testdata = new Object[][]{
                {new String[]{"a", "b"}, new String[]{"a", "b"}, true},
                {new String[]{"a"}, new String[]{"a", "b"}, false},
                {new String[]{"a"}, new String[]{"a"}, true},
                {new String[]{"b"}, new String[]{"a"}, false},
                {new String[]{"b", "a"}, new String[]{"a", "b"}, true},
                {new String[]{"a", "b", "b"}, new String[]{"a", "b"}, false},
                {new String[]{"a", "b", "b"}, new String[]{"b", "a", "b"}, true},
                {new String[0], new String[0], true},
        };

        for (int i = 0; i < testdata.length; i++) {
            String[] left = (String[]) testdata[i][0];
            String[] right = (String[]) testdata[i][1];
            boolean expected = (Boolean) testdata[i][2];
            assertEquals("Failed for input " + Arrays.toString(left), expected, CollectionUtil.sortCompare(left, right));
            assertTrue(Arrays.equals(left, (String[]) testdata[i][0]));
            assertTrue(Arrays.equals(right, (String[]) testdata[i][1]));
        }
    }

    public void testToString() {
        Object[][] testdata = new Object[][]{
                {new String[]{"a", "b"}, "a, b"},
                {new String[]{"a"}, "a"},
                {new String[]{""}, ""},
                {new String[]{"", ""}, ""},
                {new String[]{null, "b"}, "b"},
                {new String[0], ""},
                {null, "null"}
        };

        for (int i = 0; i < testdata.length; i++) {
            String expected = (String) testdata[i][1];
            String[] input = (String[]) testdata[i][0];
            assertEquals("Failed for input " + Arrays.toString(input), expected, CollectionUtil.toString(toSet(input)));
        }
    }

    private Set<String> toSet(String[] arr) {
        if (arr == null) {
            return null;
        }
        if (arr.length == 0) {
            return new HashSet<String>();
        }
        Set<String> set = new LinkedHashSet<String>();
        for (String a : arr) {
            set.add(a);
        }
        return set;
    }

    private void runAssertionSubdivide3(String csv, String expected) {
        runAssertionSubdivide(csv, expected, 3);
    }

    private void runAssertionSubdivide(String csv, String expected, int size) {
        List<String> input = new ArrayList<>(Arrays.asList(csv.split(",")));
        List<List<String>> lists = CollectionUtil.subdivide(input, size);

        StringBuilder out = new StringBuilder();
        String delimiter = "";
        for (List<String> list : lists) {
            String items = String.join(",", list.toArray(new String[0]));
            out.append(delimiter);
            out.append(items);
            delimiter = "|";
        }

        assertEquals(expected, out.toString());
    }
}
