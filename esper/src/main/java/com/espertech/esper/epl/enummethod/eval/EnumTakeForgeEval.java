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
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.epl.enummethod.codegen.EnumForgeCodegenParams;
import com.espertech.esper.epl.enummethod.codegen.EnumForgeCodegenNames;
import com.espertech.esper.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.codegen.base.CodegenMethodNode;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.util.SimpleNumberCoercerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

public class EnumTakeForgeEval implements EnumEval {

    private final ExprEvaluator sizeEval;

    public EnumTakeForgeEval(ExprEvaluator sizeEval) {
        this.sizeEval = sizeEval;
    }

    public Object evaluateEnumMethod(EventBean[] eventsLambda, Collection enumcoll, boolean isNewData, ExprEvaluatorContext context) {

        Object size = sizeEval.evaluate(eventsLambda, isNewData, context);
        if (size == null) {
            return null;
        }
        return evaluateEnumTakeMethod(enumcoll, ((Number) size).intValue());
    }

    public static CodegenExpression codegen(EnumTakeForge forge, EnumForgeCodegenParams args, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        ExprForgeCodegenSymbol scope = new ExprForgeCodegenSymbol(false, null);
        CodegenMethodNode methodNode = codegenMethodScope.makeChildWithScope(Collection.class, EnumTakeForgeEval.class, scope, codegenClassScope).addParam(EnumForgeCodegenNames.PARAMS);

        Class sizeType = forge.sizeEval.getEvaluationType();
        CodegenBlock block = methodNode.getBlock().declareVar(sizeType, "size", forge.sizeEval.evaluateCodegen(sizeType, methodNode, scope, codegenClassScope));
        if (!sizeType.isPrimitive()) {
            block.ifRefNullReturnNull("size");
        }
        block.methodReturn(staticMethod(EnumTakeForgeEval.class, "evaluateEnumTakeMethod", EnumForgeCodegenNames.REF_ENUMCOLL, SimpleNumberCoercerFactory.SimpleNumberCoercerInt.codegenInt(ref("size"), sizeType)));
        return localMethod(methodNode, args.getEps(), args.getEnumcoll(), args.getIsNewData(), args.getExprCtx());
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     * @param enumcoll collection
     * @param size size
     * @return collection
     */
    public static Collection evaluateEnumTakeMethod(Collection enumcoll, int size) {

        if (enumcoll.isEmpty()) {
            return enumcoll;
        }

        if (size <= 0) {
            return Collections.emptyList();
        }

        if (enumcoll.size() < size) {
            return enumcoll;
        }

        if (size == 1) {
            return Collections.singletonList(enumcoll.iterator().next());
        }

        ArrayList<Object> result = new ArrayList<Object>(size);
        for (Object next : enumcoll) {
            if (result.size() >= size) {
                break;
            }
            result.add(next);
        }
        return result;
    }
}
