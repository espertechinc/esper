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
package com.espertech.esper.rowregex;

import com.espertech.esper.client.EventBean;

/**
 * Interface for random access to a previous event.
 */
public interface RegexPartitionStateRandomAccess {
    /**
     * Returns an new data event given an index.
     *
     * @param index to return new data for
     * @return new data event
     */
    EventBean getPreviousEvent(int index);

    void newEventPrepare(EventBean newEvent);

    void existingEventPrepare(EventBean theEvent);

    void remove(EventBean[] oldEvents);

    void remove(EventBean oldEvent);

    boolean isEmpty();
}
