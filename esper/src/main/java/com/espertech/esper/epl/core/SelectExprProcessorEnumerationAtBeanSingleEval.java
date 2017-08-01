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
package com.espertech.esper.epl.core;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.codegen.core.CodegenContext;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.model.method.CodegenParamSetExprPremade;
import com.espertech.esper.epl.expression.core.*;

public class SelectExprProcessorEnumerationAtBeanSingleEval implements ExprEvaluator {
    private final SelectExprProcessorEnumerationAtBeanSingleForge forge;
    private final ExprEnumerationEval enumEval;

    public SelectExprProcessorEnumerationAtBeanSingleEval(SelectExprProcessorEnumerationAtBeanSingleForge forge, ExprEnumerationEval enumEval) {
        this.forge = forge;
        this.enumEval = enumEval;
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        return enumEval.evaluateGetEventBean(eventsPerStream, isNewData, context);
    }

    public static CodegenExpression codegen(SelectExprProcessorEnumerationAtBeanSingleForge forge, CodegenParamSetExprPremade params, CodegenContext context) {
        return forge.enumerationForge.evaluateGetEventBeanCodegen(params, context);
    }
}
