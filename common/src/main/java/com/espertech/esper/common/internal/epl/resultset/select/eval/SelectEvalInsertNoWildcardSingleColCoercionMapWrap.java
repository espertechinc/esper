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
package com.espertech.esper.common.internal.epl.resultset.select.eval;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.client.type.EPTypePremade;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionField;
import com.espertech.esper.common.internal.context.module.EPStatementInitServices;
import com.espertech.esper.common.internal.epl.resultset.select.core.SelectExprForgeContext;
import com.espertech.esper.common.internal.event.core.EventTypeUtility;
import com.espertech.esper.common.internal.event.core.WrapperEventType;

import java.util.Collections;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class SelectEvalInsertNoWildcardSingleColCoercionMapWrap extends SelectEvalBaseFirstPropFromWrap {

    public SelectEvalInsertNoWildcardSingleColCoercionMapWrap(SelectExprForgeContext selectExprForgeContext, WrapperEventType wrapper) {
        super(selectExprForgeContext, wrapper);
    }

    protected CodegenExpression processFirstColCodegen(EPTypeClass evaluationType, CodegenExpression expression, CodegenExpression resultEventType, CodegenExpression eventBeanFactory, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return processFirstColCodegen(expression, eventBeanFactory, codegenClassScope, wrapper, "adapterForTypedMap", EPTypePremade.MAP.getEPType());
    }

    public static CodegenExpression processFirstColCodegen(CodegenExpression expression, CodegenExpression eventBeanFactory, CodegenClassScope codegenClassScope, WrapperEventType wrapperEventType, String adapterMethod, EPTypeClass castType) {
        CodegenExpressionField memberUndType = codegenClassScope.addFieldUnshared(true, EventType.EPTYPE, EventTypeUtility.resolveTypeCodegen(wrapperEventType.getUnderlyingEventType(), EPStatementInitServices.REF));
        CodegenExpressionField memberWrapperType = codegenClassScope.addFieldUnshared(true, WrapperEventType.EPTYPE, cast(WrapperEventType.EPTYPE, EventTypeUtility.resolveTypeCodegen(wrapperEventType, EPStatementInitServices.REF)));
        CodegenExpression wrapped = exprDotMethod(eventBeanFactory, adapterMethod, castType.getType() == Object.class ? expression : cast(castType, expression), memberUndType);
        return exprDotMethod(eventBeanFactory, "adapterForTypedWrapper", wrapped, staticMethod(Collections.class, "emptyMap"), memberWrapperType);
    }
}
