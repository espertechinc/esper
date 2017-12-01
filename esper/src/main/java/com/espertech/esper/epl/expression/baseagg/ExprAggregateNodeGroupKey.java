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
package com.espertech.esper.epl.expression.baseagg;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMethodScope;
import com.espertech.esper.epl.expression.codegen.CodegenLegoEvaluateSelf;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.collection.MultiKeyUntyped;
import com.espertech.esper.epl.agg.service.common.AggregationResultFuture;
import com.espertech.esper.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.epl.expression.core.*;

import java.io.StringWriter;

public class ExprAggregateNodeGroupKey extends ExprNodeBase implements ExprForge, ExprEvaluator {
    private static final long serialVersionUID = 154204964713946760L;
    private final int groupKeyIndex;
    private final Class returnType;

    private AggregationResultFuture future;

    public ExprAggregateNodeGroupKey(int groupKeyIndex, Class returnType) {
        this.groupKeyIndex = groupKeyIndex;
        this.returnType = returnType;
    }

    public void assignFuture(AggregationResultFuture future) {
        this.future = future;
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        Object groupKey = future.getGroupKey(context.getAgentInstanceId());
        if (groupKey instanceof MultiKeyUntyped) {
            return ((MultiKeyUntyped) groupKey).getKeys()[groupKeyIndex];
        }
        return groupKey;
    }

    public CodegenExpression evaluateCodegen(Class requiredType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return CodegenLegoEvaluateSelf.evaluateSelfPlainWithCast(requiredType, this, returnType, codegenMethodScope, exprSymbol, codegenClassScope);
    }

    public ExprForgeComplexityEnum getComplexity() {
        return ExprForgeComplexityEnum.SELF;
    }

    public Class getEvaluationType() {
        return returnType;
    }

    public ExprForge getForge() {
        return this;
    }

    public ExprNode getForgeRenderable() {
        return this;
    }

    public ExprEvaluator getExprEvaluator() {
        return this;
    }

    public void toPrecedenceFreeEPL(StringWriter writer) {
    }

    public ExprPrecedenceEnum getPrecedence() {
        return ExprPrecedenceEnum.UNARY;
    }

    public String toExpressionString(ExprPrecedenceEnum precedence) {
        return null;
    }

    public boolean isConstantResult() {
        return false;
    }

    public boolean equalsNode(ExprNode node, boolean ignoreStreamPrefix) {
        return false;
    }

    public ExprNode validate(ExprValidationContext validationContext) throws ExprValidationException {
        // not required
        return null;
    }
}
