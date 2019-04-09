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

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenBlock;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.compile.multikey.MultiKeyPlanner;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprForge;

import java.util.Arrays;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.common.internal.epl.expression.codegen.CodegenLegoCompareEquals.codegenEqualsNonNullNoCoerce;

public class ExprEqualsNodeForgeNCForgeEquals {
    public static CodegenMethod codegen(ExprEqualsNodeForgeNC forge, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope, ExprForge lhs, ExprForge rhs) {
        Class lhsType = lhs.getEvaluationType();
        Class rhsType = rhs.getEvaluationType();

        CodegenMethod methodNode = codegenMethodScope.makeChild(Boolean.class, ExprEqualsNodeForgeNCForgeEquals.class, codegenClassScope);
        CodegenBlock block = methodNode.getBlock()
            .declareVar(lhsType, "left", lhs.evaluateCodegen(lhsType, methodNode, exprSymbol, codegenClassScope))
            .declareVar(rhsType, "right", rhs.evaluateCodegen(rhsType, methodNode, exprSymbol, codegenClassScope));

        if (!lhsType.isPrimitive()) {
            block.ifRefNullReturnNull("left");
        }
        if (!rhsType.isPrimitive()) {
            block.ifRefNullReturnNull("right");
        }

        CodegenExpression compare;
        if (!lhsType.isArray()) {
            compare = codegenEqualsNonNullNoCoerce(ref("left"), lhsType, ref("right"), rhsType);
        } else {
            if (!MultiKeyPlanner.requiresDeepEquals(lhsType.getComponentType())) {
                compare = staticMethod(Arrays.class, "equals", ref("left"), ref("right"));
            } else {
                compare = staticMethod(Arrays.class, "deepEquals", ref("left"), ref("right"));
            }
        }
        if (!forge.getForgeRenderable().isNotEquals()) {
            block.methodReturn(compare);
        } else {
            block.methodReturn(not(compare));
        }
        return methodNode;
    }
}
