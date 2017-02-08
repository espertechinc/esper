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

public class EnumEvalAverageScalar extends EnumEvalBase implements EnumEval {

    public EnumEvalAverageScalar(int streamCountIncoming) {
        super(streamCountIncoming);
    }

    public Object evaluateEnumMethod(EventBean[] eventsLambda, Collection target, boolean isNewData, ExprEvaluatorContext context) {
        double sum = 0d;
        int count = 0;

        for (Object next : target) {

            Number num = (Number) next;
            if (num == null) {
                continue;
            }
            count++;
            sum += num.doubleValue();
        }

        if (count == 0) {
            return null;
        }
        return sum / count;
    }
}
