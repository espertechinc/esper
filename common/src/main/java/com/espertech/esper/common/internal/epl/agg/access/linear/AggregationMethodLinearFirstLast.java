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
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;

import java.util.Collection;
import java.util.List;

public class AggregationMethodLinearFirstLast implements AggregationMultiFunctionAggregationMethod {
    private AggregationAccessorLinearType accessType;
    private ExprEvaluator optionalEvaluator;

    public void setAccessType(AggregationAccessorLinearType accessType) {
        this.accessType = accessType;
    }

    public void setOptionalEvaluator(ExprEvaluator optionalEvaluator) {
        this.optionalEvaluator = optionalEvaluator;
    }

    public Object getValue(int aggColNum, AggregationRow row, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        List<EventBean> events = (List<EventBean>) row.getCollectionOfEvents(aggColNum, eventsPerStream, isNewData, exprEvaluatorContext);
        if (events == null) {
            return null;
        }
        EventBean target;
        if (accessType == AggregationAccessorLinearType.FIRST) {
            target = events.get(0);
        } else {
            target = events.get(events.size() - 1);
        }

        if (optionalEvaluator == null) {
            return target.getUnderlying();
        }
        EventBean[] eventsPerStreamBuf = new EventBean[]{target};
        return optionalEvaluator.evaluate(eventsPerStreamBuf, isNewData, exprEvaluatorContext);
    }

    public Collection getValueCollectionScalar(int aggColNum, AggregationRow row, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        return null;
    }

    public Collection getValueCollectionEvents(int aggColNum, AggregationRow row, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        return null;
    }

    public EventBean getValueEventBean(int aggColNum, AggregationRow row, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        List<EventBean> events = (List<EventBean>) row.getCollectionOfEvents(aggColNum, eventsPerStream, isNewData, exprEvaluatorContext);
        if (events == null) {
            return null;
        }
        if (accessType == AggregationAccessorLinearType.FIRST) {
            return events.get(0);
        }
        return events.get(events.size() - 1);
    }
}
