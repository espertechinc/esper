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

import com.espertech.esper.common.internal.epl.expression.core.ExprFilterSpecLookupable;
import com.espertech.esper.common.internal.filterspec.FilterOperator;
import com.espertech.esper.common.internal.filterspec.StringRange;

import java.util.ArrayDeque;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.locks.ReadWriteLock;

public abstract class FilterParamIndexStringRangeBase extends FilterParamIndexLookupableBase {
    protected final TreeMap<StringRange, EventEvaluator> ranges;
    private EventEvaluator rangesNullEndpoints;
    private final ReadWriteLock rangesRWLock;

    protected FilterParamIndexStringRangeBase(ExprFilterSpecLookupable lookupable, ReadWriteLock readWriteLock, FilterOperator filterOperator) {
        super(filterOperator, lookupable);

        ranges = new TreeMap<>(new StringRangeComparator());
        rangesRWLock = readWriteLock;
    }

    public final EventEvaluator get(Object expressionValue) {
        if (!(expressionValue instanceof StringRange)) {
            throw new IllegalArgumentException("Supplied expressionValue must be of type StringRange");
        }

        StringRange range = (StringRange) expressionValue;

        if ((range.getMax() == null) || (range.getMin() == null)) {
            return rangesNullEndpoints;
        }

        return ranges.get(range);
    }

    public final void put(Object expressionValue, EventEvaluator matcher) {
        if (!(expressionValue instanceof StringRange)) {
            throw new IllegalArgumentException("Supplied expressionValue must be of type DoubleRange");
        }

        StringRange range = (StringRange) expressionValue;
        if ((range.getMax() == null) || (range.getMin() == null)) {
            rangesNullEndpoints = matcher;
            return;
        }

        ranges.put(range, matcher);
    }

    public final void remove(Object filterConstant) {
        StringRange range = (StringRange) filterConstant;

        if ((range.getMax() == null) || (range.getMin() == null)) {
            rangesNullEndpoints = null;
            return;
        }
        ranges.remove(range);
    }

    public final int sizeExpensive() {
        return ranges.size();
    }

    public boolean isEmpty() {
        return ranges.isEmpty();
    }

    public final ReadWriteLock getReadWriteLock() {
        return rangesRWLock;
    }

    public void getTraverseStatement(EventTypeIndexTraverse traverse, Set<Integer> statementIds, ArrayDeque<FilterItem> evaluatorStack) {
        for (Map.Entry<StringRange, EventEvaluator> entry : ranges.entrySet()) {
            evaluatorStack.add(new FilterItem(lookupable.getExpression(), getFilterOperator(), entry.getKey()));
            entry.getValue().getTraverseStatement(traverse, statementIds, evaluatorStack);
            evaluatorStack.removeLast();
        }
    }
}
