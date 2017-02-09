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

public class EnumEvalDistinctEvents extends EnumEvalBase implements EnumEval {

    public EnumEvalDistinctEvents(ExprEvaluator innerExpression, int streamCountIncoming) {
        super(innerExpression, streamCountIncoming);
    }

    public Object evaluateEnumMethod(EventBean[] eventsLambda, Collection target, boolean isNewData, ExprEvaluatorContext context) {
        Collection<EventBean> beans = (Collection<EventBean>) target;
        if (beans.isEmpty() || beans.size() == 1) {
            return beans;
        }

        Map<Comparable, EventBean> distinct = new LinkedHashMap<Comparable, EventBean>();
        for (EventBean next : beans) {
            eventsLambda[streamNumLambda] = next;

            Comparable comparable = (Comparable) innerExpression.evaluate(eventsLambda, isNewData, context);
            if (!distinct.containsKey(comparable)) {
                distinct.put(comparable, next);
            }
        }

        return distinct.values();
    }
}
