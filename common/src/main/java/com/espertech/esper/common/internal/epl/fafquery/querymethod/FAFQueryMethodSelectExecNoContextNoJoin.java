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

import java.util.Collection;
import java.util.Collections;

import static com.espertech.esper.common.internal.epl.fafquery.querymethod.FAFQueryMethodSelectExecUtil.*;

public class FAFQueryMethodSelectExecNoContextNoJoin implements FAFQueryMethodSelectExec {
    public static final FAFQueryMethodSelectExec INSTANCE = new FAFQueryMethodSelectExecNoContextNoJoin();

    private FAFQueryMethodSelectExecNoContextNoJoin() {
    }

    public EPPreparedQueryResult execute(FAFQueryMethodSelect select, ContextPartitionSelector[] contextPartitionSelectors, FAFQueryMethodAssignerSetter assignerSetter, ContextManagementService contextManagementService) {
        FireAndForgetProcessor processor = select.getProcessors()[0];
        FireAndForgetInstance processorInstance = processor.getProcessorInstanceNoContext();

        Collection<EventBean> events;
        AgentInstanceContext agentInstanceContext = null;
        if (processorInstance == null) {
            events = Collections.emptyList();
        } else {
            agentInstanceContext = processorInstance.getAgentInstanceContext();
            events = snapshot(select.getConsumerFilters()[0], processorInstance, select.getQueryGraph(), select.getAnnotations());
        }

        // get RSP
        ResultSetProcessor resultSetProcessor = processorWithAssign(select.getResultSetProcessorFactoryProvider(), agentInstanceContext, assignerSetter, select.getTableAccesses(), select.getSubselects());

        if (select.getWhereClause() != null) {
            events = filtered(events, select.getWhereClause(), agentInstanceContext);
        }

        return processedNonJoin(resultSetProcessor, events, select.getDistinctKeyGetter());
    }
}
