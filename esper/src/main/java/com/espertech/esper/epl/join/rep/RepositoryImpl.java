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
package com.espertech.esper.epl.join.rep;

import com.espertech.esper.client.EventBean;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Implements a repository for join events and lookup results.
 */
public class RepositoryImpl implements Repository {
    private final int rootStream;
    private final EventBean rootEvent;
    private final int numStreams;

    private List<Node>[] nodesPerStream;

    private static Iterator<Cursor> emptyCursorIterator = new SingleCursorIterator(null);

    /**
     * Ctor.
     *
     * @param rootStream is the stream supplying the root event
     * @param rootEvent  is the root event
     * @param numStreams is the number of streams
     */
    public RepositoryImpl(int rootStream, EventBean rootEvent, int numStreams) {
        this.rootStream = rootStream;
        this.rootEvent = rootEvent;
        this.numStreams = numStreams;
    }

    public Iterator<Cursor> getCursors(int lookupFromStream) {
        if (lookupFromStream == rootStream) {
            Cursor cursor = new Cursor(rootEvent, rootStream, null);
            return new SingleCursorIterator(cursor);
        }

        List<Node> nodeList = nodesPerStream[lookupFromStream];
        if (nodeList == null) {
            return emptyCursorIterator;
        }
        return new NodeCursorIterator(lookupFromStream, nodeList.iterator());
    }

    public void addResult(Cursor cursor, Set<EventBean> lookupResults, int resultStream) {
        if (lookupResults.isEmpty()) {
            throw new IllegalArgumentException("Attempting to add zero results");
        }

        Node parentNode = cursor.getNode();
        if (parentNode == null) {
            Node leafNode = new Node(resultStream);
            leafNode.setEvents(lookupResults);

            if (nodesPerStream == null) {
                nodesPerStream = new List[numStreams];
            }

            List<Node> nodes = nodesPerStream[resultStream];
            if (nodes == null) {
                nodes = new LinkedList<Node>();
                nodesPerStream[resultStream] = nodes;
            }
            leafNode.setParentEvent(rootEvent);

            nodes.add(leafNode);
            return;
        }

        Node leafNode = new Node(resultStream);
        leafNode.setEvents(lookupResults);
        leafNode.setParent(cursor.getNode());
        leafNode.setParentEvent(cursor.getTheEvent());

        List<Node> nodes = nodesPerStream[resultStream];
        if (nodes == null) {
            nodes = new LinkedList<Node>();
            nodesPerStream[resultStream] = nodes;
        }

        nodes.add(leafNode);
    }

    /**
     * Returns a list of nodes that are the lookup results per stream.
     *
     * @return result nodes per stream
     */
    public List<Node>[] getNodesPerStream() {
        return nodesPerStream;
    }
}
