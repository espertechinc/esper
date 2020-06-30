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
import com.espertech.esper.common.client.type.EPType;
import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.client.type.EPTypeNull;
import com.espertech.esper.common.client.type.EPTypePremade;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenBlock;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.epl.expression.core.ExprForge;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class ExprRelationalOpNodeForgeEval implements ExprEvaluator {
    private final ExprRelationalOpNodeForge forge;
    private final ExprEvaluator left;
    private final ExprEvaluator right;

    public ExprRelationalOpNodeForgeEval(ExprRelationalOpNodeForge forge, ExprEvaluator left, ExprEvaluator right) {
        this.forge = forge;
        this.left = left;
        this.right = right;
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        Object lvalue = left.evaluate(eventsPerStream, isNewData, exprEvaluatorContext);
        if (lvalue == null) {
            return null;
        }
        Object rvalue = right.evaluate(eventsPerStream, isNewData, exprEvaluatorContext);
        if (rvalue == null) {
            return null;
        }
        return forge.getComputer().compare(lvalue, rvalue);
    }

    public static CodegenExpression codegen(ExprRelationalOpNodeForge forge, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        ExprForge lhs = forge.getForgeRenderable().getChildNodes()[0].getForge();
        ExprForge rhs = forge.getForgeRenderable().getChildNodes()[1].getForge();

        EPType lhsType = lhs.getEvaluationType();
        if (lhsType == null || lhsType == EPTypeNull.INSTANCE) {
            return constantNull();
        }
        EPType rhsType = rhs.getEvaluationType();
        if (rhsType == null || rhsType == EPTypeNull.INSTANCE) {
            return constantNull();
        }
        EPTypeClass lhsTypeClass = (EPTypeClass) lhsType;
        EPTypeClass rhsTypeClass = (EPTypeClass) rhsType;

        CodegenMethod methodNode = codegenMethodScope.makeChild(EPTypePremade.BOOLEANBOXED.getEPType(), ExprRelationalOpNodeForgeEval.class, codegenClassScope);

        CodegenBlock block = methodNode.getBlock()
                .declareVar(lhsTypeClass, "left", lhs.evaluateCodegen(lhsTypeClass, methodNode, exprSymbol, codegenClassScope));
        if (!lhsTypeClass.getType().isPrimitive()) {
            block.ifRefNullReturnNull("left");
        }

        block.declareVar(rhsTypeClass, "right", rhs.evaluateCodegen(rhsTypeClass, methodNode, exprSymbol, codegenClassScope));
        if (!rhsTypeClass.getType().isPrimitive()) {
            block.ifRefNullReturnNull("right");
        }

        block.methodReturn(forge.getComputer().codegen(ref("left"), lhsTypeClass, ref("right"), rhsTypeClass));
        return localMethod(methodNode);
    }
}
