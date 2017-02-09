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
package com.espertech.esper.epl.join.exec.base;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.locks.Lock;

/**
 * Execution node for lookup in a table.
 */
public class TableLookupExecNodeTableLocking extends TableLookupExecNode {
    private final Lock lock;

    public TableLookupExecNodeTableLocking(int indexedStream, JoinExecTableLookupStrategy lookupStrategy, Lock lock) {
        super(indexedStream, lookupStrategy);
        this.lock = lock;
    }

    public void process(EventBean lookupEvent, EventBean[] prefillPath, Collection<EventBean[]> result, ExprEvaluatorContext exprEvaluatorContext) {
        // acquire table index lock
        boolean added = exprEvaluatorContext.getTableExprEvaluatorContext().addAcquiredLock(lock);
        if (added) {
            lock.lock();
        }

        // lookup events
        Set<EventBean> joinedEvents = lookupStrategy.lookup(lookupEvent, null, exprEvaluatorContext);
        if (joinedEvents == null) {
            return;
        }

        // process results
        super.processResults(prefillPath, result, joinedEvents);
    }
}
