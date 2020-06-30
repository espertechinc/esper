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

import com.espertech.esper.common.client.type.EPType;
import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.client.type.EPTypeNull;
import com.espertech.esper.common.client.type.EPTypePremade;
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
        EPType lhsType = lhs.getEvaluationType();
        EPType rhsType = rhs.getEvaluationType();
        CodegenMethod methodNode = codegenMethodScope.makeChild(EPTypePremade.BOOLEANPRIMITIVE.getEPType(), ExprEqualsNodeForgeNCForgeIs.class, codegenClassScope);

        CodegenExpression compare;
        if (rhsType != null && lhsType != null && rhsType != EPTypeNull.INSTANCE && lhsType != EPTypeNull.INSTANCE) {
            EPTypeClass lhsClass = (EPTypeClass) lhsType;
            EPTypeClass rhsClass = (EPTypeClass) rhsType;
            if (!lhsClass.getType().isArray()) {
                methodNode.getBlock()
                    .declareVar(EPTypePremade.OBJECT.getEPType(), "left", lhs.evaluateCodegen(EPTypePremade.OBJECT.getEPType(), methodNode, exprSymbol, codegenClassScope))
                    .declareVar(EPTypePremade.OBJECT.getEPType(), "right", rhs.evaluateCodegen(EPTypePremade.OBJECT.getEPType(), methodNode, exprSymbol, codegenClassScope));
                compare = exprDotMethod(ref("left"), "equals", ref("right"));
            } else {
                methodNode.getBlock()
                    .declareVar(lhsClass, "left", lhs.evaluateCodegen(lhsClass, methodNode, exprSymbol, codegenClassScope))
                    .declareVar(rhsClass, "right", rhs.evaluateCodegen(rhsClass, methodNode, exprSymbol, codegenClassScope));
                if (!MultiKeyPlanner.requiresDeepEquals(lhsClass.getType().getComponentType())) {
                    compare = staticMethod(Arrays.class, "equals", ref("left"), ref("right"));
                } else {
                    compare = staticMethod(Arrays.class, "deepEquals", ref("left"), ref("right"));
                }
            }

            methodNode.getBlock().declareVarNoInit(EPTypePremade.BOOLEANPRIMITIVE.getEPType(), "result")
                .ifRefNull("left")
                .assignRef("result", equalsNull(ref("right")))
                .ifElse()
                .assignRef("result", and(notEqualsNull(ref("right")), compare))
                .blockEnd();
        } else {
            if ((lhsType == null || lhsType == EPTypeNull.INSTANCE) && (rhsType == null || rhsType == EPTypeNull.INSTANCE)) {
                methodNode.getBlock().declareVar(EPTypePremade.BOOLEANBOXED.getEPType(), "result", constantTrue());
            } else if (lhsType == null || lhsType == EPTypeNull.INSTANCE) {
                methodNode.getBlock()
                    .declareVar(EPTypePremade.OBJECT.getEPType(), "right", rhs.evaluateCodegen(EPTypePremade.OBJECT.getEPType(), methodNode, exprSymbol, codegenClassScope))
                    .declareVar(EPTypePremade.BOOLEANBOXED.getEPType(), "result", equalsNull(ref("right")));
            } else {
                methodNode.getBlock()
                    .declareVar(EPTypePremade.OBJECT.getEPType(), "left", lhs.evaluateCodegen(EPTypePremade.OBJECT.getEPType(), methodNode, exprSymbol, codegenClassScope))
                    .declareVar(EPTypePremade.BOOLEANBOXED.getEPType(), "result", equalsNull(ref("left")));
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
