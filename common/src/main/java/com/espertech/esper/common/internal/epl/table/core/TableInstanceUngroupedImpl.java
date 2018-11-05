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
package com.espertech.esper.common.internal.epl.table.core;

import com.espertech.esper.common.client.EPException;
import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.collection.SingleEventIterable;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.epl.index.base.EventTable;
import com.espertech.esper.common.internal.epl.index.base.EventTableOrganization;
import com.espertech.esper.common.internal.epl.index.base.EventTableOrganizationType;
import com.espertech.esper.common.internal.epl.index.base.SingleReferenceEventTable;
import com.espertech.esper.common.internal.event.core.ObjectArrayBackedEventBean;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;

public class TableInstanceUngroupedImpl extends TableInstanceUngroupedBase {
    private AtomicReference<ObjectArrayBackedEventBean> eventReference;

    public TableInstanceUngroupedImpl(Table table, AgentInstanceContext agentInstanceContext) {
        super(table, agentInstanceContext);
        eventReference = new AtomicReference<>(null);
    }

    public Collection<EventBean> getEventCollection() {
        EventBean event = eventReference.get();
        if (event == null) {
            return Collections.emptyList();
        }
        return Collections.singletonList(event);
    }

    public long size() {
        return eventReference.get() == null ? 0 : 1;
    }

    public void addEvent(EventBean event) {
        if (event.getEventType() != table.getMetaData().getInternalEventType()) {
            throw new IllegalStateException("Unexpected event type for add: " + event.getEventType().getName());
        }
        if (eventReference.get() != null) {
            throw new EPException("Unique index violation, table '" + table.getMetaData().getTableName() + "' " +
                    "is a declared to hold a single un-keyed row");
        }
        agentInstanceContext.getInstrumentationProvider().qTableAddEvent(event);
        eventReference.set((ObjectArrayBackedEventBean) event);
        agentInstanceContext.getInstrumentationProvider().aTableAddEvent();
    }

    public ObjectArrayBackedEventBean getCreateRowIntoTable(ExprEvaluatorContext exprEvaluatorContext) {
        ObjectArrayBackedEventBean bean = eventReference.get();
        if (bean != null) {
            return bean;
        }
        return createRowIntoTable();
    }

    public ObjectArrayBackedEventBean getEventUngrouped() {
        return eventReference.get();
    }

    public void clearInstance() {
        clearEvents();
    }

    public void destroy() {
        clearEvents();
    }

    private void clearEvents() {
        eventReference.set(null);
    }

    public Iterable<EventBean> getIterableTableScan() {
        return new SingleEventIterable((AtomicReference<EventBean>) (AtomicReference<?>) eventReference);
    }

    public void deleteEvent(EventBean matchingEvent) {
        agentInstanceContext.getInstrumentationProvider().qTableDeleteEvent(matchingEvent);
        eventReference.set(null);
        agentInstanceContext.getInstrumentationProvider().aTableDeleteEvent();
    }

    public EventTable getIndex(String indexName) {
        if (indexName.equals(table.getName())) {
            EventTableOrganization org = new EventTableOrganization(table.getName(),
                    true, false, 0, new String[0], EventTableOrganizationType.UNORGANIZED);
            return new SingleReferenceEventTable(org, eventReference);
        }
        throw new IllegalStateException("Invalid index requested '" + indexName + "'");
    }

    public void handleRowUpdated(ObjectArrayBackedEventBean updatedEvent) {
        // no action
        if (agentInstanceContext.getInstrumentationProvider().activated()) {
            agentInstanceContext.getInstrumentationProvider().qTableUpdatedEvent(updatedEvent);
            agentInstanceContext.getInstrumentationProvider().aTableUpdatedEvent();
        }
    }

    public void handleRowUpdateKeyBeforeUpdate(ObjectArrayBackedEventBean updatedEvent) {
    }

    public void handleRowUpdateKeyAfterUpdate(ObjectArrayBackedEventBean updatedEvent) {
    }
}
