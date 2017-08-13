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
package com.espertech.esper.epl.expression.ops;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.codegen.core.CodegenContext;
import com.espertech.esper.codegen.core.CodegenMember;
import com.espertech.esper.codegen.core.CodegenMethodId;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.model.method.CodegenParamSetExprPremade;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;

import java.util.regex.Pattern;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

/**
 * Like-Node Form-1: string input, constant pattern and no or constant escape character
 */
public class ExprRegexpNodeForgeConstEval implements ExprEvaluator {
    private final ExprRegexpNodeForgeConst forge;
    private final ExprEvaluator lhsEval;

    ExprRegexpNodeForgeConstEval(ExprRegexpNodeForgeConst forge, ExprEvaluator lhsEval) {
        this.forge = forge;
        this.lhsEval = lhsEval;
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qExprRegexp(forge.getForgeRenderable());
        }

        Object value = lhsEval.evaluate(eventsPerStream, isNewData, context);
        if (value == null) {
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aExprRegexp(null);
            }
            return null;
        }

        if (forge.isNumericValue()) {
            value = value.toString();
        }

        boolean result = forge.getForgeRenderable().isNot() ^ forge.getPattern().matcher((CharSequence) value).matches();

        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aExprRegexp(result);
        }
        return result;
    }

    public static CodegenMethodId codegen(ExprRegexpNodeForgeConst forge, ExprNode lhs, CodegenContext context, CodegenParamSetExprPremade params) {
        CodegenMember mPattern = context.makeAddMember(Pattern.class, forge.getPattern());

        if (!forge.isNumericValue()) {
            return context.addMethod(Boolean.class, ExprRegexpNodeForgeConstEval.class).add(params).begin()
                    .declareVar(String.class, "value", lhs.getForge().evaluateCodegen(params, context))
                    .ifRefNullReturnNull("value")
                    .methodReturn(getRegexpCode(forge, member(mPattern.getMemberId()), ref("value")));
        }
        return context.addMethod(Boolean.class, ExprRegexpNodeForgeConstEval.class).add(params).begin()
                .declareVar(Object.class, "value", lhs.getForge().evaluateCodegen(params, context))
                .ifRefNullReturnNull("value")
                .methodReturn(getRegexpCode(forge, member(mPattern.getMemberId()), exprDotMethod(ref("value"), "toString")));
    }

    static CodegenExpression getRegexpCode(ExprRegexpNodeForge forge, CodegenExpression pattern, CodegenExpression stringExpr) {
        CodegenExpression eval = exprDotMethodChain(pattern).add("matcher", stringExpr).add("matches");
        return !forge.getForgeRenderable().isNot() ? eval : not(eval);
    }
}
