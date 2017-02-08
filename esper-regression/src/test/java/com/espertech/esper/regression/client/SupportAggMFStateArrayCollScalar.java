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
package com.espertech.esper.regression.client;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.epl.agg.access.AggregationState;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class SupportAggMFStateArrayCollScalar implements AggregationState {

    private final SupportAggMFStateArrayCollScalarFactory factory;
    private final List values = new ArrayList();

    public SupportAggMFStateArrayCollScalar(SupportAggMFStateArrayCollScalarFactory factory) {
        this.factory = factory;
    }

    public void applyEnter(EventBean[] eventsPerStream, ExprEvaluatorContext exprEvaluatorContext) {
        Object value = factory.getEvaluator().evaluate(eventsPerStream, true, exprEvaluatorContext);
        values.add(value);
    }

    public void applyLeave(EventBean[] eventsPerStream, ExprEvaluatorContext exprEvaluatorContext) {
        // ever semantics
    }

    public void clear() {
        values.clear();
    }

    public int size() {
        return values.size();
    }

    public Object getValueAsArray() {
        Object array = Array.newInstance(factory.getEvaluator().getType(), values.size());
        Iterator<Object> it = values.iterator();
        int count = 0;
        for (;it.hasNext();) {
            Object value = it.next();
            Array.set(array, count++, value);
        }
        return array;
    }

    public Object getValueAsCollection() {
        return values;
    }
}
