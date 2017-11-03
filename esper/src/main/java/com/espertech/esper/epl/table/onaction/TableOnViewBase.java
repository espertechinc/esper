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
package com.espertech.esper.epl.table.onaction;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.lookup.SubordWMatchExprLookupStrategy;
import com.espertech.esper.epl.table.mgmt.TableMetadata;
import com.espertech.esper.epl.table.mgmt.TableStateInstance;
import com.espertech.esper.util.CollectionUtil;
import com.espertech.esper.util.StopCallback;
import com.espertech.esper.view.ViewSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;

public abstract class TableOnViewBase extends ViewSupport implements StopCallback, TableOnView {
    private static final Logger log = LoggerFactory.getLogger(TableOnViewBase.class);

    protected final SubordWMatchExprLookupStrategy lookupStrategy;
    protected final TableStateInstance tableStateInstance;
    protected final ExprEvaluatorContext exprEvaluatorContext;
    protected final TableMetadata metadata;
    protected final boolean acquireWriteLock;

    protected TableOnViewBase(SubordWMatchExprLookupStrategy lookupStrategy, TableStateInstance tableStateInstance, ExprEvaluatorContext exprEvaluatorContext, TableMetadata metadata, boolean acquireWriteLock) {
        this.lookupStrategy = lookupStrategy;
        this.tableStateInstance = tableStateInstance;
        this.exprEvaluatorContext = exprEvaluatorContext;
        this.metadata = metadata;
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
            tableStateInstance.getTableLevelRWLock().writeLock().lock();
            try {
                EventBean[] eventsFound = lookupStrategy.lookup(newData, exprEvaluatorContext);
                handleMatching(newData, eventsFound);
            } finally {
                tableStateInstance.getTableLevelRWLock().writeLock().unlock();
            }
        } else {
            tableStateInstance.getTableLevelRWLock().readLock().lock();
            try {
                EventBean[] eventsFound = lookupStrategy.lookup(newData, exprEvaluatorContext);
                handleMatching(newData, eventsFound);
            } finally {
                tableStateInstance.getTableLevelRWLock().readLock().unlock();
            }
        }
    }

    /**
     * returns expr context.
     *
     * @return context
     */
    public ExprEvaluatorContext getExprEvaluatorContext() {
        return exprEvaluatorContext;
    }

    public Iterator<EventBean> iterator() {
        return CollectionUtil.NULL_EVENT_ITERATOR;
    }

    public EventType getEventType() {
        return metadata.getPublicEventType();
    }
}
