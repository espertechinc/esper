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
import com.espertech.esper.epl.join.rep.Node;
import com.espertech.esper.supportunit.epl.join.SupportJoinProcNode;
import com.espertech.esper.supportunit.epl.join.SupportJoinResultNodeFactory;
import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.List;

public class TestSingleOptionalAssemblyNode extends TestCase {
    private SupportJoinProcNode parentNode;
    private BranchOptionalAssemblyNode optAssemblyNode;
    private List<Node>[] resultMultipleEvents;
    private List<Node>[] resultSingleEvent;

    public void setUp() {
        optAssemblyNode = new BranchOptionalAssemblyNode(1, 4);
        parentNode = new SupportJoinProcNode(-1, 4);
        parentNode.addChild(optAssemblyNode);

        resultMultipleEvents = SupportJoinResultNodeFactory.makeOneStreamResult(4, 1, 2, 1); // 2 nodes 1 event each for (1)
        resultSingleEvent = SupportJoinResultNodeFactory.makeOneStreamResult(4, 1, 1, 1); // 1 nodes 1 event each for (1)
    }

    public void testProcessMultipleEvents() {
        List<EventBean[]> resultFinalRows = new ArrayList<EventBean[]>();
        optAssemblyNode.init(resultMultipleEvents);

        // generate an event row originating from a child for 1 of the 2 events in the result
        EventBean[] childRow = new EventBean[4];
        Node nodeOne = resultMultipleEvents[1].get(0);
        EventBean eventOne = nodeOne.getEvents().iterator().next();
        optAssemblyNode.result(childRow, 3, eventOne, nodeOne, resultFinalRows, null);

        // test that the node indeed manufactures event rows for any event not received from a child
        parentNode.getRowsList().clear();
        optAssemblyNode.process(resultMultipleEvents, resultFinalRows, null);

        // check generated row
        assertEquals(1, parentNode.getRowsList().size());
        EventBean[] row = parentNode.getRowsList().get(0);
        assertEquals(4, row.length);
        Node nodeTwo = resultMultipleEvents[1].get(1);
        assertEquals(nodeTwo.getEvents().iterator().next(), row[1]);
    }

    public void testProcessSingleEvent() {
        optAssemblyNode.init(resultSingleEvent);

        // test that the node indeed manufactures event rows for any event not received from a child
        List<EventBean[]> resultFinalRows = new ArrayList<EventBean[]>();
        optAssemblyNode.process(resultMultipleEvents, resultFinalRows, null);

        // check generated row
        assertEquals(1, parentNode.getRowsList().size());
        EventBean[] row = parentNode.getRowsList().get(0);
        assertEquals(4, row.length);
        Node node = resultSingleEvent[1].get(0);
        assertEquals(node.getEvents().iterator().next(), row[1]);
    }

    public void testChildResult() {
        optAssemblyNode.init(resultMultipleEvents);
        testChildResult(optAssemblyNode, parentNode);
    }

    protected static void testChildResult(BaseAssemblyNode nodeUnderTest, SupportJoinProcNode mockParentNode) {
        EventBean[] childRow = new EventBean[4];
        childRow[3] = SupportJoinResultNodeFactory.makeEvent();

        EventBean myEvent = SupportJoinResultNodeFactory.makeEvent();
        Node myNode = SupportJoinResultNodeFactory.makeNode(3, 1);

        // indicate child result
        List<EventBean[]> resultFinalRows = new ArrayList<EventBean[]>();
        nodeUnderTest.result(childRow, 3, myEvent, myNode, resultFinalRows, null);

        // assert parent node got the row
        assertEquals(1, mockParentNode.getRowsList().size());
        EventBean[] resultRow = mockParentNode.getRowsList().get(0);

        // assert the node has added his event to the row
        assertEquals(myEvent, resultRow[1]);
    }
}
