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

import com.espertech.esper.filterspec.FilterOperator;
import com.espertech.esper.epl.expression.core.ExprFilterSpecLookupable;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;

/**
 * Index for filter parameter constants to match using the equals (=) operator.
 * The implementation is based on a regular HashMap.
 */
public abstract class FilterParamIndexEqualsBase extends FilterParamIndexLookupableBase {
    protected final Map<Object, EventEvaluator> constantsMap;
    protected final ReadWriteLock constantsMapRWLock;

    protected FilterParamIndexEqualsBase(ExprFilterSpecLookupable lookupable, ReadWriteLock readWriteLock, FilterOperator filterOperator) {
        super(filterOperator, lookupable);

        constantsMap = new HashMap<Object, EventEvaluator>();
        constantsMapRWLock = readWriteLock;
    }

    public final EventEvaluator get(Object filterConstant) {
        return constantsMap.get(filterConstant);
    }

    public final void put(Object filterConstant, EventEvaluator evaluator) {
        constantsMap.put(filterConstant, evaluator);
    }

    public final void remove(Object filterConstant) {
        constantsMap.remove(filterConstant);
    }

    public final int sizeExpensive() {
        return constantsMap.size();
    }

    public boolean isEmpty() {
        return constantsMap.isEmpty();
    }

    public final ReadWriteLock getReadWriteLock() {
        return constantsMapRWLock;
    }
}
