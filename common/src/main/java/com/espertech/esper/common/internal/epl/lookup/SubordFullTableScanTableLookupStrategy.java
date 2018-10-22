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
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.epl.table.core.TableEvalLockUtil;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.locks.Lock;

/**
 * Index lookup strategy for tables.
 */
public class SubordFullTableScanTableLookupStrategy implements SubordTableLookupStrategy {
    private final Lock tableLevelLock;
    private final Iterable<EventBean> contents;

    public SubordFullTableScanTableLookupStrategy(Lock tableLevelLock, Iterable<EventBean> contents) {
        this.tableLevelLock = tableLevelLock;
        this.contents = contents;
    }

    @Override
    public Collection<EventBean> lookup(EventBean[] events, ExprEvaluatorContext context) {
        if (context.getInstrumentationProvider().activated()) {
            context.getInstrumentationProvider().qIndexSubordLookup(this, null, null);
            Collection<EventBean> result = lookupInternal(context);
            context.getInstrumentationProvider().aIndexSubordLookup(result, null);
            return result;
        }
        return lookupInternal(context);
    }

    private Collection<EventBean> lookupInternal(ExprEvaluatorContext context) {
        TableEvalLockUtil.obtainLockUnless(tableLevelLock, context);

        Iterator<EventBean> it = contents.iterator();
        if (!it.hasNext()) {
            return null;
        }
        ArrayDeque<EventBean> result = new ArrayDeque<EventBean>(2);
        for (; it.hasNext(); ) {
            EventBean eventBean = it.next();
            result.add(eventBean);
        }
        return result;
    }

    public LookupStrategyDesc getStrategyDesc() {
        return LookupStrategyDesc.SCAN;
    }

    public String toQueryPlan() {
        return this.getClass().getSimpleName();
    }
}
