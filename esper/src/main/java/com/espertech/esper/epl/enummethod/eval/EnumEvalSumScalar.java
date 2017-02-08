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
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;

import java.util.Collection;

public class EnumEvalSumScalar extends EnumEvalBase implements EnumEval {

    private final ExprDotEvalSumMethodFactory sumMethodFactory;

    public EnumEvalSumScalar(int streamCountIncoming, ExprDotEvalSumMethodFactory sumMethodFactory) {
        super(streamCountIncoming);
        this.sumMethodFactory = sumMethodFactory;
    }

    public Object evaluateEnumMethod(EventBean[] eventsLambda, Collection target, boolean isNewData, ExprEvaluatorContext context) {

        ExprDotEvalSumMethod method = sumMethodFactory.getSumAggregator();
        for (Object next : target) {
            method.enter(next);
        }
        return method.getValue();
    }
}
