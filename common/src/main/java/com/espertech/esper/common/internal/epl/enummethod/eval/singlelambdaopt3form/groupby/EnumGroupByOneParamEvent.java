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
package com.espertech.esper.common.internal.epl.enummethod.eval.singlelambdaopt3form.groupby;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenBlock;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.epl.enummethod.dot.ExprDotEvalParamLambda;
import com.espertech.esper.common.internal.epl.enummethod.eval.EnumEval;
import com.espertech.esper.common.internal.epl.enummethod.eval.singlelambdaopt3form.base.ThreeFormEventPlain;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;

import java.util.*;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class EnumGroupByOneParamEvent extends ThreeFormEventPlain {

    public EnumGroupByOneParamEvent(ExprDotEvalParamLambda lambda) {
        super(lambda);
    }

    public EnumEval getEnumEvaluator() {
        ExprEvaluator inner = innerExpression.getExprEvaluator();
        return new EnumEval() {
            public Object evaluateEnumMethod(EventBean[] eventsLambda, Collection enumcoll, boolean isNewData, ExprEvaluatorContext context) {
                if (enumcoll.isEmpty()) {
                    return Collections.emptyMap();
                }

                Map<Object, Collection> result = new LinkedHashMap<Object, Collection>();

                Collection<EventBean> beans = (Collection<EventBean>) enumcoll;
                for (EventBean next : beans) {
                    eventsLambda[getStreamNumLambda()] = next;

                    Object key = inner.evaluate(eventsLambda, isNewData, context);

                    Collection value = result.get(key);
                    if (value == null) {
                        value = new ArrayList();
                        result.put(key, value);
                    }
                    value.add(next.getUnderlying());
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
            .declareVar(Collection.class, "value", cast(Collection.class, exprDotMethod(ref("result"), "get", ref("key"))))
            .ifRefNull("value")
            .assignRef("value", newInstance(ArrayList.class))
            .expression(exprDotMethod(ref("result"), "put", ref("key"), ref("value")))
            .blockEnd()
            .expression(exprDotMethod(ref("value"), "add", exprDotUnderlying(ref("next"))));
    }

    public void returnResult(CodegenBlock block) {
        block.methodReturn(ref("result"));
    }
}
