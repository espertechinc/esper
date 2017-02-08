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

public class EnumEvalAggregateScalar extends EnumEvalAggregateBase implements EnumEval {

    private final ObjectArrayEventType evalEventType;

    public EnumEvalAggregateScalar(ExprEvaluator initialization, ExprEvaluator innerExpression, int streamNumLambda, ObjectArrayEventType resultEventType, ObjectArrayEventType evalEventType) {
        super(initialization, innerExpression, streamNumLambda, resultEventType);
        this.evalEventType = evalEventType;
    }

    public Object evaluateEnumMethod(EventBean[] eventsLambda, Collection target, boolean isNewData, ExprEvaluatorContext context) {
        Object initializationValue = initialization.evaluate(eventsLambda, isNewData, context);

        if (target.isEmpty()) {
            return initializationValue;
        }

        ObjectArrayEventBean resultEvent = new ObjectArrayEventBean(new Object[1], resultEventType);
        ObjectArrayEventBean evalEvent = new ObjectArrayEventBean(new Object[1], evalEventType);

        for (Object next : target) {

            resultEvent.getProperties()[0] = initializationValue;
            evalEvent.getProperties()[0] = next;
            eventsLambda[streamNumLambda] = resultEvent;
            eventsLambda[streamNumLambda + 1] = evalEvent;

            initializationValue = innerExpression.evaluate(eventsLambda, isNewData, context);
        }

        return initializationValue;
    }
}
