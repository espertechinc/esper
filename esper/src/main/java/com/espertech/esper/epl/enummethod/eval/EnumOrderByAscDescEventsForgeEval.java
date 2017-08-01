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
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.model.method.CodegenParamSetEnumMethodNonPremade;
import com.espertech.esper.codegen.model.method.CodegenParamSetEnumMethodPremade;
import com.espertech.esper.codegen.model.method.CodegenParamSetExprPremade;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.util.JavaClassHelper;

import java.util.*;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

public class EnumOrderByAscDescEventsForgeEval implements EnumEval {

    private final EnumOrderByAscDescEventsForge forge;
    private final ExprEvaluator innerExpression;

    public EnumOrderByAscDescEventsForgeEval(EnumOrderByAscDescEventsForge forge, ExprEvaluator innerExpression) {
        this.forge = forge;
        this.innerExpression = innerExpression;
    }

    public Object evaluateEnumMethod(EventBean[] eventsLambda, Collection enumcoll, boolean isNewData, ExprEvaluatorContext context) {
        TreeMap<Comparable, Object> sort = new TreeMap<>();
        boolean hasColl = false;

        Collection<EventBean> beans = (Collection<EventBean>) enumcoll;
        for (EventBean next : beans) {
            eventsLambda[forge.streamNumLambda] = next;

            Comparable comparable = (Comparable) innerExpression.evaluate(eventsLambda, isNewData, context);
            Object entry = sort.get(comparable);

            if (entry == null) {
                sort.put(comparable, next);
                continue;
            }

            if (entry instanceof Collection) {
                ((Collection) entry).add(next);
                continue;
            }

            Deque<Object> coll = new ArrayDeque<Object>(2);
            coll.add(entry);
            coll.add(next);
            sort.put(comparable, coll);
            hasColl = true;
        }

        return enumOrderBySortEval(sort, hasColl, forge.descending);
    }

    public static CodegenExpression codegen(EnumOrderByAscDescEventsForge forge, CodegenParamSetEnumMethodNonPremade args, CodegenContext context) {
        CodegenParamSetEnumMethodPremade premade = CodegenParamSetEnumMethodPremade.INSTANCE;
        Class innerBoxedType = JavaClassHelper.getBoxedType(forge.innerExpression.getEvaluationType());
        CodegenBlock block = context.addMethod(Collection.class, EnumOrderByAscDescEventsForgeEval.class).add(premade).begin()
                .declareVar(TreeMap.class, "sort", newInstance(TreeMap.class))
                .declareVar(boolean.class, "hasColl", constantFalse())
                .forEach(EventBean.class, "next", premade.enumcoll())
                .assignArrayElement(premade.eps(), constant(forge.streamNumLambda), ref("next"))
                .declareVar(innerBoxedType, "value", forge.innerExpression.evaluateCodegen(CodegenParamSetExprPremade.INSTANCE, context))
                .declareVar(Object.class, "entry", exprDotMethod(ref("sort"), "get", ref("value")))
                .ifCondition(equalsNull(ref("entry")))
                    .expression(exprDotMethod(ref("sort"), "put", ref("value"), ref("next")))
                    .blockContinue()
                .ifCondition(instanceOf(ref("entry"), Collection.class))
                    .exprDotMethod(cast(Collection.class, ref("entry")), "add", ref("next"))
                    .blockContinue()
                .declareVar(Deque.class, "coll", newInstance(ArrayDeque.class, constant(2)))
                .exprDotMethod(ref("coll"), "add", ref("entry"))
                .exprDotMethod(ref("coll"), "add", ref("next"))
                .exprDotMethod(ref("sort"), "put", ref("value"), ref("coll"))
                .assignRef("hasColl", constantTrue())
                .blockEnd();
        String method = block.methodReturn(staticMethod(EnumOrderByAscDescEventsForgeEval.class, "enumOrderBySortEval", ref("sort"), ref("hasColl"), constant(forge.descending)));
        return localMethodBuild(method).passAll(args).call();
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     * @param sort sorted
     * @param hasColl collection flag
     * @param descending true for descending
     * @return
     */
    public static Collection enumOrderBySortEval(TreeMap<Comparable, Object> sort, boolean hasColl, boolean descending) {
        Map<Comparable, Object> sorted;
        if (descending) {
            sorted = sort.descendingMap();
        } else {
            sorted = sort;
        }

        if (!hasColl) {
            return sorted.values();
        }

        Deque<Object> coll = new ArrayDeque<Object>();
        for (Map.Entry<Comparable, Object> entry : sorted.entrySet()) {
            if (entry.getValue() instanceof Collection) {
                coll.addAll((Collection) entry.getValue());
            } else {
                coll.add(entry.getValue());
            }
        }
        return coll;
    }
}
