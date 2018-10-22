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
package com.espertech.esper.regressionlib.support.extend.aggmultifunc;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.hook.aggmultifunc.AggregationMultiFunctionState;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;

import java.util.LinkedHashMap;
import java.util.Map;

public class SupportReferenceCountedMapState implements AggregationMultiFunctionState {
    private final Map<Object, Integer> countPerReference = new LinkedHashMap<Object, Integer>();

    public Map<Object, Integer> getCountPerReference() {
        return countPerReference;
    }

    public void enter(Object key) {
        Integer count = countPerReference.get(key);
        if (count == null) {
            countPerReference.put(key, 1);
        } else {
            countPerReference.put(key, count + 1);
        }
    }

    public void leave(Object key) {
        Integer count = countPerReference.get(key);
        if (count != null) {
            if (count == 1) {
                countPerReference.remove(key);
            } else {
                countPerReference.put(key, count - 1);
            }
        }
    }

    public void applyEnter(EventBean[] eventsPerStream, ExprEvaluatorContext exprEvaluatorContext) {
        // no need to implement, we mutate using enter and leave instead
        throw new UnsupportedOperationException("Use enter instead");
    }

    public void applyLeave(EventBean[] eventsPerStream, ExprEvaluatorContext exprEvaluatorContext) {
        // no need to implement, we mutate using enter and leave instead
        throw new UnsupportedOperationException("Use leave instead");
    }

    public void clear() {
        countPerReference.clear();
    }
}
