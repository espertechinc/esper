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

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class AggregationMethodLinearWindow implements AggregationMultiFunctionAggregationMethod {

    private Class componentType;
    private ExprEvaluator optionalEvaluator;

    public void setComponentType(Class componentType) {
        this.componentType = componentType;
    }

    public void setOptionalEvaluator(ExprEvaluator optionalEvaluator) {
        this.optionalEvaluator = optionalEvaluator;
    }

    public Object getValue(int aggColNum, AggregationRow row, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        List<EventBean> linear = (List<EventBean>) row.getCollectionOfEvents(aggColNum, eventsPerStream, isNewData, exprEvaluatorContext);
        if (linear == null) {
            return null;
        }
        Object array = Array.newInstance(componentType, linear.size());
        Iterator<EventBean> it = linear.iterator();
        int count = 0;
        if (optionalEvaluator == null) {
            for (; it.hasNext(); ) {
                EventBean bean = it.next();
                Array.set(array, count++, bean.getUnderlying());
            }
        } else {
            EventBean[] events = new EventBean[1];
            for (; it.hasNext(); ) {
                EventBean bean = it.next();
                events[0] = bean;
                Array.set(array, count++, optionalEvaluator.evaluate(events, isNewData, exprEvaluatorContext));
            }
        }
        return array;
    }

    public Collection getValueCollectionEvents(int aggColNum, AggregationRow row, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        return row.getCollectionOfEvents(aggColNum, eventsPerStream, isNewData, exprEvaluatorContext);
    }

    public Collection getValueCollectionScalar(int aggColNum, AggregationRow row, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        List<EventBean> linear = (List<EventBean>) row.getCollectionOfEvents(aggColNum, eventsPerStream, isNewData, exprEvaluatorContext);
        if (linear == null || linear.isEmpty()) {
            return null;
        }
        List<Object> values = new ArrayList<Object>(linear.size());
        Iterator<EventBean> it = linear.iterator();
        EventBean[] eventsPerStreamBuf = new EventBean[1];
        for (; it.hasNext(); ) {
            EventBean bean = it.next();
            eventsPerStreamBuf[0] = bean;
            Object value = optionalEvaluator.evaluate(eventsPerStreamBuf, true, null);
            values.add(value);
        }

        return values;
    }

    public EventBean getValueEventBean(int aggColNum, AggregationRow row, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        return null;
    }
}
