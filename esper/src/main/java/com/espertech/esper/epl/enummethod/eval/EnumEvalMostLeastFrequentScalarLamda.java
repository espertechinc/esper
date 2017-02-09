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
import com.espertech.esper.event.arr.ObjectArrayEventBean;
import com.espertech.esper.event.arr.ObjectArrayEventType;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public class EnumEvalMostLeastFrequentScalarLamda extends EnumEvalBase implements EnumEval {

    private final boolean isMostFrequent;
    private final ObjectArrayEventType resultEventType;

    public EnumEvalMostLeastFrequentScalarLamda(ExprEvaluator innerExpression, int streamCountIncoming, boolean mostFrequent, ObjectArrayEventType resultEventType) {
        super(innerExpression, streamCountIncoming);
        isMostFrequent = mostFrequent;
        this.resultEventType = resultEventType;
    }

    public Object evaluateEnumMethod(EventBean[] eventsLambda, Collection target, boolean isNewData, ExprEvaluatorContext context) {

        Map<Object, Integer> items = new LinkedHashMap<Object, Integer>();
        Collection<Object> values = (Collection<Object>) target;

        ObjectArrayEventBean resultEvent = new ObjectArrayEventBean(new Object[1], resultEventType);

        for (Object next : values) {

            resultEvent.getProperties()[0] = next;
            eventsLambda[streamNumLambda] = resultEvent;

            Object item = innerExpression.evaluate(eventsLambda, isNewData, context);
            Integer existing = items.get(item);

            if (existing == null) {
                existing = 1;
            } else {
                existing++;
            }
            items.put(item, existing);
        }

        return EnumEvalMostLeastFrequentEvent.getResult(items, isMostFrequent);
    }
}
