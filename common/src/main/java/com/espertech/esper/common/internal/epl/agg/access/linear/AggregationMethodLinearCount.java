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
package com.espertech.esper.common.internal.epl.agg.access.linear;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.hook.aggmultifunc.AggregationMultiFunctionAggregationMethod;
import com.espertech.esper.common.internal.epl.agg.core.AggregationRow;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;

import java.util.Collection;

public class AggregationMethodLinearCount implements AggregationMultiFunctionAggregationMethod {
    public final static AggregationMethodLinearCount INSTANCE = new AggregationMethodLinearCount();

    private AggregationMethodLinearCount() {
    }

    public Object getValue(int aggColNum, AggregationRow row, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        Collection<EventBean> events = row.getCollectionOfEvents(aggColNum, eventsPerStream, isNewData, exprEvaluatorContext);
        return events.size();
    }

    public Collection getValueCollectionEvents(int aggColNum, AggregationRow row, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        return null;
    }

    public Collection getValueCollectionScalar(int aggColNum, AggregationRow row, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        return null;
    }

    public EventBean getValueEventBean(int aggColNum, AggregationRow row, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        return null;
    }
}
