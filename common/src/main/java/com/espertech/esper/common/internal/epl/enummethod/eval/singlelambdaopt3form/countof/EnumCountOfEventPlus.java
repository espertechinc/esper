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
package com.espertech.esper.common.internal.epl.enummethod.eval.singlelambdaopt3form.countof;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenBlock;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.epl.enummethod.dot.ExprDotEvalParamLambda;
import com.espertech.esper.common.internal.epl.enummethod.eval.EnumEval;
import com.espertech.esper.common.internal.epl.enummethod.eval.singlelambdaopt3form.base.ThreeFormEventPlus;
import com.espertech.esper.common.internal.epl.expression.codegen.CodegenLegoBooleanExpression;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.event.arr.ObjectArrayEventBean;
import com.espertech.esper.common.internal.event.arr.ObjectArrayEventType;

import java.util.Collection;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.constant;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.ref;

public class EnumCountOfEventPlus extends ThreeFormEventPlus {
    public EnumCountOfEventPlus(ExprDotEvalParamLambda lambda, ObjectArrayEventType indexEventType, int numParameters) {
        super(lambda, indexEventType, numParameters);
    }

    public EnumEval getEnumEvaluator() {
        ExprEvaluator inner = innerExpression.getExprEvaluator();
        return (eventsLambda, enumcoll, isNewData, context) -> {
            if (enumcoll.isEmpty()) {
                return 0;
            }

            Collection<EventBean> beans = (Collection<EventBean>) enumcoll;
            ObjectArrayEventBean indexEvent = new ObjectArrayEventBean(new Object[2], fieldEventType);
            eventsLambda[getStreamNumLambda() + 1] = indexEvent;
            Object[] props = indexEvent.getProperties();
            props[1] = enumcoll.size();
            int rowcount = 0;
            int count = -1;

            for (EventBean next : beans) {
                count++;
                props[0] = count;
                eventsLambda[getStreamNumLambda()] = next;

                Object pass = inner.evaluate(eventsLambda, isNewData, context);
                if (pass == null || (!(Boolean) pass)) {
                    continue;
                }
                rowcount++;
            }
            return rowcount;
        };
    }

    public Class returnType() {
        return int.class;
    }

    public CodegenExpression returnIfEmptyOptional() {
        return constant(0);
    }

    public void initBlock(CodegenBlock block, CodegenMethod methodNode, ExprForgeCodegenSymbol scope, CodegenClassScope codegenClassScope) {
        block.declareVar(int.class, "rowcount", constant(0));
    }

    public void forEachBlock(CodegenBlock block, CodegenMethod methodNode, ExprForgeCodegenSymbol scope, CodegenClassScope codegenClassScope) {
        CodegenLegoBooleanExpression.codegenContinueIfNotNullAndNotPass(block, innerExpression.getEvaluationType(), innerExpression.evaluateCodegen(Boolean.class, methodNode, scope, codegenClassScope));
        block.incrementRef("rowcount");
    }

    public void returnResult(CodegenBlock block) {
        block.methodReturn(ref("rowcount"));
    }
}
