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
package com.espertech.esper.common.internal.epl.historical.indexingstrategy;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.epl.index.base.EventTable;
import com.espertech.esper.common.internal.epl.index.base.EventTableOrganization;
import com.espertech.esper.common.internal.epl.index.base.EventTableOrganizationType;
import com.espertech.esper.common.internal.util.CollectionUtil;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Simple table of events without an index, based on a List implementation rather then a set
 * since we know there cannot be duplicates (such as a poll returning individual rows).
 */
public class UnindexedEventTableList implements EventTable {
    private List<EventBean> eventSet;
    private int streamNum;

    /**
     * Ctor.
     *
     * @param eventSet  is a list initializing the table
     * @param streamNum stream number
     */
    public UnindexedEventTableList(List<EventBean> eventSet, int streamNum) {
        this.eventSet = eventSet;
        this.streamNum = streamNum;
    }

    public void addRemove(EventBean[] newData, EventBean[] oldData, ExprEvaluatorContext exprEvaluatorContext) {
        exprEvaluatorContext.getInstrumentationProvider().qIndexAddRemove(this, newData, oldData);

        if (newData != null) {
            Collections.addAll(eventSet, newData);
        }
        if (oldData != null) {
            for (EventBean removeEvent : oldData) {
                eventSet.remove(removeEvent);
            }
        }

        exprEvaluatorContext.getInstrumentationProvider().aIndexAddRemove();
    }

    public void add(EventBean[] events, ExprEvaluatorContext exprEvaluatorContext) {
        if (events != null) {
            Collections.addAll(eventSet, events);
        }
    }

    public void remove(EventBean[] events, ExprEvaluatorContext exprEvaluatorContext) {
        if (events != null) {
            for (EventBean removeEvent : events) {
                eventSet.remove(removeEvent);
            }
        }
    }

    public void add(EventBean event, ExprEvaluatorContext exprEvaluatorContext) {
        eventSet.add(event);
    }

    public void remove(EventBean event, ExprEvaluatorContext exprEvaluatorContext) {
        eventSet.remove(event);
    }

    public Iterator<EventBean> iterator() {
        if (eventSet == null) {
            return CollectionUtil.NULL_EVENT_ITERATOR;
        }
        return eventSet.iterator();
    }

    public boolean isEmpty() {
        return eventSet.isEmpty();
    }

    public String toString() {
        return toQueryPlan();
    }

    public String toQueryPlan() {
        return this.getClass().getSimpleName();
    }

    public void clear() {
        eventSet.clear();
    }

    public void destroy() {
        clear();
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
        return new EventTableOrganization(null, false, false, streamNum, null, EventTableOrganizationType.UNORGANIZED);
    }

    public Class getProviderClass() {
        return UnindexedEventTableList.class;
    }
}
