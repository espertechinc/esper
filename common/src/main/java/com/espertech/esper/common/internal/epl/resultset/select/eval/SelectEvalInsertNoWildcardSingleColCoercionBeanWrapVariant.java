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

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionField;
import com.espertech.esper.common.internal.epl.resultset.select.core.SelectExprForgeContext;
import com.espertech.esper.common.internal.event.variant.VariantEventType;
import com.espertech.esper.common.internal.event.variant.VariantEventTypeUtil;

import java.util.Collections;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class SelectEvalInsertNoWildcardSingleColCoercionBeanWrapVariant extends SelectEvalBaseFirstProp {

    private final VariantEventType variantEventType;

    public SelectEvalInsertNoWildcardSingleColCoercionBeanWrapVariant(SelectExprForgeContext selectExprForgeContext, EventType resultEventType, VariantEventType variantEventType) {
        super(selectExprForgeContext, resultEventType);
        this.variantEventType = variantEventType;
    }

    protected CodegenExpression processFirstColCodegen(Class evaluationType, CodegenExpression expression, CodegenExpression resultEventType, CodegenExpression eventBeanFactory, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        CodegenExpressionField type = VariantEventTypeUtil.getField(variantEventType, codegenClassScope);
        CodegenMethod method = codegenMethodScope.makeChild(EventBean.class, this.getClass(), codegenClassScope).addParam(evaluationType, "result").getBlock()
                .declareVar(EventType.class, "beanEventType", exprDotMethod(type, "eventTypeForNativeObject", ref("result")))
                .declareVar(EventBean.class, "wrappedEvent", exprDotMethod(eventBeanFactory, "adapterForTypedBean", ref("result"), ref("beanEventType")))
                .declareVar(EventBean.class, "variant", exprDotMethod(type, "getValueAddEventBean", ref("wrappedEvent")))
                .methodReturn(exprDotMethod(eventBeanFactory, "adapterForTypedWrapper", ref("variant"), staticMethod(Collections.class, "emptyMap"), resultEventType));
        return localMethodBuild(method).pass(expression).call();
    }
}
