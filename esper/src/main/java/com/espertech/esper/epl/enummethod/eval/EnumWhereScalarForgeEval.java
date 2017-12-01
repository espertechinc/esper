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
package com.espertech.esper.epl.enummethod.eval;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.codegen.base.CodegenBlock;
import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMember;
import com.espertech.esper.codegen.base.CodegenMethodScope;
import com.espertech.esper.epl.expression.codegen.CodegenLegoBooleanExpression;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder;
import com.espertech.esper.epl.enummethod.codegen.EnumForgeCodegenParams;
import com.espertech.esper.epl.enummethod.codegen.EnumForgeCodegenNames;
import com.espertech.esper.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.codegen.base.CodegenMethodNode;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.event.arr.ObjectArrayEventBean;
import com.espertech.esper.event.arr.ObjectArrayEventType;

import java.util.ArrayDeque;
import java.util.Collection;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

public class EnumWhereScalarForgeEval implements EnumEval {

    private final EnumWhereScalarForge forge;
    private final ExprEvaluator innerExpression;

    public EnumWhereScalarForgeEval(EnumWhereScalarForge forge, ExprEvaluator innerExpression) {
        this.forge = forge;
        this.innerExpression = innerExpression;
    }

    public Object evaluateEnumMethod(EventBean[] eventsLambda, Collection enumcoll, boolean isNewData, ExprEvaluatorContext context) {
        if (enumcoll.isEmpty()) {
            return enumcoll;
        }

        ArrayDeque<Object> result = new ArrayDeque<Object>();
        ObjectArrayEventBean evalEvent = new ObjectArrayEventBean(new Object[1], forge.type);
        eventsLambda[forge.streamNumLambda] = evalEvent;
        Object[] props = evalEvent.getProperties();

        for (Object next : enumcoll) {

            props[0] = next;

            Object pass = innerExpression.evaluate(eventsLambda, isNewData, context);
            if (pass == null || (!(Boolean) pass)) {
                continue;
            }

            result.add(next);
        }

        return result;
    }

    public static CodegenExpression codegen(EnumWhereScalarForge forge, EnumForgeCodegenParams args, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        CodegenMember typeMember = codegenClassScope.makeAddMember(ObjectArrayEventType.class, forge.type);
        ExprForgeCodegenSymbol scope = new ExprForgeCodegenSymbol(false, null);
        CodegenMethodNode methodNode = codegenMethodScope.makeChildWithScope(Collection.class, EnumWhereScalarForgeEval.class, scope, codegenClassScope).addParam(EnumForgeCodegenNames.PARAMS);

        CodegenBlock block = methodNode.getBlock()
                .ifCondition(exprDotMethod(EnumForgeCodegenNames.REF_ENUMCOLL, "isEmpty"))
                .blockReturn(EnumForgeCodegenNames.REF_ENUMCOLL);
        block.declareVar(ArrayDeque.class, "result", newInstance(ArrayDeque.class))
                .declareVar(ObjectArrayEventBean.class, "evalEvent", newInstance(ObjectArrayEventBean.class, newArrayByLength(Object.class, constant(1)), CodegenExpressionBuilder.member(typeMember.getMemberId())))
                .assignArrayElement(EnumForgeCodegenNames.REF_EPS, constant(forge.streamNumLambda), ref("evalEvent"))
                .declareVar(Object[].class, "props", exprDotMethod(ref("evalEvent"), "getProperties"));
        CodegenBlock forEach = block.forEach(Object.class, "next", EnumForgeCodegenNames.REF_ENUMCOLL)
                .assignArrayElement("props", constant(0), ref("next"));
        CodegenLegoBooleanExpression.codegenContinueIfNullOrNotPass(forEach, forge.innerExpression.getEvaluationType(), forge.innerExpression.evaluateCodegen(Boolean.class, methodNode, scope, codegenClassScope));
        forEach.expression(exprDotMethod(ref("result"), "add", ref("next")));
        block.methodReturn(ref("result"));
        return localMethod(methodNode, args.getEps(), args.getEnumcoll(), args.getIsNewData(), args.getExprCtx());
    }
}
