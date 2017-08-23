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

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.exprDotMethod;

public class EvalInsertNoWildcardSingleColCoercionBean extends EvalBaseFirstProp implements SelectExprProcessor {

    public EvalInsertNoWildcardSingleColCoercionBean(SelectExprForgeContext selectExprForgeContext, EventType resultEventType) {
        super(selectExprForgeContext, resultEventType);
    }

    public EventBean processFirstCol(Object result) {
        return super.getEventAdapterService().adapterForTypedBean(result, super.getResultEventType());
    }

    protected CodegenExpression processFirstColCodegen(Class evaluationType, CodegenExpression expression, CodegenMember memberResultEventType, CodegenMember memberEventAdapterService, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return exprDotMethod(CodegenExpressionBuilder.member(memberEventAdapterService.getMemberId()), "adapterForTypedBean", expression, CodegenExpressionBuilder.member(memberResultEventType.getMemberId()));
    }
}
