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
import com.espertech.esper.common.client.type.EPTypePremade;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenBlock;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.epl.enummethod.eval.EnumEval;
import com.espertech.esper.common.internal.epl.enummethod.eval.twolambda.base.TwoLambdaThreeFormScalar;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.epl.expression.core.ExprForge;
import com.espertech.esper.common.internal.event.arr.ObjectArrayEventBean;
import com.espertech.esper.common.internal.event.arr.ObjectArrayEventType;

import java.util.*;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class EnumGroupByTwoParamScalar extends TwoLambdaThreeFormScalar {

    public EnumGroupByTwoParamScalar(ExprForge innerExpression, int streamCountIncoming, ExprForge secondExpression, ObjectArrayEventType resultEventType, int numParameters) {
        super(innerExpression, streamCountIncoming, secondExpression, resultEventType, numParameters);
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

                ObjectArrayEventBean resultEvent = new ObjectArrayEventBean(new Object[3], resultEventType);
                Object[] props = resultEvent.getProperties();
                props[2] = enumcoll.size();
                eventsLambda[getStreamNumLambda()] = resultEvent;
                Collection<Object> values = (Collection<Object>) enumcoll;

                int count = -1;
                for (Object next : values) {
                    count++;
                    props[1] = count;
                    props[0] = next;
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
        block.declareVar(EPTypePremade.MAP.getEPType(), "result", newInstance(EPTypePremade.LINKEDHASHMAP.getEPType()));
    }

    public void forEachBlock(CodegenBlock block, CodegenMethod methodNode, ExprForgeCodegenSymbol scope, CodegenClassScope codegenClassScope) {
        block.declareVar(EPTypePremade.OBJECT.getEPType(), "key", innerExpression.evaluateCodegen(EPTypePremade.OBJECT.getEPType(), methodNode, scope, codegenClassScope))
            .declareVar(EPTypePremade.OBJECT.getEPType(), "entry", secondExpression.evaluateCodegen(EPTypePremade.OBJECT.getEPType(), methodNode, scope, codegenClassScope))
            .declareVar(EPTypePremade.COLLECTION.getEPType(), "value", cast(EPTypePremade.COLLECTION.getEPType(), exprDotMethod(ref("result"), "get", ref("key"))))
            .ifRefNull("value")
            .assignRef("value", newInstance(EPTypePremade.ARRAYLIST.getEPType()))
            .expression(exprDotMethod(ref("result"), "put", ref("key"), ref("value")))
            .blockEnd()
            .expression(exprDotMethod(ref("value"), "add", ref("entry")));
    }

    public void returnResult(CodegenBlock block) {
        block.methodReturn(ref("result"));
    }
}
