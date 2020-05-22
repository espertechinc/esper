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
package com.espertech.esper.common.internal.epl.enummethod.eval.twolambda.groupby;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenBlock;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.epl.enummethod.eval.EnumEval;
import com.espertech.esper.common.internal.epl.enummethod.eval.twolambda.base.TwoLambdaThreeFormEventPlus;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.epl.expression.core.ExprForge;
import com.espertech.esper.common.internal.event.arr.ObjectArrayEventBean;
import com.espertech.esper.common.internal.event.arr.ObjectArrayEventType;

import java.util.*;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class EnumGroupByTwoParamEventPlus extends TwoLambdaThreeFormEventPlus {

    public EnumGroupByTwoParamEventPlus(ExprForge innerExpression, int streamNumLambda, ObjectArrayEventType indexEventType, ExprForge secondExpression, int numParameters) {
        super(innerExpression, streamNumLambda, indexEventType, secondExpression, numParameters);
    }

    public EnumEval getEnumEvaluator() {
        ExprEvaluator first = innerExpression.getExprEvaluator();
        ExprEvaluator second = secondExpression.getExprEvaluator();
        return new EnumEval() {
            public Object evaluateEnumMethod(EventBean[] eventsLambda, Collection enumcoll, boolean isNewData, ExprEvaluatorContext context) {
                if (enumcoll.isEmpty()) {
                    return Collections.emptyMap();
                }

                Map<Object, Collection> result = new LinkedHashMap<Object, Collection>();
                ObjectArrayEventBean indexEvent = new ObjectArrayEventBean(new Object[2], fieldEventType);
                Object[] props = indexEvent.getProperties();
                props[1] = enumcoll.size();
                eventsLambda[getStreamNumLambda() + 1] = indexEvent;
                Collection<EventBean> beans = (Collection<EventBean>) enumcoll;

                int count = -1;
                for (EventBean next : beans) {
                    count++;
                    props[0] = count;
                    eventsLambda[getStreamNumLambda()] = next;

                    Object key = first.evaluate(eventsLambda, isNewData, context);
                    Object entry = second.evaluate(eventsLambda, isNewData, context);

                    Collection value = result.get(key);
                    if (value == null) {
                        value = new ArrayList();
                        result.put(key, value);
                    }
                    value.add(entry);
                }

                return result;
            }
        };
    }

    public Class returnType() {
        return Map.class;
    }

    public CodegenExpression returnIfEmptyOptional() {
        return staticMethod(Collections.class, "emptyMap");
    }

    public void initBlock(CodegenBlock block, CodegenMethod methodNode, ExprForgeCodegenSymbol scope, CodegenClassScope codegenClassScope) {
        block.declareVar(Map.class, "result", newInstance(LinkedHashMap.class));
    }

    public void forEachBlock(CodegenBlock block, CodegenMethod methodNode, ExprForgeCodegenSymbol scope, CodegenClassScope codegenClassScope) {
        block.declareVar(Object.class, "key", innerExpression.evaluateCodegen(Object.class, methodNode, scope, codegenClassScope))
            .declareVar(Object.class, "entry", secondExpression.evaluateCodegen(Object.class, methodNode, scope, codegenClassScope))
            .declareVar(Collection.class, "value", cast(Collection.class, exprDotMethod(ref("result"), "get", ref("key"))))
            .ifRefNull("value")
            .assignRef("value", newInstance(ArrayList.class))
            .expression(exprDotMethod(ref("result"), "put", ref("key"), ref("value")))
            .blockEnd()
            .expression(exprDotMethod(ref("value"), "add", ref("entry")));
    }

    public void returnResult(CodegenBlock block) {
        block.methodReturn(ref("result"));
    }
}
