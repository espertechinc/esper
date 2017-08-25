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
package com.espertech.esper.epl.core.select.eval;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMember;
import com.espertech.esper.codegen.base.CodegenMethodScope;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.epl.core.engineimport.EngineImportService;
import com.espertech.esper.epl.core.select.SelectExprProcessor;
import com.espertech.esper.epl.core.select.SelectExprProcessorForge;
import com.espertech.esper.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.codegen.base.CodegenMethodNode;
import com.espertech.esper.epl.core.select.SelectExprProcessorCodegenSymbol;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.expression.core.ExprForge;
import com.espertech.esper.epl.expression.codegen.ExprNodeCompiler;
import com.espertech.esper.event.EventAdapterService;

public abstract class EvalBaseFirstProp implements SelectExprProcessor, SelectExprProcessorForge {

    private final SelectExprForgeContext selectExprForgeContext;
    private final EventType resultEventType;
    private ExprEvaluator firstEvaluator;

    public EvalBaseFirstProp(SelectExprForgeContext selectExprForgeContext, EventType resultEventType) {
        this.selectExprForgeContext = selectExprForgeContext;
        this.resultEventType = resultEventType;
    }

    protected abstract EventBean processFirstCol(Object result);

    protected abstract CodegenExpression processFirstColCodegen(Class evaluationType, CodegenExpression expression, CodegenMember memberResultEventType, CodegenMember memberEventAdapterService, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope);

    public EventBean process(EventBean[] eventsPerStream, boolean isNewData, boolean isSynthesize, ExprEvaluatorContext exprEvaluatorContext) {
        Object first = firstEvaluator.evaluate(eventsPerStream, isNewData, exprEvaluatorContext);
        return processFirstCol(first);
    }

    public CodegenMethodNode processCodegen(CodegenMember memberResultEventType, CodegenMember memberEventAdapterService, CodegenMethodScope codegenMethodScope, SelectExprProcessorCodegenSymbol selectSymbol, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        CodegenMethodNode methodNode = codegenMethodScope.makeChild(EventBean.class, this.getClass(), codegenClassScope);
        ExprForge first = selectExprForgeContext.getExprForges()[0];
        Class evaluationType = first.getEvaluationType();
        methodNode.getBlock().methodReturn(processFirstColCodegen(evaluationType, first.evaluateCodegen(evaluationType, methodNode, exprSymbol, codegenClassScope), memberResultEventType, memberEventAdapterService, methodNode, codegenClassScope));
        return methodNode;
    }

    public EventAdapterService getEventAdapterService() {
        return selectExprForgeContext.getEventAdapterService();
    }

    public EventType getResultEventType() {
        return resultEventType;
    }

    public SelectExprProcessor getSelectExprProcessor(EngineImportService engineImportService, boolean isFireAndForget, String statementName) {
        if (firstEvaluator == null) {
            firstEvaluator = ExprNodeCompiler.allocateEvaluator(selectExprForgeContext.getExprForges()[0], engineImportService, this.getClass(), isFireAndForget, statementName);
        }
        return this;
    }
}
