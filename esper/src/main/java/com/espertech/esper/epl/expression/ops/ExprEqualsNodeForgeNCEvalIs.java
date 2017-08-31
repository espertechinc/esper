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
package com.espertech.esper.epl.expression.ops;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.codegen.base.CodegenBlock;
import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMethodNode;
import com.espertech.esper.codegen.base.CodegenMethodScope;
import com.espertech.esper.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.expression.core.ExprForge;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

public class ExprEqualsNodeForgeNCEvalIs implements ExprEvaluator {
    private final ExprEqualsNodeImpl parent;
    private final ExprEvaluator lhs;
    private final ExprEvaluator rhs;

    public ExprEqualsNodeForgeNCEvalIs(ExprEqualsNodeImpl parent, ExprEvaluator lhs, ExprEvaluator rhs) {
        this.parent = parent;
        this.lhs = lhs;
        this.rhs = rhs;
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qExprIs(parent);
        }

        Object left = lhs.evaluate(eventsPerStream, isNewData, context);
        Object right = rhs.evaluate(eventsPerStream, isNewData, context);

        boolean result;
        if (left == null) {
            result = right == null;
        } else {
            result = right != null && left.equals(right);
        }
        result = result ^ parent.isNotEquals();

        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aExprIs(result);
        }
        return result;
    }

    public static CodegenMethodNode codegen(ExprEqualsNodeForgeNC forge, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope, ExprForge lhs, ExprForge rhs) {
        CodegenMethodNode methodNode = codegenMethodScope.makeChild(boolean.class, ExprEqualsNodeForgeNCEvalIs.class, codegenClassScope);
        CodegenBlock block = methodNode.getBlock()
                .declareVar(Object.class, "left", lhs.evaluateCodegen(Object.class, methodNode, exprSymbol, codegenClassScope))
                .declareVar(Object.class, "right", rhs.evaluateCodegen(Object.class, methodNode, exprSymbol, codegenClassScope));
        block.declareVarNoInit(boolean.class, "result")
                .ifRefNull("left")
                .assignRef("result", equalsNull(ref("right")))
                .ifElse()
                .assignRef("result", and(notEqualsNull(ref("right")), exprDotMethod(ref("left"), "equals", ref("right"))))
                .blockEnd();
        if (!forge.getForgeRenderable().isNotEquals()) {
            block.methodReturn(ref("result"));
        } else {
            block.methodReturn(not(ref("result")));
        }
        return methodNode;
    }
}
