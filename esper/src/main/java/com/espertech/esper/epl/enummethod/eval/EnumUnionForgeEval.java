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
import com.espertech.esper.epl.expression.core.ExprEnumerationEval;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;

import java.util.ArrayList;
import java.util.Collection;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

public class EnumUnionForgeEval implements EnumEval {

    private final EnumUnionForge forge;
    private final ExprEnumerationEval evaluator;

    public EnumUnionForgeEval(EnumUnionForge forge, ExprEnumerationEval evaluator) {
        this.forge = forge;
        this.evaluator = evaluator;
    }

    public Object evaluateEnumMethod(EventBean[] eventsLambda, Collection enumcoll, boolean isNewData, ExprEvaluatorContext context) {
        Collection other;
        if (forge.scalar) {
            other = evaluator.evaluateGetROCollectionScalar(eventsLambda, isNewData, context);
        } else {
            other = evaluator.evaluateGetROCollectionEvents(eventsLambda, isNewData, context);
        }

        if (other == null || other.isEmpty()) {
            return enumcoll;
        }

        ArrayList<Object> result = new ArrayList<Object>(enumcoll.size() + other.size());
        result.addAll(enumcoll);
        result.addAll(other);

        return result;
    }

    public static CodegenExpression codegen(EnumUnionForge forge, CodegenParamSetEnumMethodNonPremade args, CodegenContext context) {
        CodegenParamSetEnumMethodPremade premade = CodegenParamSetEnumMethodPremade.INSTANCE;
        CodegenBlock block = context.addMethod(Collection.class, EnumUnionForgeEval.class).add(premade).begin();
        if (forge.scalar) {
            block.declareVar(Collection.class, "other", forge.evaluatorForge.evaluateGetROCollectionScalarCodegen(CodegenParamSetExprPremade.INSTANCE, context));
        } else {
            block.declareVar(Collection.class, "other", forge.evaluatorForge.evaluateGetROCollectionEventsCodegen(CodegenParamSetExprPremade.INSTANCE, context));
        }
        String method = block.ifCondition(or(equalsNull(ref("other")), exprDotMethod(ref("other"), "isEmpty")))
                .blockReturn(premade.enumcoll())
                .declareVar(ArrayList.class, "result", newInstance(ArrayList.class, op(exprDotMethod(premade.enumcoll(), "size"), "+", exprDotMethod(ref("other"), "size"))))
                .expression(exprDotMethod(ref("result"), "addAll", premade.enumcoll()))
                .expression(exprDotMethod(ref("result"), "addAll", ref("other")))
                .methodReturn(ref("result"));
        return localMethodBuild(method).passAll(args).call();
    }
}
