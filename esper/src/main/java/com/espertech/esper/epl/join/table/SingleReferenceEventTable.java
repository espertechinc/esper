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
package com.espertech.esper.epl.join.table;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.collection.SingleEventIterator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.event.ObjectArrayBackedEventBean;

import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

public class SingleReferenceEventTable implements EventTable, EventTableAsSet {
    private final EventTableOrganization organization;
    private final AtomicReference<ObjectArrayBackedEventBean> eventReference;

    public SingleReferenceEventTable(EventTableOrganization organization, AtomicReference<ObjectArrayBackedEventBean> eventReference) {
        this.organization = organization;
        this.eventReference = eventReference;
    }

    public void addRemove(EventBean[] newData, EventBean[] oldData, ExprEvaluatorContext exprEvaluatorContext) {
        throw new UnsupportedOperationException();
    }

    public void add(EventBean[] events, ExprEvaluatorContext exprEvaluatorContext) {
        throw new UnsupportedOperationException();
    }

    public void add(EventBean event, ExprEvaluatorContext exprEvaluatorContext) {
        throw new UnsupportedOperationException();
    }

    public void remove(EventBean[] events, ExprEvaluatorContext exprEvaluatorContext) {
        throw new UnsupportedOperationException();
    }

    public void remove(EventBean event, ExprEvaluatorContext exprEvaluatorContext) {
        throw new UnsupportedOperationException();
    }

    public Iterator<EventBean> iterator() {
        return new SingleEventIterator(eventReference.get());
    }

    public boolean isEmpty() {
        return eventReference.get() == null;
    }

    public void clear() {
        throw new UnsupportedOperationException();
    }

    public void destroy() {
    }

    public String toQueryPlan() {
        return "single-reference";
    }

    public Integer getNumberOfEvents() {
        return eventReference.get() == null ? 0 : 1;
    }

    public int getNumKeys() {
        return 0;
    }

    public Object getIndex() {
        return null;
    }

    public EventTableOrganization getOrganization() {
        return organization;
    }

    public Set<EventBean> allValues() {
        EventBean event = eventReference.get();
        if (event != null) {
            return Collections.singleton(event);
        }
        return Collections.emptySet();
    }

    public Class getProviderClass() {
        return SingleReferenceEventTable.class;
    }
}
