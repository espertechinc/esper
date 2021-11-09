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
package com.espertech.esper.common.internal.epl.enummethod.eval.singlelambdaopt3form.arrayOf;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.client.type.EPTypePremade;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenBlock;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.epl.enummethod.codegen.EnumForgeCodegenNames;
import com.espertech.esper.common.internal.epl.enummethod.codegen.EnumForgeCodegenParams;
import com.espertech.esper.common.internal.epl.enummethod.eval.EnumEval;
import com.espertech.esper.common.internal.epl.enummethod.eval.EnumForge;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.util.JavaClassHelper;

import java.lang.reflect.Array;
import java.util.Collection;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class EnumArrayOfScalarNoParams implements EnumForge {

    private final EPTypeClass arrayComponentType;

    public EnumArrayOfScalarNoParams(EPTypeClass arrayComponentType) {
        this.arrayComponentType = arrayComponentType;
    }

    public EPTypeClass getArrayComponentType() {
        return arrayComponentType;
    }

    public EnumEval getEnumEvaluator() {
        return new EnumEval() {
            public Object evaluateEnumMethod(EventBean[] eventsLambda, Collection enumcoll, boolean isNewData, ExprEvaluatorContext context) {
                Object array = Array.newInstance(arrayComponentType.getType(), enumcoll.size());
                if (enumcoll.isEmpty()) {
                    return array;
                }
                int count = 0;
                for (Object next : enumcoll) {
                    Array.set(array, count, next);
                    count++;
                }
                return array;
            }
        };
    }

    public CodegenExpression codegen(EnumForgeCodegenParams premade, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        EPTypeClass arrayType = JavaClassHelper.getArrayType(arrayComponentType);
        ExprForgeCodegenSymbol scope = new ExprForgeCodegenSymbol(false, null);
        CodegenMethod methodNode = codegenMethodScope.makeChildWithScope(arrayType, EnumArrayOfScalarNoParams.class, scope, codegenClassScope).addParam(EnumForgeCodegenNames.PARAMSCOLLOBJ);

        CodegenBlock block = methodNode.getBlock()
            .ifCondition(exprDotMethod(EnumForgeCodegenNames.REF_ENUMCOLL, "isEmpty"))
            .blockReturn(newArrayByLength(arrayComponentType, constant(0)))
            .declareVar(arrayType, "result", newArrayByLength(arrayComponentType, exprDotMethod(EnumForgeCodegenNames.REF_ENUMCOLL, "size")))
            .declareVar(EPTypePremade.INTEGERPRIMITIVE.getEPType(), "count", constant(0));
        block.forEach(EPTypePremade.OBJECT.getEPType(), "next", EnumForgeCodegenNames.REF_ENUMCOLL)
            .assignArrayElement(ref("result"), ref("count"), cast(arrayComponentType, ref("next")))
            .incrementRef("count");
        block.methodReturn(ref("result"));
        return localMethod(methodNode, premade.getEps(), premade.getEnumcoll(), premade.getIsNewData(), premade.getExprCtx());
    }

    public int getStreamNumSize() {
        return 0;
    }
}
