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

public class EnumTakeLastForgeEval implements EnumEval {

    private ExprEvaluator sizeEval;

    public EnumTakeLastForgeEval(ExprEvaluator sizeEval) {
        this.sizeEval = sizeEval;
    }

    public Object evaluateEnumMethod(EventBean[] eventsLambda, Collection enumcoll, boolean isNewData, ExprEvaluatorContext context) {

        Object sizeObj = sizeEval.evaluate(eventsLambda, isNewData, context);
        if (sizeObj == null) {
            return null;
        }
        return evaluateEnumMethodTakeLast(enumcoll, ((Number) sizeObj).intValue());
    }

    public static CodegenExpression codegen(EnumTakeLastForge forge, EnumForgeCodegenParams args, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        Class sizeType = forge.sizeEval.getEvaluationType();

        ExprForgeCodegenSymbol scope = new ExprForgeCodegenSymbol(false, null);
        CodegenMethodNode methodNode = codegenMethodScope.makeChildWithScope(Collection.class, EnumTakeLastForgeEval.class, scope, codegenClassScope).addParam(EnumForgeCodegenNames.PARAMS);

        CodegenBlock block = methodNode.getBlock().declareVar(sizeType, "size", forge.sizeEval.evaluateCodegen(sizeType, methodNode, scope, codegenClassScope));
        if (!sizeType.isPrimitive()) {
            block.ifRefNullReturnNull("size");
        }
        block.methodReturn(staticMethod(EnumTakeLastForgeEval.class, "evaluateEnumMethodTakeLast", EnumForgeCodegenNames.REF_ENUMCOLL, SimpleNumberCoercerFactory.SimpleNumberCoercerInt.codegenInt(ref("size"), sizeType)));
        return localMethod(methodNode, args.getEps(), args.getEnumcoll(), args.getIsNewData(), args.getExprCtx());
    }

    public static Collection evaluateEnumMethodTakeLast(Collection enumcoll, int size) {

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
            Object last = null;
            for (Object next : enumcoll) {
                last = next;
            }
            return Collections.singletonList(last);
        }

        ArrayList<Object> result = new ArrayList<>();
        for (Object next : enumcoll) {
            result.add(next);
            if (result.size() > size) {
                result.remove(0);
            }
        }
        return result;
    }
}
