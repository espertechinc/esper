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
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.event.arr.ObjectArrayEventBean;
import com.espertech.esper.common.internal.event.arr.ObjectArrayEventType;
import com.espertech.esper.common.internal.util.JavaClassHelper;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.common.internal.epl.enummethod.eval.singlelambdaopt3form.mostleastfreq.EnumMostLeastFrequentHelper.getEnumMostLeastFrequentResult;

public class EnumMostLeastFrequentScalar extends ThreeFormScalar {

    protected final boolean isMostFrequent;
    private final EPTypeClass returnType;

    public EnumMostLeastFrequentScalar(ExprDotEvalParamLambda lambda, ObjectArrayEventType fieldEventType, int numParameters, boolean isMostFrequent) {
        super(lambda, fieldEventType, numParameters);
        this.isMostFrequent = isMostFrequent;
        this.returnType = JavaClassHelper.getBoxedType((EPTypeClass) innerExpression.getEvaluationType());
    }

    public EnumEval getEnumEvaluator() {
        ExprEvaluator inner = innerExpression.getExprEvaluator();
        return new EnumEval() {
            public Object evaluateEnumMethod(EventBean[] eventsLambda, Collection enumcoll, boolean isNewData, ExprEvaluatorContext context) {
                if (enumcoll.isEmpty()) {
                    return null;
                }

                Map<Object, Integer> items = new LinkedHashMap<Object, Integer>();
                Collection<Object> values = (Collection<Object>) enumcoll;

                ObjectArrayEventBean resultEvent = new ObjectArrayEventBean(new Object[3], fieldEventType);
                eventsLambda[getStreamNumLambda()] = resultEvent;
                Object[] props = resultEvent.getProperties();
                props[2] = enumcoll.size();

                int count = -1;
                for (Object next : values) {
                    count++;
                    props[1] = count;
                    props[0] = next;

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

    public EPTypeClass returnTypeOfMethod() {
        return returnType;
    }

    public CodegenExpression returnIfEmptyOptional() {
        return constantNull();
    }

    public void initBlock(CodegenBlock block, CodegenMethod methodNode, ExprForgeCodegenSymbol scope, CodegenClassScope codegenClassScope) {
        block.declareVar(EPTypePremade.MAP.getEPType(), "items", newInstance(EPTypePremade.LINKEDHASHMAP.getEPType()));
    }

    public void forEachBlock(CodegenBlock block, CodegenMethod methodNode, ExprForgeCodegenSymbol scope, CodegenClassScope codegenClassScope) {
        block
            .declareVar(EPTypePremade.OBJECT.getEPType(), "key", innerExpression.evaluateCodegen(EPTypePremade.OBJECT.getEPType(), methodNode, scope, codegenClassScope))
            .declareVar(EPTypePremade.INTEGERBOXED.getEPType(), "existing", cast(EPTypePremade.INTEGERBOXED.getEPType(), exprDotMethod(ref("items"), "get", ref("key"))))
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
