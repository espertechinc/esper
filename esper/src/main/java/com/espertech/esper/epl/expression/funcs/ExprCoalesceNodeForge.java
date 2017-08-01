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

import com.espertech.esper.codegen.core.CodegenContext;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.model.method.CodegenParamSetExprPremade;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprForge;
import com.espertech.esper.epl.expression.core.ExprForgeComplexityEnum;
import com.espertech.esper.epl.expression.core.ExprNodeUtility;

public class ExprCoalesceNodeForge implements ExprForge {
    private final ExprCoalesceNode parent;
    private final Class resultType;
    private final boolean[] isNumericCoercion;

    public ExprCoalesceNodeForge(ExprCoalesceNode parent, Class resultType, boolean[] isNumericCoercion) {
        this.parent = parent;
        this.resultType = resultType;
        this.isNumericCoercion = isNumericCoercion;
    }

    public ExprCoalesceNode getForgeRenderable() {
        return parent;
    }

    public boolean[] getIsNumericCoercion() {
        return isNumericCoercion;
    }

    public ExprEvaluator getExprEvaluator() {
        return new ExprCoalesceNodeForgeEval(this, ExprNodeUtility.getEvaluatorsNoCompile(parent.getChildNodes()));
    }

    public Class getEvaluationType() {
        return resultType;
    }

    public CodegenExpression evaluateCodegen(CodegenParamSetExprPremade params, CodegenContext context) {
        return ExprCoalesceNodeForgeEval.codegen(this, context, params);
    }

    public ExprForgeComplexityEnum getComplexity() {
        return ExprForgeComplexityEnum.INTER;
    }
}
