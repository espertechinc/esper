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

import com.espertech.esper.client.EPException;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.codegen.core.CodegenBlock;
import com.espertech.esper.codegen.core.CodegenContext;
import com.espertech.esper.codegen.core.CodegenMethodId;
import com.espertech.esper.codegen.model.method.CodegenParamSetExprPremade;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.epl.expression.ops.ExprRegexpNodeForgeConstEval.getRegexpCode;

public class ExprRegexpNodeForgeNonconstEval implements ExprEvaluator {
    private final ExprRegexpNodeForgeNonconst forge;
    private final ExprEvaluator lhsEval;
    private final ExprEvaluator patternEval;

    public ExprRegexpNodeForgeNonconstEval(ExprRegexpNodeForgeNonconst forge, ExprEvaluator lhsEval, ExprEvaluator patternEval) {
        this.forge = forge;
        this.lhsEval = lhsEval;
        this.patternEval = patternEval;
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param text regex pattern
     * @return pattern
     */
    public static Pattern exprRegexNodeCompilePattern(String text) {
        try {
            return Pattern.compile(text);
        } catch (PatternSyntaxException ex) {
            throw new EPException("Error compiling regex pattern '" + text + "': " + ex.getMessage(), ex);
        }
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qExprRegexp(forge.getForgeRenderable());
        }

        String patternText = (String) patternEval.evaluate(eventsPerStream, isNewData, context);
        if (patternText == null) {
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aExprRegexp(null);
            }
            return null;
        }

        Pattern pattern = exprRegexNodeCompilePattern(patternText);

        Object evalValue = lhsEval.evaluate(eventsPerStream, isNewData, context);
        if (evalValue == null) {
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aExprRegexp(null);
            }
            return null;
        }

        if (forge.isNumericValue()) {
            evalValue = evalValue.toString();
        }

        boolean result = forge.getForgeRenderable().isNot() ^ pattern.matcher((CharSequence) evalValue).matches();

        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aExprRegexp(result);
        }
        return result;
    }

    public static CodegenMethodId codegen(ExprRegexpNodeForgeNonconst forge, ExprNode lhs, ExprNode pattern, CodegenContext context, CodegenParamSetExprPremade params) {
        CodegenBlock blockMethod = context.addMethod(Boolean.class, ExprRegexpNodeForgeNonconstEval.class).add(params).begin()
                .declareVar(String.class, "patternText", pattern.getForge().evaluateCodegen(params, context))
                .ifRefNullReturnNull("patternText");

        // initial like-setup
        blockMethod.declareVar(Pattern.class, "pattern", staticMethod(ExprRegexpNodeForgeNonconstEval.class, "exprRegexNodeCompilePattern", ref("patternText")));

        if (!forge.isNumericValue()) {
            return blockMethod
                    .declareVar(String.class, "value", lhs.getForge().evaluateCodegen(params, context))
                    .ifRefNullReturnNull("value")
                    .methodReturn(getRegexpCode(forge, ref("pattern"), ref("value")));
        }
        return blockMethod
                .declareVar(Object.class, "value", lhs.getForge().evaluateCodegen(params, context))
                .ifRefNullReturnNull("value")
                .methodReturn(getRegexpCode(forge, ref("pattern"), exprDotMethod(ref("value"), "toString")));
    }

}
