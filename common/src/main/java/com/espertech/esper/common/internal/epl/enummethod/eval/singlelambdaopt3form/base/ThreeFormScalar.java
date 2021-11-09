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

import com.espertech.esper.common.client.type.EPTypeClass;
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
import com.espertech.esper.common.internal.epl.enummethod.dot.ExprDotEvalParamLambda;
import com.espertech.esper.common.internal.epl.enummethod.eval.EnumForgeBasePlain;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.event.arr.ObjectArrayEventBean;
import com.espertech.esper.common.internal.event.arr.ObjectArrayEventType;
import com.espertech.esper.common.internal.event.core.EventTypeUtility;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.common.internal.epl.enummethod.codegen.EnumForgeCodegenNames.REF_ENUMCOLL;

public abstract class ThreeFormScalar extends EnumForgeBasePlain {

    protected final ObjectArrayEventType fieldEventType;
    protected final int numParameters;

    public abstract EPTypeClass returnTypeOfMethod();

    public abstract CodegenExpression returnIfEmptyOptional();

    public abstract void initBlock(CodegenBlock block, CodegenMethod methodNode, ExprForgeCodegenSymbol scope, CodegenClassScope codegenClassScope);

    public boolean hasForEachLoop() {
        return true;
    }

    public abstract void forEachBlock(CodegenBlock block, CodegenMethod methodNode, ExprForgeCodegenSymbol scope, CodegenClassScope codegenClassScope);

    public abstract void returnResult(CodegenBlock block);

    public ThreeFormScalar(ExprDotEvalParamLambda lambda, ObjectArrayEventType fieldEventType, int numParameters) {
        super(lambda);
        this.fieldEventType = fieldEventType;
        this.numParameters = numParameters;
    }

    public CodegenExpression codegen(EnumForgeCodegenParams premade, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        CodegenExpressionField resultTypeMember = codegenClassScope.addFieldUnshared(true, ObjectArrayEventType.EPTYPE, cast(ObjectArrayEventType.EPTYPE, EventTypeUtility.resolveTypeCodegen(fieldEventType, EPStatementInitServices.REF)));

        ExprForgeCodegenSymbol scope = new ExprForgeCodegenSymbol(false, null);
        CodegenMethod methodNode = codegenMethodScope.makeChildWithScope(returnTypeOfMethod(), getClass(), scope, codegenClassScope).addParam(EnumForgeCodegenNames.PARAMSCOLLOBJ);
        CodegenBlock block = methodNode.getBlock();
        boolean hasIndex = numParameters >= 2;
        boolean hasSize = numParameters >= 3;

        CodegenExpression returnEmpty = returnIfEmptyOptional();
        if (returnEmpty != null) {
            block.ifCondition(exprDotMethod(REF_ENUMCOLL, "isEmpty"))
                .blockReturn(returnEmpty);
        }

        block.declareVar(ObjectArrayEventBean.EPTYPE, "resultEvent", newInstance(ObjectArrayEventBean.EPTYPE, newArrayByLength(EPTypePremade.OBJECT.getEPType(), constant(numParameters)), resultTypeMember))
            .assignArrayElement(EnumForgeCodegenNames.REF_EPS, constant(getStreamNumLambda()), ref("resultEvent"))
            .declareVar(EPTypePremade.OBJECTARRAY.getEPType(), "props", exprDotMethod(ref("resultEvent"), "getProperties"));
        if (hasIndex) {
            block.declareVar(EPTypePremade.INTEGERPRIMITIVE.getEPType(), "count", constant(-1));
        }
        if (hasSize) {
            block.assignArrayElement(ref("props"), constant(2), exprDotMethod(REF_ENUMCOLL, "size"));
        }
        initBlock(block, methodNode, scope, codegenClassScope);

        if (hasForEachLoop()) {
            CodegenBlock forEach = block.forEach(EPTypePremade.OBJECT.getEPType(), "next", REF_ENUMCOLL)
                .assignArrayElement("props", constant(0), ref("next"));
            if (hasIndex) {
                forEach.incrementRef("count").assignArrayElement("props", constant(1), ref("count"));
            }
            forEachBlock(forEach, methodNode, scope, codegenClassScope);
        }

        returnResult(block);
        return localMethod(methodNode, premade.getEps(), premade.getEnumcoll(), premade.getIsNewData(), premade.getExprCtx());
    }

    public int getNumParameters() {
        return numParameters;
    }
}
