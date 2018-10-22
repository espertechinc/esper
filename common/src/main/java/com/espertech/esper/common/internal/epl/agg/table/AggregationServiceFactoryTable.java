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
package com.espertech.esper.common.internal.epl.agg.table;

import com.espertech.esper.common.client.hook.aggmultifunc.AggregationMultiFunctionAgent;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.epl.agg.core.AggregationGroupByRollupDesc;
import com.espertech.esper.common.internal.epl.agg.core.AggregationService;
import com.espertech.esper.common.internal.epl.agg.core.AggregationServiceFactory;
import com.espertech.esper.common.internal.epl.table.core.*;
import com.espertech.esper.common.internal.settings.ClasspathImportServiceRuntime;

public class AggregationServiceFactoryTable implements AggregationServiceFactory {
    private Table table;
    private TableColumnMethodPairEval[] methodPairs;
    private AggregationMultiFunctionAgent[] accessAgents;
    private int[] accessColumnsZeroOffset;
    private AggregationGroupByRollupDesc groupByRollupDesc;

    public void setTable(Table table) {
        this.table = table;
    }

    public void setMethodPairs(TableColumnMethodPairEval[] methodPairs) {
        this.methodPairs = methodPairs;
    }

    public void setAccessAgents(AggregationMultiFunctionAgent[] accessAgents) {
        this.accessAgents = accessAgents;
    }

    public void setAccessColumnsZeroOffset(int[] accessColumnsZeroOffset) {
        this.accessColumnsZeroOffset = accessColumnsZeroOffset;
    }

    public void setGroupByRollupDesc(AggregationGroupByRollupDesc groupByRollupDesc) {
        this.groupByRollupDesc = groupByRollupDesc;
    }

    public AggregationService makeService(AgentInstanceContext agentInstanceContext, ClasspathImportServiceRuntime classpathImportService, boolean isSubquery, Integer subqueryNumber, int[] groupId) {
        TableInstance tableInstance = table.getTableInstance(agentInstanceContext.getAgentInstanceId());
        if (!table.getMetaData().isKeyed()) {
            TableInstanceUngrouped tableInstanceUngrouped = (TableInstanceUngrouped) tableInstance;
            return new AggSvcGroupAllWTableImpl(tableInstanceUngrouped, methodPairs, accessAgents, accessColumnsZeroOffset);
        }

        TableInstanceGrouped tableInstanceGrouped = (TableInstanceGrouped) tableInstance;
        if (groupByRollupDesc == null) {
            return new AggSvcGroupByWTableImpl(tableInstanceGrouped, methodPairs, accessAgents, accessColumnsZeroOffset);
        }

        if (table.getMetaData().getKeyTypes().length > 1) {
            return new AggSvcGroupByWTableRollupMultiKeyImpl(tableInstanceGrouped, methodPairs, accessAgents, accessColumnsZeroOffset, groupByRollupDesc);
        } else {
            return new AggSvcGroupByWTableRollupSingleKeyImpl(tableInstanceGrouped, methodPairs, accessAgents, accessColumnsZeroOffset);
        }
    }
}
