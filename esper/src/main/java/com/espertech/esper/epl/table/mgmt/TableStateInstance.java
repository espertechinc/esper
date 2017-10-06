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

import com.espertech.esper.client.EventBean;
import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.epl.agg.access.AggregationServicePassThru;
import com.espertech.esper.epl.agg.service.common.AggregationRowPair;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.expression.core.ExprValidationException;
import com.espertech.esper.epl.join.plan.QueryPlanIndexItem;
import com.espertech.esper.epl.join.table.EventTable;
import com.espertech.esper.epl.lookup.EventTableIndexRepository;
import com.espertech.esper.event.ObjectArrayBackedEventBean;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;

import java.util.Collection;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public abstract class TableStateInstance {

    protected final TableMetadata tableMetadata;
    protected final AgentInstanceContext agentInstanceContext;
    private final ReentrantReadWriteLock tableLevelRWLock = new ReentrantReadWriteLock();
    protected final EventTableIndexRepository indexRepository;

    public abstract Iterable<EventBean> getIterableTableScan();

    public abstract void addEvent(EventBean theEvent);

    public abstract void deleteEvent(EventBean matchingEvent);

    public abstract void clearInstance();

    public abstract void destroyInstance();

    public abstract void addExplicitIndex(String explicitIndexName, QueryPlanIndexItem explicitIndexDesc, boolean isRecoveringResilient, boolean allowIndexExists) throws ExprValidationException;

    public abstract String[] getSecondaryIndexes();

    public abstract EventTable getIndex(String indexName);

    public abstract ObjectArrayBackedEventBean getCreateRowIntoTable(Object groupByKey, ExprEvaluatorContext exprEvaluatorContext);

    public abstract Collection<EventBean> getEventCollection();

    public abstract int getRowCount();

    public abstract AggregationServicePassThru getAggregationServicePassThru();

    public void handleRowUpdated(ObjectArrayBackedEventBean row) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qaTableUpdatedEvent(row);
        }
    }

    public void addEventUnadorned(EventBean event) {
        ObjectArrayBackedEventBean oa = (ObjectArrayBackedEventBean) event;
        AggregationRowPair aggs = tableMetadata.getRowFactory().makeAggs(agentInstanceContext.getAgentInstanceId(), null, null, getAggregationServicePassThru());
        oa.getProperties()[0] = aggs;
        addEvent(oa);
    }

    protected TableStateInstance(TableMetadata tableMetadata, AgentInstanceContext agentInstanceContext) {
        this.tableMetadata = tableMetadata;
        this.agentInstanceContext = agentInstanceContext;
        this.indexRepository = new EventTableIndexRepository(tableMetadata.getEventTableIndexMetadataRepo());
    }

    public TableMetadata getTableMetadata() {
        return tableMetadata;
    }

    public AgentInstanceContext getAgentInstanceContext() {
        return agentInstanceContext;
    }

    public ReentrantReadWriteLock getTableLevelRWLock() {
        return tableLevelRWLock;
    }

    public EventTableIndexRepository getIndexRepository() {
        return indexRepository;
    }

    public void handleRowUpdateKeyBeforeUpdate(ObjectArrayBackedEventBean updatedEvent) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qaTableUpdatedEventWKeyBefore(updatedEvent);
        }
        // no action
    }

    public void handleRowUpdateKeyAfterUpdate(ObjectArrayBackedEventBean updatedEvent) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qaTableUpdatedEventWKeyAfter(updatedEvent);
        }
        // no action
    }

    public void removeExplicitIndex(String indexName) {
        indexRepository.removeExplicitIndex(indexName);
    }
}
