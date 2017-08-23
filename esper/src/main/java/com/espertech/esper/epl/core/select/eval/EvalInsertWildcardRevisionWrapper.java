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
import com.espertech.esper.codegen.model.expression.CodegenExpressionRef;
import com.espertech.esper.epl.core.engineimport.EngineImportService;
import com.espertech.esper.epl.core.select.SelectExprProcessor;
import com.espertech.esper.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.codegen.base.CodegenMethodNode;
import com.espertech.esper.epl.core.select.SelectExprProcessorCodegenSymbol;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.event.vaevent.ValueAddEventProcessor;

import java.util.Map;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

public class EvalInsertWildcardRevisionWrapper extends EvalBaseMap implements SelectExprProcessor {

    private final ValueAddEventProcessor vaeProcessor;
    private final EventType wrappingEventType;

    public EvalInsertWildcardRevisionWrapper(SelectExprForgeContext selectExprForgeContext, EventType resultEventType, ValueAddEventProcessor vaeProcessor, EventType wrappingEventType) {
        super(selectExprForgeContext, resultEventType);
        this.vaeProcessor = vaeProcessor;
        this.wrappingEventType = wrappingEventType;
    }

    protected void initSelectExprProcessorSpecific(EngineImportService engineImportService, boolean isFireAndForget, String statementName) {
    }

    public EventBean processSpecific(Map<String, Object> props, EventBean[] eventsPerStream, boolean isNewData, boolean isSynthesize, ExprEvaluatorContext exprEvaluatorContext) {
        EventBean underlying = eventsPerStream[0];
        EventBean wrapped = super.getEventAdapterService().adapterForTypedWrapper(underlying, props, wrappingEventType);
        return vaeProcessor.getValueAddEventBean(wrapped);
    }

    protected CodegenExpression processSpecificCodegen(CodegenMember memberResultEventType, CodegenMember memberEventAdapterService, CodegenExpression props, CodegenMethodNode methodNode, SelectExprProcessorCodegenSymbol selectEnv, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        CodegenMember processor = codegenClassScope.makeAddMember(ValueAddEventProcessor.class, vaeProcessor);
        CodegenMember innerType = codegenClassScope.makeAddMember(EventType.class, wrappingEventType);
        CodegenExpressionRef refEPS = exprSymbol.getAddEPS(methodNode);
        CodegenExpression wrapped = exprDotMethod(member(memberEventAdapterService.getMemberId()), "adapterForTypedWrapper", arrayAtIndex(refEPS, constant(0)), ref("props"), member(innerType.getMemberId()));
        return exprDotMethod(member(processor.getMemberId()), "getValueAddEventBean", wrapped);
    }
}
