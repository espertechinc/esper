/**************************************************************************************
 * Copyright (C) 2006-2015 EsperTech Inc. All rights reserved.                        *
 * http://www.espertech.com/esper                                                          *
 * http://www.espertech.com                                                           *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the GPL license       *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package com.espertech.esper.core.start;

import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.core.service.EPServicesContext;
import com.espertech.esper.epl.expression.table.ExprTableAccessEvalStrategy;
import com.espertech.esper.epl.expression.table.ExprTableAccessNode;
import com.espertech.esper.epl.table.mgmt.TableMetadata;
import com.espertech.esper.epl.table.mgmt.TableStateInstance;
import com.espertech.esper.epl.table.strategy.ExprTableEvalStrategyFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class EPStatementStartMethodHelperTableAccess
{
    public static Map<ExprTableAccessNode, ExprTableAccessEvalStrategy> attachTableAccess(EPServicesContext services, AgentInstanceContext agentInstanceContext, ExprTableAccessNode[] tableNodes) {
        if (tableNodes == null || tableNodes.length == 0) {
            return Collections.emptyMap();
        }

        Map<ExprTableAccessNode, ExprTableAccessEvalStrategy> strategies = new HashMap<ExprTableAccessNode, ExprTableAccessEvalStrategy>();
        for (ExprTableAccessNode tableNode : tableNodes) {
            TableStateInstance state = services.getTableService().getState(tableNode.getTableName(), agentInstanceContext.getAgentInstanceId());
            TableMetadata tableMetadata = services.getTableService().getTableMetadata(tableNode.getTableName());
            ExprTableAccessEvalStrategy strategy = ExprTableEvalStrategyFactory.getTableAccessEvalStrategy(agentInstanceContext.getStatementContext().isWritesToTables(), tableNode, state, tableMetadata);
            strategies.put(tableNode, strategy);
        }

        return strategies;
    }
}
