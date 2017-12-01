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
package com.espertech.esper.epl.expression.funcs;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.codegen.base.CodegenBlock;
import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMethodScope;
import com.espertech.esper.epl.expression.codegen.CodegenLegoCompareEquals;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.model.expression.CodegenExpressionRef;
import com.espertech.esper.collection.UniformPair;
import com.espertech.esper.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.codegen.base.CodegenMethodNode;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.util.JavaClassHelper;

import java.util.List;
import java.util.Map;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.epl.expression.funcs.ExprCaseNodeForgeEvalSyntax1.codegenToType;

public class ExprCaseNodeForgeEvalSyntax2 implements ExprEvaluator {

    private final ExprCaseNodeForge forge;
    private final List<UniformPair<ExprEvaluator>> whenThenNodeList;
    private final ExprEvaluator compareExprNode;
    private final ExprEvaluator optionalElseExprNode;

    ExprCaseNodeForgeEvalSyntax2(ExprCaseNodeForge forge, List<UniformPair<ExprEvaluator>> whenThenNodeList, ExprEvaluator compareExprNode, ExprEvaluator optionalElseExprNode) {
        this.forge = forge;
        this.whenThenNodeList = whenThenNodeList;
        this.compareExprNode = compareExprNode;
        this.optionalElseExprNode = optionalElseExprNode;
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        // Case 2 expression example:
        //      case p when p1 then x [when p2 then y...] [else z]
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qExprCase(forge.getForgeRenderable());
        }

        Object checkResult = compareExprNode.evaluate(eventsPerStream, isNewData, exprEvaluatorContext);
        Object caseResult = null;
        boolean matched = false;
        for (UniformPair<ExprEvaluator> p : whenThenNodeList) {
            Object whenResult = p.getFirst().evaluate(eventsPerStream, isNewData, exprEvaluatorContext);

            if (compare(checkResult, whenResult)) {
                caseResult = p.getSecond().evaluate(eventsPerStream, isNewData, exprEvaluatorContext);
                matched = true;
                break;
            }
        }

        if ((!matched) && (optionalElseExprNode != null)) {
            caseResult = optionalElseExprNode.evaluate(eventsPerStream, isNewData, exprEvaluatorContext);
        }

        if (caseResult == null) {
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aExprCase(null);
            }
            return null;
        }

        if ((caseResult.getClass() != forge.getEvaluationType()) && forge.isNumericResult()) {
            caseResult = JavaClassHelper.coerceBoxed((Number) caseResult, forge.getEvaluationType());
        }

        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aExprCase(caseResult);
        }
        return caseResult;
    }

    public static CodegenExpression codegen(ExprCaseNodeForge forge, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        Class evaluationType = forge.getEvaluationType() == null ? Map.class : forge.getEvaluationType();
        Class compareType = forge.getOptionalCompareExprNode().getForge().getEvaluationType();
        CodegenMethodNode methodNode = codegenMethodScope.makeChild(evaluationType, ExprCaseNodeForgeEvalSyntax2.class, codegenClassScope);

        Class checkResultType = compareType == null ? Object.class : compareType;
        CodegenBlock block = methodNode.getBlock()
                .declareVar(checkResultType, "checkResult", forge.getOptionalCompareExprNode().getForge().evaluateCodegen(checkResultType, methodNode, exprSymbol, codegenClassScope));
        int num = 0;
        for (UniformPair<ExprNode> pair : forge.getWhenThenNodeList()) {
            String refname = "r" + num;
            Class lhsType = pair.getFirst().getForge().getEvaluationType();
            Class lhsDeclaredType = lhsType == null ? Object.class : lhsType;
            block.declareVar(lhsDeclaredType, refname, pair.getFirst().getForge().evaluateCodegen(lhsDeclaredType, methodNode, exprSymbol, codegenClassScope));
            CodegenExpression compareExpression = codegenCompare(ref("checkResult"), compareType, ref(refname), pair.getFirst().getForge().getEvaluationType(), forge, methodNode, codegenClassScope);
            block.ifCondition(compareExpression)
                    .blockReturn(codegenToType(forge, pair.getSecond(), methodNode, exprSymbol, codegenClassScope));
            num++;
        }

        if (forge.getOptionalElseExprNode() != null) {
            block.methodReturn(codegenToType(forge, forge.getOptionalElseExprNode(), methodNode, exprSymbol, codegenClassScope));
        } else {
            block.methodReturn(constantNull());
        }
        return localMethod(methodNode);
    }

    private boolean compare(Object leftResult, Object rightResult) {
        if (leftResult == null) {
            return rightResult == null;
        }
        if (rightResult == null) {
            return false;
        }

        if (!forge.isMustCoerce()) {
            return leftResult.equals(rightResult);
        } else {
            Number left = forge.getCoercer().coerceBoxed((Number) leftResult);
            Number right = forge.getCoercer().coerceBoxed((Number) rightResult);
            return left.equals(right);
        }
    }

    private static CodegenExpression codegenCompare(CodegenExpressionRef lhs, Class lhsType, CodegenExpressionRef rhs, Class rhsType, ExprCaseNodeForge forge, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        if (lhsType == null) {
            return equalsNull(rhs);
        }
        if (rhsType == null) {
            return equalsNull(lhs);
        }
        if (lhsType.isPrimitive() && rhsType.isPrimitive() && !forge.isMustCoerce()) {
            return CodegenLegoCompareEquals.codegenEqualsNonNullNoCoerce(lhs, lhsType, rhs, rhsType);
        }
        CodegenBlock block = codegenMethodScope.makeChild(boolean.class, ExprCaseNodeForgeEvalSyntax2.class, codegenClassScope).addParam(lhsType, "leftResult").addParam(rhsType, "rightResult").getBlock();
        if (!lhsType.isPrimitive()) {
            CodegenBlock ifBlock = block.ifCondition(equalsNull(ref("leftResult")));
            if (rhsType.isPrimitive()) {
                ifBlock.blockReturn(constantFalse());
            } else {
                ifBlock.blockReturn(equalsNull(ref("rightResult")));
            }
        }
        if (!rhsType.isPrimitive()) {
            block.ifCondition(equalsNull(ref("rightResult"))).blockReturn(constantFalse());
        }
        CodegenMethodNode method;
        if (!forge.isMustCoerce()) {
            method = block.methodReturn(CodegenLegoCompareEquals.codegenEqualsNonNullNoCoerce(ref("leftResult"), lhsType, ref("rightResult"), rhsType));
        } else {
            block.declareVar(Number.class, "left", forge.getCoercer().coerceCodegen(ref("leftResult"), lhsType));
            block.declareVar(Number.class, "right", forge.getCoercer().coerceCodegen(ref("rightResult"), rhsType));
            method = block.methodReturn(exprDotMethod(ref("left"), "equals", ref("right")));
        }
        return localMethodBuild(method).pass(lhs).pass(rhs).call();
    }
}
