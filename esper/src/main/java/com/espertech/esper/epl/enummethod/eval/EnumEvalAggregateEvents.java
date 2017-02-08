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

public class EnumEvalAggregateEvents extends EnumEvalAggregateBase implements EnumEval {

    public EnumEvalAggregateEvents(ExprEvaluator initialization, ExprEvaluator innerExpression, int streamNumLambda, ObjectArrayEventType resultEventType) {
        super(initialization, innerExpression, streamNumLambda, resultEventType);
    }

    public Object evaluateEnumMethod(EventBean[] eventsLambda, Collection target, boolean isNewData, ExprEvaluatorContext context) {
        Object initializationValue = initialization.evaluate(eventsLambda, isNewData, context);

        if (target.isEmpty()) {
            return initializationValue;
        }

        Collection<EventBean> beans = (Collection<EventBean>) target;
        ObjectArrayEventBean resultEvent = new ObjectArrayEventBean(new Object[1], resultEventType);

        for (EventBean next : beans) {

            resultEvent.getProperties()[0] = initializationValue;
            eventsLambda[streamNumLambda + 1] = next;
            eventsLambda[streamNumLambda] = resultEvent;

            initializationValue = innerExpression.evaluate(eventsLambda, isNewData, context);
        }

        return initializationValue;
    }
}
