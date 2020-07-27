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
package com.espertech.esper.common.internal.epl.table.strategy;

import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.epl.table.core.Table;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ExprTableEvalHelperStart {
    public static Map<Integer, ExprTableEvalStrategy> startTableAccess(Map<Integer, ExprTableEvalStrategyFactory> tableAccesses, ExprEvaluatorContext exprEvaluatorContext) {
        if (tableAccesses == null || tableAccesses.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Integer, ExprTableEvalStrategy> evals = new HashMap<>(tableAccesses.size(), 1f);
        for (Map.Entry<Integer, ExprTableEvalStrategyFactory> entry : tableAccesses.entrySet()) {
            Table table = entry.getValue().getTable();
            TableAndLockProvider provider = table.getStateProvider(exprEvaluatorContext.getAgentInstanceId(), exprEvaluatorContext.isWritesToTables());
            ExprTableEvalStrategy strategy = entry.getValue().makeStrategy(provider);
            evals.put(entry.getKey(), strategy);
        }
        return evals;
    }
}
