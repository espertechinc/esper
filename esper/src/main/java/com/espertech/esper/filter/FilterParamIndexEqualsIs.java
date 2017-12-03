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
import com.espertech.esper.filterspec.FilterOperator;
import com.espertech.esper.epl.expression.core.ExprFilterSpecLookupable;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;

import java.util.Collection;
import java.util.concurrent.locks.ReadWriteLock;

/**
 * Index for filter parameter constants to match using the equals (=) operator.
 * The implementation is based on a regular HashMap.
 */
public final class FilterParamIndexEqualsIs extends FilterParamIndexEqualsBase {
    public FilterParamIndexEqualsIs(ExprFilterSpecLookupable lookupable, ReadWriteLock readWriteLock) {
        super(lookupable, readWriteLock, FilterOperator.IS);
    }

    public final void matchEvent(EventBean theEvent, Collection<FilterHandle> matches) {
        Object attributeValue = lookupable.getGetter().get(theEvent);
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qFilterReverseIndex(this, attributeValue);
        }

        EventEvaluator evaluator = null;
        constantsMapRWLock.readLock().lock();
        try {
            evaluator = constantsMap.get(attributeValue);
        } finally {
            constantsMapRWLock.readLock().unlock();
        }

        // No listener found for the value, return
        if (evaluator == null) {
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aFilterReverseIndex(false);
            }
            return;
        }

        evaluator.matchEvent(theEvent, matches);
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aFilterReverseIndex(true);
        }
    }
}
