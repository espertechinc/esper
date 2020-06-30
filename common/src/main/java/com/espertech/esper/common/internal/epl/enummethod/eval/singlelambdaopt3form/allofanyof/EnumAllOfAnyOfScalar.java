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
package com.espertech.esper.common.internal.epl.enummethod.eval.singlelambdaopt3form.allofanyof;

import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.client.type.EPTypePremade;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenBlock;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.epl.enummethod.dot.ExprDotEvalParamLambda;
import com.espertech.esper.common.internal.epl.enummethod.eval.EnumEval;
import com.espertech.esper.common.internal.epl.enummethod.eval.singlelambdaopt3form.base.ThreeFormScalar;
import com.espertech.esper.common.internal.epl.expression.codegen.CodegenLegoBooleanExpression;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.event.arr.ObjectArrayEventBean;
import com.espertech.esper.common.internal.event.arr.ObjectArrayEventType;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.constant;

public class EnumAllOfAnyOfScalar extends ThreeFormScalar {

    private final boolean all;

    public EnumAllOfAnyOfScalar(ExprDotEvalParamLambda lambda, ObjectArrayEventType resultEventType, int numParameters, boolean all) {
        super(lambda, resultEventType, numParameters);
        this.all = all;
    }

    public EnumEval getEnumEvaluator() {
        ExprEvaluator inner = innerExpression.getExprEvaluator();
        return (eventsLambda, enumcoll, isNewData, context) -> {
            if (enumcoll.isEmpty()) {
                return all;
            }

            ObjectArrayEventBean evalEvent = new ObjectArrayEventBean(new Object[3], fieldEventType);
            eventsLambda[getStreamNumLambda()] = evalEvent;
            Object[] props = evalEvent.getProperties();
            int count = -1;
            props[2] = enumcoll.size();

            for (Object next : enumcoll) {
                count++;
                props[0] = next;
                props[1] = count;

                Object pass = inner.evaluate(eventsLambda, isNewData, context);
                if (all) {
                    if (pass == null || (!(Boolean) pass)) {
                        return false;
                    }
                } else {
                    if (pass != null && ((Boolean) pass)) {
                        return true;
                    }
                }
            }

            return all;
        };
    }

    public EPTypeClass returnTypeOfMethod() {
        return EPTypePremade.BOOLEANBOXED.getEPType();
    }

    public CodegenExpression returnIfEmptyOptional() {
        return constant(all);
    }

    public void initBlock(CodegenBlock block, CodegenMethod methodNode, ExprForgeCodegenSymbol scope, CodegenClassScope codegenClassScope) {
    }

    public void forEachBlock(CodegenBlock block, CodegenMethod methodNode, ExprForgeCodegenSymbol scope, CodegenClassScope codegenClassScope) {
        CodegenLegoBooleanExpression.codegenReturnBoolIfNullOrBool(block, innerExpression.getEvaluationType(), innerExpression.evaluateCodegen(EPTypePremade.BOOLEANPRIMITIVE.getEPType(), methodNode, scope, codegenClassScope), all, all ? false : null, !all, !all);
    }

    public void returnResult(CodegenBlock block) {
        block.methodReturn(constant(all));
    }
}
