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

public class ExprInstanceofNodeForge implements ExprForge {
    private final ExprInstanceofNode parent;
    private final Class[] classes;

    ExprInstanceofNodeForge(ExprInstanceofNode parent, Class[] classes) {
        this.parent = parent;
        this.classes = classes;
    }

    public ExprInstanceofNode getForgeRenderable() {
        return parent;
    }

    public Class[] getClasses() {
        return classes;
    }

    public ExprEvaluator getExprEvaluator() {
        return new ExprInstanceofNodeForgeEval(this, parent.getChildNodes()[0].getForge().getExprEvaluator());
    }

    public CodegenExpression evaluateCodegen(CodegenParamSetExprPremade params, CodegenContext context) {
        return ExprInstanceofNodeForgeEval.codegen(this, context, params);
    }

    public ExprForgeComplexityEnum getComplexity() {
        return ExprForgeComplexityEnum.INTER;
    }

    public Class getEvaluationType() {
        return Boolean.class;
    }
}
