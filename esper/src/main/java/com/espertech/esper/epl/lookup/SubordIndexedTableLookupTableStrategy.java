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
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.table.strategy.ExprTableEvalLockUtil;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.locks.Lock;

/**
 * Index lookup strategy for subqueries against tables, full table scan.
 */
public class SubordIndexedTableLookupTableStrategy implements SubordTableLookupStrategy {
    private final SubordTableLookupStrategy inner;
    private final Lock lock;

    public SubordIndexedTableLookupTableStrategy(SubordTableLookupStrategy inner, Lock lock) {
        this.inner = inner;
        this.lock = lock;
    }

    @Override
    public Collection<EventBean> lookup(EventBean[] events, ExprEvaluatorContext context) {
        ExprTableEvalLockUtil.obtainLockUnless(lock, context);

        Collection<EventBean> result = inner.lookup(events, context);
        if (result == null) {
            return Collections.emptyList();
        }
        return result;
    }

    public LookupStrategyDesc getStrategyDesc() {
        return inner.getStrategyDesc();
    }

    public String toQueryPlan() {
        return this.getClass().getSimpleName() + " inner " + inner.toQueryPlan();
    }
}
