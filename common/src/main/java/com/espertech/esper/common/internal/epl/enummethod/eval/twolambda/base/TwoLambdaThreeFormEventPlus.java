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
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenBlock;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionField;
import com.espertech.esper.common.internal.context.module.EPStatementInitServices;
import com.espertech.esper.common.internal.epl.enummethod.codegen.EnumForgeCodegenNames;
import com.espertech.esper.common.internal.epl.enummethod.codegen.EnumForgeCodegenParams;
import com.espertech.esper.common.internal.epl.enummethod.eval.EnumForgeBaseWFields;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprForge;
import com.espertech.esper.common.internal.event.arr.ObjectArrayEventBean;
import com.espertech.esper.common.internal.event.arr.ObjectArrayEventType;
import com.espertech.esper.common.internal.event.core.EventTypeUtility;

import java.util.Map;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.common.internal.epl.enummethod.codegen.EnumForgeCodegenNames.REF_ENUMCOLL;

public abstract class TwoLambdaThreeFormEventPlus extends EnumForgeBaseWFields {

    protected final ExprForge secondExpression;
    protected final int numParameters;

    public TwoLambdaThreeFormEventPlus(ExprForge innerExpression, int streamNumLambda, ObjectArrayEventType indexEventType, ExprForge secondExpression, int numParameters) {
        super(innerExpression, streamNumLambda, indexEventType);
        this.secondExpression = secondExpression;
        this.numParameters = numParameters;
    }

    public abstract Class returnType();

    public abstract CodegenExpression returnIfEmptyOptional();

    public abstract void initBlock(CodegenBlock block, CodegenMethod methodNode, ExprForgeCodegenSymbol scope, CodegenClassScope codegenClassScope);

    public abstract void forEachBlock(CodegenBlock block, CodegenMethod methodNode, ExprForgeCodegenSymbol scope, CodegenClassScope codegenClassScope);

    public abstract void returnResult(CodegenBlock block);

    public CodegenExpression codegen(EnumForgeCodegenParams premade, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        CodegenExpressionField resultTypeMember = codegenClassScope.addFieldUnshared(true, ObjectArrayEventType.class, cast(ObjectArrayEventType.class, EventTypeUtility.resolveTypeCodegen(getFieldEventType(), EPStatementInitServices.REF)));

        ExprForgeCodegenSymbol scope = new ExprForgeCodegenSymbol(false, null);
        CodegenMethod methodNode = codegenMethodScope.makeChildWithScope(Map.class, getClass(), scope, codegenClassScope).addParam(EnumForgeCodegenNames.PARAMS);
        boolean hasSize = numParameters >= 3;

        CodegenExpression returnIfEmpty = returnIfEmptyOptional();
        if (returnIfEmpty != null) {
            methodNode.getBlock()
                .ifCondition(exprDotMethod(EnumForgeCodegenNames.REF_ENUMCOLL, "isEmpty"))
                .blockReturn(returnIfEmpty);
        }

        initBlock(methodNode.getBlock(), methodNode, scope, codegenClassScope);

        methodNode.getBlock().declareVar(ObjectArrayEventBean.class, "indexEvent", newInstance(ObjectArrayEventBean.class, newArrayByLength(Object.class, constant(numParameters - 1)), resultTypeMember))
            .assignArrayElement(EnumForgeCodegenNames.REF_EPS, constant(getStreamNumLambda() + 1), ref("indexEvent"))
            .declareVar(Object[].class, "props", exprDotMethod(ref("indexEvent"), "getProperties"))
            .declareVar(int.class, "count", constant(-1));
        if (hasSize) {
            methodNode.getBlock().assignArrayElement(ref("props"), constant(1), exprDotMethod(REF_ENUMCOLL, "size"));
        }

        CodegenBlock forEach = methodNode.getBlock().forEach(EventBean.class, "next", EnumForgeCodegenNames.REF_ENUMCOLL)
            .incrementRef("count")
            .assignArrayElement("props", constant(0), ref("count"))
            .assignArrayElement(EnumForgeCodegenNames.REF_EPS, constant(getStreamNumLambda()), ref("next"));
        forEachBlock(forEach, methodNode, scope, codegenClassScope);

        returnResult(methodNode.getBlock());
        return localMethod(methodNode, premade.getEps(), premade.getEnumcoll(), premade.getIsNewData(), premade.getExprCtx());
    }
}
