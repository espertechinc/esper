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
package com.espertech.esper.core.start;

import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.core.service.EPServicesContext;
import com.espertech.esper.epl.expression.table.ExprTableAccessEvalStrategy;
import com.espertech.esper.epl.expression.table.ExprTableAccessNode;

import java.util.Map;

public class EPPreparedExecuteTableHelper {
    public static void assignTableAccessStrategies(EPServicesContext services, ExprTableAccessNode[] optionalTableNodes, AgentInstanceContext agentInstanceContext) {
        if (optionalTableNodes == null) {
            return;
        }
        Map<ExprTableAccessNode, ExprTableAccessEvalStrategy> strategies = EPStatementStartMethodHelperTableAccess.attachTableAccess(services, agentInstanceContext, optionalTableNodes, true);
        for (Map.Entry<ExprTableAccessNode, ExprTableAccessEvalStrategy> strategyEntry : strategies.entrySet()) {
            strategyEntry.getKey().setStrategy(strategyEntry.getValue());
        }
    }
}
