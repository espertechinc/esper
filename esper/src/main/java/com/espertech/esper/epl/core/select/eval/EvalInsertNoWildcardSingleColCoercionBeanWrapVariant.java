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
import com.espertech.esper.epl.core.select.SelectExprProcessor;
import com.espertech.esper.codegen.base.CodegenMethodNode;
import com.espertech.esper.event.vaevent.ValueAddEventProcessor;

import java.util.Collections;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

public class EvalInsertNoWildcardSingleColCoercionBeanWrapVariant extends EvalBaseFirstProp implements SelectExprProcessor {

    private final ValueAddEventProcessor vaeProcessor;

    public EvalInsertNoWildcardSingleColCoercionBeanWrapVariant(SelectExprForgeContext selectExprForgeContext, EventType resultEventType, ValueAddEventProcessor vaeProcessor) {
        super(selectExprForgeContext, resultEventType);
        this.vaeProcessor = vaeProcessor;
    }

    public EventBean processFirstCol(Object result) {
        EventBean wrappedEvent = super.getEventAdapterService().adapterForBean(result);
        EventBean variant = vaeProcessor.getValueAddEventBean(wrappedEvent);
        return super.getEventAdapterService().adapterForTypedWrapper(variant, Collections.emptyMap(), super.getResultEventType());
    }

    protected CodegenExpression processFirstColCodegen(Class evaluationType, CodegenExpression expression, CodegenMember memberResultEventType, CodegenMember memberEventAdapterService, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        CodegenMember processor = codegenClassScope.makeAddMember(ValueAddEventProcessor.class, vaeProcessor);
        CodegenMethodNode method = codegenMethodScope.makeChild(EventBean.class, this.getClass(), codegenClassScope).addParam(evaluationType, "result").getBlock()
                .declareVar(EventBean.class, "wrappedEvent", exprDotMethod(member(memberEventAdapterService.getMemberId()), "adapterForBean", ref("result")))
                .declareVar(EventBean.class, "variant", exprDotMethod(member(processor.getMemberId()), "getValueAddEventBean", ref("wrappedEvent")))
                .methodReturn(exprDotMethod(member(memberEventAdapterService.getMemberId()), "adapterForTypedWrapper", ref("variant"), staticMethod(Collections.class, "emptyMap"), member(memberResultEventType.getMemberId())));
        return localMethodBuild(method).pass(expression).call();
    }
}
