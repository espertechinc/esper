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

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.client.context.ContextPartitionSelector;
import com.espertech.esper.collection.MultiKey;
import com.espertech.esper.collection.UniformPair;
import com.espertech.esper.core.context.mgr.ContextPropertyRegistryImpl;
import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.core.service.*;
import com.espertech.esper.epl.core.resultset.core.ResultSetProcessor;
import com.espertech.esper.epl.core.resultset.core.ResultSetProcessorFactoryDesc;
import com.espertech.esper.epl.core.resultset.core.ResultSetProcessorFactoryFactory;
import com.espertech.esper.epl.core.streamtype.StreamTypeService;
import com.espertech.esper.epl.core.streamtype.StreamTypeServiceImpl;
import com.espertech.esper.epl.expression.core.*;
import com.espertech.esper.epl.expression.table.ExprTableAccessNode;
import com.espertech.esper.epl.join.base.*;
import com.espertech.esper.epl.join.hint.ExcludePlanHint;
import com.espertech.esper.epl.join.plan.FilterExprAnalyzer;
import com.espertech.esper.epl.join.plan.OuterJoinAnalyzer;
import com.espertech.esper.epl.join.plan.QueryGraph;
import com.espertech.esper.epl.spec.NamedWindowConsumerStreamSpec;
import com.espertech.esper.core.service.speccompiled.StatementSpecCompiled;
import com.espertech.esper.epl.spec.StreamSpecCompiled;
import com.espertech.esper.epl.spec.TableQueryStreamSpec;
import com.espertech.esper.epl.util.ExprNodeUtilityRich;
import com.espertech.esper.epl.virtualdw.VirtualDWView;
import com.espertech.esper.epl.virtualdw.VirtualDWViewProviderForAgentInstance;
import com.espertech.esper.event.EventBeanReader;
import com.espertech.esper.event.EventBeanReaderDefaultImpl;
import com.espertech.esper.event.EventBeanUtility;
import com.espertech.esper.event.EventTypeSPI;
import com.espertech.esper.util.AuditPath;
import com.espertech.esper.view.Viewable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Starts and provides the stop method for EPL statements.
 */
public class EPPreparedExecuteMethodQuery implements EPPreparedExecuteMethod {
    private static final Logger QUERY_PLAN_LOG = LoggerFactory.getLogger(AuditPath.QUERYPLAN_LOG);
    private static final Logger log = LoggerFactory.getLogger(EPPreparedExecuteMethodQuery.class);

    private final StatementSpecCompiled statementSpec;
    private final EventType resultEventType;
    private final ResultSetProcessor resultSetProcessor;
    private final FireAndForgetProcessor[] processors;
    private final AgentInstanceContext agentInstanceContext;
    private final EPServicesContext services;
    private EventBeanReader eventBeanReader;
    private JoinSetComposerPrototype joinSetComposerPrototype;
    private final QueryGraph queryGraph;
    private boolean hasTableAccess;

    /**
     * Ctor.
     *
     * @param statementSpec    is a container for the definition of all statement constructs that
     *                         may have been used in the statement, i.e. if defines the select clauses, insert into, outer joins etc.
     * @param services         is the service instances for dependency injection
     * @param statementContext is statement-level information and statement services
     * @throws ExprValidationException if the preparation failed
     */
    public EPPreparedExecuteMethodQuery(StatementSpecCompiled statementSpec,
                                        EPServicesContext services,
                                        StatementContext statementContext)
            throws ExprValidationException {
        boolean queryPlanLogging = services.getConfigSnapshot().getEngineDefaults().getLogging().isEnableQueryPlan();
        if (queryPlanLogging) {
            QUERY_PLAN_LOG.info("Query plans for Fire-and-forget query '" + statementContext.getExpression() + "'");
        }

        this.hasTableAccess = statementSpec.getTableNodes() != null && statementSpec.getTableNodes().length > 0;
        for (StreamSpecCompiled streamSpec : statementSpec.getStreamSpecs()) {
            hasTableAccess |= streamSpec instanceof TableQueryStreamSpec;
        }

        this.statementSpec = statementSpec;
        this.services = services;

        EPPreparedExecuteMethodHelper.validateFAFQuery(statementSpec);

        int numStreams = statementSpec.getStreamSpecs().length;
        EventType[] typesPerStream = new EventType[numStreams];
        String[] namesPerStream = new String[numStreams];
        processors = new FireAndForgetProcessor[numStreams];
        agentInstanceContext = new AgentInstanceContext(statementContext, null, -1, null, null, statementContext.getDefaultAgentInstanceScriptContext());

        // resolve types and processors
        for (int i = 0; i < numStreams; i++) {
            final StreamSpecCompiled streamSpec = statementSpec.getStreamSpecs()[i];
            processors[i] = FireAndForgetProcessorFactory.validateResolveProcessor(streamSpec, services);

            String streamName = processors[i].getNamedWindowOrTableName();
            if (streamSpec.getOptionalStreamName() != null) {
                streamName = streamSpec.getOptionalStreamName();
            }
            namesPerStream[i] = streamName;
            typesPerStream[i] = processors[i].getEventTypeResultSetProcessor();
        }

        // compile filter to optimize access to named window
        boolean optionalStreamsIfAny = OuterJoinAnalyzer.optionalStreamsIfAny(statementSpec.getOuterJoinDescList());
        StreamTypeServiceImpl types = new StreamTypeServiceImpl(typesPerStream, namesPerStream, new boolean[numStreams], services.getEngineURI(), false, optionalStreamsIfAny);
        ExcludePlanHint excludePlanHint = ExcludePlanHint.getHint(types.getStreamNames(), statementContext);
        queryGraph = new QueryGraph(numStreams, excludePlanHint, false);
        if (statementSpec.getFilterRootNode() != null) {
            for (int i = 0; i < numStreams; i++) {
                try {
                    ExprEvaluatorContextStatement evaluatorContextStmt = new ExprEvaluatorContextStatement(statementContext, false);
                    ExprValidationContext validationContext = new ExprValidationContext(types, statementContext.getEngineImportService(), statementContext.getStatementExtensionServicesContext(), null, statementContext.getTimeProvider(), statementContext.getVariableService(), statementContext.getTableService(), evaluatorContextStmt, statementContext.getEventAdapterService(), statementContext.getStatementName(), statementContext.getStatementId(), statementContext.getAnnotations(), statementContext.getContextDescriptor(), false, false, true, false, null, true);
                    ExprNode validated = ExprNodeUtilityRich.getValidatedSubtree(ExprNodeOrigin.FILTER, statementSpec.getFilterRootNode(), validationContext);
                    FilterExprAnalyzer.analyze(validated, queryGraph, false);
                } catch (Exception ex) {
                    log.warn("Unexpected exception analyzing filter paths: " + ex.getMessage(), ex);
                }
            }
        }

        // obtain result set processor
        boolean[] isIStreamOnly = new boolean[namesPerStream.length];
        Arrays.fill(isIStreamOnly, true);
        StreamTypeService typeService = new StreamTypeServiceImpl(typesPerStream, namesPerStream, isIStreamOnly, services.getEngineURI(), true, optionalStreamsIfAny);
        EPStatementStartMethodHelperValidate.validateNodes(statementSpec, statementContext, typeService, null);

        ResultSetProcessorFactoryDesc resultSetProcessorPrototype = ResultSetProcessorFactoryFactory.getProcessorPrototype(statementSpec, statementContext, typeService, null, new boolean[0], true, ContextPropertyRegistryImpl.EMPTY_REGISTRY, null, services.getConfigSnapshot(), services.getResultSetProcessorHelperFactory(), true, false);
        resultEventType = resultSetProcessorPrototype.getResultEventType();
        resultSetProcessor = EPStatementStartMethodHelperAssignExpr.getAssignResultSetProcessor(agentInstanceContext, resultSetProcessorPrototype, false, null, true);

        if (statementSpec.getSelectClauseSpec().isDistinct()) {
            if (resultEventType instanceof EventTypeSPI) {
                eventBeanReader = ((EventTypeSPI) resultEventType).getReader();
            }
            if (eventBeanReader == null) {
                eventBeanReader = new EventBeanReaderDefaultImpl(resultEventType);
            }
        }

        // check context partition use
        if (statementSpec.getOptionalContextName() != null) {
            if (numStreams > 1) {
                throw new ExprValidationException("Joins in runtime queries for context partitions are not supported");
            }
        }

        // plan joins or simple queries
        if (numStreams > 1) {
            StreamJoinAnalysisResult streamJoinAnalysisResult = new StreamJoinAnalysisResult(numStreams);
            Arrays.fill(streamJoinAnalysisResult.getNamedWindow(), true);
            for (int i = 0; i < numStreams; i++) {
                final FireAndForgetInstance processorInstance = processors[i].getProcessorInstance(agentInstanceContext);
                if (processors[i].isVirtualDataWindow()) {
                    streamJoinAnalysisResult.getViewExternal()[i] = new VirtualDWViewProviderForAgentInstance() {
                        public VirtualDWView getView(AgentInstanceContext agentInstanceContext) {
                            return processorInstance.getVirtualDataWindow();
                        }
                    };
                }
                String[][] uniqueIndexes = processors[i].getUniqueIndexes(processorInstance);
                streamJoinAnalysisResult.getUniqueKeys()[i] = uniqueIndexes;
            }

            boolean hasAggregations = !resultSetProcessorPrototype.getAggregationServiceFactoryDesc().getExpressions().isEmpty();
            joinSetComposerPrototype = JoinSetComposerPrototypeFactory.makeComposerPrototype(null, -1,
                    statementSpec.getOuterJoinDescList(), statementSpec.getFilterRootNode(), typesPerStream, namesPerStream,
                    streamJoinAnalysisResult, queryPlanLogging, statementContext, new HistoricalViewableDesc(numStreams), agentInstanceContext, false, hasAggregations, services.getTableService(), true, services.getEventTableIndexService().allowInitIndex(false));
        }
    }

    /**
     * Returns the event type of the prepared statement.
     *
     * @return event type
     */
    public EventType getEventType() {
        return resultEventType;
    }

    /**
     * Executes the prepared query.
     *
     * @return query results
     */
    public EPPreparedQueryResult execute(ContextPartitionSelector[] contextPartitionSelectors) {
        try {
            int numStreams = processors.length;

            if (contextPartitionSelectors != null && contextPartitionSelectors.length != numStreams) {
                throw new IllegalArgumentException("Number of context partition selectors does not match the number of named windows in the from-clause");
            }

            // handle non-context case
            if (statementSpec.getOptionalContextName() == null) {

                Collection<EventBean>[] snapshots = new Collection[numStreams];
                for (int i = 0; i < numStreams; i++) {

                    ContextPartitionSelector selector = contextPartitionSelectors == null ? null : contextPartitionSelectors[i];
                    snapshots[i] = getStreamFilterSnapshot(i, selector);
                }

                resultSetProcessor.clear();
                return process(snapshots);
            }

            List<ContextPartitionResult> contextPartitionResults = new ArrayList<ContextPartitionResult>();
            ContextPartitionSelector singleSelector = contextPartitionSelectors != null && contextPartitionSelectors.length > 0 ? contextPartitionSelectors[0] : null;

            // context partition runtime query
            Collection<Integer> agentInstanceIds = EPPreparedExecuteMethodHelper.getAgentInstanceIds(processors[0], singleSelector, services.getContextManagementService(), statementSpec.getOptionalContextName());

            // collect events and agent instances
            for (int agentInstanceId : agentInstanceIds) {
                FireAndForgetInstance processorInstance = processors[0].getProcessorInstanceContextById(agentInstanceId);
                if (processorInstance != null) {
                    EPPreparedExecuteTableHelper.assignTableAccessStrategies(services, statementSpec.getTableNodes(), processorInstance.getAgentInstanceContext());
                    Collection<EventBean> coll = processorInstance.snapshotBestEffort(this, queryGraph, statementSpec.getAnnotations());
                    contextPartitionResults.add(new ContextPartitionResult(coll, processorInstance.getAgentInstanceContext()));
                }
            }

            // process context partitions
            ArrayDeque<EventBean[]> events = new ArrayDeque<EventBean[]>();
            for (ContextPartitionResult contextPartitionResult : contextPartitionResults) {
                Collection<EventBean> snapshot = contextPartitionResult.getEvents();
                if (statementSpec.getFilterRootNode() != null) {
                    snapshot = getFiltered(snapshot, Collections.singletonList(statementSpec.getFilterRootNode()));
                }
                EventBean[] rows = snapshot.toArray(new EventBean[snapshot.size()]);
                resultSetProcessor.setAgentInstanceContext(contextPartitionResult.getContext());
                UniformPair<EventBean[]> results = resultSetProcessor.processViewResult(rows, null, true);
                if (results != null && results.getFirst() != null && results.getFirst().length > 0) {
                    events.add(results.getFirst());
                }
            }
            return new EPPreparedQueryResult(resultEventType, EventBeanUtility.flatten(events));
        } finally {
            if (hasTableAccess) {
                services.getTableService().getTableExprEvaluatorContext().releaseAcquiredLocks();
            }
        }
    }

    private Collection<EventBean> getStreamFilterSnapshot(int streamNum, ContextPartitionSelector contextPartitionSelector) {
        final StreamSpecCompiled streamSpec = statementSpec.getStreamSpecs()[streamNum];
        List<ExprNode> filterExpressions = Collections.emptyList();
        if (streamSpec instanceof NamedWindowConsumerStreamSpec) {
            NamedWindowConsumerStreamSpec namedSpec = (NamedWindowConsumerStreamSpec) streamSpec;
            filterExpressions = namedSpec.getFilterExpressions();
        } else {
            TableQueryStreamSpec tableSpec = (TableQueryStreamSpec) streamSpec;
            filterExpressions = tableSpec.getFilterExpressions();
        }

        FireAndForgetProcessor fireAndForgetProcessor = processors[streamNum];

        // handle the case of a single or matching agent instance
        FireAndForgetInstance processorInstance = fireAndForgetProcessor.getProcessorInstance(agentInstanceContext);
        if (processorInstance != null) {
            EPPreparedExecuteTableHelper.assignTableAccessStrategies(services, statementSpec.getTableNodes(), agentInstanceContext);
            return getStreamSnapshotInstance(streamNum, filterExpressions, processorInstance);
        }

        // context partition runtime query
        Collection<Integer> contextPartitions = EPPreparedExecuteMethodHelper.getAgentInstanceIds(fireAndForgetProcessor, contextPartitionSelector, services.getContextManagementService(), fireAndForgetProcessor.getContextName());

        // collect events
        ArrayDeque<EventBean> events = new ArrayDeque<EventBean>();
        for (int agentInstanceId : contextPartitions) {
            processorInstance = fireAndForgetProcessor.getProcessorInstanceContextById(agentInstanceId);
            if (processorInstance != null) {
                Collection<EventBean> coll = processorInstance.snapshotBestEffort(this, queryGraph, statementSpec.getAnnotations());
                events.addAll(coll);
            }
        }
        return events;
    }

    private Collection<EventBean> getStreamSnapshotInstance(int streamNum, List<ExprNode> filterExpressions, FireAndForgetInstance processorInstance) {
        Collection<EventBean> coll = processorInstance.snapshotBestEffort(this, queryGraph, statementSpec.getAnnotations());
        if (filterExpressions.size() != 0) {
            coll = getFiltered(coll, filterExpressions);
        }
        return coll;
    }

    private EPPreparedQueryResult process(Collection<EventBean>[] snapshots) {

        int numStreams = processors.length;

        UniformPair<EventBean[]> results;
        if (numStreams == 1) {
            if (statementSpec.getFilterRootNode() != null) {
                snapshots[0] = getFiltered(snapshots[0], Arrays.asList(statementSpec.getFilterRootNode()));
            }
            EventBean[] rows = snapshots[0].toArray(new EventBean[snapshots[0].size()]);
            results = resultSetProcessor.processViewResult(rows, null, true);
        } else {
            Viewable[] viewablePerStream = new Viewable[numStreams];
            for (int i = 0; i < numStreams; i++) {
                FireAndForgetInstance instance = processors[i].getProcessorInstance(agentInstanceContext);
                if (instance == null) {
                    throw new UnsupportedOperationException("Joins against named windows that are under context are not supported");
                }
                viewablePerStream[i] = instance.getTailViewInstance();
            }

            JoinSetComposerDesc joinSetComposerDesc = joinSetComposerPrototype.create(viewablePerStream, true, agentInstanceContext, false);
            JoinSetComposer joinComposer = joinSetComposerDesc.getJoinSetComposer();
            JoinSetFilter joinFilter;
            if (joinSetComposerDesc.getPostJoinFilterEvaluator() != null) {
                joinFilter = new JoinSetFilter(joinSetComposerDesc.getPostJoinFilterEvaluator());
            } else {
                joinFilter = null;
            }

            EventBean[][] oldDataPerStream = new EventBean[numStreams][];
            EventBean[][] newDataPerStream = new EventBean[numStreams][];
            for (int i = 0; i < numStreams; i++) {
                newDataPerStream[i] = snapshots[i].toArray(new EventBean[snapshots[i].size()]);
            }
            UniformPair<Set<MultiKey<EventBean>>> result = joinComposer.join(newDataPerStream, oldDataPerStream, agentInstanceContext);
            if (joinFilter != null) {
                joinFilter.process(result.getFirst(), null, agentInstanceContext);
            }
            results = resultSetProcessor.processJoinResult(result.getFirst(), null, true);
        }

        EventBean[] queryResult = results == null ? null : results.getFirst();

        if (queryResult != null && statementSpec.getSelectClauseSpec().isDistinct()) {
            queryResult = EventBeanUtility.getDistinctByProp(queryResult, eventBeanReader);
        }

        return new EPPreparedQueryResult(resultEventType, queryResult);
    }

    private Collection<EventBean> getFiltered(Collection<EventBean> snapshot, List<ExprNode> filterExpressions) {
        ArrayDeque<EventBean> deque = new ArrayDeque<EventBean>(Math.min(snapshot.size(), 16));
        ExprNodeUtilityCore.applyFilterExpressionsIterable(snapshot, filterExpressions, agentInstanceContext, deque);
        return deque;
    }

    public EPServicesContext getServices() {
        return services;
    }

    public ExprTableAccessNode[] getTableNodes() {
        return statementSpec.getTableNodes();
    }

    public AgentInstanceContext getAgentInstanceContext() {
        return agentInstanceContext;
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
