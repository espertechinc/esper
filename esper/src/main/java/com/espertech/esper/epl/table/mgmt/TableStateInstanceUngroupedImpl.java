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
package com.espertech.esper.epl.table.mgmt;

import com.espertech.esper.client.EPException;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.collection.SingleEventIterable;
import com.espertech.esper.collection.SingleEventIterator;
import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.epl.agg.access.AggregationServicePassThru;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.expression.core.ExprValidationException;
import com.espertech.esper.epl.join.plan.QueryPlanIndexItem;
import com.espertech.esper.epl.join.table.EventTable;
import com.espertech.esper.epl.join.table.EventTableOrganization;
import com.espertech.esper.epl.join.table.EventTableOrganizationType;
import com.espertech.esper.epl.join.table.SingleReferenceEventTable;
import com.espertech.esper.event.ObjectArrayBackedEventBean;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicReference;

public class TableStateInstanceUngroupedImpl extends TableStateInstance implements TableStateInstanceUngrouped, Iterable<EventBean> {
    private AtomicReference<ObjectArrayBackedEventBean> eventReference;

    public TableStateInstanceUngroupedImpl(TableMetadata tableMetadata, AgentInstanceContext agentInstanceContext) {
        super(tableMetadata, agentInstanceContext);
        eventReference = new AtomicReference<ObjectArrayBackedEventBean>(null);
    }

    public Iterable<EventBean> getIterableTableScan() {
        return new SingleEventIterable((AtomicReference<EventBean>) (AtomicReference<?>) eventReference);
    }

    public void addEvent(EventBean theEvent) {
        if (eventReference.get() != null) {
            throw new EPException("Unique index violation, table '" + tableMetadata.getTableName() + "' " +
                    "is a declared to hold a single un-keyed row");
        }
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qTableAddEvent(theEvent);
        }
        eventReference.set((ObjectArrayBackedEventBean) theEvent);
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aTableAddEvent();
        }
    }

    public void deleteEvent(EventBean matchingEvent) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qTableDeleteEvent(matchingEvent);
        }
        eventReference.set(null);
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aTableDeleteEvent();
        }
    }

    public AtomicReference<ObjectArrayBackedEventBean> getEventReference() {
        return eventReference;
    }

    public ObjectArrayBackedEventBean getEventUngrouped() {
        return eventReference.get();
    }

    public void addExplicitIndex(String explicitIndexName, QueryPlanIndexItem explicitIndexDesc, boolean isRecoveringResilient, boolean allowIndexExists) throws ExprValidationException {
        throw new ExprValidationException("Tables without primary key column(s) do not allow creating an index");
    }

    public EventTable getIndex(String indexName) {
        if (indexName.equals(tableMetadata.getTableName())) {
            EventTableOrganization org = new EventTableOrganization(tableMetadata.getTableName(),
                    true, false, 0, new String[0], EventTableOrganizationType.UNORGANIZED);
            return new SingleReferenceEventTable(org, eventReference);
        }
        throw new IllegalStateException("Invalid index requested '" + indexName + "'");
    }

    public String[] getSecondaryIndexes() {
        return new String[0];
    }

    public Iterator<EventBean> iterator() {
        return new SingleEventIterator(eventReference.get());
    }

    public void clearInstance() {
        clearEvents();
    }

    public void destroyInstance() {
        clearEvents();
    }

    public Collection<EventBean> getEventCollection() {
        EventBean event = eventReference.get();
        if (event == null) {
            return Collections.emptyList();
        }
        return Collections.singletonList(event);
    }

    public int getRowCount() {
        return eventReference.get() == null ? 0 : 1;
    }

    public ObjectArrayBackedEventBean getCreateRowIntoTable(Object groupByKey, ExprEvaluatorContext exprEvaluatorContext) {
        ObjectArrayBackedEventBean bean = eventReference.get();
        if (bean != null) {
            return bean;
        }
        ObjectArrayBackedEventBean row = tableMetadata.getRowFactory().makeOA(exprEvaluatorContext.getAgentInstanceId(), groupByKey, null, getAggregationServicePassThru());
        addEvent(row);
        return row;
    }

    public AggregationServicePassThru getAggregationServicePassThru() {
        return null;
    }

    private void clearEvents() {
        eventReference.set(null);
    }
}
