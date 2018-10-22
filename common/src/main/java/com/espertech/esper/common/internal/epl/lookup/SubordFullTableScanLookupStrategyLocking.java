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
package com.espertech.esper.common.internal.epl.lookup;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.context.util.StatementAgentInstanceLock;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;

import java.util.ArrayDeque;
import java.util.Collection;

/**
 * Index lookup strategy for subqueries.
 */
public class SubordFullTableScanLookupStrategyLocking implements SubordTableLookupStrategy {
    private final Iterable<EventBean> contents;
    private final StatementAgentInstanceLock statementLock;

    public SubordFullTableScanLookupStrategyLocking(Iterable<EventBean> contents, StatementAgentInstanceLock statementLock) {
        this.contents = contents;
        this.statementLock = statementLock;
    }

    @Override
    public Collection<EventBean> lookup(EventBean[] events, ExprEvaluatorContext context) {
        if (context.getInstrumentationProvider().activated()) {
            context.getInstrumentationProvider().qIndexSubordLookup(this, null, null);
            Collection<EventBean> result = lookupInternal();
            context.getInstrumentationProvider().aIndexSubordLookup(result, null);
            return result;
        }
        return lookupInternal();
    }

    private Collection<EventBean> lookupInternal() {
        statementLock.acquireReadLock();
        try {
            ArrayDeque<EventBean> result = new ArrayDeque<EventBean>();
            for (EventBean eventBean : contents) {
                result.add(eventBean);
            }
            return result;
        } finally {
            statementLock.releaseReadLock();
        }
    }

    public LookupStrategyDesc getStrategyDesc() {
        return LookupStrategyDesc.SCAN;
    }

    public String toQueryPlan() {
        return this.getClass().getSimpleName();
    }
}
