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
package com.espertech.esper.epl.core.eval;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.codegen.core.CodegenContext;
import com.espertech.esper.codegen.core.CodegenMember;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.model.method.CodegenParamSetSelectPremade;
import com.espertech.esper.epl.core.EngineImportService;
import com.espertech.esper.epl.core.SelectExprProcessor;
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

    protected CodegenExpression processSpecificCodegen(CodegenMember memberResultEventType, CodegenMember memberEventAdapterService, CodegenExpression props, CodegenParamSetSelectPremade params, CodegenContext context) {
        CodegenMember processor = context.makeAddMember(ValueAddEventProcessor.class, vaeProcessor);
        CodegenMember innerType = context.makeAddMember(EventType.class, wrappingEventType);
        CodegenExpression wrapped = exprDotMethod(member(memberEventAdapterService.getMemberId()), "adapterForTypedWrapper", arrayAtIndex(params.passEPS(), constant(0)), ref("props"), member(innerType.getMemberId()));
        return exprDotMethod(member(processor.getMemberId()), "getValueAddEventBean", wrapped);
    }
}
