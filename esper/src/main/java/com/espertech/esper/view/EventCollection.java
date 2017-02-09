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
package com.espertech.esper.view;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;

import java.util.Iterator;

/**
 * Interface that marks an event collection.
 * Every event in the event collection must be of the same event type, as defined by the getEventType() call.
 */
public interface EventCollection extends Iterable<EventBean> {
    /**
     * Provides metadata information about the type of object the event collection contains.
     *
     * @return metadata for the objects in the collection
     */
    public EventType getEventType();

    /**
     * Allows iteration through all elements in this event collection.
     * The iterator will return the elements in the collection in their natural order, or,
     * if there is no natural ordering, in some unpredictable order.
     *
     * @return an iterator which will go through all current elements in the collection.
     */
    public Iterator<EventBean> iterator();
}
