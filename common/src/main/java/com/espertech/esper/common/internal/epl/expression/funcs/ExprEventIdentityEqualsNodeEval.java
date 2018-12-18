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
package com.espertech.esper.common.internal.epl.expression.funcs;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.*;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class ExprEventIdentityEqualsNodeEval implements ExprEvaluator {
    private final int streamLeft;
    private final int streamRight;

    public ExprEventIdentityEqualsNodeEval(int streamLeft, int streamRight) {
        this.streamLeft = streamLeft;
        this.streamRight = streamRight;
    }

    public static CodegenExpression evaluateCodegen(ExprEventIdentityEqualsNodeForge forge, CodegenMethodScope parent, ExprForgeCodegenSymbol symbols, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(Boolean.class, ExprEventIdentityEqualsNodeEval.class, classScope);
        method.getBlock()
            .declareVar(EventBean.class, "left", arrayAtIndex(symbols.getAddEPS(method), constant(forge.getUndLeft().getStreamId())))
            .declareVar(EventBean.class, "right", arrayAtIndex(symbols.getAddEPS(method), constant(forge.getUndRight().getStreamId())))
            .ifCondition(or(equalsNull(ref("left")), equalsNull(ref("right"))))
                .blockReturn(constantNull())
            .methodReturn(exprDotMethod(ref("left"), "equals", ref("right")));
        return localMethod(method);
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        EventBean left = eventsPerStream[streamLeft];
        EventBean right = eventsPerStream[streamRight];
        if (left == null || right == null) {
            return null;
        }
        return left.equals(right);
    }
}
