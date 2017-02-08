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
import java.util.Collections;

public class EnumEvalTakeWhileLastIndexEvents extends EnumEvalBaseIndex implements EnumEval {

    public EnumEvalTakeWhileLastIndexEvents(ExprEvaluator innerExpression, int streamNumLambda, ObjectArrayEventType indexEventType) {
        super(innerExpression, streamNumLambda, indexEventType);
    }

    public Object evaluateEnumMethod(EventBean[] eventsLambda, Collection target, boolean isNewData, ExprEvaluatorContext context) {
        if (target.isEmpty()) {
            return target;
        }
        ObjectArrayEventBean indexEvent = new ObjectArrayEventBean(new Object[1], indexEventType);
        Collection<EventBean> beans = (Collection<EventBean>) target;
        if (target.size() == 1) {
            EventBean item = beans.iterator().next();
            indexEvent.getProperties()[0] = 0;
            eventsLambda[streamNumLambda] = item;
            eventsLambda[streamNumLambda + 1] = indexEvent;

            Object pass = innerExpression.evaluate(eventsLambda, isNewData, context);
            if (pass == null || (!(Boolean) pass)) {
                return Collections.emptyList();
            }
            return Collections.singletonList(item);
        }

        int size = target.size();
        EventBean[] all = new EventBean[size];
        int count = 0;
        for (EventBean item : beans) {
            all[count++] = item;
        }

        ArrayDeque<Object> result = new ArrayDeque<Object>();
        int index = 0;
        for (int i = all.length - 1; i >= 0; i--) {

            indexEvent.getProperties()[0] = index++;
            eventsLambda[streamNumLambda] = all[i];
            eventsLambda[streamNumLambda + 1] = indexEvent;

            Object pass = innerExpression.evaluate(eventsLambda, isNewData, context);
            if (pass == null || (!(Boolean) pass)) {
                break;
            }
            result.addFirst(all[i]);
        }

        return result;
    }
}
