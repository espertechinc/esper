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
package com.espertech.esper.epl.enummethod.eval;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public class EnumEvalMostLeastFrequentEvent extends EnumEvalBase implements EnumEval {

    private final boolean isMostFrequent;

    public EnumEvalMostLeastFrequentEvent(ExprEvaluator innerExpression, int streamCountIncoming, boolean mostFrequent) {
        super(innerExpression, streamCountIncoming);
        isMostFrequent = mostFrequent;
    }

    public Object evaluateEnumMethod(EventBean[] eventsLambda, Collection target, boolean isNewData, ExprEvaluatorContext context) {

        Map<Object, Integer> items = new LinkedHashMap<Object, Integer>();
        Collection<EventBean> beans = (Collection<EventBean>) target;

        for (EventBean next : beans) {
            eventsLambda[streamNumLambda] = next;

            Object item = innerExpression.evaluate(eventsLambda, isNewData, context);
            Integer existing = items.get(item);

            if (existing == null) {
                existing = 1;
            } else {
                existing++;
            }
            items.put(item, existing);
        }

        return getResult(items, isMostFrequent);
    }

    protected static Object getResult(Map<Object, Integer> items, boolean mostFrequent) {
        if (mostFrequent) {
            Object maxKey = null;
            int max = Integer.MIN_VALUE;
            for (Map.Entry<Object, Integer> entry : items.entrySet()) {
                if (entry.getValue() > max) {
                    maxKey = entry.getKey();
                    max = entry.getValue();
                }
            }
            return maxKey;
        }

        int min = Integer.MAX_VALUE;
        Object minKey = null;
        for (Map.Entry<Object, Integer> entry : items.entrySet()) {
            if (entry.getValue() < min) {
                minKey = entry.getKey();
                min = entry.getValue();
            }
        }
        return minKey;
    }
}
