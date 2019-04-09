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

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.compile.multikey.MultiKeyPlanner;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprForge;

import java.util.Arrays;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class ExprEqualsNodeForgeNCForgeIs {
    public static CodegenMethod codegen(ExprEqualsNodeForgeNC forge, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope, ExprForge lhs, ExprForge rhs) {
        Class lhsType = lhs.getEvaluationType();
        Class rhsType = rhs.getEvaluationType();
        CodegenMethod methodNode = codegenMethodScope.makeChild(boolean.class, ExprEqualsNodeForgeNCForgeIs.class, codegenClassScope);

        CodegenExpression compare;
        if (rhsType != null && lhsType != null) {
            if (!lhsType.isArray()) {
                methodNode.getBlock()
                    .declareVar(Object.class, "left", lhs.evaluateCodegen(Object.class, methodNode, exprSymbol, codegenClassScope))
                    .declareVar(Object.class, "right", rhs.evaluateCodegen(Object.class, methodNode, exprSymbol, codegenClassScope));
                compare = exprDotMethod(ref("left"), "equals", ref("right"));
            } else {
                methodNode.getBlock()
                    .declareVar(lhsType, "left", lhs.evaluateCodegen(lhsType, methodNode, exprSymbol, codegenClassScope))
                    .declareVar(rhsType, "right", rhs.evaluateCodegen(rhsType, methodNode, exprSymbol, codegenClassScope));
                if (!MultiKeyPlanner.requiresDeepEquals(lhsType.getComponentType())) {
                    compare = staticMethod(Arrays.class, "equals", ref("left"), ref("right"));
                } else {
                    compare = staticMethod(Arrays.class, "deepEquals", ref("left"), ref("right"));
                }
            }

            methodNode.getBlock().declareVarNoInit(boolean.class, "result")
                .ifRefNull("left")
                .assignRef("result", equalsNull(ref("right")))
                .ifElse()
                .assignRef("result", and(notEqualsNull(ref("right")), compare))
                .blockEnd();
        } else {
            if (lhsType == null && rhsType == null) {
                methodNode.getBlock().declareVar(boolean.class, "result", constantTrue());
            } else if (lhsType == null) {
                methodNode.getBlock()
                    .declareVar(Object.class, "right", rhs.evaluateCodegen(Object.class, methodNode, exprSymbol, codegenClassScope))
                    .declareVar(boolean.class, "result", equalsNull(ref("right")));
            } else {
                methodNode.getBlock()
                    .declareVar(Object.class, "left", lhs.evaluateCodegen(Object.class, methodNode, exprSymbol, codegenClassScope))
                    .declareVar(boolean.class, "result", equalsNull(ref("left")));
            }
        }

        if (!forge.getForgeRenderable().isNotEquals()) {
            methodNode.getBlock().methodReturn(ref("result"));
        } else {
            methodNode.getBlock().methodReturn(not(ref("result")));
        }
        return methodNode;
    }
}
