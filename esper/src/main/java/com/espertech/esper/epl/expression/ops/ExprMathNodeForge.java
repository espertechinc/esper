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

import com.espertech.esper.codegen.core.CodegenContext;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.model.method.CodegenParamSetExprPremade;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprForge;
import com.espertech.esper.epl.expression.core.ExprForgeComplexityEnum;
import com.espertech.esper.type.MathArithTypeEnum;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.localMethodBuild;

public class ExprMathNodeForge implements ExprForge {
    private final ExprMathNode parent;
    private final MathArithTypeEnum.Computer arithTypeEnumComputer;
    private final Class resultType;

    public ExprMathNodeForge(ExprMathNode parent, MathArithTypeEnum.Computer arithTypeEnumComputer, Class resultType) {
        this.parent = parent;
        this.arithTypeEnumComputer = arithTypeEnumComputer;
        this.resultType = resultType;
    }

    public ExprEvaluator getExprEvaluator() {
        return new ExprMathNodeForgeEval(this, parent.getChildNodes()[0].getForge().getExprEvaluator(), parent.getChildNodes()[1].getForge().getExprEvaluator());
    }

    public Class getEvaluationType() {
        return resultType;
    }

    public CodegenExpression evaluateCodegen(CodegenParamSetExprPremade params, CodegenContext context) {
        return localMethodBuild(ExprMathNodeForgeEval.codegen(this, context, params, parent.getChildNodes()[0], parent.getChildNodes()[1])).passAll(params).call();
    }

    public ExprForgeComplexityEnum getComplexity() {
        return ExprForgeComplexityEnum.INTER;
    }

    MathArithTypeEnum.Computer getArithTypeEnumComputer() {
        return arithTypeEnumComputer;
    }

    public ExprMathNode getForgeRenderable() {
        return parent;
    }
}
