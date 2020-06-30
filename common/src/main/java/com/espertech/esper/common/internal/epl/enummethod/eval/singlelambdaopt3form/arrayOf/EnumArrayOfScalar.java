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

import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.client.type.EPTypePremade;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenBlock;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.epl.enummethod.codegen.EnumForgeCodegenNames;
import com.espertech.esper.common.internal.epl.enummethod.dot.ExprDotEvalParamLambda;
import com.espertech.esper.common.internal.epl.enummethod.eval.EnumEval;
import com.espertech.esper.common.internal.epl.enummethod.eval.singlelambdaopt3form.base.ThreeFormScalar;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.event.arr.ObjectArrayEventBean;
import com.espertech.esper.common.internal.event.arr.ObjectArrayEventType;
import com.espertech.esper.common.internal.util.JavaClassHelper;

import java.lang.reflect.Array;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class EnumArrayOfScalar extends ThreeFormScalar {

    private final EPTypeClass arrayComponentType;

    public EnumArrayOfScalar(ExprDotEvalParamLambda lambda, ObjectArrayEventType fieldEventType, int numParameters, EPTypeClass arrayComponentType) {
        super(lambda, fieldEventType, numParameters);
        this.arrayComponentType = arrayComponentType;
    }

    public EnumEval getEnumEvaluator() {
        final ExprEvaluator inner = innerExpression.getExprEvaluator();
        return (eventsLambda, enumcoll, isNewData, context) -> {

            Object array = Array.newInstance(arrayComponentType.getType(), enumcoll.size());
            if (enumcoll.isEmpty()) {
                return array;
            }

            ObjectArrayEventBean evalEvent = new ObjectArrayEventBean(new Object[3], fieldEventType);
            eventsLambda[getStreamNumLambda()] = evalEvent;
            Object[] evalProps = evalEvent.getProperties();
            evalProps[2] = enumcoll.size();
            int count = -1;

            for (Object next : enumcoll) {
                count++;
                evalProps[0] = next;
                evalProps[1] = count;
                Object item = inner.evaluate(eventsLambda, isNewData, context);
                Array.set(array, count, item);
            }
            return array;
        };
    }

    public EPTypeClass returnTypeOfMethod() {
        return JavaClassHelper.getArrayType(arrayComponentType);
    }

    public CodegenExpression returnIfEmptyOptional() {
        return newArrayByLength(arrayComponentType, constant(0));
    }

    public void initBlock(CodegenBlock block, CodegenMethod methodNode, ExprForgeCodegenSymbol scope, CodegenClassScope codegenClassScope) {
        EPTypeClass arrayType = returnTypeOfMethod();
        block.declareVar(arrayType, "result", newArrayByLength(arrayComponentType, exprDotMethod(EnumForgeCodegenNames.REF_ENUMCOLL, "size")))
            .declareVar(EPTypePremade.INTEGERPRIMITIVE.getEPType(), "index", constant(0));
    }

    public void forEachBlock(CodegenBlock block, CodegenMethod methodNode, ExprForgeCodegenSymbol scope, CodegenClassScope codegenClassScope) {
        block.declareVar(EPTypePremade.OBJECT.getEPType(), "item", innerExpression.evaluateCodegen(EPTypePremade.OBJECT.getEPType(), methodNode, scope, codegenClassScope))
            .assignArrayElement(ref("result"), ref("index"), cast(arrayComponentType, ref("item")))
            .incrementRef("index");
    }

    public void returnResult(CodegenBlock block) {
        block.methodReturn(ref("result"));
    }
}
