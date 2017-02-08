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

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;

public class EnumEvalSelectFromEvents extends EnumEvalBase implements EnumEval {

    public EnumEvalSelectFromEvents(ExprEvaluator innerExpression, int streamCountIncoming) {
        super(innerExpression, streamCountIncoming);
    }

    public Object evaluateEnumMethod(EventBean[] eventsLambda, Collection target, boolean isNewData, ExprEvaluatorContext context) {

        if (target.isEmpty()) {
            return target;
        }

        Collection<EventBean> beans = (Collection<EventBean>) target;
        Deque queue = new ArrayDeque();
        for (EventBean next : beans) {
            eventsLambda[streamNumLambda] = next;

            Object item = innerExpression.evaluate(eventsLambda, isNewData, context);
            if (item != null) {
                queue.add(item);
            }
        }

        return queue;
    }
}
