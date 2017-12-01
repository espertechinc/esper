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
import com.espertech.esper.codegen.base.CodegenMethodScope;
import com.espertech.esper.epl.expression.codegen.CodegenLegoBooleanExpression;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.epl.enummethod.codegen.EnumForgeCodegenParams;
import com.espertech.esper.epl.enummethod.codegen.EnumForgeCodegenNames;
import com.espertech.esper.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.codegen.base.CodegenMethodNode;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.codegen.model.expression.CodegenExpressionRelational.CodegenRelational.GE;

public class EnumTakeWhileLastEventsForgeEval implements EnumEval {

    private final EnumTakeWhileLastEventsForge forge;
    private final ExprEvaluator innerExpression;

    public EnumTakeWhileLastEventsForgeEval(EnumTakeWhileLastEventsForge forge, ExprEvaluator innerExpression) {
        this.forge = forge;
        this.innerExpression = innerExpression;
    }

    public Object evaluateEnumMethod(EventBean[] eventsLambda, Collection enumcoll, boolean isNewData, ExprEvaluatorContext context) {
        if (enumcoll.isEmpty()) {
            return enumcoll;
        }

        Collection<EventBean> beans = (Collection<EventBean>) enumcoll;
        if (enumcoll.size() == 1) {
            EventBean item = beans.iterator().next();
            eventsLambda[forge.streamNumLambda] = item;

            Object pass = innerExpression.evaluate(eventsLambda, isNewData, context);
            if (pass == null || (!(Boolean) pass)) {
                return Collections.emptyList();
            }
            return Collections.singletonList(item);
        }

        EventBean[] all = takeWhileLastEventBeanToArray(enumcoll);
        ArrayDeque<Object> result = new ArrayDeque<Object>();

        for (int i = all.length - 1; i >= 0; i--) {
            eventsLambda[forge.streamNumLambda] = all[i];

            Object pass = innerExpression.evaluate(eventsLambda, isNewData, context);
            if (pass == null || (!(Boolean) pass)) {
                break;
            }

            result.addFirst(all[i]);
        }

        return result;
    }

    public static CodegenExpression codegen(EnumTakeWhileLastEventsForge forge, EnumForgeCodegenParams args, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        ExprForgeCodegenSymbol scope = new ExprForgeCodegenSymbol(false, null);
        CodegenMethodNode methodNode = codegenMethodScope.makeChildWithScope(Collection.class, EnumTakeWhileLastEventsForgeEval.class, scope, codegenClassScope).addParam(EnumForgeCodegenNames.PARAMS);
        CodegenExpression innerValue = forge.innerExpression.evaluateCodegen(Boolean.class, methodNode, scope, codegenClassScope);
        CodegenBlock block = methodNode.getBlock()
                .ifCondition(exprDotMethod(EnumForgeCodegenNames.REF_ENUMCOLL, "isEmpty"))
                .blockReturn(EnumForgeCodegenNames.REF_ENUMCOLL);

        CodegenBlock blockSingle = block.ifCondition(equalsIdentity(exprDotMethod(EnumForgeCodegenNames.REF_ENUMCOLL, "size"), constant(1)))
                .declareVar(EventBean.class, "item", cast(EventBean.class, exprDotMethodChain(EnumForgeCodegenNames.REF_ENUMCOLL).add("iterator").add("next")))
                .assignArrayElement(EnumForgeCodegenNames.REF_EPS, constant(forge.streamNumLambda), ref("item"));
        CodegenLegoBooleanExpression.codegenReturnValueIfNullOrNotPass(blockSingle, forge.innerExpression.getEvaluationType(), innerValue, staticMethod(Collections.class, "emptyList"));
        blockSingle.blockReturn(staticMethod(Collections.class, "singletonList", ref("item")));

        block.declareVar(ArrayDeque.class, "result", newInstance(ArrayDeque.class))
                .declareVar(EventBean[].class, "all", staticMethod(EnumTakeWhileLastEventsForgeEval.class, "takeWhileLastEventBeanToArray", EnumForgeCodegenNames.REF_ENUMCOLL));

        CodegenBlock forEach = block.forLoop(int.class, "i", op(arrayLength(ref("all")), "-", constant(1)), relational(ref("i"), GE, constant(0)), decrement("i"))
                .assignArrayElement(EnumForgeCodegenNames.REF_EPS, constant(forge.streamNumLambda), arrayAtIndex(ref("all"), ref("i")));
        CodegenLegoBooleanExpression.codegenBreakIfNullOrNotPass(forEach, forge.innerExpression.getEvaluationType(), innerValue);
        forEach.expression(exprDotMethod(ref("result"), "addFirst", arrayAtIndex(ref("all"), ref("i"))));
        block.methodReturn(ref("result"));
        return localMethod(methodNode, args.getEps(), args.getEnumcoll(), args.getIsNewData(), args.getExprCtx());
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     * @param enumcoll events
     * @return array
     */
    public static EventBean[] takeWhileLastEventBeanToArray(Collection<EventBean> enumcoll) {
        int size = enumcoll.size();
        EventBean[] all = new EventBean[size];
        int count = 0;
        for (EventBean item : enumcoll) {
            all[count++] = item;
        }
        return all;
    }
}
