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
package com.espertech.esper.common.internal.epl.expression.core;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class ExprEvaluatorWildcard implements ExprEvaluator {
    public final static ExprEvaluatorWildcard INSTANCE = new ExprEvaluatorWildcard();

    private ExprEvaluatorWildcard() {
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        EventBean event = eventsPerStream[0];
        if (event == null) {
            return null;
        }
        return event.getUnderlying();
    }

    public static CodegenExpression codegen(Class requiredType, Class underlyingType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        CodegenMethod methodNode = codegenMethodScope.makeChild(requiredType == Object.class ? Object.class : underlyingType, ExprEvaluatorWildcard.class, codegenClassScope);
        CodegenExpressionRef refEPS = exprSymbol.getAddEPS(methodNode);
        methodNode.getBlock()
                .declareVar(EventBean.class, "event", arrayAtIndex(refEPS, constant(0)))
                .ifRefNullReturnNull("event");
        if (requiredType == Object.class) {
            methodNode.getBlock().methodReturn(exprDotMethod(ref("event"), "getUnderlying"));
        } else {
            methodNode.getBlock().methodReturn(cast(underlyingType, exprDotMethod(ref("event"), "getUnderlying")));
        }
        return localMethod(methodNode);
    }
}
