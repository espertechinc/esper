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
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionField;
import com.espertech.esper.common.internal.context.module.EPStatementInitServices;
import com.espertech.esper.common.internal.epl.resultset.select.core.SelectExprForgeContext;
import com.espertech.esper.common.internal.event.core.EventTypeUtility;
import com.espertech.esper.common.internal.event.core.WrapperEventType;

import java.util.Collections;
import java.util.Map;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class SelectEvalInsertNoWildcardSingleColCoercionMapWrap extends SelectEvalBaseFirstPropFromWrap {

    public SelectEvalInsertNoWildcardSingleColCoercionMapWrap(SelectExprForgeContext selectExprForgeContext, WrapperEventType wrapper) {
        super(selectExprForgeContext, wrapper);
    }

    protected CodegenExpression processFirstColCodegen(Class evaluationType, CodegenExpression expression, CodegenExpression resultEventType, CodegenExpression eventBeanFactory, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return processFirstColCodegen(expression, eventBeanFactory, codegenClassScope, wrapper, "adapterForTypedMap", Map.class);
    }

    public static CodegenExpression processFirstColCodegen(CodegenExpression expression, CodegenExpression eventBeanFactory, CodegenClassScope codegenClassScope, WrapperEventType wrapperEventType, String adapterMethod, Class castType) {
        CodegenExpressionField memberUndType = codegenClassScope.addFieldUnshared(true, EventType.class, EventTypeUtility.resolveTypeCodegen(wrapperEventType.getUnderlyingEventType(), EPStatementInitServices.REF));
        CodegenExpressionField memberWrapperType = codegenClassScope.addFieldUnshared(true, WrapperEventType.class, cast(WrapperEventType.class, EventTypeUtility.resolveTypeCodegen(wrapperEventType, EPStatementInitServices.REF)));
        CodegenExpression wrapped = exprDotMethod(eventBeanFactory, adapterMethod, castType == Object.class ? expression : cast(castType, expression), memberUndType);
        return exprDotMethod(eventBeanFactory, "adapterForTypedWrapper", wrapped, staticMethod(Collections.class, "emptyMap"), memberWrapperType);
    }
}
