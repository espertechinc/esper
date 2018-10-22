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
package com.espertech.esper.common.client.fireandforget;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;

import java.util.Iterator;

/**
 * Result for fire-and-forget queries.
 */
public interface EPFireAndForgetQueryResult {
    /**
     * Returns an array representing query result rows, may return a null value or empty array to indicate an empty result set.
     *
     * @return result array
     */
    EventBean[] getArray();

    /**
     * Returns the event type of the result.
     *
     * @return event type of result row
     */
    EventType getEventType();

    /**
     * Returns an iterator representing query result rows.
     *
     * @return result row iterator
     */
    Iterator<EventBean> iterator();
}
