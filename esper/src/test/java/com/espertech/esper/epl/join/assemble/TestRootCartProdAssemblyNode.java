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
import com.espertech.esper.supportunit.epl.join.SupportJoinProcNode;
import com.espertech.esper.supportunit.epl.join.SupportJoinResultNodeFactory;
import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.List;

public class TestRootCartProdAssemblyNode extends TestCase {
    private SupportJoinProcNode parentNode;
    private RootCartProdAssemblyNode rootCartNodeOneReq;

    public void setUp() {
        rootCartNodeOneReq = new RootCartProdAssemblyNode(1, 5, false, new int[]{0, 0, 0, 1, 2});

        parentNode = new SupportJoinProcNode(-1, 5);
        parentNode.addChild(rootCartNodeOneReq);

        // add child nodes to indicate what sub-streams to build the cartesian product from
        rootCartNodeOneReq.addChild(new SupportJoinProcNode(2, 5));
        rootCartNodeOneReq.addChild(new SupportJoinProcNode(3, 5));
        rootCartNodeOneReq.addChild(new SupportJoinProcNode(4, 5));
    }

    public void testFlowOptional() {
        RootCartProdAssemblyNode rootCartNodeAllOpt = (RootCartProdAssemblyNode) new RootCartProdAssemblyNodeFactory(1, 5, true).makeAssemblerUnassociated();
        rootCartNodeAllOpt.addChild(new SupportJoinProcNode(2, 5));
        rootCartNodeAllOpt.addChild(new SupportJoinProcNode(3, 5));
        rootCartNodeAllOpt.addChild(new SupportJoinProcNode(4, 5));

        parentNode.addChild(rootCartNodeAllOpt);

        rootCartNodeAllOpt.init(null);
        List<EventBean[]> resultFinalRows = new ArrayList<EventBean[]>();
        rootCartNodeAllOpt.process(null, resultFinalRows, null);

        // 5 generated rows: 2 (stream 2) + 2 (stream 3) + 1 (self, Node 2)
        assertEquals(1, parentNode.getRowsList().size());

        EventBean[][] rowArr = SupportJoinResultNodeFactory.convertTo2DimArr(parentNode.getRowsList());
        EPAssertionUtil.assertEqualsAnyOrder(new EventBean[][]{
                new EventBean[]{null, null, null, null, null}}, rowArr);
    }

    public void testFlowRequired() {
        rootCartNodeOneReq.init(null);

        EventBean[] stream2Events = SupportJoinResultNodeFactory.makeEvents(2); // for identifying rows in cartesian product
        EventBean[] stream3Events = SupportJoinResultNodeFactory.makeEvents(2); // for identifying rows in cartesian product
        EventBean[] stream4Events = SupportJoinResultNodeFactory.makeEvents(2); // for identifying rows in cartesian product

        // Post result from 3, send 2 rows
        List<EventBean[]> resultFinalRows = new ArrayList<EventBean[]>();
        EventBean[] childRow = new EventBean[5];
        childRow[3] = stream3Events[0];
        rootCartNodeOneReq.result(childRow, 3, null, null, resultFinalRows, null);
        childRow = new EventBean[5];
        childRow[3] = stream3Events[1];
        rootCartNodeOneReq.result(childRow, 3, null, null, resultFinalRows, null);

        // Post result from 2, send 2 rows
        childRow = new EventBean[5];
        childRow[2] = stream2Events[0];
        rootCartNodeOneReq.result(childRow, 2, null, null, resultFinalRows, null);
        childRow = new EventBean[5];
        childRow[2] = stream2Events[1];
        rootCartNodeOneReq.result(childRow, 2, null, null, resultFinalRows, null);

        // Post result from 4
        childRow = new EventBean[5];
        childRow[4] = stream4Events[0];
        rootCartNodeOneReq.result(childRow, 4, null, null, resultFinalRows, null);
        childRow = new EventBean[5];
        childRow[4] = stream4Events[1];
        rootCartNodeOneReq.result(childRow, 4, null, null, resultFinalRows, null);

        // process posted rows (child rows were stored and are compared to find other rows to generate)
        rootCartNodeOneReq.process(null, resultFinalRows, null);

        // 5 generated rows: 2 (stream 2) + 2 (stream 3) + 1 (self, Node 2)
        assertEquals(8, parentNode.getRowsList().size());

        EventBean[][] rowArr = SupportJoinResultNodeFactory.convertTo2DimArr(parentNode.getRowsList());
        EPAssertionUtil.assertEqualsAnyOrder(new EventBean[][]{
                        new EventBean[]{null, null, stream2Events[0], stream3Events[0], stream4Events[0]},
                        new EventBean[]{null, null, stream2Events[0], stream3Events[1], stream4Events[0]},
                        new EventBean[]{null, null, stream2Events[1], stream3Events[0], stream4Events[0]},
                        new EventBean[]{null, null, stream2Events[1], stream3Events[1], stream4Events[0]},
                        new EventBean[]{null, null, stream2Events[0], stream3Events[0], stream4Events[1]},
                        new EventBean[]{null, null, stream2Events[0], stream3Events[1], stream4Events[1]},
                        new EventBean[]{null, null, stream2Events[1], stream3Events[0], stream4Events[1]},
                        new EventBean[]{null, null, stream2Events[1], stream3Events[1], stream4Events[1]},
                }
                , rowArr);
    }

    public void testComputeCombined() {
        assertNull(RootCartProdAssemblyNode.computeCombined(new int[][]{{2}}));
        assertNull(RootCartProdAssemblyNode.computeCombined(new int[][]{{1}, {2}}));

        int[][] result = RootCartProdAssemblyNode.computeCombined(
                new int[][]{{3, 4}, {2, 5}, {6}});
        assertEquals(1, result.length);
        EPAssertionUtil.assertEqualsAnyOrder(new int[]{3, 4, 2, 5}, result[0]);

        result = RootCartProdAssemblyNode.computeCombined(
                new int[][]{{3, 4}, {2, 5}, {6}, {0, 8, 9}});
        assertEquals(2, result.length);
        EPAssertionUtil.assertEqualsAnyOrder(new int[]{3, 4, 2, 5}, result[0]);
        EPAssertionUtil.assertEqualsAnyOrder(new int[]{3, 4, 2, 5, 6}, result[1]);

        result = RootCartProdAssemblyNode.computeCombined(
                new int[][]{{3, 4}, {2, 5}, {6}, {0, 8, 9}, {1}});
        assertEquals(3, result.length);
        EPAssertionUtil.assertEqualsAnyOrder(new int[]{3, 4, 2, 5}, result[0]);
        EPAssertionUtil.assertEqualsAnyOrder(new int[]{3, 4, 2, 5, 6}, result[1]);
        EPAssertionUtil.assertEqualsAnyOrder(new int[]{3, 4, 2, 5, 6, 0, 8, 9}, result[2]);
    }
}
