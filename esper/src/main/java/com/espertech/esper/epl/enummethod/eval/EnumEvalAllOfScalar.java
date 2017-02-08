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

public class EnumEvalAllOfScalar extends EnumEvalBaseScalar implements EnumEval {

    public EnumEvalAllOfScalar(ExprEvaluator innerExpression, int streamCountIncoming, ObjectArrayEventType type) {
        super(innerExpression, streamCountIncoming, type);
    }

    public Object evaluateEnumMethod(EventBean[] eventsLambda, Collection target, boolean isNewData, ExprEvaluatorContext context) {
        if (target.isEmpty()) {
            return true;
        }

        ObjectArrayEventBean evalEvent = new ObjectArrayEventBean(new Object[1], type);
        for (Object next : target) {

            evalEvent.getProperties()[0] = next;
            eventsLambda[streamNumLambda] = evalEvent;

            Object pass = innerExpression.evaluate(eventsLambda, isNewData, context);
            if (pass != null && (!(Boolean) pass)) {
                return false;
            }
        }

        return true;
    }
}
