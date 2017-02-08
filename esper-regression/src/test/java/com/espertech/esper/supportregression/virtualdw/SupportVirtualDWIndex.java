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
package com.espertech.esper.supportregression.virtualdw;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.hook.VirtualDataWindowContext;
import com.espertech.esper.client.hook.VirtualDataWindowLookup;

import java.util.HashSet;
import java.util.Set;

public class SupportVirtualDWIndex implements VirtualDataWindowLookup {

    private final SupportVirtualDW supportVirtualDW;
    private final VirtualDataWindowContext context;

    public SupportVirtualDWIndex(SupportVirtualDW supportVirtualDW, VirtualDataWindowContext context) {
        this.supportVirtualDW = supportVirtualDW;
        this.context = context;
    }

    public Set<EventBean> lookup(Object[] keys, EventBean[] eventsPerStream) {
        supportVirtualDW.setLastKeys(keys);
        supportVirtualDW.setLastAccessEvents(eventsPerStream);
        Set<EventBean> events = new HashSet<EventBean>();
        for (Object item : supportVirtualDW.getData()) {
            events.add(context.getEventFactory().wrap(item));
        }
        return events;
    }
}
