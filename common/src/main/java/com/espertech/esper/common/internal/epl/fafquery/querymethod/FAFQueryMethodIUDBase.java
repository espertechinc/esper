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

import com.espertech.esper.common.client.EPException;
import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.context.ContextPartitionSelector;
import com.espertech.esper.common.internal.context.mgr.ContextManagementService;
import com.espertech.esper.common.internal.context.module.StatementAIFactoryAssignmentsImpl;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.context.util.AgentInstanceStopCallback;
import com.espertech.esper.common.internal.context.util.InternalEventRouteDest;
import com.espertech.esper.common.internal.context.util.StatementContextRuntimeServices;
import com.espertech.esper.common.internal.epl.fafquery.processor.FireAndForgetInstance;
import com.espertech.esper.common.internal.epl.fafquery.processor.FireAndForgetProcessor;
import com.espertech.esper.common.internal.epl.join.querygraph.QueryGraph;
import com.espertech.esper.common.internal.epl.subselect.SubSelectFactory;
import com.espertech.esper.common.internal.epl.subselect.SubSelectFactoryResult;
import com.espertech.esper.common.internal.epl.subselect.SubSelectHelperStart;
import com.espertech.esper.common.internal.epl.table.strategy.ExprTableEvalHelperStart;
import com.espertech.esper.common.internal.epl.table.strategy.ExprTableEvalStrategy;
import com.espertech.esper.common.internal.epl.table.strategy.ExprTableEvalStrategyFactory;
import com.espertech.esper.common.internal.util.CollectionUtil;

import java.lang.annotation.Annotation;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.espertech.esper.common.internal.epl.fafquery.querymethod.FAFQueryMethodUtil.initializeSubselects;

/**
 * Starts and provides the stop method for EPL statements.
 */
public abstract class FAFQueryMethodIUDBase implements FAFQueryMethod {
    private String contextName;
    private FireAndForgetProcessor processor;
    private InternalEventRouteDest internalEventRouteDest;
    protected QueryGraph queryGraph;
    private Annotation[] annotations;
    private Map<Integer, ExprTableEvalStrategyFactory> tableAccesses;
    private boolean hasTableAccess;
    private Map<Integer, SubSelectFactory> subselects;

    public void setContextName(String contextName) {
        this.contextName = contextName;
    }

    public void setProcessor(FireAndForgetProcessor processor) {
        this.processor = processor;
    }

    public void setInternalEventRouteDest(InternalEventRouteDest internalEventRouteDest) {
        this.internalEventRouteDest = internalEventRouteDest;
    }

    public void setQueryGraph(QueryGraph queryGraph) {
        this.queryGraph = queryGraph;
    }

    public void setAnnotations(Annotation[] annotations) {
        this.annotations = annotations;
    }

    public void setTableAccesses(Map<Integer, ExprTableEvalStrategyFactory> tableAccesses) {
        this.tableAccesses = tableAccesses;
    }

    public void setHasTableAccess(boolean hasTableAccess) {
        this.hasTableAccess = hasTableAccess;
    }

    public Map<Integer, SubSelectFactory> getSubselects() {
        return subselects;
    }

    public void setSubselects(Map<Integer, SubSelectFactory> subselects) {
        this.subselects = subselects;
    }

    protected abstract EventBean[] execute(FireAndForgetInstance fireAndForgetProcessorInstance);

    public void ready(StatementContextRuntimeServices services) {
        if (!subselects.isEmpty()) {
            initializeSubselects(services, annotations, subselects);
        }
    }

    public EPPreparedQueryResult execute(AtomicBoolean serviceStatusProvider, FAFQueryMethodAssignerSetter assignerSetter, ContextPartitionSelector[] contextPartitionSelectors, ContextManagementService contextManagementService) {
        if (!serviceStatusProvider.get()) {
            throw FAFQueryMethodUtil.runtimeDestroyed();
        }
        try {
            if (contextPartitionSelectors != null && contextPartitionSelectors.length != 1) {
                throw new IllegalArgumentException("Number of context partition selectors must be one");
            }
            ContextPartitionSelector optionalSingleSelector = contextPartitionSelectors != null && contextPartitionSelectors.length > 0 ? contextPartitionSelectors[0] : null;

            // validate context
            if (processor.getContextName() != null &&
                    contextName != null &&
                    !processor.getContextName().equals(contextName)) {
                throw new EPException("Context for named window is '" + processor.getContextName() + "' and query specifies context '" + contextName + "'");
            }

            // handle non-specified context
            if (contextName == null) {
                FireAndForgetInstance processorInstance = processor.getProcessorInstanceNoContext();
                if (processorInstance != null) {
                    assign(processorInstance.getAgentInstanceContext(), assignerSetter);
                    EventBean[] rows = execute(processorInstance);
                    if (rows != null && rows.length > 0) {
                        dispatch();
                    }
                    return new EPPreparedQueryResult(processor.getEventTypePublic(), rows);
                }
            }

            // context partition runtime query
            Collection<Integer> agentInstanceIds = FAFQueryMethodUtil.agentInstanceIds(processor, optionalSingleSelector, contextManagementService);

            // collect events and agent instances
            if (agentInstanceIds.isEmpty()) {
                return new EPPreparedQueryResult(processor.getEventTypeResultSetProcessor(), CollectionUtil.EVENTBEANARRAY_EMPTY);
            }

            if (agentInstanceIds.size() == 1) {
                int agentInstanceId = agentInstanceIds.iterator().next();
                FireAndForgetInstance processorInstance = processor.getProcessorInstanceContextById(agentInstanceId);
                assign(processorInstance.getAgentInstanceContext(), assignerSetter);
                EventBean[] rows = execute(processorInstance);
                if (rows.length > 0) {
                    dispatch();
                }
                return new EPPreparedQueryResult(processor.getEventTypeResultSetProcessor(), rows);
            }

            ArrayDeque<EventBean> allRows = new ArrayDeque<EventBean>();
            for (int agentInstanceId : agentInstanceIds) {
                FireAndForgetInstance processorInstance = processor.getProcessorInstanceContextById(agentInstanceId);
                if (processorInstance != null) {
                    assign(processorInstance.getAgentInstanceContext(), assignerSetter);
                    EventBean[] rows = execute(processorInstance);
                    allRows.addAll(Arrays.asList(rows));
                }
            }
            if (allRows.size() > 0) {
                dispatch();
            }
            return new EPPreparedQueryResult(processor.getEventTypeResultSetProcessor(), allRows.toArray(new EventBean[allRows.size()]));
        } finally {
            if (hasTableAccess) {
                processor.getStatementContext().getTableExprEvaluatorContext().releaseAcquiredLocks();
            }
        }
    }

    /**
     * Returns the event type of the prepared statement.
     *
     * @return event type
     */
    public EventType getEventType() {
        return processor.getEventTypeResultSetProcessor();
    }

    public Annotation[] getAnnotations() {
        return annotations;
    }

    protected void dispatch() {
        internalEventRouteDest.processThreadWorkQueue();
    }

    private void assign(AgentInstanceContext agentInstanceContext, FAFQueryMethodAssignerSetter assignerSetter) {
        // start table-access
        Map<Integer, ExprTableEvalStrategy> tableAccessEvals = ExprTableEvalHelperStart.startTableAccess(tableAccesses, agentInstanceContext);

        // start subselects
        List<AgentInstanceStopCallback> subselectStopCallbacks = new ArrayList<>(2);
        Map<Integer, SubSelectFactoryResult> subselectActivations = SubSelectHelperStart.startSubselects(subselects, agentInstanceContext, subselectStopCallbacks, false);

        // assign
        assignerSetter.assign(new StatementAIFactoryAssignmentsImpl(null, null, null, subselectActivations, tableAccessEvals, null));
    }
}
