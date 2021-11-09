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
package com.espertech.esper.common.internal.epl.enummethod.eval.aggregate;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.type.EPType;
import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.client.type.EPTypeNull;
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
import com.espertech.esper.common.internal.util.JavaClassHelper;

import java.util.Collection;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class EnumAggregateScalar extends EnumForgeBasePlain {
    private final ExprForge initialization;
    private final ExprForge innerExpression;
    private final ObjectArrayEventType eventType;
    private final int numParameters;

    public EnumAggregateScalar(int streamCountIncoming, ExprForge initialization, ExprForge innerExpression, ObjectArrayEventType eventType, int numParameters) {
        super(streamCountIncoming);
        this.initialization = initialization;
        this.innerExpression = innerExpression;
        this.eventType = eventType;
        this.numParameters = numParameters;
    }

    public EnumEval getEnumEvaluator() {
        ExprEvaluator init = initialization.getExprEvaluator();
        ExprEvaluator inner = innerExpression.getExprEvaluator();
        return new EnumEval() {
            public Object evaluateEnumMethod(EventBean[] eventsLambda, Collection enumcoll, boolean isNewData, ExprEvaluatorContext context) {
                Object value = init.evaluate(eventsLambda, isNewData, context);
                if (enumcoll.isEmpty()) {
                    return value;
                }

                ObjectArrayEventBean event = new ObjectArrayEventBean(new Object[4], eventType);
                eventsLambda[getStreamNumLambda()] = event;
                Object[] props = event.getProperties();
                props[3] = enumcoll.size();

                int count = -1;
                for (Object next : enumcoll) {
                    count++;
                    props[0] = value;
                    props[1] = next;
                    props[2] = count;
                    value = inner.evaluate(eventsLambda, isNewData, context);
                }

                return value;
            }
        };
    }

    public CodegenExpression codegen(EnumForgeCodegenParams premade, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        CodegenExpressionField typeMember = codegenClassScope.addFieldUnshared(true, ObjectArrayEventType.EPTYPE, cast(ObjectArrayEventType.EPTYPE, EventTypeUtility.resolveTypeCodegen(eventType, EPStatementInitServices.REF)));

        EPTypeClass initializationType = JavaClassHelper.getBoxedType((EPTypeClass) initialization.getEvaluationType());
        EPType innerType = innerExpression.getEvaluationType();

        ExprForgeCodegenSymbol scope = new ExprForgeCodegenSymbol(false, null);
        CodegenMethod methodNode = codegenMethodScope.makeChildWithScope(initializationType, EnumAggregateScalar.class, scope, codegenClassScope).addParam(EnumForgeCodegenNames.PARAMSCOLLOBJ);

        CodegenBlock block = methodNode.getBlock();
        block.declareVar(initializationType, "value", initialization.evaluateCodegen(initializationType, methodNode, scope, codegenClassScope))
            .ifCondition(exprDotMethod(EnumForgeCodegenNames.REF_ENUMCOLL, "isEmpty"))
            .blockReturn(ref("value"));
        block.declareVar(ObjectArrayEventBean.EPTYPE, "event", newInstance(ObjectArrayEventBean.EPTYPE, newArrayByLength(EPTypePremade.OBJECT.getEPType(), constant(numParameters)), typeMember))
            .assignArrayElement(EnumForgeCodegenNames.REF_EPS, constant(getStreamNumLambda()), ref("event"))
            .declareVar(EPTypePremade.OBJECTARRAY.getEPType(), "props", exprDotMethod(ref("event"), "getProperties"));
        if (numParameters > 3) {
            block.assignArrayElement("props", constant(3), exprDotMethod(EnumForgeCodegenNames.REF_ENUMCOLL, "size"));
        }
        if (numParameters > 2) {
            block.declareVar(EPTypePremade.INTEGERPRIMITIVE.getEPType(), "count", constant(-1));
        }

        CodegenBlock forEach = block.forEach(EPTypePremade.OBJECT.getEPType(), "next", EnumForgeCodegenNames.REF_ENUMCOLL)
            .assignArrayElement("props", constant(0), ref("value"))
            .assignArrayElement("props", constant(1), ref("next"));
        if (numParameters > 2) {
            forEach.incrementRef("count")
                .assignArrayElement("props", constant(2), ref("count"));
        }

        if (innerType == EPTypeNull.INSTANCE) {
            forEach.assignRef("value", constantNull());
        } else {
            forEach.assignRef("value", innerExpression.evaluateCodegen((EPTypeClass) innerType, methodNode, scope, codegenClassScope));
        }
        forEach.blockEnd();

        block.methodReturn(ref("value"));
        return localMethod(methodNode, premade.getEps(), premade.getEnumcoll(), premade.getIsNewData(), premade.getExprCtx());
    }
}
