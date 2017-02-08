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

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;

public class EnumEvalSelectFromScalarLambda extends EnumEvalBase implements EnumEval {

    private final ObjectArrayEventType resultEventType;

    public EnumEvalSelectFromScalarLambda(ExprEvaluator innerExpression, int streamCountIncoming, ObjectArrayEventType resultEventType) {
        super(innerExpression, streamCountIncoming);
        this.resultEventType = resultEventType;
    }

    public Object evaluateEnumMethod(EventBean[] eventsLambda, Collection target, boolean isNewData, ExprEvaluatorContext context) {

        if (target.isEmpty()) {
            return target;
        }

        ObjectArrayEventBean resultEvent = new ObjectArrayEventBean(new Object[1], resultEventType);
        Collection<Object> values = (Collection<Object>) target;
        Deque queue = new ArrayDeque();
        for (Object next : values) {

            resultEvent.getProperties()[0] = next;
            eventsLambda[streamNumLambda] = resultEvent;

            Object item = innerExpression.evaluate(eventsLambda, isNewData, context);
            if (item != null) {
                queue.add(item);
            }
        }

        return queue;
    }
}
