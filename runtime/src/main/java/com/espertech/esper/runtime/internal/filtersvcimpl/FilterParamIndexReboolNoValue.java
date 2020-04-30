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
package com.espertech.esper.runtime.internal.filtersvcimpl;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.epl.expression.core.ExprFilterSpecLookupable;
import com.espertech.esper.common.internal.filterspec.FilterOperator;
import com.espertech.esper.common.internal.filtersvc.FilterHandle;
import com.espertech.esper.runtime.internal.metrics.instrumentation.InstrumentationHelper;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;

/**
 * Index for filter parameter constants to match using the equals (=) operator.
 * The implementation is based on a regular HashMap.
 */
public class FilterParamIndexReboolNoValue extends FilterParamIndexLookupableBase {
    protected EventEvaluator eventEvaluator;
    protected final ReadWriteLock constantsMapRWLock;

    protected FilterParamIndexReboolNoValue(ExprFilterSpecLookupable lookupable, ReadWriteLock readWriteLock) {
        super(FilterOperator.REBOOL, lookupable);
        constantsMapRWLock = readWriteLock;
    }

    public final EventEvaluator get(Object filterConstant) {
        return eventEvaluator;
    }

    public final void put(Object filterConstant, EventEvaluator evaluator) {
        this.eventEvaluator = evaluator;
    }

    public final void remove(Object filterConstant) {
        this.eventEvaluator = null;
    }

    public final int sizeExpensive() {
        return eventEvaluator == null ? 0 : 1;
    }

    public boolean isEmpty() {
        return eventEvaluator == null;
    }

    public final ReadWriteLock getReadWriteLock() {
        return constantsMapRWLock;
    }

    public void getTraverseStatement(EventTypeIndexTraverse traverse, Set<Integer> statementIds, ArrayDeque<FilterItem> evaluatorStack) {
        if (eventEvaluator != null) {
            evaluatorStack.add(new FilterItem(lookupable.getExpression(), getFilterOperator(), null, this));
            eventEvaluator.getTraverseStatement(traverse, statementIds, evaluatorStack);
            evaluatorStack.removeLast();
        }
    }

    public void matchEvent(EventBean theEvent, Collection<FilterHandle> matches, ExprEvaluatorContext ctx) {
        if (eventEvaluator == null) {
            return;
        }
        EventBean[] events = new EventBean[] {theEvent};
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qFilterReverseIndex(this, null);
        }
        Boolean result = (Boolean) lookupable.getExpr().evaluate(events, true, ctx);
        if (result != null && result) {
            eventEvaluator.matchEvent(theEvent, matches, ctx);
        }
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aFilterReverseIndex(result);
        }
    }
}
