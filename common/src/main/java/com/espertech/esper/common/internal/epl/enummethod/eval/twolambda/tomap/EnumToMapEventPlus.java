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
package com.espertech.esper.common.internal.epl.enummethod.eval.twolambda.tomap;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.type.EPTypePremade;
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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class EnumToMapEventPlus extends TwoLambdaThreeFormEventPlus {

    public EnumToMapEventPlus(ExprForge innerExpression, int streamNumLambda, ObjectArrayEventType indexEventType, ExprForge secondExpression, int numParameters) {
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

                Map map = new HashMap();
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
                    Object value = second.evaluate(eventsLambda, isNewData, context);
                    map.put(key, value);
                }

                return map;
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
        block.declareVar(EPTypePremade.MAP.getEPType(), "map", newInstance(EPTypePremade.HASHMAP.getEPType()));
    }

    public void forEachBlock(CodegenBlock block, CodegenMethod methodNode, ExprForgeCodegenSymbol scope, CodegenClassScope codegenClassScope) {
        block.declareVar(EPTypePremade.OBJECT.getEPType(), "key", innerExpression.evaluateCodegen(EPTypePremade.OBJECT.getEPType(), methodNode, scope, codegenClassScope))
            .declareVar(EPTypePremade.OBJECT.getEPType(), "value", secondExpression.evaluateCodegen(EPTypePremade.OBJECT.getEPType(), methodNode, scope, codegenClassScope))
            .expression(exprDotMethod(ref("map"), "put", ref("key"), ref("value")));
    }

    public void returnResult(CodegenBlock block) {
        block.methodReturn(ref("map"));
    }
}
