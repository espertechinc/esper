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
package com.espertech.esper.codegen.model.blocks;

import com.espertech.esper.codegen.core.CodegenContext;
import com.espertech.esper.codegen.core.CodegenMember;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.model.method.CodegenParamSetExprPremade;
import com.espertech.esper.epl.expression.core.ExprEnumerationEval;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprTypableReturnEval;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.exprDotMethod;
import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.ref;

public class CodegenLegoEvaluateSelf {
    public static CodegenExpression evaluateSelfGetROCollectionEvents(ExprEnumerationEval provider, CodegenParamSetExprPremade params, CodegenContext context) {
        return call(provider, params, context, "evaluateGetROCollectionEvents");
    }

    public static CodegenExpression evaluateSelfGetROCollectionScalar(ExprEnumerationEval provider, CodegenParamSetExprPremade params, CodegenContext context) {
        return call(provider, params, context, "evaluateGetROCollectionScalar");
    }

    public static CodegenExpression evaluateSelfGetEventBean(ExprEnumerationEval provider, CodegenParamSetExprPremade params, CodegenContext context) {
        return call(provider, params, context, "evaluateGetEventBean");
    }

    private static CodegenExpression call(ExprEnumerationEval provider, CodegenParamSetExprPremade params, CodegenContext context, String methodName) {
        CodegenMember member = context.makeAddMember(ExprEnumerationEval.class, provider);
        return exprDotMethod(ref(member.getMemberName()), methodName, params.passEPS(), params.passIsNewData(), params.passEvalCtx());
    }

    public static CodegenExpression evaluateSelfPlainWithCast(ExprEvaluator provider, Class returnType, CodegenParamSetExprPremade params, CodegenContext context) {
        CodegenMember member = context.makeAddMember(ExprEvaluator.class, provider);
        return CodegenLegoCast.castSafeFromObjectType(returnType, exprDotMethod(ref(member.getMemberName()), "evaluate", params.passEPS(), params.passIsNewData(), params.passEvalCtx()));
    }

    public static CodegenExpression evaluateSelfTypableMulti(ExprTypableReturnEval typableEval, CodegenParamSetExprPremade params, CodegenContext context) {
        return evaluateSelfTypable(typableEval, params, context, "evaluateTypableMulti");
    }

    public static CodegenExpression evaluateSelfTypableSingle(ExprTypableReturnEval typableEval, CodegenParamSetExprPremade params, CodegenContext context) {
        return evaluateSelfTypable(typableEval, params, context, "evaluateTypableSingle");
    }

    private static CodegenExpression evaluateSelfTypable(ExprTypableReturnEval typableEval, CodegenParamSetExprPremade params, CodegenContext context, String methodName) {
        CodegenMember member = context.makeAddMember(ExprTypableReturnEval.class, typableEval);
        return exprDotMethod(ref(member.getMemberName()), methodName, params.passEPS(), params.passIsNewData(), params.passEvalCtx());
    }
}
