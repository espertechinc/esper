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

public class EnumEvalSumEvents extends EnumEvalBase implements EnumEval {

    private final ExprDotEvalSumMethodFactory sumMethodFactory;

    public EnumEvalSumEvents(ExprEvaluator innerExpression, int streamCountIncoming, ExprDotEvalSumMethodFactory sumMethodFactory) {
        super(innerExpression, streamCountIncoming);
        this.sumMethodFactory = sumMethodFactory;
    }

    public Object evaluateEnumMethod(EventBean[] eventsLambda, Collection target, boolean isNewData, ExprEvaluatorContext context) {

        ExprDotEvalSumMethod method = sumMethodFactory.getSumAggregator();

        Collection<EventBean> beans = (Collection<EventBean>) target;
        for (EventBean next : beans) {
            eventsLambda[streamNumLambda] = next;

            Object value = innerExpression.evaluate(eventsLambda, isNewData, context);
            method.enter(value);
        }

        return method.getValue();
    }
}
