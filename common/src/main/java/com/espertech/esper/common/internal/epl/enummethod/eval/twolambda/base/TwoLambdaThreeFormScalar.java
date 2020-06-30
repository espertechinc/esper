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

import com.espertech.esper.common.client.type.EPTypePremade;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenBlock;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionField;
import com.espertech.esper.common.internal.context.module.EPStatementInitServices;
import com.espertech.esper.common.internal.epl.enummethod.codegen.EnumForgeCodegenNames;
import com.espertech.esper.common.internal.epl.enummethod.codegen.EnumForgeCodegenParams;
import com.espertech.esper.common.internal.epl.enummethod.eval.EnumForgeBasePlain;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprForge;
import com.espertech.esper.common.internal.event.arr.ObjectArrayEventBean;
import com.espertech.esper.common.internal.event.arr.ObjectArrayEventType;
import com.espertech.esper.common.internal.event.core.EventTypeUtility;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.common.internal.epl.enummethod.codegen.EnumForgeCodegenNames.REF_ENUMCOLL;

public abstract class TwoLambdaThreeFormScalar extends EnumForgeBasePlain {

    protected final ExprForge secondExpression;
    protected final ObjectArrayEventType resultEventType;
    protected final int numParameters;

    public abstract Class returnType();

    public abstract CodegenExpression returnIfEmptyOptional();

    public abstract void initBlock(CodegenBlock block, CodegenMethod methodNode, ExprForgeCodegenSymbol scope, CodegenClassScope codegenClassScope);

    public abstract void forEachBlock(CodegenBlock block, CodegenMethod methodNode, ExprForgeCodegenSymbol scope, CodegenClassScope codegenClassScope);

    public abstract void returnResult(CodegenBlock block);

    public TwoLambdaThreeFormScalar(ExprForge innerExpression, int streamCountIncoming, ExprForge secondExpression, ObjectArrayEventType resultEventType, int numParameters) {
        super(innerExpression, streamCountIncoming);
        this.secondExpression = secondExpression;
        this.resultEventType = resultEventType;
        this.numParameters = numParameters;
    }

    public CodegenExpression codegen(EnumForgeCodegenParams premade, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        CodegenExpressionField resultTypeMember = codegenClassScope.addFieldUnshared(true, ObjectArrayEventType.EPTYPE, cast(ObjectArrayEventType.EPTYPE, EventTypeUtility.resolveTypeCodegen(resultEventType, EPStatementInitServices.REF)));

        ExprForgeCodegenSymbol scope = new ExprForgeCodegenSymbol(false, null);
        CodegenMethod methodNode = codegenMethodScope.makeChildWithScope(EPTypePremade.MAP.getEPType(), getClass(), scope, codegenClassScope).addParam(EnumForgeCodegenNames.PARAMS);
        boolean hasIndex = numParameters >= 2;
        boolean hasSize = numParameters >= 3;

        CodegenExpression returnIfEmpty = returnIfEmptyOptional();
        if (returnIfEmpty != null) {
            methodNode.getBlock()
                    .ifCondition(exprDotMethod(EnumForgeCodegenNames.REF_ENUMCOLL, "isEmpty"))
                    .blockReturn(returnIfEmpty);
        }

        initBlock(methodNode.getBlock(), methodNode, scope, codegenClassScope);
        methodNode.getBlock().declareVar(ObjectArrayEventBean.EPTYPE, "resultEvent", newInstance(ObjectArrayEventBean.EPTYPE, newArrayByLength(EPTypePremade.OBJECT.getEPType(), constant(numParameters)), resultTypeMember))
                .assignArrayElement(EnumForgeCodegenNames.REF_EPS, constant(getStreamNumLambda()), ref("resultEvent"))
                .declareVar(EPTypePremade.OBJECTARRAY.getEPType(), "props", exprDotMethod(ref("resultEvent"), "getProperties"));
        if (hasIndex) {
            methodNode.getBlock().declareVar(EPTypePremade.INTEGERPRIMITIVE.getEPType(), "count", constant(-1));
        }
        if (hasSize) {
            methodNode.getBlock().assignArrayElement(ref("props"), constant(2), exprDotMethod(REF_ENUMCOLL, "size"));
        }

        CodegenBlock forEach = methodNode.getBlock().forEach(EPTypePremade.OBJECT.getEPType(), "next", EnumForgeCodegenNames.REF_ENUMCOLL)
                .assignArrayElement("props", constant(0), ref("next"));
        if (hasIndex) {
            forEach.incrementRef("count").assignArrayElement("props", constant(1), ref("count"));
        }

        forEachBlock(forEach, methodNode, scope, codegenClassScope);

        returnResult(methodNode.getBlock());
        return localMethod(methodNode, premade.getEps(), premade.getEnumcoll(), premade.getIsNewData(), premade.getExprCtx());
    }
}
