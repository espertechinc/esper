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

/**
 * A streams is a conduct for incoming events. Incoming data is placed into streams for consumption by queries.
 */
public interface EventStream extends Viewable {
    /**
     * Insert new events onto the stream.
     *
     * @param events to insert
     */
    public void insert(EventBean[] events);

    /**
     * Insert a new event onto the stream.
     *
     * @param theEvent to insert
     */
    public void insert(EventBean theEvent);
}
