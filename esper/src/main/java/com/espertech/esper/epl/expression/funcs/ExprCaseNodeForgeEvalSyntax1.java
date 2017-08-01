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
package com.espertech.esper.epl.expression.funcs;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.codegen.core.CodegenBlock;
import com.espertech.esper.codegen.core.CodegenContext;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.model.method.CodegenParamSetExprPremade;
import com.espertech.esper.collection.UniformPair;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.util.JavaClassHelper;

import java.util.List;
import java.util.Map;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

public class ExprCaseNodeForgeEvalSyntax1 implements ExprEvaluator {

    private final ExprCaseNodeForge forge;
    private final List<UniformPair<ExprEvaluator>> whenThenNodeList;
    private final ExprEvaluator optionalElseExprNode;

    public ExprCaseNodeForgeEvalSyntax1(ExprCaseNodeForge forge, List<UniformPair<ExprEvaluator>> whenThenNodeList, ExprEvaluator optionalElseExprNode) {
        this.forge = forge;
        this.whenThenNodeList = whenThenNodeList;
        this.optionalElseExprNode = optionalElseExprNode;
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        // Case 1 expression example:
        //      case when a=b then x [when c=d then y...] [else y]
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qExprCase(forge.getForgeRenderable());
        }

        Object caseResult = null;
        boolean matched = false;
        for (UniformPair<ExprEvaluator> p : whenThenNodeList) {
            Boolean whenResult = (Boolean) p.getFirst().evaluate(eventsPerStream, isNewData, exprEvaluatorContext);

            // If the 'when'-expression returns true
            if ((whenResult != null) && whenResult) {
                caseResult = p.getSecond().evaluate(eventsPerStream, isNewData, exprEvaluatorContext);
                matched = true;
                break;
            }
        }

        if (!matched && optionalElseExprNode != null) {
            caseResult = optionalElseExprNode.evaluate(eventsPerStream, isNewData, exprEvaluatorContext);
        }

        if (caseResult == null) {
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aExprCase(null);
            }
            return null;
        }

        if ((caseResult.getClass() != forge.getEvaluationType()) && forge.isNumericResult()) {
            caseResult = JavaClassHelper.coerceBoxed((Number) caseResult, forge.getEvaluationType());
        }

        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aExprCase(caseResult);
        }
        return caseResult;
    }

    public static CodegenExpression codegen(ExprCaseNodeForge forge, CodegenContext context, CodegenParamSetExprPremade params) {
        Class evaluationType = forge.getEvaluationType() == null ? Map.class : forge.getEvaluationType();
        CodegenBlock block = context.addMethod(evaluationType, ExprCaseNodeForgeEvalSyntax1.class).add(params).begin()
                .declareVar(Boolean.class, "when", constantFalse());

        for (UniformPair<ExprNode> pair : forge.getWhenThenNodeList()) {
            block.assignRef("when", pair.getFirst().getForge().evaluateCodegen(params, context));
            block.ifCondition(and(notEqualsNull(ref("when")), ref("when")))
                    .blockReturn(codegenToType(forge, pair.getSecond(), context, params));
        }
        String method;
        if (forge.getOptionalElseExprNode() != null) {
            method = block.methodReturn(codegenToType(forge, forge.getOptionalElseExprNode(), context, params));
        } else {
            method = block.methodReturn(constantNull());
        }
        return localMethodBuild(method).passAll(params).call();
    }

    protected static CodegenExpression codegenToType(ExprCaseNodeForge forge, ExprNode node, CodegenContext context, CodegenParamSetExprPremade params) {
        if (node.getForge().getEvaluationType() == forge.getEvaluationType() || !forge.isNumericResult()) {
            return node.getForge().evaluateCodegen(params, context);
        }
        if (node.getForge().getEvaluationType() == null) {
            return constantNull();
        }
        return JavaClassHelper.coerceNumberToBoxedCodegen(node.getForge().evaluateCodegen(params, context), node.getForge().getEvaluationType(), forge.getEvaluationType());
    }
}
