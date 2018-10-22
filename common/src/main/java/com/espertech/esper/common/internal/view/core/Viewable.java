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
package com.espertech.esper.common.internal.view.core;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;

import java.util.Iterator;

public interface Viewable extends Iterable<EventBean> {
    void setChild(View view);

    View getChild();

    /**
     * Provides metadata information about the type of object the event collection contains.
     *
     * @return metadata for the objects in the collection
     */
    public EventType getEventType();

    /**
     * Allows iteration through all elements in this viewable.
     * The iterator will return the elements in the collection in their natural order, or,
     * if there is no natural ordering, in some unpredictable order.
     *
     * @return an iterator which will go through all current elements in the collection.
     */
    public Iterator<EventBean> iterator();
}
