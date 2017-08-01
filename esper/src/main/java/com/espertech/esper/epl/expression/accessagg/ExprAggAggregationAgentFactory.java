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
package com.espertech.esper.epl.expression.accessagg;

import com.espertech.esper.epl.agg.access.*;
import com.espertech.esper.epl.core.EngineImportService;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.epl.expression.core.ExprNodeCompiler;

public class ExprAggAggregationAgentFactory {
    public static AggregationAgent make(int streamNum, ExprNode optionalFilter, EngineImportService engineImportService, boolean isFireAndForget, String statementName) {
        ExprEvaluator evaluator = optionalFilter == null ? null : ExprNodeCompiler.allocateEvaluator(optionalFilter.getForge(), engineImportService, ExprAggAggregationAgentFactory.class, isFireAndForget, statementName);
        if (streamNum == 0) {
            if (optionalFilter == null) {
                return AggregationAgentDefault.INSTANCE;
            } else {
                return new AggregationAgentDefaultWFilter(evaluator);
            }
        } else {
            if (optionalFilter == null) {
                return new AggregationAgentRewriteStream(streamNum);
            } else {
                return new AggregationAgentRewriteStreamWFilter(streamNum, evaluator);
            }
        }
    }
}
