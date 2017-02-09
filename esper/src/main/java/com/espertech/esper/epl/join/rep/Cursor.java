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

/**
 * This class supplies position information for {@link com.espertech.esper.epl.join.exec.base.LookupInstructionExec}
 * to use for iterating over events for lookup.
 */
public class Cursor {
    private final EventBean theEvent;
    private final int stream;
    private final Node node;

    /**
     * Ctor.
     *
     * @param theEvent is the current event
     * @param stream   is the current stream
     * @param node     is the node containing the set of events to which the event belongs to
     */
    public Cursor(EventBean theEvent, int stream, Node node) {
        this.theEvent = theEvent;
        this.stream = stream;
        this.node = node;
    }

    /**
     * Supplies current event.
     *
     * @return event
     */
    public EventBean getTheEvent() {
        return theEvent;
    }

    /**
     * Returns current stream the event belongs to.
     *
     * @return stream number for event
     */
    public int getStream() {
        return stream;
    }

    /**
     * Returns current result node the event belong to.
     *
     * @return result node of event
     */
    public Node getNode() {
        return node;
    }
}
