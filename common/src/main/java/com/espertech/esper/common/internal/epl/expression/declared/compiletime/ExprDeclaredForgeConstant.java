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
package com.espertech.esper.common.internal.epl.expression.declared.compiletime;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.compile.stage1.spec.ExpressionDeclItem;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.*;
import com.espertech.esper.common.internal.metrics.instrumentation.InstrumentationBuilderExpr;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.common.internal.epl.expression.declared.compiletime.ExprDeclaredForgeBase.getInstrumentationQParams;

public class ExprDeclaredForgeConstant implements ExprForgeInstrumentable, ExprEvaluator {
    private final ExprDeclaredNodeImpl parent;
    private final Class returnType;
    private final ExpressionDeclItem prototype;
    private final Object value;
    private final boolean audit;
    private final String statementName;

    public ExprDeclaredForgeConstant(ExprDeclaredNodeImpl parent, Class returnType, ExpressionDeclItem prototype, Object value, boolean audit, String statementName) {
        this.parent = parent;
        this.returnType = returnType;
        this.prototype = prototype;
        this.value = value;
        this.audit = audit;
        this.statementName = statementName;
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        return value;
    }

    public ExprEvaluator getExprEvaluator() {
        return this;
    }

    public CodegenExpression evaluateCodegenUninstrumented(Class requiredType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        if (!audit) {
            return constant(value);
        }
        CodegenMethod methodNode = codegenMethodScope.makeChild(returnType, ExprDeclaredForgeConstant.class, codegenClassScope);

        methodNode.getBlock()
                .expression(exprDotMethodChain(exprSymbol.getAddExprEvalCtx(methodNode)).add("getAuditProvider").add("exprdef", constant(parent.getPrototype().getName()), constant(value), exprSymbol.getAddExprEvalCtx(methodNode)))
                .methodReturn(constant(value));
        return localMethod(methodNode);
    }

    public CodegenExpression evaluateCodegen(Class requiredType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return new InstrumentationBuilderExpr(this.getClass(), this, "ExprDeclared", requiredType, codegenMethodScope, exprSymbol, codegenClassScope).qparams(getInstrumentationQParams(parent, codegenClassScope)).build();
    }

    public Class getEvaluationType() {
        return returnType;
    }

    public ExprNodeRenderable getForgeRenderable() {
        return parent;
    }

    public ExprForgeConstantType getForgeConstantType() {
        return ExprForgeConstantType.COMPILETIMECONST;
    }
}
