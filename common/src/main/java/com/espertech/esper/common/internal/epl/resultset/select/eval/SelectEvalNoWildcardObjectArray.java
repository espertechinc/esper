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
import com.espertech.esper.common.client.type.EPTypePremade;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.util.CodegenRepetitiveLengthBuilder;
import com.espertech.esper.common.internal.epl.expression.codegen.CodegenLegoMayVoid;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.resultset.select.core.SelectExprForgeContext;
import com.espertech.esper.common.internal.epl.resultset.select.core.SelectExprProcessorCodegenSymbol;
import com.espertech.esper.common.internal.epl.resultset.select.core.SelectExprProcessorForge;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class SelectEvalNoWildcardObjectArray implements SelectExprProcessorForge {

    private final SelectExprForgeContext context;
    private final EventType resultEventType;

    public SelectEvalNoWildcardObjectArray(SelectExprForgeContext context, EventType resultEventType) {
        this.context = context;
        this.resultEventType = resultEventType;
    }

    public CodegenMethod processCodegen(CodegenExpression resultEventType, CodegenExpression eventBeanFactory, CodegenMethodScope codegenMethodScope, SelectExprProcessorCodegenSymbol selectSymbol, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        CodegenMethod method = codegenMethodScope.makeChild(EventBean.EPTYPE, this.getClass(), codegenClassScope);
        method.getBlock()
                .declareVar(EPTypePremade.OBJECTARRAY.getEPType(), "props", newArrayByLength(EPTypePremade.OBJECT.getEPType(), constant(this.context.getExprForges().length)));
        new CodegenRepetitiveLengthBuilder(this.context.getExprForges().length, method, codegenClassScope, this.getClass())
                .addParam(EPTypePremade.OBJECTARRAY.getEPType(), "props")
                .setConsumer((index, leaf) -> {
                    CodegenExpression expression = CodegenLegoMayVoid.expressionMayVoid(EPTypePremade.OBJECT.getEPType(), this.context.getExprForges()[index], leaf, exprSymbol, codegenClassScope);
                    leaf.getBlock().assignArrayElement("props", constant(index), expression);
                }).build();
        method.getBlock().methodReturn(exprDotMethod(eventBeanFactory, "adapterForTypedObjectArray", ref("props"), resultEventType));
        return method;
    }

    public EventType getResultEventType() {
        return resultEventType;
    }
}
