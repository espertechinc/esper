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
package com.espertech.esper.event;

import com.espertech.esper.event.bean.BeanEventType;

import java.util.concurrent.atomic.AtomicInteger;

public class EventTypeIdGeneratorImpl implements EventTypeIdGenerator {

    private final AtomicInteger currentEventTypeId = new AtomicInteger();

    public EventTypeIdGeneratorImpl() {
    }

    public int getTypeId(String typeName) {
        return currentEventTypeId.incrementAndGet();
    }

    public void assignedType(String name, BeanEventType eventType) {
        // no op required
    }
}
