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

import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMethodScope;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.epl.core.engineimport.EngineImportService;
import com.espertech.esper.epl.expression.codegen.ExprNodeCompiler;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprForge;

public class AggregationAgentRewriteStreamWFilterForge implements AggregationAgentForge {

    private final int streamNum;
    private final ExprForge filterEval;

    public AggregationAgentRewriteStreamWFilterForge(int streamNum, ExprForge filterEval) {
        this.streamNum = streamNum;
        this.filterEval = filterEval;
    }

    public AggregationAgent makeAgent(EngineImportService engineImportService, boolean isFireAndForget, String statementName) {
        ExprEvaluator evaluator = ExprNodeCompiler.allocateEvaluator(filterEval, engineImportService, AggregationAgentRewriteStreamWFilterForge.class, isFireAndForget, statementName);
        return new AggregationAgentRewriteStreamWFilter(streamNum, evaluator);
    }

    public CodegenExpression applyEnterCodegen(CodegenMethodScope parent, AggregationAgentCodegenSymbols symbols, CodegenClassScope classScope) {
        return AggregationAgentRewriteStreamWFilter.applyEnterCodegen(this, parent, symbols, classScope);
    }

    public CodegenExpression applyLeaveCodegen(CodegenMethodScope parent, AggregationAgentCodegenSymbols symbols, CodegenClassScope classScope) {
        return AggregationAgentRewriteStreamWFilter.applyLeaveCodegen(this, parent, symbols, classScope);
    }

    public int getStreamNum() {
        return streamNum;
    }

    public ExprForge getFilterEval() {
        return filterEval;
    }

    public ExprForge getOptionalFilter() {
        return filterEval;
    }
}
