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
package com.espertech.esper.filter;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.filterspec.ExprNodeAdapterBase;
import com.espertech.esper.filterspec.FilterOperator;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;

/**
 * Index that simply maintains a list of boolean expressions.
 */
public final class FilterParamIndexBooleanExpr extends FilterParamIndexBase {
    private final Map<ExprNodeAdapterBase, EventEvaluator> evaluatorsMap;
    private final ReadWriteLock constantsMapRWLock;

    public FilterParamIndexBooleanExpr(ReadWriteLock readWriteLock) {
        super(FilterOperator.BOOLEAN_EXPRESSION);

        evaluatorsMap = new LinkedHashMap<ExprNodeAdapterBase, EventEvaluator>();
        constantsMapRWLock = readWriteLock;
    }

    public final EventEvaluator get(Object filterConstant) {
        ExprNodeAdapterBase keyValues = (ExprNodeAdapterBase) filterConstant;
        return evaluatorsMap.get(keyValues);
    }

    public final void put(Object filterConstant, EventEvaluator evaluator) {
        ExprNodeAdapterBase keys = (ExprNodeAdapterBase) filterConstant;
        evaluatorsMap.put(keys, evaluator);
    }

    public final void remove(Object filterConstant) {
        ExprNodeAdapterBase keys = (ExprNodeAdapterBase) filterConstant;
        evaluatorsMap.remove(keys);
    }

    public final int sizeExpensive() {
        return evaluatorsMap.size();
    }

    public boolean isEmpty() {
        return evaluatorsMap.isEmpty();
    }

    public final ReadWriteLock getReadWriteLock() {
        return constantsMapRWLock;
    }

    public final void matchEvent(EventBean theEvent, Collection<FilterHandle> matches) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qFilterBoolean(this);
        }
        constantsMapRWLock.readLock().lock();
        try {

            if (InstrumentationHelper.ENABLED) {
                int i = -1;
                for (Map.Entry<ExprNodeAdapterBase, EventEvaluator> evals : evaluatorsMap.entrySet()) {
                    i++;
                    InstrumentationHelper.get().qFilterBooleanExpr(i, evals);
                    boolean result = evals.getKey().evaluate(theEvent);
                    InstrumentationHelper.get().aFilterBooleanExpr(result);
                    if (result) {
                        evals.getValue().matchEvent(theEvent, matches);
                    }
                }
            } else {
                for (Map.Entry<ExprNodeAdapterBase, EventEvaluator> evals : evaluatorsMap.entrySet()) {
                    if (evals.getKey().evaluate(theEvent)) {
                        evals.getValue().matchEvent(theEvent, matches);
                    }
                }
            }
        } finally {
            constantsMapRWLock.readLock().unlock();
        }
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aFilterBoolean();
        }
    }
}
