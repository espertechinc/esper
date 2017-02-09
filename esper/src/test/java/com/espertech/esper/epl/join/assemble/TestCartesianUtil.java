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
package com.espertech.esper.epl.join.assemble;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.supportunit.epl.join.SupportJoinResultNodeFactory;
import junit.framework.TestCase;

import java.util.LinkedList;
import java.util.List;

public class TestCartesianUtil extends TestCase {
    private static final int NUM_COL = 4;

    private int[] substreamsA;
    private int[] substreamsB;
    private List<EventBean[]> results;

    public void setUp() {
        substreamsA = new int[]{0, 3};
        substreamsB = new int[]{1};
        results = new LinkedList<EventBean[]>();
    }

    public void testCompute() {
        // test null
        List<EventBean[]> rowsA = null;
        List<EventBean[]> rowsB = null;
        tryCompute(rowsA, rowsB);
        assertTrue(results.isEmpty());

        // test no rows A
        rowsA = new LinkedList<EventBean[]>();
        tryCompute(rowsA, rowsB);
        assertTrue(results.isEmpty());

        // test no rows B
        rowsA = null;
        rowsB = new LinkedList<EventBean[]>();
        tryCompute(rowsA, rowsB);
        assertTrue(results.isEmpty());

        // test side A one row, B empty
        rowsA = makeRowsA(1);
        rowsB = null;
        tryCompute(rowsA, rowsB);
        assertEquals(1, results.size());
        EPAssertionUtil.assertEqualsExactOrder(results.get(0), rowsA.get(0));

        // test side B one row, A empty
        rowsA = null;
        rowsB = makeRowsB(1);
        tryCompute(rowsA, rowsB);
        assertEquals(1, results.size());
        EPAssertionUtil.assertEqualsExactOrder(results.get(0), rowsB.get(0));

        // test A and B one row
        rowsA = makeRowsA(1);
        rowsB = makeRowsB(1);
        tryCompute(rowsA, rowsB);
        assertEquals(1, results.size());
        EPAssertionUtil.assertEqualsExactOrder(
                results.get(0), new EventBean[]{rowsA.get(0)[0], rowsB.get(0)[1], null, rowsA.get(0)[3]});

        // test A=2 rows and B=1 row
        rowsA = makeRowsA(2);
        rowsB = makeRowsB(1);
        tryCompute(rowsA, rowsB);
        assertEquals(2, results.size());
        EPAssertionUtil.assertEqualsAnyOrder(new EventBean[][]{
                        new EventBean[]{rowsA.get(0)[0], rowsB.get(0)[1], null, rowsA.get(0)[3]},
                        new EventBean[]{rowsA.get(1)[0], rowsB.get(0)[1], null, rowsA.get(1)[3]}
                }
                , SupportJoinResultNodeFactory.convertTo2DimArr(results));

        // test A=1 rows and B=2 row
        rowsA = makeRowsA(1);
        rowsB = makeRowsB(2);
        tryCompute(rowsA, rowsB);
        assertEquals(2, results.size());
        EPAssertionUtil.assertEqualsAnyOrder(new EventBean[][]{
                        new EventBean[]{rowsA.get(0)[0], rowsB.get(0)[1], null, rowsA.get(0)[3]},
                        new EventBean[]{rowsA.get(0)[0], rowsB.get(1)[1], null, rowsA.get(0)[3]}
                }
                , SupportJoinResultNodeFactory.convertTo2DimArr(results));

        // test A=2 rows and B=2 row
        rowsA = makeRowsA(2);
        rowsB = makeRowsB(2);
        tryCompute(rowsA, rowsB);
        assertEquals(4, results.size());
        EPAssertionUtil.assertEqualsAnyOrder(new EventBean[][]{
                        new EventBean[]{rowsA.get(0)[0], rowsB.get(0)[1], null, rowsA.get(0)[3]},
                        new EventBean[]{rowsA.get(0)[0], rowsB.get(1)[1], null, rowsA.get(0)[3]},
                        new EventBean[]{rowsA.get(1)[0], rowsB.get(0)[1], null, rowsA.get(1)[3]},
                        new EventBean[]{rowsA.get(1)[0], rowsB.get(1)[1], null, rowsA.get(1)[3]}
                }
                , SupportJoinResultNodeFactory.convertTo2DimArr(results));

        // test A=2 rows and B=3 row
        rowsA = makeRowsA(2);
        rowsB = makeRowsB(3);
        tryCompute(rowsA, rowsB);
        assertEquals(6, results.size());
        EPAssertionUtil.assertEqualsAnyOrder(new EventBean[][]{
                        new EventBean[]{rowsA.get(0)[0], rowsB.get(0)[1], null, rowsA.get(0)[3]},
                        new EventBean[]{rowsA.get(0)[0], rowsB.get(1)[1], null, rowsA.get(0)[3]},
                        new EventBean[]{rowsA.get(0)[0], rowsB.get(2)[1], null, rowsA.get(0)[3]},
                        new EventBean[]{rowsA.get(1)[0], rowsB.get(0)[1], null, rowsA.get(1)[3]},
                        new EventBean[]{rowsA.get(1)[0], rowsB.get(1)[1], null, rowsA.get(1)[3]},
                        new EventBean[]{rowsA.get(1)[0], rowsB.get(2)[1], null, rowsA.get(1)[3]}
                }
                , SupportJoinResultNodeFactory.convertTo2DimArr(results));
    }

    private void tryCompute(List<EventBean[]> rowsOne, List<EventBean[]> rowsTwo) {
        results.clear();
        CartesianUtil.computeCartesian(rowsOne, substreamsA, rowsTwo, substreamsB, results);
    }

    private List<EventBean[]> makeRowsA(int numRows) {
        return makeRows(numRows, substreamsA);
    }

    private List<EventBean[]> makeRowsB(int numRows) {
        return makeRows(numRows, substreamsB);
    }

    private static List<EventBean[]> makeRows(int numRows, int[] substreamsPopulated) {
        List<EventBean[]> result = new LinkedList<EventBean[]>();
        for (int i = 0; i < numRows; i++) {
            EventBean[] row = new EventBean[NUM_COL];
            for (int j = 0; j < substreamsPopulated.length; j++) {
                int index = substreamsPopulated[j];
                row[index] = SupportJoinResultNodeFactory.makeEvent();
            }
            result.add(row);
        }
        return result;
    }
}
