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
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;

public final class FilterParamIndexReboolWithValue extends FilterParamIndexEqualsBase {
    public FilterParamIndexReboolWithValue(ExprFilterSpecLookupable lookupable, ReadWriteLock readWriteLock) {
        super(lookupable, readWriteLock, FilterOperator.REBOOL);
    }

    public final void matchEvent(EventBean theEvent, Collection<FilterHandle> matches, ExprEvaluatorContext ctx) {
        EventBean[] events = new EventBean[] {theEvent};
        for (Map.Entry<Object, EventEvaluator> entry : constantsMap.entrySet()) {
            ctx.setFilterReboolConstant(entry.getKey());
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().qFilterReverseIndex(this, entry.getKey());
            }
            Boolean result = (Boolean) lookupable.getExpr().evaluate(events, true, ctx);
            if (result != null && result) {
                entry.getValue().matchEvent(theEvent, matches, ctx);
            }
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aFilterReverseIndex(result);
            }
        }
    }
}
