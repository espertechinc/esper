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
package com.espertech.esper.common.internal.epl.expression.ops;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.client.type.EPTypePremade;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenBlock;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.util.SimpleNumberCoercer;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

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
        return evaluateInternal(eventsPerStream, isNewData, context);
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

    public static CodegenMethod codegen(ExprEqualsNodeForgeCoercion forge, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope, ExprNode lhs, ExprNode rhs) {
        EPTypeClass lhsType = forge.getLhsTypeClass();
        EPTypeClass rhsType = forge.getRhsTypeClass();

        CodegenMethod methodNode = codegenMethodScope.makeChild(EPTypePremade.BOOLEANBOXED.getEPType(), ExprEqualsNodeForgeCoercionEval.class, codegenClassScope);
        CodegenBlock block = methodNode.getBlock()
                .declareVar(lhsType, "l", lhs.getForge().evaluateCodegen(lhsType, methodNode, exprSymbol, codegenClassScope))
                .declareVar(rhsType, "r", rhs.getForge().evaluateCodegen(rhsType, methodNode, exprSymbol, codegenClassScope));

        if (!forge.getForgeRenderable().isIs()) {
            if (!lhsType.getType().isPrimitive()) {
                block.ifRefNullReturnNull("l");
            }
            if (!rhsType.getType().isPrimitive()) {
                block.ifRefNullReturnNull("r");
            }
        } else {
            if (!lhsType.getType().isPrimitive() && !rhsType.getType().isPrimitive()) {
                block.ifRefNull("l").blockReturn(equalsNull(ref("r")));
            }
            if (!rhsType.getType().isPrimitive()) {
                block.ifRefNull("r").blockReturn(constantFalse());
            }
        }

        block.declareVar(EPTypePremade.NUMBER.getEPType(), "left", forge.getNumberCoercerLHS().coerceCodegen(ref("l"), lhsType));
        block.declareVar(EPTypePremade.NUMBER.getEPType(), "right", forge.getNumberCoercerRHS().coerceCodegen(ref("r"), rhsType));
        CodegenExpression compare = exprDotMethod(ref("left"), "equals", ref("right"));
        if (!forge.getForgeRenderable().isNotEquals()) {
            block.methodReturn(compare);
        } else {
            block.methodReturn(not(compare));
        }
        return methodNode;
    }
}
