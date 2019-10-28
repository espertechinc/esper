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
package com.espertech.esper.common.internal.epl.fafquery.querymethod;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.context.ContextPartitionSelector;
import com.espertech.esper.common.internal.context.mgr.ContextManagementService;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.epl.fafquery.processor.FireAndForgetInstance;
import com.espertech.esper.common.internal.epl.fafquery.processor.FireAndForgetProcessor;
import com.espertech.esper.common.internal.epl.resultset.core.ResultSetProcessor;

import java.util.ArrayDeque;
import java.util.Collection;

import static com.espertech.esper.common.internal.epl.fafquery.querymethod.FAFQueryMethodSelectExecUtil.*;
import static com.espertech.esper.common.internal.epl.fafquery.querymethod.FAFQueryMethodUtil.agentInstanceIds;

public class FAFQueryMethodSelectExecSomeContextNoJoin implements FAFQueryMethodSelectExec {
    public static final FAFQueryMethodSelectExec INSTANCE = new FAFQueryMethodSelectExecSomeContextNoJoin();

    private FAFQueryMethodSelectExecSomeContextNoJoin() {
    }

    public EPPreparedQueryResult execute(FAFQueryMethodSelect select, ContextPartitionSelector[] contextPartitionSelectors, FAFQueryMethodAssignerSetter assignerSetter, ContextManagementService contextManagementService) {
        FireAndForgetProcessor processor = select.getProcessors()[0];

        ContextPartitionSelector singleSelector = contextPartitionSelectors != null && contextPartitionSelectors.length > 0 ? contextPartitionSelectors[0] : null;
        Collection<Integer> agentInstanceIds = agentInstanceIds(processor, singleSelector, contextManagementService);

        Collection<EventBean> events = new ArrayDeque<>();
        AgentInstanceContext agentInstanceContext = null;
        for (int agentInstanceId : agentInstanceIds) {
            FireAndForgetInstance processorInstance = processor.getProcessorInstanceContextById(agentInstanceId);
            if (processorInstance != null) {
                agentInstanceContext = processorInstance.getAgentInstanceContext();
                Collection<EventBean> coll = processorInstance.snapshotBestEffort(select.getQueryGraph(), select.getAnnotations());
                events.addAll(coll);
            }
        }

        // get RSP
        ResultSetProcessor resultSetProcessor = processorWithAssign(select.getResultSetProcessorFactoryProvider(), agentInstanceContext, assignerSetter, select.getTableAccesses(), select.getSubselects());

        if (select.getWhereClause() != null) {
            events = filtered(events, select.getWhereClause(), agentInstanceContext);
        }

        return processedNonJoin(resultSetProcessor, events, select.getDistinctKeyGetter());
    }
}
