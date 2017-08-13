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
import com.espertech.esper.codegen.core.CodegenBlock;
import com.espertech.esper.codegen.core.CodegenContext;
import com.espertech.esper.codegen.core.CodegenMember;
import com.espertech.esper.codegen.core.CodegenMethodId;
import com.espertech.esper.codegen.model.blocks.CodegenLegoBooleanExpression;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.model.method.CodegenParamSetEnumMethodNonPremade;
import com.espertech.esper.codegen.model.method.CodegenParamSetEnumMethodPremade;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.event.arr.ObjectArrayEventBean;
import com.espertech.esper.event.arr.ObjectArrayEventType;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.codegen.model.expression.CodegenExpressionRelational.CodegenRelational.GE;

public class EnumTakeWhileLastIndexScalarForgeEval implements EnumEval {

    private final EnumTakeWhileLastIndexScalarForge forge;
    private final ExprEvaluator innerExpression;

    public EnumTakeWhileLastIndexScalarForgeEval(EnumTakeWhileLastIndexScalarForge forge, ExprEvaluator innerExpression) {
        this.forge = forge;
        this.innerExpression = innerExpression;
    }

    public Object evaluateEnumMethod(EventBean[] eventsLambda, Collection enumcoll, boolean isNewData, ExprEvaluatorContext context) {
        if (enumcoll.isEmpty()) {
            return enumcoll;
        }

        ObjectArrayEventBean evalEvent = new ObjectArrayEventBean(new Object[1], forge.evalEventType);
        eventsLambda[forge.streamNumLambda] = evalEvent;
        Object[] evalProps = evalEvent.getProperties();
        ObjectArrayEventBean indexEvent = new ObjectArrayEventBean(new Object[1], forge.indexEventType);
        eventsLambda[forge.streamNumLambda + 1] = indexEvent;
        Object[] indexProps = indexEvent.getProperties();

        if (enumcoll.size() == 1) {
            Object item = enumcoll.iterator().next();
            evalProps[0] = item;
            indexProps[0] = 0;

            Object pass = innerExpression.evaluate(eventsLambda, isNewData, context);
            if (pass == null || (!(Boolean) pass)) {
                return Collections.emptyList();
            }
            return Collections.singletonList(item);
        }

        Object[] all = takeWhileLastScalarToArray(enumcoll);
        ArrayDeque<Object> result = new ArrayDeque<Object>();
        int index = 0;

        for (int i = all.length - 1; i >= 0; i--) {

            evalProps[0] = all[i];
            indexProps[0] = index++;

            Object pass = innerExpression.evaluate(eventsLambda, isNewData, context);
            if (pass == null || (!(Boolean) pass)) {
                break;
            }
            result.addFirst(all[i]);
        }

        return result;
    }

    public static CodegenExpression codegen(EnumTakeWhileLastIndexScalarForge forge, CodegenParamSetEnumMethodNonPremade args, CodegenContext context) {
        CodegenMember indexTypeMember = context.makeAddMember(ObjectArrayEventType.class, forge.indexEventType);
        CodegenMember evalTypeMember = context.makeAddMember(ObjectArrayEventType.class, forge.evalEventType);
        CodegenParamSetEnumMethodPremade premade = CodegenParamSetEnumMethodPremade.INSTANCE;
        CodegenBlock block = context.addMethod(Collection.class, EnumTakeWhileLastIndexScalarForgeEval.class).add(premade).begin()
                .ifCondition(exprDotMethod(premade.enumcoll(), "isEmpty"))
                .blockReturn(premade.enumcoll())
                .declareVar(ObjectArrayEventBean.class, "evalEvent", newInstance(ObjectArrayEventBean.class, newArray(Object.class, constant(1)), member(evalTypeMember.getMemberId())))
                .assignArrayElement(premade.eps(), constant(forge.streamNumLambda), ref("evalEvent"))
                .declareVar(Object[].class, "evalProps", exprDotMethod(ref("evalEvent"), "getProperties"))
                .declareVar(ObjectArrayEventBean.class, "indexEvent", newInstance(ObjectArrayEventBean.class, newArray(Object.class, constant(1)), member(indexTypeMember.getMemberId())))
                .assignArrayElement(premade.eps(), constant(forge.streamNumLambda + 1), ref("indexEvent"))
                .declareVar(Object[].class, "indexProps", exprDotMethod(ref("indexEvent"), "getProperties"));

        CodegenBlock blockSingle = block.ifCondition(equalsIdentity(exprDotMethod(premade.enumcoll(), "size"), constant(1)))
                .declareVar(Object.class, "item", exprDotMethodChain(premade.enumcoll()).add("iterator").add("next"))
                .assignArrayElement("evalProps", constant(0), ref("item"))
                .assignArrayElement("indexProps", constant(0), constant(0));
        CodegenLegoBooleanExpression.codegenReturnValueIfNullOrNotPass(blockSingle, forge.innerExpression, context, staticMethod(Collections.class, "emptyList"));
        blockSingle.blockReturn(staticMethod(Collections.class, "singletonList", ref("item")));

        block.declareVar(ArrayDeque.class, "result", newInstance(ArrayDeque.class))
                .declareVar(Object[].class, "all", staticMethod(EnumTakeWhileLastIndexScalarForgeEval.class, "takeWhileLastScalarToArray", premade.enumcoll()))
                .declareVar(int.class, "index", constant(0));
        CodegenBlock forEach = block.forLoop(int.class, "i", op(arrayLength(ref("all")), "-", constant(1)), relational(ref("i"), GE, constant(0)), decrement("i"))
                .assignArrayElement("evalProps", constant(0), arrayAtIndex(ref("all"), ref("i")))
                .assignArrayElement("indexProps", constant(0), increment("index"));
        CodegenLegoBooleanExpression.codegenBreakIfNullOrNotPass(forEach, forge.innerExpression, context);
        forEach.expression(exprDotMethod(ref("result"), "addFirst", arrayAtIndex(ref("all"), ref("i"))));
        CodegenMethodId method = block.methodReturn(ref("result"));
        return localMethodBuild(method).passAll(args).call();
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     * @param enumcoll coll
     * @return array
     */
    public static Object[] takeWhileLastScalarToArray(Collection enumcoll) {
        int size = enumcoll.size();
        Object[] all = new Object[size];
        int count = 0;
        for (Object item : enumcoll) {
            all[count++] = item;
        }
        return all;
    }
}
