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
package com.espertech.esper.regressionlib.support.extend.vdw;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.hook.vdw.*;

import java.io.Serializable;
import java.util.*;

public class SupportVirtualDW implements VirtualDataWindow {
    private static Set<Object> initializationData;

    public static final String ITERATE = "iterate";

    private final VirtualDataWindowContext context;
    private Set<Object> data;
    private boolean destroyed;
    private List<VirtualDataWindowLookupContext> requestedLookups = new ArrayList<>();
    private Object[] lastAccessKeys;
    private EventBean[] lastAccessEvents;
    private EventBean[] lastUpdateNew;
    private EventBean[] lastUpdateOld;
    private List<VirtualDataWindowEvent> events = new ArrayList<VirtualDataWindowEvent>();

    public SupportVirtualDW(VirtualDataWindowContext context) {
        this.context = context;
        this.data = initializationData;
    }

    public static void setInitializationData(Set initializationData) {
        SupportVirtualDW.initializationData = initializationData;
    }

    public VirtualDataWindowContext getContext() {
        return context;
    }

    public void setData(Set data) {
        this.data = data;
    }

    public boolean isDestroyed() {
        return destroyed;
    }

    public Set<Object> getData() {
        return data;
    }

    public VirtualDataWindowLookupContext getLastRequestedLookup() {
        return requestedLookups.get(0);
    }

    public List<VirtualDataWindowLookupContext> getRequestedLookups() {
        return requestedLookups;
    }

    public VirtualDataWindowLookup getLookup(VirtualDataWindowLookupContext desc) {
        requestedLookups.add(0, desc);
        return new SupportVirtualDWIndex(this, context);
    }

    public void destroy() {
        destroyed = true;
    }

    public void setLastKeys(Object[] keys) {
        lastAccessKeys = keys;
    }

    public Object[] getLastAccessKeys() {
        return lastAccessKeys;
    }

    public void setLastAccessEvents(EventBean[] lastAccessEvents) {
        this.lastAccessEvents = lastAccessEvents;
    }

    public EventBean[] getLastAccessEvents() {
        return lastAccessEvents;
    }

    public void update(EventBean[] newData, EventBean[] oldData) {
        lastUpdateNew = newData;
        lastUpdateOld = oldData;
        context.getOutputStream().update(newData, oldData);
    }

    public EventBean[] getLastUpdateNew() {
        return lastUpdateNew;
    }

    public EventBean[] getLastUpdateOld() {
        return lastUpdateOld;
    }

    public Iterator<EventBean> iterator() {
        SupportVirtualDWFactory factory = (SupportVirtualDWFactory) context.getFactory().getFactory();
        Serializable compileTimeConfiguration = factory.getInitializeContext().getCustomConfiguration();
        if (compileTimeConfiguration != null && compileTimeConfiguration.equals(ITERATE)) {
            List<EventBean> events = new ArrayList<EventBean>();
            for (Object item : data) {
                events.add(context.getEventFactory().wrap(item));
            }
            return events.iterator();
        }
        return Collections.<EventBean>emptyList().iterator();
    }

    public void handleEvent(VirtualDataWindowEvent theEvent) {
        events.add(theEvent);
    }

    public List<VirtualDataWindowEvent> getEvents() {
        return events;
    }
}
