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
package com.espertech.esper.common.internal.epl.expression.etc;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.hook.expr.EPLMethodInvocationContext;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.*;

import java.io.StringWriter;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class ExprEvalMethodContext implements ExprForge, ExprEvaluator, ExprNodeRenderable {

    private final String functionName;

    public ExprEvalMethodContext(String functionName) {
        this.functionName = functionName;
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        if (context == null) {
            return new EPLMethodInvocationContext(null,
                    -1,
                    null,
                    functionName,
                    null,
                    null);
        }
        return new EPLMethodInvocationContext(context.getStatementName(),
                context.getAgentInstanceId(),
                context.getRuntimeURI(),
                functionName,
                context.getUserObjectCompileTime(),
                null);
    }

    public ExprEvaluator getExprEvaluator() {
        return this;
    }

    public ExprForgeConstantType getForgeConstantType() {
        return ExprForgeConstantType.NONCONST;
    }

    public CodegenExpression evaluateCodegen(Class requiredType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        CodegenMethod methodNode = codegenMethodScope.makeChild(EPLMethodInvocationContext.class, ExprEvalMethodContext.class, codegenClassScope);
        CodegenExpressionRef refExprEvalCtx = exprSymbol.getAddExprEvalCtx(methodNode);

        CodegenExpression stmtName = exprDotMethod(refExprEvalCtx, "getStatementName");
        CodegenExpression cpid = exprDotMethod(refExprEvalCtx, "getAgentInstanceId");
        CodegenExpression runtimeURI = exprDotMethod(refExprEvalCtx, "getRuntimeURI");
        CodegenExpression userObject = exprDotMethod(refExprEvalCtx, "getUserObjectCompileTime");
        CodegenExpression eventBeanSvc = exprDotMethod(refExprEvalCtx, "getEventBeanService");
        methodNode.getBlock()
                .ifCondition(equalsNull(refExprEvalCtx))
                .blockReturn(newInstance(EPLMethodInvocationContext.class, constantNull(), constant(-1), constantNull(), constant(functionName), constantNull(), constantNull()))
                .methodReturn(newInstance(EPLMethodInvocationContext.class, stmtName, cpid, runtimeURI, constant(functionName), userObject, eventBeanSvc));
        return localMethod(methodNode);
    }

    public Class getEvaluationType() {
        return EPLMethodInvocationContext.class;
    }

    public ExprNodeRenderable getForgeRenderable() {
        return this;
    }

    public void toEPL(StringWriter writer, ExprPrecedenceEnum parentPrecedence) {
        writer.append(ExprEvalMethodContext.class.getSimpleName());
    }
}
