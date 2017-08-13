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
import com.espertech.esper.codegen.core.CodegenMethodId;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.model.method.CodegenParamSetEnumMethodNonPremade;
import com.espertech.esper.codegen.model.method.CodegenParamSetEnumMethodPremade;
import com.espertech.esper.codegen.model.method.CodegenParamSetExprPremade;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.util.SimpleNumberCoercerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.localMethodBuild;
import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.ref;
import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.staticMethod;

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

    public static CodegenExpression codegen(EnumTakeForge forge, CodegenParamSetEnumMethodNonPremade args, CodegenContext context) {
        CodegenParamSetEnumMethodPremade premade = CodegenParamSetEnumMethodPremade.INSTANCE;
        Class sizeType = forge.sizeEval.getEvaluationType();
        CodegenBlock block = context.addMethod(Collection.class, EnumTakeForgeEval.class).add(premade).begin()
                .declareVar(sizeType, "size", forge.sizeEval.evaluateCodegen(CodegenParamSetExprPremade.INSTANCE, context));
        if (!sizeType.isPrimitive()) {
            block.ifRefNullReturnNull("size");
        }
        CodegenMethodId method = block.methodReturn(staticMethod(EnumTakeForgeEval.class, "evaluateEnumTakeMethod", premade.enumcoll(), SimpleNumberCoercerFactory.SimpleNumberCoercerInt.codegenInt(ref("size"), sizeType)));
        return localMethodBuild(method).passAll(args).call();
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
