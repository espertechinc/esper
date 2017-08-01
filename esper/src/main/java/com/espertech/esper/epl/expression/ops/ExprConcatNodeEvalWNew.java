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
package com.espertech.esper.epl.expression.ops;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;

public class ExprConcatNodeEvalWNew implements ExprEvaluator {
    private final ExprConcatNode parent;
    private final ExprEvaluator[] evaluators;

    public ExprConcatNodeEvalWNew(ExprConcatNode parent, ExprEvaluator[] evaluators) {
        this.parent = parent;
        this.evaluators = evaluators;
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        StringBuffer buffer = new StringBuffer();
        return evaluate(eventsPerStream, isNewData, context, buffer, evaluators, parent);
    }

    protected static String evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context, StringBuffer buffer, ExprEvaluator[] evaluators, ExprConcatNode parent) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qExprConcat(parent);
        }
        for (ExprEvaluator child : evaluators) {
            String result = (String) child.evaluate(eventsPerStream, isNewData, context);
            if (result == null) {
                if (InstrumentationHelper.ENABLED) {
                    InstrumentationHelper.get().aExprConcat(null);
                }
                return null;
            }
            buffer.append(result);
        }
        String result = buffer.toString();
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aExprConcat(result);
        }
        return result;
    }

}
