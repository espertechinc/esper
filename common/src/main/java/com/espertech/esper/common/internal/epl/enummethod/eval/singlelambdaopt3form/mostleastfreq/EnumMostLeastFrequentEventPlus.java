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
package com.espertech.esper.common.internal.epl.enummethod.eval.singlelambdaopt3form.mostleastfreq;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenBlock;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.epl.enummethod.dot.ExprDotEvalParamLambda;
import com.espertech.esper.common.internal.epl.enummethod.eval.EnumEval;
import com.espertech.esper.common.internal.epl.enummethod.eval.singlelambdaopt3form.base.ThreeFormEventPlus;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.event.arr.ObjectArrayEventBean;
import com.espertech.esper.common.internal.event.arr.ObjectArrayEventType;
import com.espertech.esper.common.internal.util.JavaClassHelper;

import java.util.*;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.common.internal.epl.enummethod.eval.singlelambdaopt3form.mostleastfreq.EnumMostLeastFrequentHelper.getEnumMostLeastFrequentResult;

public class EnumMostLeastFrequentEventPlus extends ThreeFormEventPlus {

    protected final boolean isMostFrequent;
    private final Class returnType;

    public EnumMostLeastFrequentEventPlus(ExprDotEvalParamLambda lambda, ObjectArrayEventType indexEventType, int numParameters, boolean isMostFrequent) {
        super(lambda, indexEventType, numParameters);
        this.isMostFrequent = isMostFrequent;
        this.returnType = JavaClassHelper.getBoxedType(innerExpression.getEvaluationType());
    }

    public EnumEval getEnumEvaluator() {
        ExprEvaluator inner = innerExpression.getExprEvaluator();
        return new EnumEval() {
            public Object evaluateEnumMethod(EventBean[] eventsLambda, Collection enumcoll, boolean isNewData, ExprEvaluatorContext context) {
                if (enumcoll.isEmpty()) {
                    return null;
                }

                Map<Object, Integer> items = new LinkedHashMap<Object, Integer>();
                Collection<EventBean> beans = (Collection<EventBean>) enumcoll;
                ObjectArrayEventBean indexEvent = new ObjectArrayEventBean(new Object[2], fieldEventType);
                Object[] props = indexEvent.getProperties();
                props[1] = enumcoll.size();
                eventsLambda[getStreamNumLambda() + 1] = indexEvent;

                int count = -1;
                for (EventBean next : beans) {
                    count++;
                    props[0] = count;
                    eventsLambda[getStreamNumLambda()] = next;

                    Object item = inner.evaluate(eventsLambda, isNewData, context);
                    Integer existing = items.get(item);

                    if (existing == null) {
                        existing = 1;
                    } else {
                        existing++;
                    }
                    items.put(item, existing);
                }

                return getEnumMostLeastFrequentResult(items, isMostFrequent);
            }
        };
    }

    public Class returnType() {
        return returnType;
    }

    public CodegenExpression returnIfEmptyOptional() {
        return constantNull();
    }

    public void initBlock(CodegenBlock block, CodegenMethod methodNode, ExprForgeCodegenSymbol scope, CodegenClassScope codegenClassScope) {
        block.declareVar(Map.class, "items", newInstance(LinkedHashMap.class));
    }

    public void forEachBlock(CodegenBlock block, CodegenMethod methodNode, ExprForgeCodegenSymbol scope, CodegenClassScope codegenClassScope) {
        block.declareVar(Object.class, "key", innerExpression.evaluateCodegen(Object.class, methodNode, scope, codegenClassScope))
            .declareVar(Integer.class, "existing", cast(Integer.class, exprDotMethod(ref("items"), "get", ref("key"))))
            .ifCondition(equalsNull(ref("existing")))
            .assignRef("existing", constant(1))
            .ifElse()
            .incrementRef("existing")
            .blockEnd()
            .exprDotMethod(ref("items"), "put", ref("key"), ref("existing"));
    }

    public void returnResult(CodegenBlock block) {
        block.methodReturn(cast(returnType, staticMethod(EnumMostLeastFrequentHelper.class, "getEnumMostLeastFrequentResult", ref("items"), constant(isMostFrequent))));
    }
}
