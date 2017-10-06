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
import com.espertech.esper.epl.core.engineimport.EngineImportService;
import com.espertech.esper.epl.expression.core.ExprForge;
import com.espertech.esper.epl.expression.core.ExprNode;

public class ExprAggAggregationAgentFactory {
    public static AggregationAgentForge make(int streamNum, ExprNode optionalFilter, EngineImportService engineImportService, boolean isFireAndForget, String statementName) {
        ExprForge evaluator = optionalFilter == null ? null : optionalFilter.getForge();
        if (streamNum == 0) {
            if (optionalFilter == null) {
                return AggregationAgentDefault.INSTANCE;
            } else {
                return new AggregationAgentDefaultWFilterForge(evaluator);
            }
        } else {
            if (optionalFilter == null) {
                return new AggregationAgentRewriteStream(streamNum);
            } else {
                return new AggregationAgentRewriteStreamWFilterForge(streamNum, evaluator);
            }
        }
    }
}
