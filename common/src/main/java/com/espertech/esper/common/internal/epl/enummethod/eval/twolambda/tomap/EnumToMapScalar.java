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
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionField;
import com.espertech.esper.common.internal.context.module.EPStatementInitServices;
import com.espertech.esper.common.internal.epl.enummethod.codegen.EnumForgeCodegenNames;
import com.espertech.esper.common.internal.epl.enummethod.codegen.EnumForgeCodegenParams;
import com.espertech.esper.common.internal.epl.enummethod.eval.EnumEval;
import com.espertech.esper.common.internal.epl.enummethod.eval.EnumForgeBasePlain;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.epl.expression.core.ExprForge;
import com.espertech.esper.common.internal.event.arr.ObjectArrayEventBean;
import com.espertech.esper.common.internal.event.arr.ObjectArrayEventType;
import com.espertech.esper.common.internal.event.core.EventTypeUtility;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.common.internal.epl.enummethod.codegen.EnumForgeCodegenNames.REF_ENUMCOLL;

public class EnumToMapScalar extends EnumForgeBasePlain {

    protected final ExprForge secondExpression;
    protected final ObjectArrayEventType resultEventType;
    protected final int numParameters;

    public EnumToMapScalar(ExprForge innerExpression, int streamCountIncoming, ExprForge secondExpression, ObjectArrayEventType resultEventType, int numParameters) {
        super(innerExpression, streamCountIncoming);
        this.secondExpression = secondExpression;
        this.resultEventType = resultEventType;
        this.numParameters = numParameters;
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
                ObjectArrayEventBean resultEvent = new ObjectArrayEventBean(new Object[3], resultEventType);
                eventsLambda[getStreamNumLambda()] = resultEvent;
                Object[] props = resultEvent.getProperties();
                props[2] = enumcoll.size();
                Collection<Object> values = (Collection<Object>) enumcoll;

                int count = -1;
                for (Object next : values) {
                    count++;
                    props[1] = count;
                    props[0] = next;

                    Object key = first.evaluate(eventsLambda, isNewData, context);
                    Object value = second.evaluate(eventsLambda, isNewData, context);
                    map.put(key, value);
                }

                return map;
            }
        };
    }

    public CodegenExpression codegen(EnumForgeCodegenParams premade, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        CodegenExpressionField resultTypeMember = codegenClassScope.addFieldUnshared(true, ObjectArrayEventType.EPTYPE, cast(ObjectArrayEventType.EPTYPE, EventTypeUtility.resolveTypeCodegen(resultEventType, EPStatementInitServices.REF)));

        ExprForgeCodegenSymbol scope = new ExprForgeCodegenSymbol(false, null);
        CodegenMethod methodNode = codegenMethodScope.makeChildWithScope(EPTypePremade.MAP.getEPType(), EnumToMapScalar.class, scope, codegenClassScope).addParam(EnumForgeCodegenNames.PARAMS);
        boolean hasIndex = numParameters >= 2;
        boolean hasSize = numParameters >= 3;

        CodegenBlock block = methodNode.getBlock()
            .ifCondition(exprDotMethod(EnumForgeCodegenNames.REF_ENUMCOLL, "isEmpty"))
            .blockReturn(staticMethod(Collections.class, "emptyMap"));

        block.declareVar(EPTypePremade.MAP.getEPType(), "map", newInstance(EPTypePremade.HASHMAP.getEPType()))
            .declareVar(ObjectArrayEventBean.EPTYPE, "resultEvent", newInstance(ObjectArrayEventBean.EPTYPE, newArrayByLength(EPTypePremade.OBJECT.getEPType(), constant(numParameters)), resultTypeMember))
            .assignArrayElement(EnumForgeCodegenNames.REF_EPS, constant(getStreamNumLambda()), ref("resultEvent"))
            .declareVar(EPTypePremade.OBJECTARRAY.getEPType(), "props", exprDotMethod(ref("resultEvent"), "getProperties"));
        if (hasIndex) {
            block.declareVar(EPTypePremade.INTEGERPRIMITIVE.getEPType(), "count", constant(-1));
        }
        if (hasSize) {
            block.assignArrayElement(ref("props"), constant(2), exprDotMethod(REF_ENUMCOLL, "size"));
        }

        CodegenBlock forEach = block.forEach(EPTypePremade.OBJECT.getEPType(), "next", EnumForgeCodegenNames.REF_ENUMCOLL)
            .assignArrayElement("props", constant(0), ref("next"));
        if (hasIndex) {
            forEach.incrementRef("count").assignArrayElement("props", constant(1), ref("count"));
        }
        forEach.declareVar(EPTypePremade.OBJECT.getEPType(), "key", innerExpression.evaluateCodegen(EPTypePremade.OBJECT.getEPType(), methodNode, scope, codegenClassScope))
            .declareVar(EPTypePremade.OBJECT.getEPType(), "value", secondExpression.evaluateCodegen(EPTypePremade.OBJECT.getEPType(), methodNode, scope, codegenClassScope))
            .expression(exprDotMethod(ref("map"), "put", ref("key"), ref("value")));

        block.methodReturn(ref("map"));
        return localMethod(methodNode, premade.getEps(), premade.getEnumcoll(), premade.getIsNewData(), premade.getExprCtx());
    }
}
