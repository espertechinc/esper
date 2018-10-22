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
package com.espertech.esper.common.internal.epl.ontrigger;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.epl.lookupplansubord.SubordWMatchExprLookupStrategy;
import com.espertech.esper.common.internal.epl.table.core.TableInstance;
import com.espertech.esper.common.internal.util.CollectionUtil;
import com.espertech.esper.common.internal.util.StopCallback;
import com.espertech.esper.common.internal.view.core.ViewSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;

public abstract class OnExprViewTableBase extends ViewSupport implements StopCallback {
    private static final Logger log = LoggerFactory.getLogger(OnExprViewTableBase.class);

    protected final SubordWMatchExprLookupStrategy lookupStrategy;
    protected final TableInstance tableInstance;
    protected final AgentInstanceContext agentInstanceContext;
    protected final boolean acquireWriteLock;

    protected OnExprViewTableBase(SubordWMatchExprLookupStrategy lookupStrategy, TableInstance tableInstance, AgentInstanceContext agentInstanceContext, boolean acquireWriteLock) {
        this.lookupStrategy = lookupStrategy;
        this.tableInstance = tableInstance;
        this.agentInstanceContext = agentInstanceContext;
        this.acquireWriteLock = acquireWriteLock;
    }

    public abstract void handleMatching(EventBean[] triggerEvents, EventBean[] matchingEvents);

    public void stop() {
        log.debug(".stop");
    }

    public void update(EventBean[] newData, EventBean[] oldData) {
        if (newData == null) {
            return;
        }

        if (acquireWriteLock) {
            tableInstance.getTableLevelRWLock().writeLock().lock();
            try {
                processLocked(newData);
            } finally {
                tableInstance.getTableLevelRWLock().writeLock().unlock();
            }
        } else {
            tableInstance.getTableLevelRWLock().readLock().lock();
            try {
                processLocked(newData);
            } finally {
                tableInstance.getTableLevelRWLock().readLock().unlock();
            }
        }
    }

    /**
     * returns expr context.
     *
     * @return context
     */
    public ExprEvaluatorContext getExprEvaluatorContext() {
        return agentInstanceContext;
    }

    public Iterator<EventBean> iterator() {
        return CollectionUtil.NULL_EVENT_ITERATOR;
    }

    public EventType getEventType() {
        return tableInstance.getTable().getMetaData().getPublicEventType();
    }

    private void processLocked(EventBean[] newData) {
        if (newData.length == 1) {
            process(newData);
            return;
        }

        EventBean[] eventsPerStream = new EventBean[1];
        for (EventBean event : newData) {
            eventsPerStream[0] = event;
            process(eventsPerStream);
        }
    }

    private void process(EventBean[] events) {
        EventBean[] eventsFound = lookupStrategy.lookup(events, agentInstanceContext);
        handleMatching(events, eventsFound);
    }
}
