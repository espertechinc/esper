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
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder;
import com.espertech.esper.epl.core.engineimport.EngineImportService;
import com.espertech.esper.epl.core.select.SelectExprProcessor;
import com.espertech.esper.epl.core.select.SelectExprProcessorForge;
import com.espertech.esper.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.codegen.base.CodegenMethodNode;
import com.espertech.esper.epl.core.select.SelectExprProcessorCodegenSymbol;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;

import java.util.Map;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.exprDotMethod;
import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.localMethod;

public class EvalSelectWildcardJoin extends EvalBaseMap implements SelectExprProcessor, SelectExprProcessorForge {

    private final SelectExprProcessorForge joinWildcardProcessorForge;
    private SelectExprProcessor joinWildcardProcessor;

    public EvalSelectWildcardJoin(SelectExprForgeContext selectExprForgeContext, EventType resultEventType, SelectExprProcessorForge joinWildcardProcessorForge) {
        super(selectExprForgeContext, resultEventType);
        this.joinWildcardProcessorForge = joinWildcardProcessorForge;
    }

    protected void initSelectExprProcessorSpecific(EngineImportService engineImportService, boolean isFireAndForget, String statementName) {
        this.joinWildcardProcessor = joinWildcardProcessorForge.getSelectExprProcessor(engineImportService, isFireAndForget, statementName);
    }

    public EventBean processSpecific(Map<String, Object> props, EventBean[] eventsPerStream, boolean isNewData, boolean isSynthesize, ExprEvaluatorContext exprEvaluatorContext) {
        EventBean theEvent = joinWildcardProcessor.process(eventsPerStream, isNewData, isSynthesize, exprEvaluatorContext);

        // Using a wrapper bean since we cannot use the same event type else same-type filters match.
        // Wrapping it even when not adding properties is very inexpensive.
        return super.getEventAdapterService().adapterForTypedWrapper(theEvent, props, super.getResultEventType());
    }

    protected CodegenExpression processSpecificCodegen(CodegenMember memberResultEventType, CodegenMember memberEventAdapterService, CodegenExpression props, CodegenMethodNode methodNode, SelectExprProcessorCodegenSymbol selectEnv, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        CodegenMethodNode inner = joinWildcardProcessorForge.processCodegen(memberResultEventType, memberEventAdapterService, methodNode, selectEnv, exprSymbol, codegenClassScope);
        return exprDotMethod(CodegenExpressionBuilder.member(memberEventAdapterService.getMemberId()), "adapterForTypedWrapper", localMethod(inner), props, CodegenExpressionBuilder.member(memberResultEventType.getMemberId()));
    }
}
