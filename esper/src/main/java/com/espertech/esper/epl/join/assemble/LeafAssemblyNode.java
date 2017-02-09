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
import com.espertech.esper.util.IndentWriter;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Assembly node for an event stream that is a leaf with a no child nodes below it.
 */
public class LeafAssemblyNode extends BaseAssemblyNode {
    /**
     * Ctor.
     *
     * @param streamNum  - is the stream number
     * @param numStreams - is the number of streams
     */
    public LeafAssemblyNode(int streamNum, int numStreams) {
        super(streamNum, numStreams);
    }

    public void init(List<Node>[] result) {
    }

    public void process(List<Node>[] result, Collection<EventBean[]> resultFinalRows, EventBean resultRootEvent) {
        List<Node> nodes = result[streamNum];
        if (nodes == null) {
            return;
        }

        for (Node node : nodes) {
            Set<EventBean> events = node.getEvents();
            for (EventBean theEvent : events) {
                processEvent(theEvent, node, resultFinalRows, resultRootEvent);
            }
        }
    }

    private void processEvent(EventBean theEvent, Node currentNode, Collection<EventBean[]> resultFinalRows, EventBean resultRootEvent) {
        EventBean[] row = new EventBean[numStreams];
        row[streamNum] = theEvent;
        parentNode.result(row, streamNum, currentNode.getParentEvent(), currentNode.getParent(), resultFinalRows, resultRootEvent);
    }

    public void result(EventBean[] row, int streamNum, EventBean myEvent, Node myNode, Collection<EventBean[]> resultFinalRows, EventBean resultRootEvent) {
        throw new UnsupportedOperationException("Leaf node cannot process child results");
    }

    public void print(IndentWriter indentWriter) {
        indentWriter.println("LeafAssemblyNode streamNum=" + streamNum);
    }
}
