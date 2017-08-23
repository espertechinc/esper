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
package com.espertech.esper.epl.core.select;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMethodScope;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.epl.expression.core.ExprEnumerationEval;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;

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

    public static CodegenExpression codegen(SelectExprProcessorEnumerationAtBeanSingleForge forge, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return forge.enumerationForge.evaluateGetEventBeanCodegen(codegenMethodScope, exprSymbol, codegenClassScope);
    }
}
