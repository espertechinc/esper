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
package com.espertech.esper.common.internal.view.util;


import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.collection.ArrayEventIterator;
import com.espertech.esper.common.internal.collection.SingleEventIterator;
import com.espertech.esper.common.internal.event.core.EventBeanReader;
import com.espertech.esper.common.internal.event.core.EventBeanReaderDefaultImpl;
import com.espertech.esper.common.internal.event.core.EventBeanUtility;
import com.espertech.esper.common.internal.event.core.EventTypeSPI;

import java.util.ArrayDeque;
import java.util.Iterator;

/**
 * Iterator for obtaining distinct events.
 */
public class EventDistinctIterator implements Iterator<EventBean> {
    private static final Iterator<EventBean> NULL_ITER = new SingleEventIterator(null);
    private final Iterator<EventBean> sourceIterator;
    private Iterator<EventBean> resultIterator;
    private EventBeanReader eventBeanReader;

    /**
     * Ctor.
     *
     * @param sourceIterator is the source event iterator
     * @param eventType      type of event
     */
    public EventDistinctIterator(Iterator<EventBean> sourceIterator, EventType eventType) {
        this.sourceIterator = sourceIterator;
        if (eventType instanceof EventTypeSPI) {
            eventBeanReader = ((EventTypeSPI) eventType).getReader();
        }
        if (eventBeanReader == null) {
            eventBeanReader = new EventBeanReaderDefaultImpl(eventType);
        }
    }

    public EventBean next() {
        if (resultIterator == null) {
            initialize();
        }
        return resultIterator.next();
    }

    public boolean hasNext() {
        if (resultIterator == null) {
            initialize();
        }
        return resultIterator.hasNext();
    }

    private void initialize() {
        if ((sourceIterator == null) || (!sourceIterator.hasNext())) {
            resultIterator = NULL_ITER;
            return;
        }

        EventBean eventFirst = sourceIterator.next();
        if (!sourceIterator.hasNext()) {
            resultIterator = new SingleEventIterator(eventFirst);
            return;
        }

        // read all events
        ArrayDeque<EventBean> events = new ArrayDeque<EventBean>();
        events.add(eventFirst);
        for (; sourceIterator.hasNext(); ) {
            events.add(sourceIterator.next());
        }

        // build distinct set
        EventBean[] unique = EventBeanUtility.getDistinctByProp(events, eventBeanReader);
        resultIterator = new ArrayEventIterator(unique);
    }

    public void remove() {
        throw new UnsupportedOperationException();
    }
}