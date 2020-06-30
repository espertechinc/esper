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
import com.espertech.esper.common.client.type.EPType;
import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.client.type.EPTypeNull;
import com.espertech.esper.common.client.type.EPTypePremade;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenBlock;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;
import com.espertech.esper.common.internal.collection.UniformPair;
import com.espertech.esper.common.internal.epl.expression.codegen.CodegenLegoCompareEquals;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.util.JavaClassHelper;

import java.util.List;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.common.internal.epl.expression.funcs.ExprCaseNodeForgeEvalSyntax1.codegenToType;

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
            return null;
        }

        if ((caseResult.getClass() != forge.getEvaluationType().getType()) && forge.isNumericResult()) {
            caseResult = JavaClassHelper.coerceBoxed((Number) caseResult, forge.getEvaluationType().getType());
        }

        return caseResult;
    }

    public static CodegenExpression codegen(ExprCaseNodeForge forge, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        EPTypeClass evaluationType = forge.getEvaluationType() == null ? EPTypePremade.MAP.getEPType() : forge.getEvaluationType();
        EPTypeClass compareType = (EPTypeClass) forge.getOptionalCompareExprNode().getForge().getEvaluationType();
        CodegenMethod methodNode = codegenMethodScope.makeChild(evaluationType, ExprCaseNodeForgeEvalSyntax2.class, codegenClassScope);

        EPTypeClass checkResultType = compareType == null ? EPTypePremade.OBJECT.getEPType() : compareType;
        CodegenBlock block = methodNode.getBlock()
                .declareVar(checkResultType, "checkResult", forge.getOptionalCompareExprNode().getForge().evaluateCodegen(checkResultType, methodNode, exprSymbol, codegenClassScope));
        int num = 0;
        for (UniformPair<ExprNode> pair : forge.getWhenThenNodeList()) {
            String refname = "r" + num;
            EPType lhsType = pair.getFirst().getForge().getEvaluationType();
            EPTypeClass lhsTypeClass = lhsType == null || lhsType == EPTypeNull.INSTANCE ? null : (EPTypeClass) lhsType;
            EPTypeClass lhsDeclaredType = lhsTypeClass == null ? EPTypePremade.OBJECT.getEPType() : lhsTypeClass;
            block.declareVar(lhsDeclaredType, refname, pair.getFirst().getForge().evaluateCodegen(lhsDeclaredType, methodNode, exprSymbol, codegenClassScope));
            CodegenExpression compareExpression = codegenCompare(ref("checkResult"), compareType, ref(refname), lhsTypeClass, forge, methodNode, codegenClassScope);
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

    private static CodegenExpression codegenCompare(CodegenExpressionRef lhs, EPType lhsType, CodegenExpressionRef rhs, EPType rhsType, ExprCaseNodeForge forge, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        if (lhsType == null || lhsType == EPTypeNull.INSTANCE) {
            return equalsNull(rhs);
        }
        if (rhsType == null || rhsType == EPTypeNull.INSTANCE) {
            return equalsNull(lhs);
        }
        EPTypeClass lhsClass = (EPTypeClass) lhsType;
        EPTypeClass rhsClass = (EPTypeClass) rhsType;
        if (lhsClass.getType().isPrimitive() && rhsClass.getType().isPrimitive() && !forge.isMustCoerce()) {
            return CodegenLegoCompareEquals.codegenEqualsNonNullNoCoerce(lhs, lhsClass, rhs, rhsClass);
        }
        CodegenBlock block = codegenMethodScope.makeChild(EPTypePremade.BOOLEANPRIMITIVE.getEPType(), ExprCaseNodeForgeEvalSyntax2.class, codegenClassScope).addParam(lhsClass, "leftResult").addParam(rhsClass, "rightResult").getBlock();
        if (!lhsClass.getType().isPrimitive()) {
            CodegenBlock ifBlock = block.ifCondition(equalsNull(ref("leftResult")));
            if (rhsClass.getType().isPrimitive()) {
                ifBlock.blockReturn(constantFalse());
            } else {
                ifBlock.blockReturn(equalsNull(ref("rightResult")));
            }
        }
        if (!rhsClass.getType().isPrimitive()) {
            block.ifCondition(equalsNull(ref("rightResult"))).blockReturn(constantFalse());
        }
        CodegenMethod method;
        if (!forge.isMustCoerce()) {
            method = block.methodReturn(CodegenLegoCompareEquals.codegenEqualsNonNullNoCoerce(ref("leftResult"), lhsClass, ref("rightResult"), rhsClass));
        } else {
            block.declareVar(EPTypePremade.NUMBER.getEPType(), "left", forge.getCoercer().coerceCodegen(ref("leftResult"), lhsClass));
            block.declareVar(EPTypePremade.NUMBER.getEPType(), "right", forge.getCoercer().coerceCodegen(ref("rightResult"), rhsClass));
            method = block.methodReturn(exprDotMethod(ref("left"), "equals", ref("right")));
        }
        return localMethodBuild(method).pass(lhs).pass(rhs).call();
    }
}
