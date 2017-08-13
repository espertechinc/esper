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
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.model.method.CodegenParamSetEnumMethodNonPremade;
import com.espertech.esper.codegen.model.method.CodegenParamSetEnumMethodPremade;
import com.espertech.esper.codegen.model.method.CodegenParamSetExprPremade;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.event.arr.ObjectArrayEventBean;
import com.espertech.esper.event.arr.ObjectArrayEventType;
import com.espertech.esper.util.JavaClassHelper;

import java.util.*;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.localMethodBuild;

public class EnumOrderByAscDescScalarLambdaForgeEval implements EnumEval {

    private final EnumOrderByAscDescScalarLambdaForge forge;
    private final ExprEvaluator innerExpression;

    public EnumOrderByAscDescScalarLambdaForgeEval(EnumOrderByAscDescScalarLambdaForge forge, ExprEvaluator innerExpression) {
        this.forge = forge;
        this.innerExpression = innerExpression;
    }

    public Object evaluateEnumMethod(EventBean[] eventsLambda, Collection enumcoll, boolean isNewData, ExprEvaluatorContext context) {
        TreeMap<Comparable, Object> sort = new TreeMap<Comparable, Object>();
        boolean hasColl = false;

        ObjectArrayEventBean resultEvent = new ObjectArrayEventBean(new Object[1], forge.resultEventType);
        eventsLambda[forge.streamNumLambda] = resultEvent;
        Object[] props = resultEvent.getProperties();

        Collection<Object> values = (Collection<Object>) enumcoll;
        for (Object next : values) {

            props[0] = next;

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

            Deque<Object> coll = new ArrayDeque<Object>();
            coll.add(entry);
            coll.add(next);
            sort.put(comparable, coll);
            hasColl = true;
        }

        return EnumOrderByAscDescEventsForgeEval.enumOrderBySortEval(sort, hasColl, forge.descending);
    }

    public static CodegenExpression codegen(EnumOrderByAscDescScalarLambdaForge forge, CodegenParamSetEnumMethodNonPremade args, CodegenContext context) {
        CodegenMember resultTypeMember = context.makeAddMember(ObjectArrayEventType.class, forge.resultEventType);
        CodegenParamSetEnumMethodPremade premade = CodegenParamSetEnumMethodPremade.INSTANCE;
        Class innerBoxedType = JavaClassHelper.getBoxedType(forge.innerExpression.getEvaluationType());

        CodegenBlock block = context.addMethod(Collection.class, EnumOrderByAscDescScalarLambdaForgeEval.class).add(premade).begin()
                .declareVar(TreeMap.class, "sort", newInstance(TreeMap.class))
                .declareVar(boolean.class, "hasColl", constantFalse())
                .declareVar(ObjectArrayEventBean.class, "resultEvent", newInstance(ObjectArrayEventBean.class, newArray(Object.class, constant(1)), member(resultTypeMember.getMemberId())))
                .assignArrayElement(premade.eps(), constant(forge.streamNumLambda), ref("resultEvent"))
                .declareVar(Object[].class, "props", exprDotMethod(ref("resultEvent"), "getProperties"))

                .forEach(Object.class, "next", premade.enumcoll())
                .assignArrayElement("props", constant(0), ref("next"))
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
        CodegenMethodId method = block.methodReturn(staticMethod(EnumOrderByAscDescEventsForgeEval.class, "enumOrderBySortEval", ref("sort"), ref("hasColl"), constant(forge.descending)));
        return localMethodBuild(method).passAll(args).call();
    }
}
