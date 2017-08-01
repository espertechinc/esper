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

import com.espertech.esper.codegen.core.CodegenContext;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.model.method.CodegenParamSetExprPremade;
import com.espertech.esper.collection.UniformPair;
import com.espertech.esper.epl.expression.core.*;
import com.espertech.esper.util.SimpleNumberCoercer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.constantNull;

public class ExprCaseNodeForge implements ExprTypableReturnForge {
    private final ExprCaseNode parent;
    private final Class resultType;
    protected final LinkedHashMap<String, Object> mapResultType;
    private final boolean isNumericResult;
    private final boolean mustCoerce;
    private final SimpleNumberCoercer coercer;
    private final List<UniformPair<ExprNode>> whenThenNodeList;
    private final ExprNode optionalCompareExprNode;
    private final ExprNode optionalElseExprNode;

    ExprCaseNodeForge(ExprCaseNode parent, Class resultType, LinkedHashMap<String, Object> mapResultType, boolean isNumericResult, boolean mustCoerce, SimpleNumberCoercer coercer, List<UniformPair<ExprNode>> whenThenNodeList, ExprNode optionalCompareExprNode, ExprNode optionalElseExprNode) {
        this.parent = parent;
        this.resultType = resultType;
        this.mapResultType = mapResultType;
        this.isNumericResult = isNumericResult;
        this.mustCoerce = mustCoerce;
        this.coercer = coercer;
        this.whenThenNodeList = whenThenNodeList;
        this.optionalCompareExprNode = optionalCompareExprNode;
        this.optionalElseExprNode = optionalElseExprNode;
    }

    public List<UniformPair<ExprNode>> getWhenThenNodeList() {
        return whenThenNodeList;
    }

    public ExprNode getOptionalCompareExprNode() {
        return optionalCompareExprNode;
    }

    public ExprNode getOptionalElseExprNode() {
        return optionalElseExprNode;
    }

    public ExprCaseNode getForgeRenderable() {
        return parent;
    }

    public Class getEvaluationType() {
        return resultType;
    }

    boolean isNumericResult() {
        return isNumericResult;
    }

    public boolean isMustCoerce() {
        return mustCoerce;
    }

    public SimpleNumberCoercer getCoercer() {
        return coercer;
    }

    public ExprEvaluator getExprEvaluator() {
        List<UniformPair<ExprEvaluator>> evals = new ArrayList<>();
        for (UniformPair<ExprNode> pair : whenThenNodeList) {
            evals.add(new UniformPair<>(pair.getFirst().getForge().getExprEvaluator(), pair.getSecond().getForge().getExprEvaluator()));
        }
        if (!parent.isCase2()) {
            return new ExprCaseNodeForgeEvalSyntax1(this, evals, optionalElseExprNode == null ? null : optionalElseExprNode.getForge().getExprEvaluator());
        } else {
            return new ExprCaseNodeForgeEvalSyntax2(this, evals, optionalCompareExprNode.getForge().getExprEvaluator(), optionalElseExprNode == null ? null : optionalElseExprNode.getForge().getExprEvaluator());
        }
    }

    public ExprTypableReturnEval getTypableReturnEvaluator() {
        return new ExprCaseNodeForgeEvalTypable(this);
    }

    public CodegenExpression evaluateCodegen(CodegenParamSetExprPremade params, CodegenContext context) {
        if (!parent.isCase2()) {
            return ExprCaseNodeForgeEvalSyntax1.codegen(this, context, params);
        } else {
            return ExprCaseNodeForgeEvalSyntax2.codegen(this, context, params);
        }
    }

    public ExprForgeComplexityEnum getComplexity() {
        return ExprForgeComplexityEnum.INTER;
    }

    public CodegenExpression evaluateTypableSingleCodegen(CodegenParamSetExprPremade params, CodegenContext context) {
        return ExprCaseNodeForgeEvalTypable.codegenTypeableSingle(this, params, context);
    }

    public CodegenExpression evaluateTypableMultiCodegen(CodegenParamSetExprPremade params, CodegenContext context) {
        return constantNull();
    }

    public Boolean isMultirow() {
        return mapResultType == null ? null : false;
    }

    public LinkedHashMap<String, Object> getRowProperties() throws ExprValidationException {
        return mapResultType;
    }
}
