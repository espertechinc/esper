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
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * Iterator over a set of nodes supplying node and event-within-node position information in a {@link Cursor}.
 */
public class NodeCursorIterator implements Iterator<Cursor> {
    private final Iterator<Node> nodeIterator;
    private final int stream;
    private Iterator<EventBean> currentIterator;
    private Node currentNode;

    /**
     * Ctor.
     *
     * @param stream       is the stream that the events in the Node belong to
     * @param nodeIterator is the iterator over all nodes to position over
     */
    public NodeCursorIterator(int stream, Iterator<Node> nodeIterator) {
        this.nodeIterator = nodeIterator;
        this.stream = stream;
        nextIterable();
    }

    public Cursor next() {
        if (currentIterator == null) {
            throw new NoSuchElementException();
        }
        if (currentIterator.hasNext()) {
            return makeCursor(currentIterator.next());
        }

        nextIterable();

        if (currentIterator == null) {
            throw new NoSuchElementException();
        }
        return makeCursor(currentIterator.next());
    }

    public boolean hasNext() {
        if (currentIterator == null) {
            return false;
        }

        if (currentIterator.hasNext()) {
            return true;
        }

        nextIterable();

        if (currentIterator == null) {
            return false;
        }

        return true;
    }

    public void remove() {
        throw new UnsupportedOperationException();
    }

    private void nextIterable() {
        while (nodeIterator.hasNext()) {
            currentNode = nodeIterator.next();
            Set<EventBean> events = currentNode.getEvents();
            if (events != null) {
                currentIterator = events.iterator();
                if (currentIterator.hasNext()) {
                    return;
                }
            }
        }

        currentIterator = null;
    }

    private Cursor makeCursor(EventBean theEvent) {
        return new Cursor(theEvent, stream, currentNode);
    }
}
