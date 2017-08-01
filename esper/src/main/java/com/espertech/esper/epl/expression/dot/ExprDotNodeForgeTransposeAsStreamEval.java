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
import com.espertech.esper.codegen.core.CodegenContext;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.model.method.CodegenParamSetExprPremade;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;

public class ExprDotNodeForgeTransposeAsStreamEval implements ExprEvaluator {

    private final ExprDotNodeForgeTransposeAsStream forge;
    private final ExprEvaluator inner;

    public ExprDotNodeForgeTransposeAsStreamEval(ExprDotNodeForgeTransposeAsStream forge, ExprEvaluator inner) {
        this.forge = forge;
        this.inner = inner;
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        return inner.evaluate(eventsPerStream, isNewData, context);
    }

    public static CodegenExpression codegen(ExprDotNodeForgeTransposeAsStream forge, CodegenParamSetExprPremade params, CodegenContext context) {
        return forge.inner.evaluateCodegen(params, context);
    }
}
