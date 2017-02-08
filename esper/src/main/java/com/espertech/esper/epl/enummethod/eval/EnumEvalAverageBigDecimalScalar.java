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
import com.espertech.esper.epl.agg.aggregator.AggregatorAvgBigDecimal;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;

import java.math.MathContext;
import java.util.Collection;

public class EnumEvalAverageBigDecimalScalar extends EnumEvalBase implements EnumEval {

    private final MathContext optionalMathContext;

    public EnumEvalAverageBigDecimalScalar(int streamCountIncoming, MathContext optionalMathContext) {
        super(streamCountIncoming);
        this.optionalMathContext = optionalMathContext;
    }

    public Object evaluateEnumMethod(EventBean[] eventsLambda, Collection target, boolean isNewData, ExprEvaluatorContext context) {

        AggregatorAvgBigDecimal agg = new AggregatorAvgBigDecimal(optionalMathContext);

        for (Object next : target) {

            Number num = (Number) next;
            if (num == null) {
                continue;
            }
            agg.enter(num);
        }

        return agg.getValue();
    }
}
