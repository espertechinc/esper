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
package com.espertech.esper.example.virtualdw;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.hook.VirtualDataWindowContext;
import com.espertech.esper.client.hook.VirtualDataWindowLookup;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class SampleVirtualDataWindowLookup implements VirtualDataWindowLookup {

    private final VirtualDataWindowContext context;

    public SampleVirtualDataWindowLookup(VirtualDataWindowContext context) {
        this.context = context;
    }

    public Set<EventBean> lookup(Object[] keys, EventBean[] eventsPerStream) {
        // Add code to interogate lookup-keys here.

        // Create sample event.
        Map<String, Object> eventData = new HashMap<String, Object>();
        eventData.put("key1", "sample1");
        eventData.put("key2", "sample2");
        eventData.put("value1", 100);
        eventData.put("value2", 1.5d);
        EventBean theEvent = context.getEventFactory().wrap(eventData);
        return Collections.singleton(theEvent);
    }
}
