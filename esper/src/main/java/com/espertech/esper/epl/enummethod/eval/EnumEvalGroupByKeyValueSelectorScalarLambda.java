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

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public class EnumEvalGroupByKeyValueSelectorScalarLambda extends EnumEvalBase implements EnumEval {

    private final ExprEvaluator secondExpression;
    private final ObjectArrayEventType resultEventType;

    public EnumEvalGroupByKeyValueSelectorScalarLambda(ExprEvaluator innerExpression, int streamCountIncoming, ExprEvaluator secondExpression, ObjectArrayEventType resultEventType) {
        super(innerExpression, streamCountIncoming);
        this.secondExpression = secondExpression;
        this.resultEventType = resultEventType;
    }

    public Object evaluateEnumMethod(EventBean[] eventsLambda, Collection target, boolean isNewData, ExprEvaluatorContext context) {
        Map<Object, Collection> result = new LinkedHashMap<Object, Collection>();

        ObjectArrayEventBean resultEvent = new ObjectArrayEventBean(new Object[1], resultEventType);
        Collection<Object> values = (Collection<Object>) target;
        for (Object next : values) {

            resultEvent.getProperties()[0] = next;
            eventsLambda[streamNumLambda] = resultEvent;
            Object key = innerExpression.evaluate(eventsLambda, isNewData, context);

            resultEvent.getProperties()[0] = next;
            Object entry = secondExpression.evaluate(eventsLambda, isNewData, context);

            Collection value = result.get(key);
            if (value == null) {
                value = new ArrayList();
                result.put(key, value);
            }
            value.add(entry);
        }

        return result;
    }
}
