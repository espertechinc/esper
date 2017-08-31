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
package com.espertech.esper.epl.expression.core;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.hook.EPLMethodInvocationContext;
import com.espertech.esper.client.hook.EventBeanService;
import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMember;
import com.espertech.esper.codegen.base.CodegenMethodScope;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.model.expression.CodegenExpressionRef;
import com.espertech.esper.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.codegen.base.CodegenMethodNode;

import java.io.StringWriter;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

public class ExprNodeUtilExprMethodContext implements ExprForge, ExprEvaluator, ExprNodeRenderable {

    private final EPLMethodInvocationContext defaultContextForFilters;

    public ExprNodeUtilExprMethodContext(String engineURI, String functionName, EventBeanService eventBeanService) {
        this.defaultContextForFilters = new EPLMethodInvocationContext(null, -1, engineURI, functionName, null, eventBeanService);
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        if (context == null) {
            return defaultContextForFilters;
        }
        return new EPLMethodInvocationContext(context.getStatementName(),
                context.getAgentInstanceId(),
                defaultContextForFilters.getEngineURI(),
                defaultContextForFilters.getFunctionName(),
                context.getStatementUserObject(),
                defaultContextForFilters.getEventBeanService());
    }

    public ExprEvaluator getExprEvaluator() {
        return this;
    }

    public CodegenExpression evaluateCodegen(Class requiredType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        CodegenMethodNode methodNode = codegenMethodScope.makeChild(EPLMethodInvocationContext.class, ExprNodeUtilExprMethodContext.class, codegenClassScope);
        CodegenExpressionRef refExprEvalCtx = exprSymbol.getAddExprEvalCtx(methodNode);

        CodegenExpression stmtName = exprDotMethod(refExprEvalCtx, "getStatementName");
        CodegenExpression cpid = exprDotMethod(refExprEvalCtx, "getAgentInstanceId");
        CodegenExpression engineURI = constant(defaultContextForFilters.getEngineURI());
        CodegenExpression functionName = constant(defaultContextForFilters.getFunctionName());
        CodegenExpression userObject = exprDotMethod(refExprEvalCtx, "getStatementUserObject");
        CodegenMember defaultCtx = codegenClassScope.makeAddMember(EPLMethodInvocationContext.class, defaultContextForFilters);
        methodNode.getBlock()
                .ifCondition(equalsNull(refExprEvalCtx))
                .blockReturn(member(defaultCtx.getMemberId()))
                .methodReturn(newInstance(EPLMethodInvocationContext.class, stmtName, cpid, engineURI, functionName, userObject,
                        exprDotMethod(member(defaultCtx.getMemberId()), "getEventBeanService")));
        return localMethod(methodNode);
    }

    public Class getEvaluationType() {
        return EPLMethodInvocationContext.class;
    }

    public ExprForgeComplexityEnum getComplexity() {
        return ExprForgeComplexityEnum.SINGLE;
    }

    public ExprNodeRenderable getForgeRenderable() {
        return this;
    }

    public void toEPL(StringWriter writer, ExprPrecedenceEnum parentPrecedence) {
        writer.append(ExprNodeUtilExprMethodContext.class.getSimpleName());
    }
}
