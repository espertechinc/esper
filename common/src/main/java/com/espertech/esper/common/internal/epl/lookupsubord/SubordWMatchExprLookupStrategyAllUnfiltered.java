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
package com.espertech.esper.common.internal.epl.lookupsubord;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.epl.lookupplansubord.SubordWMatchExprLookupStrategy;

import java.util.ArrayList;
import java.util.Iterator;

public class SubordWMatchExprLookupStrategyAllUnfiltered implements SubordWMatchExprLookupStrategy {
    private Iterable<EventBean> source;

    public SubordWMatchExprLookupStrategyAllUnfiltered(Iterable<EventBean> source) {
        this.source = source;
    }

    public EventBean[] lookup(EventBean[] newData, ExprEvaluatorContext exprEvaluatorContext) {
        exprEvaluatorContext.getInstrumentationProvider().qInfraTriggeredLookup("fulltablescan_unfiltered");
        ArrayList<EventBean> events = new ArrayList<EventBean>();
        for (Iterator<EventBean> it = source.iterator(); it.hasNext(); ) {
            events.add(it.next());
        }
        EventBean[] out = events.toArray(new EventBean[events.size()]);

        exprEvaluatorContext.getInstrumentationProvider().aInfraTriggeredLookup(out);
        return out;
    }

    public String toString() {
        return toQueryPlan();
    }

    public String toQueryPlan() {
        return this.getClass().getSimpleName();
    }
}
