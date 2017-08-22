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
import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMethodScope;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;

public class ExprDotNodeForgeTransposeAsStreamEval implements ExprEvaluator {

    private final ExprEvaluator inner;

    public ExprDotNodeForgeTransposeAsStreamEval(ExprEvaluator inner) {
        this.inner = inner;
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        return inner.evaluate(eventsPerStream, isNewData, context);
    }

    public static CodegenExpression codegen(ExprDotNodeForgeTransposeAsStream forge, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return forge.inner.evaluateCodegen(forge.inner.getEvaluationType(), codegenMethodScope, exprSymbol, codegenClassScope);
    }
}
