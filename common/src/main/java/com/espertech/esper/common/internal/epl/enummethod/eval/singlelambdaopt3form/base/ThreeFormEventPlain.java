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
package com.espertech.esper.common.internal.epl.enummethod.eval.singlelambdaopt3form.base;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenBlock;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.epl.enummethod.codegen.EnumForgeCodegenNames;
import com.espertech.esper.common.internal.epl.enummethod.codegen.EnumForgeCodegenParams;
import com.espertech.esper.common.internal.epl.enummethod.dot.ExprDotEvalParamLambda;
import com.espertech.esper.common.internal.epl.enummethod.eval.EnumForgeBasePlain;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public abstract class ThreeFormEventPlain extends EnumForgeBasePlain {
    public abstract EPTypeClass returnTypeOfMethod();

    public abstract CodegenExpression returnIfEmptyOptional();

    public abstract void initBlock(CodegenBlock block, CodegenMethod methodNode, ExprForgeCodegenSymbol scope, CodegenClassScope codegenClassScope);

    public boolean hasForEachLoop() {
        return true;
    }

    public abstract void forEachBlock(CodegenBlock block, CodegenMethod methodNode, ExprForgeCodegenSymbol scope, CodegenClassScope codegenClassScope);

    public abstract void returnResult(CodegenBlock block);

    public ThreeFormEventPlain(ExprDotEvalParamLambda lambda) {
        super(lambda);
    }

    public CodegenExpression codegen(EnumForgeCodegenParams premade, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        ExprForgeCodegenSymbol scope = new ExprForgeCodegenSymbol(false, null);
        CodegenMethod methodNode = codegenMethodScope.makeChildWithScope(returnTypeOfMethod(), getClass(), scope, codegenClassScope).addParam(EnumForgeCodegenNames.PARAMSCOLLBEAN);
        CodegenBlock block = methodNode.getBlock();

        CodegenExpression returnEmpty = returnIfEmptyOptional();
        if (returnEmpty != null) {
            block.ifCondition(exprDotMethod(EnumForgeCodegenNames.REF_ENUMCOLL, "isEmpty"))
                .blockReturn(returnEmpty);
        }

        initBlock(block, methodNode, scope, codegenClassScope);

        if (hasForEachLoop()) {
            CodegenBlock forEach = block.forEach(EventBean.EPTYPE, "next", EnumForgeCodegenNames.REF_ENUMCOLL)
                .assignArrayElement(EnumForgeCodegenNames.REF_EPS, constant(getStreamNumLambda()), ref("next"));
            forEachBlock(forEach, methodNode, scope, codegenClassScope);
        }

        returnResult(block);
        return localMethod(methodNode, premade.getEps(), premade.getEnumcoll(), premade.getIsNewData(), premade.getExprCtx());
    }
}
