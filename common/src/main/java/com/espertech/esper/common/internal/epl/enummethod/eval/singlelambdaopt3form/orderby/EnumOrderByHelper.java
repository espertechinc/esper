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
package com.espertech.esper.common.internal.epl.enummethod.eval.singlelambdaopt3form.orderby;

import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.client.type.EPTypePremade;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenBlock;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprForge;

import java.util.*;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class EnumOrderByHelper {

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param sort       sorted
     * @param hasColl    collection flag
     * @param descending true for descending
     * @return collection
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

    public static void sortingCode(CodegenBlock block, EPTypeClass innerBoxedType, ExprForge innerExpression, CodegenMethod methodNode, ExprForgeCodegenSymbol scope, CodegenClassScope codegenClassScope) {
        block.declareVar(innerBoxedType, "value", innerExpression.evaluateCodegen(innerBoxedType, methodNode, scope, codegenClassScope))
            .declareVar(EPTypePremade.OBJECT.getEPType(), "entry", exprDotMethod(ref("sort"), "get", ref("value")))
            .ifCondition(equalsNull(ref("entry")))
            .expression(exprDotMethod(ref("sort"), "put", ref("value"), ref("next")))
            .blockContinue()
            .ifCondition(instanceOf(ref("entry"), EPTypePremade.COLLECTION.getEPType()))
            .exprDotMethod(cast(EPTypePremade.COLLECTION.getEPType(), ref("entry")), "add", ref("next"))
            .blockContinue()
            .declareVar(EPTypePremade.DEQUE.getEPType(), "coll", newInstance(EPTypePremade.ARRAYDEQUE.getEPType(), constant(2)))
            .exprDotMethod(ref("coll"), "add", ref("entry"))
            .exprDotMethod(ref("coll"), "add", ref("next"))
            .exprDotMethod(ref("sort"), "put", ref("value"), ref("coll"))
            .assignRef("hasColl", constantTrue())
            .blockEnd();
    }
}
