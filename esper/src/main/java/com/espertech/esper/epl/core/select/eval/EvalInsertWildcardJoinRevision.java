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
import com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder;
import com.espertech.esper.epl.core.engineimport.EngineImportService;
import com.espertech.esper.epl.core.select.SelectExprProcessor;
import com.espertech.esper.epl.core.select.SelectExprProcessorForge;
import com.espertech.esper.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.codegen.base.CodegenMethodNode;
import com.espertech.esper.epl.core.select.SelectExprProcessorCodegenSymbol;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.event.vaevent.ValueAddEventProcessor;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.exprDotMethod;
import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.localMethod;

public class EvalInsertWildcardJoinRevision extends EvalBase implements SelectExprProcessor, SelectExprProcessorForge {

    private final SelectExprProcessorForge joinWildcardProcessorForge;
    private final ValueAddEventProcessor vaeProcessor;
    private SelectExprProcessor joinWildcardProcessor;

    public EvalInsertWildcardJoinRevision(SelectExprForgeContext selectExprForgeContext, EventType resultEventType, SelectExprProcessorForge joinWildcardProcessorForge, ValueAddEventProcessor vaeProcessor) {
        super(selectExprForgeContext, resultEventType);
        this.joinWildcardProcessorForge = joinWildcardProcessorForge;
        this.vaeProcessor = vaeProcessor;
    }

    public SelectExprProcessor getSelectExprProcessor(EngineImportService engineImportService, boolean isFireAndForget, String statementName) {
        joinWildcardProcessor = joinWildcardProcessorForge.getSelectExprProcessor(engineImportService, isFireAndForget, statementName);
        return this;
    }

    public EventBean process(EventBean[] eventsPerStream, boolean isNewData, boolean isSynthesize, ExprEvaluatorContext exprEvaluatorContext) {
        EventBean theEvent = joinWildcardProcessor.process(eventsPerStream, isNewData, isSynthesize, exprEvaluatorContext);
        return vaeProcessor.getValueAddEventBean(theEvent);
    }

    public CodegenMethodNode processCodegen(CodegenMember memberResultEventType, CodegenMember memberEventAdapterService, CodegenMethodScope codegenMethodScope, SelectExprProcessorCodegenSymbol selectSymbol, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        CodegenMember processor = codegenClassScope.makeAddMember(ValueAddEventProcessor.class, vaeProcessor);
        CodegenMethodNode methodNode = codegenMethodScope.makeChild(EventBean.class, this.getClass(), codegenClassScope);
        CodegenMethodNode jw = joinWildcardProcessorForge.processCodegen(memberResultEventType, memberEventAdapterService, methodNode, selectSymbol, exprSymbol, codegenClassScope);
        methodNode.getBlock().methodReturn(exprDotMethod(CodegenExpressionBuilder.member(processor.getMemberId()), "getValueAddEventBean", localMethod(jw)));
        return methodNode;
    }
}