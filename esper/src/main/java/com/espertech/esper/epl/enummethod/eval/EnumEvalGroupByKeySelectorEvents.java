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

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public class EnumEvalGroupByKeySelectorEvents extends EnumEvalBase implements EnumEval {

    public EnumEvalGroupByKeySelectorEvents(ExprEvaluator innerExpression, int streamCountIncoming) {
        super(innerExpression, streamCountIncoming);
    }

    public Object evaluateEnumMethod(EventBean[] eventsLambda, Collection target, boolean isNewData, ExprEvaluatorContext context) {
        Map<Object, Collection> result = new LinkedHashMap<Object, Collection>();

        Collection<EventBean> beans = (Collection<EventBean>) target;
        for (EventBean next : beans) {
            eventsLambda[streamNumLambda] = next;

            Object key = innerExpression.evaluate(eventsLambda, isNewData, context);

            Collection value = result.get(key);
            if (value == null) {
                value = new ArrayList();
                result.put(key, value);
            }
            value.add(next.getUnderlying());
        }

        return result;
    }
}
