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
package com.espertech.esper.collection;

import junit.framework.TestCase;

public class TestSortedDoubleVector extends TestCase {
    private SortedDoubleVector vector;

    public void setUp() {
        vector = new SortedDoubleVector();
    }

    public void testAdd() {
        assertEquals(0, vector.size());

        vector.add(10);
        vector.add(0);
        vector.add(5);
        double[] expected = new double[]{0, 5, 10};
        compare(expected, vector);

        vector.add(10);
        vector.add(1);
        vector.add(5.5);
        expected = new double[]{0, 1, 5, 5.5, 10, 10};
        compare(expected, vector);

        vector.add(9);
        vector.add(2);
        vector.add(5.5);
        expected = new double[]{0, 1, 2, 5, 5.5, 5.5, 9, 10, 10};
        compare(expected, vector);
    }

    public void testRemove() {
        vector.add(5);
        vector.add(1);
        vector.add(0);
        vector.add(-1);
        vector.add(1);
        vector.add(0.5);
        double[] expected = new double[]{-1, 0, 0.5, 1, 1, 5};
        compare(expected, vector);

        vector.remove(1);
        expected = new double[]{-1, 0, 0.5, 1, 5};
        compare(expected, vector);

        vector.remove(-1);
        vector.add(5);
        expected = new double[]{0, 0.5, 1, 5, 5};
        compare(expected, vector);

        vector.remove(5);
        vector.remove(5);
        expected = new double[]{0, 0.5, 1};
        compare(expected, vector);

        vector.add(99);
        vector.remove(99);
        vector.remove(99);

        vector.add(Double.NaN);
        vector.remove(Double.NaN);
    }

    public void testFindInsertIndex() {
        assertEquals(-1, vector.findInsertIndex(1));

        // test distinct values, 10 to 80
        vector.getValues().add(10D);
        assertEquals(0, vector.findInsertIndex(1));
        assertEquals(0, vector.findInsertIndex(10));
        assertEquals(-1, vector.findInsertIndex(11));

        vector.getValues().add(20D);
        assertEquals(0, vector.findInsertIndex(1));
        assertEquals(0, vector.findInsertIndex(10));
        assertEquals(1, vector.findInsertIndex(11));
        assertEquals(1, vector.findInsertIndex(19));
        assertEquals(1, vector.findInsertIndex(20));
        assertEquals(-1, vector.findInsertIndex(21));

        vector.getValues().add(30D);
        assertEquals(0, vector.findInsertIndex(1));
        assertEquals(0, vector.findInsertIndex(10));
        assertEquals(1, vector.findInsertIndex(11));
        assertEquals(1, vector.findInsertIndex(19));
        assertEquals(1, vector.findInsertIndex(20));
        assertEquals(2, vector.findInsertIndex(21));
        assertEquals(2, vector.findInsertIndex(29));
        assertEquals(2, vector.findInsertIndex(30));
        assertEquals(-1, vector.findInsertIndex(31));

        vector.getValues().add(40D);
        assertEquals(0, vector.findInsertIndex(1));
        assertEquals(0, vector.findInsertIndex(10));
        assertEquals(1, vector.findInsertIndex(11));
        assertEquals(1, vector.findInsertIndex(19));
        assertEquals(1, vector.findInsertIndex(20));
        assertEquals(2, vector.findInsertIndex(21));
        assertEquals(2, vector.findInsertIndex(29));
        assertEquals(2, vector.findInsertIndex(30));
        assertEquals(3, vector.findInsertIndex(31));
        assertEquals(3, vector.findInsertIndex(39));
        assertEquals(3, vector.findInsertIndex(40));
        assertEquals(-1, vector.findInsertIndex(41));

        vector.getValues().add(50D);
        assertEquals(0, vector.findInsertIndex(1));
        assertEquals(0, vector.findInsertIndex(10));
        assertEquals(1, vector.findInsertIndex(11));
        assertEquals(1, vector.findInsertIndex(19));
        assertEquals(1, vector.findInsertIndex(20));
        assertEquals(2, vector.findInsertIndex(21));
        assertEquals(2, vector.findInsertIndex(29));
        assertEquals(2, vector.findInsertIndex(30));
        assertEquals(3, vector.findInsertIndex(31));
        assertEquals(3, vector.findInsertIndex(39));
        assertEquals(3, vector.findInsertIndex(40));
        assertEquals(4, vector.findInsertIndex(41));
        assertEquals(4, vector.findInsertIndex(49));
        assertEquals(4, vector.findInsertIndex(50));
        assertEquals(-1, vector.findInsertIndex(51));

        vector.getValues().add(60D);
        assertEquals(0, vector.findInsertIndex(1));
        assertEquals(0, vector.findInsertIndex(10));
        assertEquals(1, vector.findInsertIndex(11));
        assertEquals(1, vector.findInsertIndex(19));
        assertEquals(1, vector.findInsertIndex(20));
        assertEquals(2, vector.findInsertIndex(21));
        assertEquals(2, vector.findInsertIndex(29));
        assertEquals(2, vector.findInsertIndex(30));
        assertEquals(3, vector.findInsertIndex(31));
        assertEquals(3, vector.findInsertIndex(39));
        assertEquals(3, vector.findInsertIndex(40));
        assertEquals(4, vector.findInsertIndex(41));
        assertEquals(4, vector.findInsertIndex(49));
        assertEquals(4, vector.findInsertIndex(50));
        assertEquals(5, vector.findInsertIndex(51));
        assertEquals(5, vector.findInsertIndex(59));
        assertEquals(5, vector.findInsertIndex(60));
        assertEquals(-1, vector.findInsertIndex(61));

        vector.getValues().add(70D);
        assertEquals(0, vector.findInsertIndex(1));
        assertEquals(0, vector.findInsertIndex(10));
        assertEquals(1, vector.findInsertIndex(11));
        assertEquals(1, vector.findInsertIndex(19));
        assertEquals(1, vector.findInsertIndex(20));
        assertEquals(2, vector.findInsertIndex(21));
        assertEquals(2, vector.findInsertIndex(29));
        assertEquals(2, vector.findInsertIndex(30));
        assertEquals(3, vector.findInsertIndex(31));
        assertEquals(3, vector.findInsertIndex(39));
        assertEquals(3, vector.findInsertIndex(40));
        assertEquals(4, vector.findInsertIndex(41));
        assertEquals(4, vector.findInsertIndex(49));
        assertEquals(4, vector.findInsertIndex(50));
        assertEquals(5, vector.findInsertIndex(51));
        assertEquals(5, vector.findInsertIndex(59));
        assertEquals(5, vector.findInsertIndex(60));
        assertEquals(6, vector.findInsertIndex(61));
        assertEquals(6, vector.findInsertIndex(69));
        assertEquals(6, vector.findInsertIndex(70));
        assertEquals(-1, vector.findInsertIndex(71));

        vector.getValues().add(80D);
        assertEquals(0, vector.findInsertIndex(1));
        assertEquals(0, vector.findInsertIndex(10));
        assertEquals(1, vector.findInsertIndex(11));
        assertEquals(1, vector.findInsertIndex(19));
        assertEquals(1, vector.findInsertIndex(20));
        assertEquals(2, vector.findInsertIndex(21));
        assertEquals(2, vector.findInsertIndex(29));
        assertEquals(2, vector.findInsertIndex(30));
        assertEquals(3, vector.findInsertIndex(31));
        assertEquals(3, vector.findInsertIndex(39));
        assertEquals(3, vector.findInsertIndex(40));
        assertEquals(4, vector.findInsertIndex(41));
        assertEquals(4, vector.findInsertIndex(49));
        assertEquals(4, vector.findInsertIndex(50));
        assertEquals(5, vector.findInsertIndex(51));
        assertEquals(5, vector.findInsertIndex(59));
        assertEquals(5, vector.findInsertIndex(60));
        assertEquals(6, vector.findInsertIndex(61));
        assertEquals(6, vector.findInsertIndex(69));
        assertEquals(6, vector.findInsertIndex(70));
        assertEquals(7, vector.findInsertIndex(71));
        assertEquals(7, vector.findInsertIndex(79));
        assertEquals(7, vector.findInsertIndex(80));
        assertEquals(-1, vector.findInsertIndex(81));

        // test homogenous values, all 1
        vector.getValues().clear();
        vector.getValues().add(1D);
        assertEquals(0, vector.findInsertIndex(0));
        assertEquals(0, vector.findInsertIndex(1));
        assertEquals(-1, vector.findInsertIndex(2));
        for (int i = 0; i < 100; i++) {
            vector.getValues().add(1D);
            assertEquals("for i=" + i, 0, vector.findInsertIndex(0));
            assertTrue("for i=" + i, vector.findInsertIndex(1) != -1);
            assertEquals("for i=" + i, -1, vector.findInsertIndex(2));
        }

        // test various other cases
        double[] vector = new double[]{1, 1, 2, 2, 2, 3, 4, 5, 5, 6};
        assertEquals(0, findIndex(vector, 0));
        assertEquals(0, findIndex(vector, 0.5));
        assertEquals(0, findIndex(vector, 1));
        assertEquals(2, findIndex(vector, 1.5));
        assertEquals(2, findIndex(vector, 2));
        assertEquals(5, findIndex(vector, 2.5));
        assertEquals(5, findIndex(vector, 3));
        assertEquals(6, findIndex(vector, 3.5));
        assertEquals(6, findIndex(vector, 4));
        assertEquals(7, findIndex(vector, 4.5));
        assertEquals(7, findIndex(vector, 5));
        assertEquals(9, findIndex(vector, 5.5));
        assertEquals(9, findIndex(vector, 6));
        assertEquals(-1, findIndex(vector, 6.5));
        assertEquals(-1, findIndex(vector, 7));

        // test various other cases
        vector = new double[]{1, 8, 100, 1000, 1000, 10000, 10000, 99999};
        assertEquals(0, findIndex(vector, 0));
        assertEquals(0, findIndex(vector, 1));
        assertEquals(1, findIndex(vector, 2));
        assertEquals(1, findIndex(vector, 7));
        assertEquals(1, findIndex(vector, 8));
        assertEquals(2, findIndex(vector, 9));
        assertEquals(2, findIndex(vector, 99));
        assertEquals(2, findIndex(vector, 100));
        assertEquals(3, findIndex(vector, 101));
        assertEquals(3, findIndex(vector, 999));
        assertEquals(4, findIndex(vector, 1000));
        assertEquals(5, findIndex(vector, 1001));
        assertEquals(5, findIndex(vector, 9999));
        assertEquals(6, findIndex(vector, 10000));
        assertEquals(7, findIndex(vector, 10001));
        assertEquals(7, findIndex(vector, 99998));
        assertEquals(7, findIndex(vector, 99999));
        assertEquals(-1, findIndex(vector, 100000));
    }

    private int findIndex(double[] data, double value) {
        vector.getValues().clear();
        for (double aData : data) {
            vector.getValues().add(aData);
        }
        return vector.findInsertIndex(value);
    }

    private void compare(double[] expected, SortedDoubleVector vector) {
        assertEquals(expected.length, vector.size());
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], vector.getValue(i));
        }
    }
}
