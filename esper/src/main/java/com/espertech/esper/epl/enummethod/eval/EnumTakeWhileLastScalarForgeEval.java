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
import java.util.Collections;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.codegen.model.expression.CodegenExpressionRelational.CodegenRelational.GE;
import static com.espertech.esper.epl.enummethod.eval.EnumTakeWhileLastIndexScalarForgeEval.METHOD_TAKEWHILELASTSCALARTOARRAY;
import static com.espertech.esper.epl.enummethod.eval.EnumTakeWhileLastIndexScalarForgeEval.takeWhileLastScalarToArray;

public class EnumTakeWhileLastScalarForgeEval implements EnumEval {

    private final EnumTakeWhileLastScalarForge forge;
    private final ExprEvaluator innerExpression;

    public EnumTakeWhileLastScalarForgeEval(EnumTakeWhileLastScalarForge forge, ExprEvaluator innerExpression) {
        this.forge = forge;
        this.innerExpression = innerExpression;
    }

    public Object evaluateEnumMethod(EventBean[] eventsLambda, Collection enumcoll, boolean isNewData, ExprEvaluatorContext context) {
        if (enumcoll.isEmpty()) {
            return enumcoll;
        }

        ObjectArrayEventBean evalEvent = new ObjectArrayEventBean(new Object[1], forge.type);
        eventsLambda[forge.streamNumLambda] = evalEvent;
        Object[] props = evalEvent.getProperties();

        if (enumcoll.size() == 1) {
            Object item = enumcoll.iterator().next();
            props[0] = item;

            Object pass = innerExpression.evaluate(eventsLambda, isNewData, context);
            if (pass == null || (!(Boolean) pass)) {
                return Collections.emptyList();
            }
            return Collections.singletonList(item);
        }

        Object[] all = takeWhileLastScalarToArray(enumcoll);
        ArrayDeque<Object> result = new ArrayDeque<Object>();

        for (int i = all.length - 1; i >= 0; i--) {
            props[0] = all[i];

            Object pass = innerExpression.evaluate(eventsLambda, isNewData, context);
            if (pass == null || (!(Boolean) pass)) {
                break;
            }

            result.addFirst(all[i]);
        }

        return result;
    }

    public static CodegenExpression codegen(EnumTakeWhileLastScalarForge forge, EnumForgeCodegenParams args, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        CodegenMember typeMember = codegenClassScope.makeAddMember(ObjectArrayEventType.class, forge.type);

        ExprForgeCodegenSymbol scope = new ExprForgeCodegenSymbol(false, null);
        CodegenMethodNode methodNode = codegenMethodScope.makeChildWithScope(Collection.class, EnumTakeWhileLastScalarForgeEval.class, scope, codegenClassScope).addParam(EnumForgeCodegenNames.PARAMS);

        CodegenExpression innerValue = forge.innerExpression.evaluateCodegen(Boolean.class, methodNode, scope, codegenClassScope);
        CodegenBlock block = methodNode.getBlock()
                .ifCondition(exprDotMethod(EnumForgeCodegenNames.REF_ENUMCOLL, "isEmpty"))
                .blockReturn(EnumForgeCodegenNames.REF_ENUMCOLL);
        block.declareVar(ObjectArrayEventBean.class, "evalEvent", newInstance(ObjectArrayEventBean.class, newArrayByLength(Object.class, constant(1)), member(typeMember.getMemberId())))
                .assignArrayElement(EnumForgeCodegenNames.REF_EPS, constant(forge.streamNumLambda), ref("evalEvent"))
                .declareVar(Object[].class, "props", exprDotMethod(ref("evalEvent"), "getProperties"));

        CodegenBlock blockSingle = block.ifCondition(equalsIdentity(exprDotMethod(EnumForgeCodegenNames.REF_ENUMCOLL, "size"), constant(1)))
                .declareVar(Object.class, "item", exprDotMethodChain(EnumForgeCodegenNames.REF_ENUMCOLL).add("iterator").add("next"))
                .assignArrayElement("props", constant(0), ref("item"));
        CodegenLegoBooleanExpression.codegenReturnValueIfNullOrNotPass(blockSingle, forge.innerExpression.getEvaluationType(), innerValue, staticMethod(Collections.class, "emptyList"));
        blockSingle.blockReturn(staticMethod(Collections.class, "singletonList", ref("item")));

        block.declareVar(ArrayDeque.class, "result", newInstance(ArrayDeque.class))
                .declareVar(Object[].class, "all", staticMethod(EnumTakeWhileLastIndexScalarForgeEval.class, METHOD_TAKEWHILELASTSCALARTOARRAY, EnumForgeCodegenNames.REF_ENUMCOLL));

        CodegenBlock forEach = block.forLoop(int.class, "i", op(arrayLength(ref("all")), "-", constant(1)), relational(ref("i"), GE, constant(0)), decrement("i"))
                .assignArrayElement("props", constant(0), arrayAtIndex(ref("all"), ref("i")));
        CodegenLegoBooleanExpression.codegenBreakIfNullOrNotPass(forEach, forge.innerExpression.getEvaluationType(), innerValue);
        forEach.expression(exprDotMethod(ref("result"), "addFirst", arrayAtIndex(ref("all"), ref("i"))));
        block.methodReturn(ref("result"));
        return localMethod(methodNode, args.getEps(), args.getEnumcoll(), args.getIsNewData(), args.getExprCtx());
    }
}
