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
import com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder;
import com.espertech.esper.epl.core.select.SelectExprProcessor;
import com.espertech.esper.event.vaevent.ValueAddEventProcessor;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.exprDotMethod;

public class EvalInsertNoWildcardSingleColCoercionRevisionBean extends EvalBaseFirstProp implements SelectExprProcessor {

    private final ValueAddEventProcessor vaeProcessor;
    private final EventType vaeInnerEventType;

    public EvalInsertNoWildcardSingleColCoercionRevisionBean(SelectExprForgeContext selectExprForgeContext, EventType resultEventType, ValueAddEventProcessor vaeProcessor, EventType vaeInnerEventType) {
        super(selectExprForgeContext, resultEventType);
        this.vaeProcessor = vaeProcessor;
        this.vaeInnerEventType = vaeInnerEventType;
    }

    public EventBean processFirstCol(Object result) {
        return vaeProcessor.getValueAddEventBean(super.getEventAdapterService().adapterForTypedBean(result, vaeInnerEventType));
    }

    protected CodegenExpression processFirstColCodegen(Class evaluationType, CodegenExpression expression, CodegenMember memberResultEventType, CodegenMember memberEventAdapterService, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        CodegenMember memberProcessor = codegenClassScope.makeAddMember(ValueAddEventProcessor.class, vaeProcessor);
        CodegenMember memberType = codegenClassScope.makeAddMember(EventType.class, vaeInnerEventType);
        return exprDotMethod(CodegenExpressionBuilder.member(memberProcessor.getMemberId()), "getValueAddEventBean", exprDotMethod(CodegenExpressionBuilder.member(memberEventAdapterService.getMemberId()), "adapterForTypedBean", expression, CodegenExpressionBuilder.member(memberType.getMemberId())));
    }
}
