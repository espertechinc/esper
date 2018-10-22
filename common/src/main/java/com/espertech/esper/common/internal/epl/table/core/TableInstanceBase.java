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

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.epl.agg.core.AggregationRow;
import com.espertech.esper.common.internal.epl.lookupplansubord.EventTableIndexRepository;
import com.espertech.esper.common.internal.event.core.ObjectArrayBackedEventBean;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public abstract class TableInstanceBase implements TableInstance {

    protected final Table table;
    protected final AgentInstanceContext agentInstanceContext;
    protected final ReentrantReadWriteLock tableLevelRWLock = new ReentrantReadWriteLock();
    protected final EventTableIndexRepository indexRepository;

    public TableInstanceBase(Table table, AgentInstanceContext agentInstanceContext) {
        this.table = table;
        this.agentInstanceContext = agentInstanceContext;
        this.indexRepository = new EventTableIndexRepository(table.getEventTableIndexMetadata());
    }

    public void addEventUnadorned(EventBean event) {
        if (event.getEventType() != table.getMetaData().getInternalEventType()) {
            throw new IllegalStateException("Unexpected event type for add: " + event.getEventType().getName());
        }
        ObjectArrayBackedEventBean oa = (ObjectArrayBackedEventBean) event;
        AggregationRow aggs = table.getAggregationRowFactory().make();
        oa.getProperties()[0] = aggs;
        addEvent(event);
    }

    public AgentInstanceContext getAgentInstanceContext() {
        return agentInstanceContext;
    }

    public Table getTable() {
        return table;
    }

    public EventTableIndexRepository getIndexRepository() {
        return indexRepository;
    }

    public ReadWriteLock getTableLevelRWLock() {
        return tableLevelRWLock;
    }
}
