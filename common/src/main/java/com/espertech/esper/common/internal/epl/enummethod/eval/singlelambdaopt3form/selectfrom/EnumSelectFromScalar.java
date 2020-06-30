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
package com.espertech.esper.common.internal.epl.enummethod.eval.singlelambdaopt3form.selectfrom;

import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.client.type.EPTypePremade;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenBlock;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.epl.enummethod.dot.ExprDotEvalParamLambda;
import com.espertech.esper.common.internal.epl.enummethod.eval.EnumEval;
import com.espertech.esper.common.internal.epl.enummethod.eval.singlelambdaopt3form.base.ThreeFormScalar;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.event.arr.ObjectArrayEventBean;
import com.espertech.esper.common.internal.event.arr.ObjectArrayEventType;

import java.util.ArrayDeque;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.common.internal.epl.enummethod.codegen.EnumForgeCodegenNames.REF_ENUMCOLL;

public class EnumSelectFromScalar extends ThreeFormScalar {

    public EnumSelectFromScalar(ExprDotEvalParamLambda lambda, ObjectArrayEventType resultEventType, int numParameters) {
        super(lambda, resultEventType, numParameters);
    }

    public EnumEval getEnumEvaluator() {
        final ExprEvaluator inner = innerExpression.getExprEvaluator();
        return (eventsLambda, enumcoll, isNewData, context) -> {
            if (enumcoll.isEmpty()) {
                return enumcoll;
            }

            ArrayDeque<Object> result = new ArrayDeque<>();
            ObjectArrayEventBean evalEvent = new ObjectArrayEventBean(new Object[3], fieldEventType);
            eventsLambda[getStreamNumLambda()] = evalEvent;
            Object[] evalProps = evalEvent.getProperties();

            int count = -1;
            evalProps[2] = enumcoll.size();

            for (Object next : enumcoll) {
                count++;
                evalProps[0] = next;
                evalProps[1] = count;

                Object value = inner.evaluate(eventsLambda, isNewData, context);
                if (value != null) {
                    result.add(value);
                }
            }
            return result;
        };
    }

    public EPTypeClass returnTypeOfMethod() {
        return EPTypePremade.COLLECTION.getEPType();
    }

    public CodegenExpression returnIfEmptyOptional() {
        return REF_ENUMCOLL;
    }

    public void initBlock(CodegenBlock block, CodegenMethod methodNode, ExprForgeCodegenSymbol scope, CodegenClassScope codegenClassScope) {
        block.declareVar(EPTypePremade.ARRAYDEQUE.getEPType(), "result", newInstance(EPTypePremade.ARRAYDEQUE.getEPType(), exprDotMethod(REF_ENUMCOLL, "size")));
    }

    public void forEachBlock(CodegenBlock block, CodegenMethod methodNode, ExprForgeCodegenSymbol scope, CodegenClassScope codegenClassScope) {
        block.declareVar(EPTypePremade.OBJECT.getEPType(), "item", innerExpression.evaluateCodegen(EPTypePremade.OBJECT.getEPType(), methodNode, scope, codegenClassScope))
            .ifCondition(notEqualsNull(ref("item")))
            .expression(exprDotMethod(ref("result"), "add", ref("item")));
    }

    public void returnResult(CodegenBlock block) {
        block.methodReturn(ref("result"));
    }
}
