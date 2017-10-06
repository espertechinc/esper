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

import com.espertech.esper.epl.core.engineimport.EngineImportService;
import com.espertech.esper.epl.expression.codegen.ExprNodeCompiler;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprForge;
import com.espertech.esper.plugin.PlugInAggregationMultiFunctionCodegenType;

public class AggregationAccessorFirstLastIndexNoEvalForge implements AggregationAccessorForge {
    private final ExprForge indexNode;
    private final int constant;
    private final boolean isFirst;

    public AggregationAccessorFirstLastIndexNoEvalForge(ExprForge indexNode, int constant, boolean first) {
        this.indexNode = indexNode;
        this.constant = constant;
        isFirst = first;
    }

    public AggregationAccessor getAccessor(EngineImportService engineImportService, boolean isFireAndForget, String statementName) {
        ExprEvaluator index = indexNode == null ? null : ExprNodeCompiler.allocateEvaluator(indexNode, engineImportService, this.getClass(), isFireAndForget, statementName);
        return new AggregationAccessorFirstLastIndexNoEval(index, constant, isFirst);
    }

    public PlugInAggregationMultiFunctionCodegenType getPluginCodegenType() {
        return PlugInAggregationMultiFunctionCodegenType.CODEGEN_ALL; // not currently applicable as table-related only
    }

    public void getValueCodegen(AggregationAccessorForgeGetCodegenContext context) {
        throw new UnsupportedOperationException();
    }

    public void getEnumerableEventsCodegen(AggregationAccessorForgeGetCodegenContext context) {
        throw new UnsupportedOperationException();
    }

    public void getEnumerableEventCodegen(AggregationAccessorForgeGetCodegenContext context) {
        throw new UnsupportedOperationException();
    }

    public void getEnumerableScalarCodegen(AggregationAccessorForgeGetCodegenContext context) {
        throw new UnsupportedOperationException();
    }
}
