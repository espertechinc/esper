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

public class EnumEvalWhereScalarIndex extends EnumEvalBaseScalarIndex implements EnumEval {

    public EnumEvalWhereScalarIndex(ExprEvaluator innerExpression, int streamNumLambda, ObjectArrayEventType evalEventType, ObjectArrayEventType indexEventType) {
        super(innerExpression, streamNumLambda, evalEventType, indexEventType);
    }

    public Object evaluateEnumMethod(EventBean[] eventsLambda, Collection target, boolean isNewData, ExprEvaluatorContext context) {
        if (target.isEmpty()) {
            return target;
        }

        ArrayDeque<Object> result = new ArrayDeque<Object>();
        ObjectArrayEventBean evalEvent = new ObjectArrayEventBean(new Object[1], evalEventType);
        ObjectArrayEventBean indexEvent = new ObjectArrayEventBean(new Object[1], indexEventType);

        int count = -1;
        for (Object next : target) {

            count++;

            evalEvent.getProperties()[0] = next;
            eventsLambda[streamNumLambda] = evalEvent;

            indexEvent.getProperties()[0] = count;
            eventsLambda[streamNumLambda + 1] = indexEvent;

            Object pass = innerExpression.evaluate(eventsLambda, isNewData, context);
            if (pass == null || (!(Boolean) pass)) {
                continue;
            }

            result.add(next);
        }

        return result;
    }
}
