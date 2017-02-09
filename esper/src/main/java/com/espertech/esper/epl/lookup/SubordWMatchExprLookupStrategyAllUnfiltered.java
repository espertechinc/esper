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
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;

import java.util.ArrayList;
import java.util.Iterator;

public class SubordWMatchExprLookupStrategyAllUnfiltered implements SubordWMatchExprLookupStrategy {
    private Iterable<EventBean> source;

    public SubordWMatchExprLookupStrategyAllUnfiltered(Iterable<EventBean> source) {
        this.source = source;
    }

    public EventBean[] lookup(EventBean[] newData, ExprEvaluatorContext exprEvaluatorContext) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qInfraTriggeredLookup(SubordWMatchExprLookupStrategyType.FULLTABLESCAN_UNFILTERED);
        }

        ArrayList<EventBean> events = new ArrayList<EventBean>();
        for (Iterator<EventBean> it = source.iterator(); it.hasNext(); ) {
            events.add(it.next());
        }
        EventBean[] result = events.toArray(new EventBean[events.size()]);

        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aInfraTriggeredLookup(result);
        }
        return result;
    }

    public String toString() {
        return toQueryPlan();
    }

    public String toQueryPlan() {
        return this.getClass().getSimpleName();
    }
}
