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
import com.espertech.esper.epl.join.rep.Node;
import com.espertech.esper.supportunit.epl.join.SupportJoinProcNode;
import com.espertech.esper.supportunit.epl.join.SupportJoinResultNodeFactory;
import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.List;

public class TestCartesianProdAssemblyNode extends TestCase {
    private SupportJoinProcNode parentNode;
    private CartesianProdAssemblyNode optCartNode;
    private List<Node>[] resultMultipleEvents;
    private List<Node>[] resultSingleEvent;

    public void setUp() {
        optCartNode = new CartesianProdAssemblyNode(1, 4, true, new int[]{0, 0, 0, 1});

        parentNode = new SupportJoinProcNode(-1, 4);
        parentNode.addChild(optCartNode);

        // add child nodes to indicate what sub-streams to build the cartesian product from
        optCartNode.addChild(new SupportJoinProcNode(2, 4));
        optCartNode.addChild(new SupportJoinProcNode(3, 4));

        resultMultipleEvents = SupportJoinResultNodeFactory.makeOneStreamResult(4, 1, 2, 1); // 2 nodes 1 event each for (1)
        resultSingleEvent = SupportJoinResultNodeFactory.makeOneStreamResult(4, 1, 1, 1); // 1 nodes 1 event each for (1)
    }

    public void testFlow() {
        optCartNode.init(resultMultipleEvents);

        EventBean[] stream2Events = SupportJoinResultNodeFactory.makeEvents(2); // for identifying rows in cartesian product
        EventBean[] stream3Events = SupportJoinResultNodeFactory.makeEvents(2); // for identifying rows in cartesian product

        Node nodeOne = resultMultipleEvents[1].get(0);
        EventBean eventOneStreamOne = nodeOne.getEvents().iterator().next();
        Node nodeTwo = resultMultipleEvents[1].get(1);
        EventBean eventTwoStreamOne = nodeTwo.getEvents().iterator().next();

        // generate an event row originating from child 1
        List<EventBean[]> resultFinalRows = new ArrayList<EventBean[]>();
        EventBean[] childRow = new EventBean[4];        // new rows for each result
        childRow[2] = stream2Events[0];
        optCartNode.result(childRow, 2, eventOneStreamOne, nodeOne, resultFinalRows, null); // child is stream 2
        childRow = new EventBean[4];
        childRow[2] = stream2Events[1];
        optCartNode.result(childRow, 2, eventOneStreamOne, nodeOne, resultFinalRows, null); // child is stream 2

        // generate an event row originating from child 2
        childRow = new EventBean[4];
        childRow[3] = stream3Events[0];
        optCartNode.result(childRow, 3, eventOneStreamOne, nodeOne, resultFinalRows, null); // child is stream 3
        childRow = new EventBean[4];
        childRow[3] = stream3Events[1];
        optCartNode.result(childRow, 3, eventOneStreamOne, nodeOne, resultFinalRows, null); // child is stream 3

        // process posted rows (child rows were stored and are compared to find other rows to generate)
        optCartNode.process(resultMultipleEvents, resultFinalRows, null);

        // 5 generated rows: 2 (stream 2) + 2 (stream 3) + 1 (self, Node 2)
        assertEquals(5, parentNode.getRowsList().size());

        EventBean[][] rowArr = SupportJoinResultNodeFactory.convertTo2DimArr(parentNode.getRowsList());
        EPAssertionUtil.assertEqualsAnyOrder(new EventBean[][]{
                        new EventBean[]{null, eventOneStreamOne, stream2Events[0], stream3Events[0]},
                        new EventBean[]{null, eventOneStreamOne, stream2Events[0], stream3Events[1]},
                        new EventBean[]{null, eventOneStreamOne, stream2Events[1], stream3Events[0]},
                        new EventBean[]{null, eventOneStreamOne, stream2Events[1], stream3Events[1]},
                        new EventBean[]{null, eventTwoStreamOne, null, null},
                }
                , rowArr);
    }

    public void testProcessSingleEvent() {
        optCartNode.init(resultSingleEvent);

        // test that the node indeed manufactures event rows for any event not received from a child
        List<EventBean[]> resultFinalRows = new ArrayList<EventBean[]>();
        optCartNode.process(resultSingleEvent, resultFinalRows, null);

        // check generated row
        assertEquals(1, parentNode.getRowsList().size());
        EventBean[] row = parentNode.getRowsList().get(0);
        assertEquals(4, row.length);
        Node node = resultSingleEvent[1].get(0);
        assertEquals(node.getEvents().iterator().next(), row[1]);
    }
}
