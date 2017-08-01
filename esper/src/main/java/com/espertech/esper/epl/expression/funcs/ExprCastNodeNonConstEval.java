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
package com.espertech.esper.epl.expression.funcs;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;

public class ExprCastNodeNonConstEval implements ExprEvaluator {
    private final ExprCastNode parent;
    private final ExprEvaluator evaluator;
    private final ExprCastNode.CasterParserComputer casterParserComputer;

    public ExprCastNodeNonConstEval(ExprCastNode parent, ExprEvaluator evaluator, ExprCastNode.CasterParserComputer casterParserComputer) {
        this.parent = parent;
        this.evaluator = evaluator;
        this.casterParserComputer = casterParserComputer;
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qExprCast(parent);
        }

        Object result = evaluator.evaluate(eventsPerStream, isNewData, context);
        if (result != null) {
            result = casterParserComputer.compute(result, eventsPerStream, isNewData, context);
        }

        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aExprCast(result);
        }
        return result;
    }

}
