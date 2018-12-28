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

public class AggregationMethodLinearFirstLastIndex implements AggregationMultiFunctionAggregationMethod {
    private AggregationAccessorLinearType accessType;
    private Integer optionalConstIndex;
    private ExprEvaluator optionalIndexEval;

    public void setAccessType(AggregationAccessorLinearType accessType) {
        this.accessType = accessType;
    }

    public void setOptionalConstIndex(Integer optionalConstIndex) {
        this.optionalConstIndex = optionalConstIndex;
    }

    public void setOptionalIndexEval(ExprEvaluator optionalIndexEval) {
        this.optionalIndexEval = optionalIndexEval;
    }

    public Object getValue(int aggColNum, AggregationRow row, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        List<EventBean> events = (List<EventBean>) row.getCollectionOfEvents(aggColNum, eventsPerStream, isNewData, exprEvaluatorContext);
        if (events == null) {
            return null;
        }
        EventBean target = getBean(events);
        if (target == null) {
            return null;
        }
        return target.getUnderlying();
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

    private EventBean getBean(List<EventBean> events) {
        int index;
        if (optionalConstIndex != null) {
            index = optionalConstIndex;
        } else {
            Object result = optionalIndexEval.evaluate(null, true, null);
            if ((result == null) || (!(result instanceof Integer))) {
                return null;
            }
            index = (Integer) result;
        }
        if (index < 0) {
            return null;
        }
        if (index >= events.size()) {
            return null;
        }
        if (accessType == AggregationAccessorLinearType.FIRST) {
            return events.get(index);
        }
        return events.get(events.size() - index - 1);
    }
}
