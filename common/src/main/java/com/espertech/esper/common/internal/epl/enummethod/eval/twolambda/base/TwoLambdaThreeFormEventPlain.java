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
package com.espertech.esper.common.internal.epl.enummethod.eval.twolambda.base;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenBlock;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.epl.enummethod.codegen.EnumForgeCodegenNames;
import com.espertech.esper.common.internal.epl.enummethod.codegen.EnumForgeCodegenParams;
import com.espertech.esper.common.internal.epl.enummethod.eval.EnumForgeBasePlain;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprForge;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public abstract class TwoLambdaThreeFormEventPlain extends EnumForgeBasePlain {

    protected ExprForge secondExpression;

    public abstract EPTypeClass returnType();

    public abstract CodegenExpression returnIfEmptyOptional();

    public abstract void initBlock(CodegenBlock block, CodegenMethod methodNode, ExprForgeCodegenSymbol scope, CodegenClassScope codegenClassScope);

    public abstract void forEachBlock(CodegenBlock block, CodegenMethod methodNode, ExprForgeCodegenSymbol scope, CodegenClassScope codegenClassScope);

    public abstract void returnResult(CodegenBlock block);

    public TwoLambdaThreeFormEventPlain(ExprForge innerExpression, int streamCountIncoming, ExprForge secondExpression) {
        super(innerExpression, streamCountIncoming);
        this.secondExpression = secondExpression;
    }

    public CodegenExpression codegen(EnumForgeCodegenParams premade, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        ExprForgeCodegenSymbol scope = new ExprForgeCodegenSymbol(false, null);
        CodegenMethod methodNode = codegenMethodScope.makeChildWithScope(returnType(), this.getClass(), scope, codegenClassScope).addParam(EnumForgeCodegenNames.PARAMSCOLLBEAN);

        CodegenExpression returnIfEmpty = returnIfEmptyOptional();
        if (returnIfEmpty != null) {
            methodNode.getBlock()
                .ifCondition(exprDotMethod(EnumForgeCodegenNames.REF_ENUMCOLL, "isEmpty"))
                .blockReturn(returnIfEmpty);
        }

        initBlock(methodNode.getBlock(), methodNode, scope, codegenClassScope);

        CodegenBlock forEach = methodNode.getBlock().forEach(EventBean.EPTYPE, "next", EnumForgeCodegenNames.REF_ENUMCOLL)
            .assignArrayElement(EnumForgeCodegenNames.REF_EPS, constant(getStreamNumLambda()), ref("next"));
        forEachBlock(forEach, methodNode, scope, codegenClassScope);

        returnResult(methodNode.getBlock());
        return localMethod(methodNode, premade.getEps(), premade.getEnumcoll(), premade.getIsNewData(), premade.getExprCtx());
    }
}
