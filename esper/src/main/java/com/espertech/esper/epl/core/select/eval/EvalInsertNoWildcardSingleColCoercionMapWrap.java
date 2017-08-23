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
import com.espertech.esper.event.WrapperEventType;

import java.util.Collections;
import java.util.Map;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

public class EvalInsertNoWildcardSingleColCoercionMapWrap extends EvalBaseFirstPropFromWrap implements SelectExprProcessor {

    public EvalInsertNoWildcardSingleColCoercionMapWrap(SelectExprForgeContext selectExprForgeContext, WrapperEventType wrapper) {
        super(selectExprForgeContext, wrapper);
    }

    public EventBean processFirstCol(Object result) {
        EventBean wrappedEvent = super.getEventAdapterService().adapterForTypedMap((Map) result, wrapper.getUnderlyingEventType());
        return super.getEventAdapterService().adapterForTypedWrapper(wrappedEvent, Collections.emptyMap(), wrapper);
    }

    protected CodegenExpression processFirstColCodegen(Class evaluationType, CodegenExpression expression, CodegenMember memberResultEventType, CodegenMember memberEventAdapterService, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return processFirstColCodegen(expression, memberEventAdapterService, codegenClassScope, wrapper, "adapterForTypedMap", Map.class);
    }

    public static CodegenExpression processFirstColCodegen(CodegenExpression expression, CodegenMember memberEventAdapterService, CodegenClassScope codegenClassScope, WrapperEventType wrapperEventType, String adapterMethod, Class castType) {
        CodegenMember memberUndType = codegenClassScope.makeAddMember(EventType.class, wrapperEventType.getUnderlyingEventType());
        CodegenMember memberWrapperType = codegenClassScope.makeAddMember(WrapperEventType.class, wrapperEventType);
        CodegenExpression wrapped = exprDotMethod(member(memberEventAdapterService.getMemberId()), adapterMethod, castType == Object.class ? expression : cast(castType, expression), member(memberUndType.getMemberId()));
        return exprDotMethod(member(memberEventAdapterService.getMemberId()), "adapterForTypedWrapper", wrapped, staticMethod(Collections.class, "emptyMap"), member(memberWrapperType.getMemberId()));
    }
}
