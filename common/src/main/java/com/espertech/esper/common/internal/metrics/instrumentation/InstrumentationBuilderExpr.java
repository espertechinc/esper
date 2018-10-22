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
package com.espertech.esper.common.internal.metrics.instrumentation;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprForgeInstrumentable;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityPrint;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class InstrumentationBuilderExpr {
    private final Class generator;
    private final ExprForgeInstrumentable forge;
    private final String qname;
    private final Class requiredType;
    private final CodegenMethodScope codegenMethodScope;
    private final ExprForgeCodegenSymbol exprSymbol;
    private final CodegenClassScope codegenClassScope;
    private final List<CodegenExpression> qParams = new ArrayList<>();

    public InstrumentationBuilderExpr(Class generator, ExprForgeInstrumentable forge, String qname, Class requiredType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        this.generator = generator;
        this.forge = forge;
        this.qname = qname;
        this.requiredType = requiredType;
        this.codegenMethodScope = codegenMethodScope;
        this.exprSymbol = exprSymbol;
        this.codegenClassScope = codegenClassScope;

        String text = ExprNodeUtilityPrint.toExpressionStringMinPrecedence(forge);
        this.qParams.add(0, constant(text));
    }

    public CodegenExpression build() {
        if (!codegenClassScope.isInstrumented()) {
            return forge.evaluateCodegenUninstrumented(requiredType, codegenMethodScope, exprSymbol, codegenClassScope);
        }

        Class evaluationType = forge.getEvaluationType();
        if (evaluationType == void.class) {
            return constantNull();
        }

        if (evaluationType == null) {
            CodegenMethod method = codegenMethodScope.makeChild(Object.class, generator, codegenClassScope);
            method.getBlock()
                    .ifCondition(publicConstValue(InstrumentationCommon.RUNTIME_HELPER_CLASS, "ENABLED"))
                    .expression(exprDotMethodChain(staticMethod(InstrumentationCommon.RUNTIME_HELPER_CLASS, "get")).add("q" + qname, qParams.toArray(new CodegenExpression[qParams.size()])))
                    .expression(exprDotMethodChain(staticMethod(InstrumentationCommon.RUNTIME_HELPER_CLASS, "get")).add("a" + qname, constantNull()))
                    .blockEnd()
                    .methodReturn(constantNull());
            return localMethod(method);
        }

        CodegenMethod method = codegenMethodScope.makeChild(evaluationType, generator, codegenClassScope);
        CodegenExpression expr = forge.evaluateCodegenUninstrumented(evaluationType, method, exprSymbol, codegenClassScope);
        method.getBlock()
                .ifCondition(publicConstValue(InstrumentationCommon.RUNTIME_HELPER_CLASS, "ENABLED"))
                .expression(exprDotMethodChain(staticMethod(InstrumentationCommon.RUNTIME_HELPER_CLASS, "get")).add("q" + qname, qParams.toArray(new CodegenExpression[qParams.size()])))
                .declareVar(evaluationType, "result", expr)
                .expression(exprDotMethodChain(staticMethod(InstrumentationCommon.RUNTIME_HELPER_CLASS, "get")).add("a" + qname, ref("result")))
                .blockReturn(ref("result"))
                .methodReturn(expr);
        return localMethod(method);
    }

    public InstrumentationBuilderExpr noqparam() {
        qParams.clear();
        return this;
    }

    public InstrumentationBuilderExpr qparam(CodegenExpression qparam) {
        this.qParams.add(qparam);
        return this;
    }

    public InstrumentationBuilderExpr qparams(CodegenExpression... qparams) {
        this.qParams.addAll(Arrays.asList(qparams));
        return this;
    }
}
