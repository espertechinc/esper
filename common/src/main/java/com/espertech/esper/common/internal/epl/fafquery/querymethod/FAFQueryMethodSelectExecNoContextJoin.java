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
import com.espertech.esper.common.internal.collection.MultiKeyArrayOfKeys;
import com.espertech.esper.common.internal.collection.UniformPair;
import com.espertech.esper.common.internal.context.mgr.ContextManagementService;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.epl.fafquery.processor.FireAndForgetInstance;
import com.espertech.esper.common.internal.epl.fafquery.processor.FireAndForgetProcessor;
import com.espertech.esper.common.internal.epl.join.base.JoinSetComposer;
import com.espertech.esper.common.internal.epl.join.base.JoinSetComposerDesc;
import com.espertech.esper.common.internal.epl.join.base.JoinSetComposerUtil;
import com.espertech.esper.common.internal.epl.resultset.core.ResultSetProcessor;
import com.espertech.esper.common.internal.event.core.EventBeanUtility;
import com.espertech.esper.common.internal.view.core.Viewable;

import java.util.Collection;
import java.util.Set;

import static com.espertech.esper.common.internal.epl.fafquery.querymethod.FAFQueryMethodSelectExecUtil.processorWithAssign;
import static com.espertech.esper.common.internal.epl.fafquery.querymethod.FAFQueryMethodSelectExecUtil.snapshot;

public class FAFQueryMethodSelectExecNoContextJoin implements FAFQueryMethodSelectExec {
    public static final FAFQueryMethodSelectExec INSTANCE = new FAFQueryMethodSelectExecNoContextJoin();

    private FAFQueryMethodSelectExecNoContextJoin() {
    }

    public EPPreparedQueryResult execute(FAFQueryMethodSelect select, ContextPartitionSelector[] contextPartitionSelectors, FAFQueryMethodAssignerSetter assignerSetter, ContextManagementService contextManagementService) {
        int numStreams = select.getProcessors().length;
        Collection<EventBean>[] snapshots = new Collection[numStreams];

        AgentInstanceContext agentInstanceContext = null;
        Viewable[] viewablePerStream = new Viewable[numStreams];

        for (int i = 0; i < numStreams; i++) {
            FireAndForgetProcessor processor = select.getProcessors()[i];
            FireAndForgetInstance processorInstance = processor.getProcessorInstanceNoContext();
            snapshots[i] = snapshot(select.getConsumerFilters()[i], processorInstance, select.getQueryGraph(), select.getAnnotations());
            agentInstanceContext = processorInstance.getAgentInstanceContext();
            viewablePerStream[i] = processorInstance.getTailViewInstance();
        }

        // get RSP
        ResultSetProcessor resultSetProcessor = processorWithAssign(select.getResultSetProcessorFactoryProvider(), agentInstanceContext, assignerSetter, select.getTableAccesses(), select.getSubselects());

        // determine join
        JoinSetComposerDesc joinSetComposerDesc = select.getJoinSetComposerPrototype().create(viewablePerStream, true, agentInstanceContext, false);
        JoinSetComposer joinComposer = joinSetComposerDesc.getJoinSetComposer();

        EventBean[][] oldDataPerStream = new EventBean[numStreams][];
        EventBean[][] newDataPerStream = new EventBean[numStreams][];
        for (int i = 0; i < numStreams; i++) {
            newDataPerStream[i] = snapshots[i].toArray(new EventBean[snapshots[i].size()]);
        }
        UniformPair<Set<MultiKeyArrayOfKeys<EventBean>>> result = joinComposer.join(newDataPerStream, oldDataPerStream, agentInstanceContext);
        if (joinSetComposerDesc.getPostJoinFilterEvaluator() != null) {
            JoinSetComposerUtil.filter(joinSetComposerDesc.getPostJoinFilterEvaluator(), result.getFirst(), true, agentInstanceContext);
        }
        UniformPair<EventBean[]> results = resultSetProcessor.processJoinResult(result.getFirst(), null, true);

        EventBean[] distinct = EventBeanUtility.getDistinctByProp(results.getFirst(), select.getDistinctKeyGetter());

        return new EPPreparedQueryResult(resultSetProcessor.getResultEventType(), distinct);
    }
}
