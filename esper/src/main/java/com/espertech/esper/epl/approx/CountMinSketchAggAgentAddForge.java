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
package com.espertech.esper.epl.approx;

import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMethodScope;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.model.expression.CodegenExpressionNoOp;
import com.espertech.esper.epl.agg.access.AggregationAgent;
import com.espertech.esper.epl.agg.access.AggregationAgentCodegenSymbols;
import com.espertech.esper.epl.agg.access.AggregationAgentForge;
import com.espertech.esper.epl.core.engineimport.EngineImportService;
import com.espertech.esper.epl.expression.codegen.ExprNodeCompiler;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprForge;

public class CountMinSketchAggAgentAddForge implements AggregationAgentForge {

    protected final ExprForge stringEvaluator;
    protected final ExprForge optionalFilterForge;

    public CountMinSketchAggAgentAddForge(ExprForge stringEvaluator, ExprForge optionalFilterForge) {
        this.stringEvaluator = stringEvaluator;
        this.optionalFilterForge = optionalFilterForge;
    }

    public AggregationAgent makeAgent(EngineImportService engineImportService, boolean isFireAndForget, String statementName) {
        ExprEvaluator eval = ExprNodeCompiler.allocateEvaluator(stringEvaluator, engineImportService, this.getClass(), isFireAndForget, statementName);
        if (optionalFilterForge == null) {
            return new CountMinSketchAggAgentAdd(eval);
        }
        ExprEvaluator filter = ExprNodeCompiler.allocateEvaluator(optionalFilterForge, engineImportService, this.getClass(), isFireAndForget, statementName);
        return new CountMinSketchAggAgentAddFilter(eval, filter);
    }

    public CodegenExpression applyEnterCodegen(CodegenMethodScope parent, AggregationAgentCodegenSymbols symbols, CodegenClassScope classScope) {
        return CountMinSketchAggAgentAddFilter.applyEnterCodegen(this, parent, symbols, classScope);
    }

    public CodegenExpression applyLeaveCodegen(CodegenMethodScope parent, AggregationAgentCodegenSymbols symbols, CodegenClassScope classScope) {
        // no code
        return CodegenExpressionNoOp.INSTANCE;
    }

    public ExprForge getStringEvaluator() {
        return stringEvaluator;
    }

    public ExprForge getOptionalFilterForge() {
        return optionalFilterForge;
    }

    public ExprForge getOptionalFilter() {
        return optionalFilterForge;
    }
}
