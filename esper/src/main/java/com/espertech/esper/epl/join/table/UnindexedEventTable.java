/**************************************************************************************
 * Copyright (C) 2006-2015 EsperTech Inc. All rights reserved.                        *
 * http://www.espertech.com/esper                                                          *
 * http://www.espertech.com                                                           *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the GPL license       *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package com.espertech.esper.epl.join.table;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Simple table of events without an index.
 */
public class UnindexedEventTable implements EventTable
{
    private final int streamNum;
    private Set<EventBean> eventSet = new LinkedHashSet<EventBean>();

    /**
     * Ctor.
     * @param streamNum is the indexed stream's number
     */
    public UnindexedEventTable(int streamNum)
    {
        this.streamNum = streamNum;
    }

    public void clear()
    {
        eventSet.clear();
    }

    public void addRemove(EventBean[] newData, EventBean[] oldData) {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.get().qIndexAddRemove(this, newData, oldData);}
        if (newData != null) {
            Collections.addAll(eventSet, newData);
        }
        if (oldData != null) {
            for (EventBean removeEvent : oldData) {
                eventSet.remove(removeEvent);
            }
        }
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.get().aIndexAddRemove();}
    }

    public void add(EventBean[] events)
    {
        if (events != null) {

            if (InstrumentationHelper.ENABLED && events.length > 0) {
                InstrumentationHelper.get().qIndexAdd(this, events);
                Collections.addAll(eventSet, events);
                InstrumentationHelper.get().aIndexAdd();
                return;
            }

            Collections.addAll(eventSet, events);
        }
    }

    public void remove(EventBean[] events)
    {
        if (events != null) {

            if (InstrumentationHelper.ENABLED && events.length > 0) {
                InstrumentationHelper.get().qIndexRemove(this, events);
                for (EventBean removeEvent : events) {
                    eventSet.remove(removeEvent);
                }
                InstrumentationHelper.get().aIndexRemove();
                return;
            }

            for (EventBean removeEvent : events) {
                eventSet.remove(removeEvent);
            }
        }
    }

    public void add(EventBean event) {
        eventSet.add(event);
    }

    public void remove(EventBean event) {
        eventSet.remove(event);
    }

    public boolean isEmpty()
    {
        return eventSet.isEmpty();
    }

    /**
     * Returns events in table.
     * @return all events
     */
    public Set<EventBean> getEventSet()
    {
        return eventSet;
    }

    public Iterator<EventBean> iterator()
    {
        return eventSet.iterator();
    }

    public String toString()
    {
        return toQueryPlan();
    }

    public String toQueryPlan()
    {
        return this.getClass().getSimpleName() + " streamNum=" + streamNum;
    }

    public Integer getNumberOfEvents() {
        return eventSet.size();
    }

    public int getNumKeys() {
        return 0;
    }

    public Object getIndex() {
        return eventSet;
    }

    public EventTableOrganization getOrganization() {
        return new EventTableOrganization(null, false, false, streamNum, null, EventTableOrganization.EventTableOrganizationType.UNORGANIZED);
    }
}
