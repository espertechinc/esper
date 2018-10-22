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
package com.espertech.esper.common.internal.epl.expression.ops;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.epl.expression.core.ExprForgeConstantType;
import com.espertech.esper.common.internal.epl.expression.core.ExprForgeInstrumentable;
import com.espertech.esper.common.internal.metrics.instrumentation.InstrumentationBuilderExpr;
import com.espertech.esper.common.internal.type.MathArithTypeEnum;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.constant;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.localMethod;

public class ExprMathNodeForge implements ExprForgeInstrumentable {
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

    public CodegenExpression evaluateCodegenUninstrumented(Class requiredType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        CodegenMethod methodNode = ExprMathNodeForgeEval.codegen(this, codegenMethodScope, exprSymbol, codegenClassScope, parent.getChildNodes()[0], parent.getChildNodes()[1]);
        return localMethod(methodNode);
    }

    public CodegenExpression evaluateCodegen(Class requiredType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return new InstrumentationBuilderExpr(this.getClass(), this, "ExprMath", requiredType, codegenMethodScope, exprSymbol, codegenClassScope).qparam(constant(parent.getMathArithTypeEnum().getExpressionText())).build();
    }

    MathArithTypeEnum.Computer getArithTypeEnumComputer() {
        return arithTypeEnumComputer;
    }

    public ExprMathNode getForgeRenderable() {
        return parent;
    }

    public ExprForgeConstantType getForgeConstantType() {
        return ExprForgeConstantType.NONCONST;
    }
}
