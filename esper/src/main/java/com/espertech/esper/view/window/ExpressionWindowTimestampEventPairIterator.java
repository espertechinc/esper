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
package com.espertech.esper.view.window;

import com.espertech.esper.client.EventBean;

import java.util.Iterator;

public class ExpressionWindowTimestampEventPairIterator implements Iterator<EventBean> {
    private final Iterator<ExpressionWindowTimestampEventPair> events;

    public ExpressionWindowTimestampEventPairIterator(Iterator<ExpressionWindowTimestampEventPair> events) {
        this.events = events;
    }

    public boolean hasNext() {
        return events.hasNext();
    }

    public EventBean next() {
        return events.next().getTheEvent();
    }

    public void remove() {
        throw new UnsupportedOperationException();
    }
}

