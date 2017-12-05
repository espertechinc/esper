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

import com.espertech.esper.client.EPException;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.client.context.ContextPartitionSelector;
import com.espertech.esper.core.service.EPPreparedQueryResult;
import com.espertech.esper.core.service.EPServicesContext;
import com.espertech.esper.core.service.StatementContext;
import com.espertech.esper.core.service.speccompiled.StatementSpecCompiled;
import com.espertech.esper.epl.core.streamtype.StreamTypeServiceImpl;
import com.espertech.esper.epl.expression.core.ExprValidationException;
import com.espertech.esper.epl.join.hint.ExcludePlanHint;
import com.espertech.esper.epl.join.plan.QueryGraph;
import com.espertech.esper.epl.spec.*;
import com.espertech.esper.epl.util.EPLValidationUtil;
import com.espertech.esper.util.AuditPath;
import com.espertech.esper.util.CollectionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collection;

/**
 * Starts and provides the stop method for EPL statements.
 */
public abstract class EPPreparedExecuteIUDSingleStream implements EPPreparedExecuteMethod {
    private static final Logger QUERY_PLAN_LOG = LoggerFactory.getLogger(AuditPath.QUERYPLAN_LOG);
    private static final Logger log = LoggerFactory.getLogger(EPPreparedExecuteIUDSingleStream.class);

    protected final StatementSpecCompiled statementSpec;
    protected final FireAndForgetProcessor processor;
    protected final EPServicesContext services;
    protected final EPPreparedExecuteIUDSingleStreamExec executor;
    protected final StatementContext statementContext;
    protected boolean hasTableAccess;

    public abstract EPPreparedExecuteIUDSingleStreamExec getExecutor(QueryGraph queryGraph, String aliasName)
            throws ExprValidationException;

    /**
     * Ctor.
     *
     * @param statementSpec    is a container for the definition of all statement constructs that
     *                         may have been used in the statement, i.e. if defines the select clauses, insert into, outer joins etc.
     * @param services         is the service instances for dependency injection
     * @param statementContext is statement-level information and statement services
     * @throws com.espertech.esper.epl.expression.core.ExprValidationException if the preparation failed
     */
    public EPPreparedExecuteIUDSingleStream(StatementSpecCompiled statementSpec,
                                            EPServicesContext services,
                                            StatementContext statementContext)
            throws ExprValidationException {
        boolean queryPlanLogging = services.getConfigSnapshot().getEngineDefaults().getLogging().isEnableQueryPlan();
        if (queryPlanLogging) {
            QUERY_PLAN_LOG.info("Query plans for Fire-and-forget query '" + statementContext.getExpression() + "'");
        }

        this.hasTableAccess = statementSpec.getIntoTableSpec() != null ||
                (statementSpec.getTableNodes() != null && statementSpec.getTableNodes().length > 0);
        if (statementSpec.getInsertIntoDesc() != null && services.getTableService().getTableMetadata(statementSpec.getInsertIntoDesc().getEventTypeName()) != null) {
            hasTableAccess = true;
        }
        if (statementSpec.getFireAndForgetSpec() instanceof FireAndForgetSpecUpdate ||
                statementSpec.getFireAndForgetSpec() instanceof FireAndForgetSpecDelete) {
            hasTableAccess |= statementSpec.getStreamSpecs()[0] instanceof TableQueryStreamSpec;
        }

        this.statementSpec = statementSpec;
        this.services = services;
        this.statementContext = statementContext;

        // validate general FAF criteria
        EPPreparedExecuteMethodHelper.validateFAFQuery(statementSpec);

        // obtain processor
        StreamSpecCompiled streamSpec = statementSpec.getStreamSpecs()[0];
        processor = FireAndForgetProcessorFactory.validateResolveProcessor(streamSpec, services);

        // obtain name and type
        String processorName = processor.getNamedWindowOrTableName();
        EventType eventType = processor.getEventTypeResultSetProcessor();

        // determine alias
        String aliasName = processorName;
        if (streamSpec.getOptionalStreamName() != null) {
            aliasName = streamSpec.getOptionalStreamName();
        }

        // compile filter to optimize access to named window
        StreamTypeServiceImpl typeService = new StreamTypeServiceImpl(new EventType[]{eventType}, new String[]{aliasName}, new boolean[]{true}, services.getEngineURI(), true, false);
        ExcludePlanHint excludePlanHint = ExcludePlanHint.getHint(typeService.getStreamNames(), statementContext);
        QueryGraph queryGraph = new QueryGraph(1, excludePlanHint, false);
        if (statementSpec.getFilterRootNode() != null) {
            EPLValidationUtil.validateFilterWQueryGraphSafe(queryGraph, statementSpec.getFilterRootNode(), statementContext, typeService);
        }

        // validate expressions
        EPStatementStartMethodHelperValidate.validateNodes(statementSpec, statementContext, typeService, null);

        // get executor
        executor = getExecutor(queryGraph, aliasName);
    }

    /**
     * Returns the event type of the prepared statement.
     *
     * @return event type
     */
    public EventType getEventType() {
        return processor.getEventTypeResultSetProcessor();
    }

    /**
     * Executes the prepared query.
     *
     * @return query results
     */
    public EPPreparedQueryResult execute(ContextPartitionSelector[] contextPartitionSelectors) {
        try {
            if (contextPartitionSelectors != null && contextPartitionSelectors.length != 1) {
                throw new IllegalArgumentException("Number of context partition selectors must be one");
            }
            ContextPartitionSelector optionalSingleSelector = contextPartitionSelectors != null && contextPartitionSelectors.length > 0 ? contextPartitionSelectors[0] : null;

            // validate context
            if (processor.getContextName() != null &&
                    statementSpec.getOptionalContextName() != null &&
                    !processor.getContextName().equals(statementSpec.getOptionalContextName())) {
                throw new EPException("Context for named window is '" + processor.getContextName() + "' and query specifies context '" + statementSpec.getOptionalContextName() + "'");
            }

            // handle non-specified context
            if (statementSpec.getOptionalContextName() == null) {
                FireAndForgetInstance processorInstance = processor.getProcessorInstanceNoContext();
                if (processorInstance != null) {
                    EventBean[] rows = executor.execute(processorInstance);
                    if (rows != null && rows.length > 0) {
                        dispatch();
                    }
                    return new EPPreparedQueryResult(processor.getEventTypePublic(), rows);
                }
            }

            // context partition runtime query
            Collection<Integer> agentInstanceIds = EPPreparedExecuteMethodHelper.getAgentInstanceIds(processor, optionalSingleSelector, services.getContextManagementService(), processor.getContextName());

            // collect events and agent instances
            if (agentInstanceIds.isEmpty()) {
                return new EPPreparedQueryResult(processor.getEventTypeResultSetProcessor(), CollectionUtil.EVENTBEANARRAY_EMPTY);
            }

            if (agentInstanceIds.size() == 1) {
                int agentInstanceId = agentInstanceIds.iterator().next();
                FireAndForgetInstance processorInstance = processor.getProcessorInstanceContextById(agentInstanceId);
                EventBean[] rows = executor.execute(processorInstance);
                if (rows.length > 0) {
                    dispatch();
                }
                return new EPPreparedQueryResult(processor.getEventTypeResultSetProcessor(), rows);
            }

            ArrayDeque<EventBean> allRows = new ArrayDeque<EventBean>();
            for (int agentInstanceId : agentInstanceIds) {
                FireAndForgetInstance processorInstance = processor.getProcessorInstanceContextById(agentInstanceId);
                if (processorInstance != null) {
                    EventBean[] rows = executor.execute(processorInstance);
                    allRows.addAll(Arrays.asList(rows));
                }
            }
            if (allRows.size() > 0) {
                dispatch();
            }
            return new EPPreparedQueryResult(processor.getEventTypeResultSetProcessor(), allRows.toArray(new EventBean[allRows.size()]));
        } finally {
            if (hasTableAccess) {
                services.getTableService().getTableExprEvaluatorContext().releaseAcquiredLocks();
            }
        }
    }

    protected void dispatch() {
        services.getInternalEventEngineRouteDest().processThreadWorkQueue();
    }
}
