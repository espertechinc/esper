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

import java.util.Collection;
import java.util.concurrent.locks.ReadWriteLock;

/**
 * Index for filter parameter constants to match using the equals (=) operator.
 * The implementation is based on a regular HashMap.
 */
public final class FilterParamIndexEquals extends FilterParamIndexEqualsBase {
    public FilterParamIndexEquals(ExprFilterSpecLookupable lookupable, ReadWriteLock readWriteLock) {
        super(lookupable, readWriteLock, FilterOperator.EQUAL);
    }

    public final void matchEvent(EventBean theEvent, Collection<FilterHandle> matches, ExprEvaluatorContext ctx) {
        Object attributeValue = lookupable.getEval().eval(theEvent, ctx);
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qFilterReverseIndex(this, attributeValue);
        }

        if (attributeValue == null) {   //  null cannot match, not even null: requires use of "is"
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aFilterReverseIndex(false);
            }
            return;
        }

        // Look up in hashtable
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

        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aFilterReverseIndex(true);
        }
        evaluator.matchEvent(theEvent, matches, ctx);
    }
}
