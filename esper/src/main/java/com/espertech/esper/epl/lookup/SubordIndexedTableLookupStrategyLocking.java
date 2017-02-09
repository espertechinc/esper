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
package com.espertech.esper.epl.lookup;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.core.service.StatementAgentInstanceLock;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;

/**
 * Index lookup strategy for subqueries.
 */
public class SubordIndexedTableLookupStrategyLocking implements SubordTableLookupStrategy {
    private final SubordTableLookupStrategy inner;
    private final StatementAgentInstanceLock statementLock;

    public SubordIndexedTableLookupStrategyLocking(SubordTableLookupStrategy inner, StatementAgentInstanceLock statementLock) {
        this.inner = inner;
        this.statementLock = statementLock;
    }

    @Override
    public Collection<EventBean> lookup(EventBean[] events, ExprEvaluatorContext context) {
        statementLock.acquireReadLock();
        try {
            Collection<EventBean> result = inner.lookup(events, context);
            if (result != null) {
                return new ArrayDeque<EventBean>(result);
            } else {
                return Collections.emptyList();
            }
        } finally {
            statementLock.releaseReadLock();
        }
    }

    public LookupStrategyDesc getStrategyDesc() {
        return inner.getStrategyDesc();
    }

    public String toQueryPlan() {
        return this.getClass().getSimpleName() + " inner " + inner.toQueryPlan();
    }
}
