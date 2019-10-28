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
import com.espertech.esper.common.internal.collection.UniformPair;
import com.espertech.esper.common.internal.context.mgr.ContextManagementService;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.epl.fafquery.processor.FireAndForgetInstance;
import com.espertech.esper.common.internal.epl.fafquery.processor.FireAndForgetProcessor;
import com.espertech.esper.common.internal.epl.resultset.core.ResultSetProcessor;
import com.espertech.esper.common.internal.event.core.EventBeanUtility;

import java.util.*;

import static com.espertech.esper.common.internal.epl.fafquery.querymethod.FAFQueryMethodSelectExecUtil.processorWithAssign;
import static com.espertech.esper.common.internal.epl.fafquery.querymethod.FAFQueryMethodUtil.agentInstanceIds;

public class FAFQueryMethodSelectExecGivenContextNoJoin implements FAFQueryMethodSelectExec {
    public static final FAFQueryMethodSelectExec INSTANCE = new FAFQueryMethodSelectExecGivenContextNoJoin();

    private FAFQueryMethodSelectExecGivenContextNoJoin() {
    }

    public EPPreparedQueryResult execute(FAFQueryMethodSelect select, ContextPartitionSelector[] contextPartitionSelectors, FAFQueryMethodAssignerSetter assignerSetter, ContextManagementService contextManagementService) {
        FireAndForgetProcessor processor = select.getProcessors()[0];

        ContextPartitionSelector singleSelector = contextPartitionSelectors != null && contextPartitionSelectors.length > 0 ? contextPartitionSelectors[0] : null;
        Collection<Integer> agentInstanceIds = agentInstanceIds(processor, singleSelector, contextManagementService);

        List<ContextPartitionResult> contextPartitionResults = new ArrayList<ContextPartitionResult>();
        for (int agentInstanceId : agentInstanceIds) {
            FireAndForgetInstance processorInstance = processor.getProcessorInstanceContextById(agentInstanceId);
            if (processorInstance != null) {
                Collection<EventBean> coll = processorInstance.snapshotBestEffort(select.getQueryGraph(), select.getAnnotations());
                contextPartitionResults.add(new ContextPartitionResult(coll, processorInstance.getAgentInstanceContext()));
            }
        }

        // process context partitions
        ArrayDeque<EventBean[]> events = new ArrayDeque<EventBean[]>();
        ResultSetProcessor resultSetProcessor = null;
        for (ContextPartitionResult contextPartitionResult : contextPartitionResults) {

            resultSetProcessor = processorWithAssign(select.getResultSetProcessorFactoryProvider(), contextPartitionResult.getContext(), assignerSetter, select.getTableAccesses(), select.getSubselects());

            Collection<EventBean> snapshot = contextPartitionResult.getEvents();
            if (select.getWhereClause() != null) {
                snapshot = FAFQueryMethodSelectExecUtil.filtered(snapshot, select.getWhereClause(), contextPartitionResult.context);
            }
            EventBean[] rows = snapshot.toArray(new EventBean[snapshot.size()]);
            resultSetProcessor.setAgentInstanceContext(contextPartitionResult.getContext());
            UniformPair<EventBean[]> results = resultSetProcessor.processViewResult(rows, null, true);
            if (results != null && results.getFirst() != null && results.getFirst().length > 0) {
                events.add(results.getFirst());
            }
        }

        EventBean[] distinct = EventBeanUtility.getDistinctByProp(EventBeanUtility.flatten(events), select.getDistinctKeyGetter());
        return new EPPreparedQueryResult(select.getEventType(), distinct);
    }

    private static class ContextPartitionResult {
        private final Collection<EventBean> events;
        private final AgentInstanceContext context;

        private ContextPartitionResult(Collection<EventBean> events, AgentInstanceContext context) {
            this.events = events;
            this.context = context;
        }

        public Collection<EventBean> getEvents() {
            return events;
        }

        public AgentInstanceContext getContext() {
            return context;
        }
    }
}
