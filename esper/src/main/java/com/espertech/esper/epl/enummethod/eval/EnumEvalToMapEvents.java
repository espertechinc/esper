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
import java.util.HashMap;
import java.util.Map;

public class EnumEvalToMapEvents extends EnumEvalBase implements EnumEval {

    private ExprEvaluator secondExpression;

    public EnumEvalToMapEvents(ExprEvaluator innerExpression, int streamCountIncoming, ExprEvaluator secondExpression) {
        super(innerExpression, streamCountIncoming);
        this.secondExpression = secondExpression;
    }

    public Object evaluateEnumMethod(EventBean[] eventsLambda, Collection target, boolean isNewData, ExprEvaluatorContext context) {
        Map map = new HashMap();

        Collection<EventBean> beans = (Collection<EventBean>) target;
        for (EventBean next : beans) {
            eventsLambda[streamNumLambda] = next;

            Object key = innerExpression.evaluate(eventsLambda, isNewData, context);
            Object value = secondExpression.evaluate(eventsLambda, isNewData, context);
            map.put(key, value);
        }

        return map;
    }
}
