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
import com.espertech.esper.codegen.core.CodegenBlock;
import com.espertech.esper.codegen.core.CodegenContext;
import com.espertech.esper.codegen.core.CodegenMethodId;
import com.espertech.esper.codegen.model.method.CodegenParamSetExprPremade;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.util.LikeUtil;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.epl.expression.ops.ExprLikeNodeForgeConstEval.getLikeCode;

public class ExprLikeNodeFormNonconstEval implements ExprEvaluator {
    private final ExprLikeNodeForgeNonconst form;
    private final ExprEvaluator lhsEval;
    private final ExprEvaluator patternEval;
    private final ExprEvaluator optionalEscapeEval;

    ExprLikeNodeFormNonconstEval(ExprLikeNodeForgeNonconst forge, ExprEvaluator lhsEval, ExprEvaluator patternEval, ExprEvaluator optionalEscapeEval) {
        this.form = forge;
        this.lhsEval = lhsEval;
        this.patternEval = patternEval;
        this.optionalEscapeEval = optionalEscapeEval;
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qExprLike(form.getForgeRenderable());
        }

        String pattern = (String) patternEval.evaluate(eventsPerStream, isNewData, context);
        if (pattern == null) {
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aExprLike(null);
            }
            return null;
        }

        Character es = '\\';
        if (optionalEscapeEval != null) {
            String escapeString = (String) optionalEscapeEval.evaluate(eventsPerStream, isNewData, context);
            if (escapeString != null && !escapeString.isEmpty()) {
                es = escapeString.charAt(0);
            }
        }
        LikeUtil likeUtil = new LikeUtil(pattern, es, false);

        Object value = lhsEval.evaluate(eventsPerStream, isNewData, context);
        if (value == null) {
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aExprLike(null);
            }
            return null;
        }

        if (form.isNumericValue()) {
            value = value.toString();
        }

        boolean result = form.getForgeRenderable().isNot() ^ likeUtil.compare((String) value);

        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aExprLike(result);
        }
        return result;
    }

    public static CodegenMethodId codegen(ExprLikeNodeForgeNonconst forge, ExprNode lhs, ExprNode pattern, ExprNode optionalEscape, CodegenContext context, CodegenParamSetExprPremade params) {
        CodegenBlock blockMethod = context.addMethod(Boolean.class, ExprLikeNodeFormNonconstEval.class).add(params).begin()
                .declareVar(String.class, "pattern", pattern.getForge().evaluateCodegen(params, context))
                .ifRefNullReturnNull("pattern");

        // initial like-setup
        blockMethod.declareVar(Character.class, "es", constant('\\'));
        if (optionalEscape != null) {
            blockMethod.declareVar(String.class, "escapeString", optionalEscape.getForge().evaluateCodegen(params, context));
            blockMethod.ifCondition(and(notEqualsNull(ref("escapeString")), not(exprDotMethod(ref("escapeString"), "isEmpty"))))
                    .assignRef("es", exprDotMethod(ref("escapeString"), "charAt", constant(0)));
        }
        blockMethod.declareVar(LikeUtil.class, "likeUtil", newInstance(LikeUtil.class, ref("pattern"), ref("es"), constant(false)));

        if (!forge.isNumericValue()) {
            return blockMethod
                    .declareVar(String.class, "value", lhs.getForge().evaluateCodegen(params, context))
                    .ifRefNullReturnNull("value")
                    .methodReturn(getLikeCode(forge, ref("likeUtil"), ref("value")));
        }
        return blockMethod
                .declareVar(Object.class, "value", lhs.getForge().evaluateCodegen(params, context))
                .ifRefNullReturnNull("value")
                .methodReturn(getLikeCode(forge, ref("likeUtil"), exprDotMethod(ref("value"), "toString")));
    }

}
