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
package com.espertech.esper.epl.expression.codegen;

import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMember;
import com.espertech.esper.codegen.base.CodegenMethodScope;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder;
import com.espertech.esper.codegen.model.expression.CodegenExpressionRef;
import com.espertech.esper.epl.expression.core.ExprEnumerationEval;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprTypableReturnEval;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.exprDotMethod;

public class CodegenLegoEvaluateSelf {
    public static CodegenExpression evaluateSelfGetROCollectionEvents(ExprEnumerationEval provider, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return call(provider, codegenMethodScope, exprSymbol, codegenClassScope, "evaluateGetROCollectionEvents");
    }

    public static CodegenExpression evaluateSelfGetROCollectionScalar(ExprEnumerationEval provider, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return call(provider, codegenMethodScope, exprSymbol, codegenClassScope, "evaluateGetROCollectionScalar");
    }

    public static CodegenExpression evaluateSelfGetEventBean(ExprEnumerationEval provider, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return call(provider, codegenMethodScope, exprSymbol, codegenClassScope, "evaluateGetEventBean");
    }

    private static CodegenExpression call(ExprEnumerationEval provider, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope, String methodName) {
        CodegenExpressionRef refEPS = exprSymbol.getAddEPS(codegenMethodScope);
        CodegenExpression refIsNewData = exprSymbol.getAddIsNewData(codegenMethodScope);
        CodegenExpressionRef refExprEvalCtx = exprSymbol.getAddExprEvalCtx(codegenMethodScope);
        CodegenMember member = codegenClassScope.makeAddMember(ExprEnumerationEval.class, provider);
        return exprDotMethod(CodegenExpressionBuilder.member(member.getMemberId()), methodName, refEPS, refIsNewData, refExprEvalCtx);
    }

    public static CodegenExpression evaluateSelfPlainWithCast(Class requiredType, ExprEvaluator provider, Class returnType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        CodegenExpressionRef refEPS = exprSymbol.getAddEPS(codegenMethodScope);
        CodegenExpression refIsNewData = exprSymbol.getAddIsNewData(codegenMethodScope);
        CodegenExpressionRef refExprEvalCtx = exprSymbol.getAddExprEvalCtx(codegenMethodScope);
        CodegenMember member = codegenClassScope.makeAddMember(ExprEvaluator.class, provider);
        CodegenExpression eval = exprDotMethod(CodegenExpressionBuilder.member(member.getMemberId()), "evaluate", refEPS, refIsNewData, refExprEvalCtx);
        if (requiredType == Object.class) {
            return eval;
        }
        return CodegenLegoCast.castSafeFromObjectType(returnType, eval);
    }

    public static CodegenExpression evaluateSelfTypableMulti(ExprTypableReturnEval typableEval, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return evaluateSelfTypable(typableEval, codegenMethodScope, exprSymbol, codegenClassScope, "evaluateTypableMulti");
    }

    public static CodegenExpression evaluateSelfTypableSingle(ExprTypableReturnEval typableEval, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return evaluateSelfTypable(typableEval, codegenMethodScope, exprSymbol, codegenClassScope, "evaluateTypableSingle");
    }

    private static CodegenExpression evaluateSelfTypable(ExprTypableReturnEval typableEval, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope, String methodName) {
        CodegenExpressionRef refEPS = exprSymbol.getAddEPS(codegenMethodScope);
        CodegenExpression refIsNewData = exprSymbol.getAddIsNewData(codegenMethodScope);
        CodegenExpressionRef refExprEvalCtx = exprSymbol.getAddExprEvalCtx(codegenMethodScope);
        CodegenMember member = codegenClassScope.makeAddMember(ExprTypableReturnEval.class, typableEval);
        return exprDotMethod(CodegenExpressionBuilder.member(member.getMemberId()), methodName, refEPS, refIsNewData, refExprEvalCtx);
    }
}
