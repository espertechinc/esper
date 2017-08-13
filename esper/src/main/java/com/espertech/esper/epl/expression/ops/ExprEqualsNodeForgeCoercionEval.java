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
import com.espertech.esper.codegen.core.CodegenBlock;
import com.espertech.esper.codegen.core.CodegenContext;
import com.espertech.esper.codegen.core.CodegenMethodId;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.model.method.CodegenParamSetExprPremade;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.util.SimpleNumberCoercer;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

public class ExprEqualsNodeForgeCoercionEval implements ExprEvaluator {
    private final ExprEqualsNodeImpl parent;
    private final ExprEvaluator lhs;
    private final ExprEvaluator rhs;
    private final SimpleNumberCoercer numberCoercerLHS;
    private final SimpleNumberCoercer numberCoercerRHS;

    public ExprEqualsNodeForgeCoercionEval(ExprEqualsNodeImpl parent, ExprEvaluator lhs, ExprEvaluator rhs, SimpleNumberCoercer numberCoercerLHS, SimpleNumberCoercer numberCoercerRHS) {
        this.parent = parent;
        this.lhs = lhs;
        this.rhs = rhs;
        this.numberCoercerLHS = numberCoercerLHS;
        this.numberCoercerRHS = numberCoercerRHS;
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qExprEquals(parent);
        }
        Boolean result = evaluateInternal(eventsPerStream, isNewData, context);
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aExprEquals(result);
        }
        return result;
    }

    private Boolean evaluateInternal(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        Object leftResult = lhs.evaluate(eventsPerStream, isNewData, context);
        Object rightResult = rhs.evaluate(eventsPerStream, isNewData, context);

        if (!parent.isIs()) {
            if (leftResult == null || rightResult == null) {
                // null comparison
                return null;
            }
        } else {
            if (leftResult == null) {
                return rightResult == null;
            }
            if (rightResult == null) {
                return false;
            }
        }

        Number left = numberCoercerLHS.coerceBoxed((Number) leftResult);
        Number right = numberCoercerRHS.coerceBoxed((Number) rightResult);
        return left.equals(right) ^ parent.isNotEquals();
    }

    public static CodegenMethodId codegen(ExprEqualsNodeForgeCoercion forge, CodegenContext context, CodegenParamSetExprPremade params, ExprNode lhs, ExprNode rhs) {
        Class lhsType = lhs.getForge().getEvaluationType();
        Class rhsType = rhs.getForge().getEvaluationType();

        CodegenBlock block = context.addMethod(Boolean.class, ExprEqualsNodeForgeNCEvalEquals.class).add(params).begin()
                .declareVar(lhsType, "l", lhs.getForge().evaluateCodegen(params, context))
                .declareVar(rhsType, "r", rhs.getForge().evaluateCodegen(params, context));

        if (!forge.getForgeRenderable().isIs()) {
            if (!lhsType.isPrimitive()) {
                block.ifRefNullReturnNull("l");
            }
            if (!rhsType.isPrimitive()) {
                block.ifRefNullReturnNull("r");
            }
        } else {
            if (!lhsType.isPrimitive() && !rhsType.isPrimitive()) {
                block.ifRefNull("l").blockReturn(equalsNull(ref("r")));
            }
            if (!rhsType.isPrimitive()) {
                block.ifRefNull("r").blockReturn(constantFalse());
            }
        }

        block.declareVar(Number.class, "left", forge.getNumberCoercerLHS().coerceCodegen(ref("l"), lhs.getForge().getEvaluationType()));
        block.declareVar(Number.class, "right", forge.getNumberCoercerRHS().coerceCodegen(ref("r"), rhs.getForge().getEvaluationType()));
        CodegenExpression compare = exprDotMethod(ref("left"), "equals", ref("right"));
        if (!forge.getForgeRenderable().isNotEquals()) {
            return block.methodReturn(compare);
        }
        return block.methodReturn(not(compare));
    }
}
