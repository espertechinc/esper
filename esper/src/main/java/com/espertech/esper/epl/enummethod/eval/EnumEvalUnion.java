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
import com.espertech.esper.epl.expression.core.ExprEvaluatorEnumeration;

import java.util.ArrayList;
import java.util.Collection;

public class EnumEvalUnion implements EnumEval {

    private final int numStreams;
    private final ExprEvaluatorEnumeration evaluator;
    private final boolean scalar;

    public EnumEvalUnion(int numStreams, ExprEvaluatorEnumeration evaluator, boolean scalar) {
        this.numStreams = numStreams;
        this.evaluator = evaluator;
        this.scalar = scalar;
    }

    public int getStreamNumSize() {
        return numStreams;
    }

    public Object evaluateEnumMethod(EventBean[] eventsLambda, Collection target, boolean isNewData, ExprEvaluatorContext context) {
        if (target == null) {
            return null;
        }

        Collection set;
        if (scalar) {
            set = evaluator.evaluateGetROCollectionScalar(eventsLambda, isNewData, context);
        } else {
            set = evaluator.evaluateGetROCollectionEvents(eventsLambda, isNewData, context);
        }

        if (set == null || set.isEmpty()) {
            return target;
        }

        ArrayList<Object> result = new ArrayList<Object>(target);
        result.addAll(set);

        return result;
    }
}
