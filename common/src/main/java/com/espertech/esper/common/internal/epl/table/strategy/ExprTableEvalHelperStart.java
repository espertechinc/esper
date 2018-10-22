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

import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.epl.table.core.Table;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ExprTableEvalHelperStart {
    public static Map<Integer, ExprTableEvalStrategy> startTableAccess(Map<Integer, ExprTableEvalStrategyFactory> tableAccesses, AgentInstanceContext agentInstanceContext) {
        if (tableAccesses == null || tableAccesses.isEmpty()) {
            return Collections.emptyMap();
        }
        boolean writesToTables = agentInstanceContext.getStatementContext().getStatementInformationals().isWritesToTables();
        Map<Integer, ExprTableEvalStrategy> evals = new HashMap<>(tableAccesses.size(), 1f);
        for (Map.Entry<Integer, ExprTableEvalStrategyFactory> entry : tableAccesses.entrySet()) {
            Table table = entry.getValue().getTable();
            TableAndLockProvider provider = table.getStateProvider(agentInstanceContext.getAgentInstanceId(), writesToTables);
            ExprTableEvalStrategy strategy = entry.getValue().makeStrategy(provider);
            evals.put(entry.getKey(), strategy);
        }
        return evals;
    }
}
