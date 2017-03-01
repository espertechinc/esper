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
package com.espertech.esper.epl.expression.dot;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.epl.enummethod.dot.ExprDotStaticMethodWrap;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.rettype.EPTypeHelper;
import com.espertech.esper.epl.variable.VariableReader;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;

public class ExprDotEvalVariable implements ExprEvaluator {
    private final ExprDotNode dotNode;
    private final VariableReader variableReader;
    private final ExprDotStaticMethodWrap resultWrapLambda;
    private final ExprDotEval[] chainEval;

    public ExprDotEvalVariable(ExprDotNode dotNode, VariableReader variableReader, ExprDotStaticMethodWrap resultWrapLambda, ExprDotEval[] chainEval) {
        this.dotNode = dotNode;
        this.variableReader = variableReader;
        this.resultWrapLambda = resultWrapLambda;
        this.chainEval = chainEval;
    }

    public String getVariableName() {
        return variableReader.getVariableMetaData().getVariableName();
    }

    public Class getType() {
        if (chainEval.length == 0) {
            return variableReader.getVariableMetaData().getType();
        } else {
            return EPTypeHelper.getClassSingleValued(chainEval[chainEval.length - 1].getTypeInfo());
        }
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qExprDot(dotNode);
        }

        Object result = variableReader.getValue();
        result = ExprDotNodeUtility.evaluateChainWithWrap(resultWrapLambda, result, variableReader.getVariableMetaData().getEventType(), variableReader.getVariableMetaData().getType(), chainEval, eventsPerStream, isNewData, exprEvaluatorContext);

        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aExprDot(result);
        }
        return result;
    }
}
