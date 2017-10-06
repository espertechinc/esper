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
package com.espertech.esper.epl.agg.access;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMethodScope;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.epl.core.engineimport.EngineImportService;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.expression.core.ExprForge;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

public class AggregationAgentRewriteStream implements AggregationAgent, AggregationAgentForge {

    private final int streamNum;

    public AggregationAgentRewriteStream(int streamNum) {
        this.streamNum = streamNum;
    }

    public AggregationAgent makeAgent(EngineImportService engineImportService, boolean isFireAndForget, String statementName) {
        return this;
    }

    public void applyEnter(EventBean[] eventsPerStream, ExprEvaluatorContext exprEvaluatorContext, AggregationState aggregationState) {
        EventBean[] rewrite = new EventBean[]{eventsPerStream[streamNum]};
        aggregationState.applyEnter(rewrite, exprEvaluatorContext);
    }

    public void applyLeave(EventBean[] eventsPerStream, ExprEvaluatorContext exprEvaluatorContext, AggregationState aggregationState) {
        EventBean[] rewrite = new EventBean[]{eventsPerStream[streamNum]};
        aggregationState.applyLeave(rewrite, exprEvaluatorContext);
    }

    public CodegenExpression applyEnterCodegen(CodegenMethodScope parent, AggregationAgentCodegenSymbols symbols, CodegenClassScope classScope) {
        return applyCodegen(true, parent, symbols, classScope);
    }

    public CodegenExpression applyLeaveCodegen(CodegenMethodScope parent, AggregationAgentCodegenSymbols symbols, CodegenClassScope classScope) {
        return applyCodegen(false, parent, symbols, classScope);
    }

    private CodegenExpression applyCodegen(boolean enter, CodegenMethodScope parent, AggregationAgentCodegenSymbols symbols, CodegenClassScope classScope) {
        return exprDotMethod(symbols.getAddState(parent), enter ? "applyEnter" : "applyLeave", newArrayWithInit(EventBean.class, arrayAtIndex(symbols.getAddEPS(parent), constant(streamNum))), symbols.getAddExprEvalCtx(parent));
    }

    public ExprForge getOptionalFilter() {
        return null;
    }
}
