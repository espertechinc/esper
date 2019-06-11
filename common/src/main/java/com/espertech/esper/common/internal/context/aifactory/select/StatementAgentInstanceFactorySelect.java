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
package com.espertech.esper.common.internal.context.aifactory.select;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.collection.Pair;
import com.espertech.esper.common.internal.context.activator.ViewableActivationResult;
import com.espertech.esper.common.internal.context.activator.ViewableActivator;
import com.espertech.esper.common.internal.context.activator.ViewableActivatorNamedWindow;
import com.espertech.esper.common.internal.context.aifactory.core.StatementAgentInstanceFactory;
import com.espertech.esper.common.internal.context.aifactory.core.StatementAgentInstanceFactoryUtil;
import com.espertech.esper.common.internal.context.airegistry.AIRegistryRequirementSubquery;
import com.espertech.esper.common.internal.context.airegistry.AIRegistryRequirements;
import com.espertech.esper.common.internal.context.util.*;
import com.espertech.esper.common.internal.epl.agg.core.AggregationService;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityEvaluate;
import com.espertech.esper.common.internal.epl.expression.prior.PriorEvalStrategy;
import com.espertech.esper.common.internal.epl.join.base.*;
import com.espertech.esper.common.internal.epl.namedwindow.consume.NamedWindowConsumerView;
import com.espertech.esper.common.internal.epl.output.core.OutputProcessView;
import com.espertech.esper.common.internal.epl.output.core.OutputProcessViewFactoryProvider;
import com.espertech.esper.common.internal.epl.pattern.core.EvalRootMatchRemover;
import com.espertech.esper.common.internal.epl.pattern.core.EvalRootState;
import com.espertech.esper.common.internal.epl.prior.PriorHelper;
import com.espertech.esper.common.internal.epl.resultset.core.ResultSetProcessor;
import com.espertech.esper.common.internal.epl.resultset.core.ResultSetProcessorFactoryProvider;
import com.espertech.esper.common.internal.epl.rowrecog.core.RowRecogHelper;
import com.espertech.esper.common.internal.epl.rowrecog.core.RowRecogNFAViewFactory;
import com.espertech.esper.common.internal.epl.rowrecog.core.RowRecogNFAViewService;
import com.espertech.esper.common.internal.epl.rowrecog.core.RowRecogPreviousStrategy;
import com.espertech.esper.common.internal.epl.subselect.SubSelectFactory;
import com.espertech.esper.common.internal.epl.subselect.SubSelectFactoryResult;
import com.espertech.esper.common.internal.epl.subselect.SubSelectHelperStart;
import com.espertech.esper.common.internal.epl.table.strategy.ExprTableEvalHelperStart;
import com.espertech.esper.common.internal.epl.table.strategy.ExprTableEvalStrategy;
import com.espertech.esper.common.internal.epl.table.strategy.ExprTableEvalStrategyFactory;
import com.espertech.esper.common.internal.util.CollectionUtil;
import com.espertech.esper.common.internal.view.access.ViewResourceDelegateDesc;
import com.espertech.esper.common.internal.view.core.*;
import com.espertech.esper.common.internal.view.filter.FilterExprView;
import com.espertech.esper.common.internal.view.previous.PreviousGetterStrategy;
import com.espertech.esper.common.internal.view.util.BufferView;

import java.util.*;

public class StatementAgentInstanceFactorySelect implements StatementAgentInstanceFactory {

    private String[] streamNames;
    private ViewableActivator[] viewableActivators;
    private ResultSetProcessorFactoryProvider resultSetProcessorFactoryProvider;
    private ViewFactory[][] viewFactories;
    private ExprEvaluator whereClauseEvaluator;
    private String whereClauseEvaluatorTextForAudit;
    private OutputProcessViewFactoryProvider outputProcessViewFactoryProvider;
    private ViewResourceDelegateDesc[] viewResourceDelegates;
    private JoinSetComposerPrototype joinSetComposerPrototype;
    private Map<Integer, SubSelectFactory> subselects;
    private Map<Integer, ExprTableEvalStrategyFactory> tableAccesses;
    private boolean orderByWithoutOutputRateLimit;
    private boolean unidirectionalJoin;

    public StatementAgentInstanceFactorySelect() {
    }

    public void setViewableActivators(ViewableActivator[] viewableActivators) {
        this.viewableActivators = viewableActivators;
    }

    public void setResultSetProcessorFactoryProvider(ResultSetProcessorFactoryProvider resultSetProcessorFactoryProvider) {
        this.resultSetProcessorFactoryProvider = resultSetProcessorFactoryProvider;
    }

    public void setViewFactories(ViewFactory[][] viewFactories) {
        this.viewFactories = viewFactories;
    }

    public void setOutputProcessViewFactoryProvider(OutputProcessViewFactoryProvider outputProcessViewFactoryProvider) {
        this.outputProcessViewFactoryProvider = outputProcessViewFactoryProvider;
    }

    public void setViewResourceDelegates(ViewResourceDelegateDesc[] viewResourceDelegates) {
        this.viewResourceDelegates = viewResourceDelegates;
    }

    public void setWhereClauseEvaluator(ExprEvaluator whereClauseEvaluator) {
        this.whereClauseEvaluator = whereClauseEvaluator;
    }

    public void setStreamNames(String[] streamNames) {
        this.streamNames = streamNames;
    }

    public void setJoinSetComposerPrototype(JoinSetComposerPrototype joinSetComposerPrototype) {
        this.joinSetComposerPrototype = joinSetComposerPrototype;
    }

    public void setSubselects(Map<Integer, SubSelectFactory> subselects) {
        this.subselects = subselects;
    }

    public void setOrderByWithoutOutputRateLimit(boolean orderByWithoutOutputRateLimit) {
        this.orderByWithoutOutputRateLimit = orderByWithoutOutputRateLimit;
    }

    public void setUnidirectionalJoin(boolean unidirectionalJoin) {
        this.unidirectionalJoin = unidirectionalJoin;
    }

    public void setTableAccesses(Map<Integer, ExprTableEvalStrategyFactory> tableAccesses) {
        this.tableAccesses = tableAccesses;
    }

    public void setWhereClauseEvaluatorTextForAudit(String whereClauseEvaluatorTextForAudit) {
        this.whereClauseEvaluatorTextForAudit = whereClauseEvaluatorTextForAudit;
    }

    public void statementCreate(StatementContext statementContext) {
    }

    public void statementDestroy(StatementContext statementContext) {
    }

    public StatementAgentInstanceLock obtainAgentInstanceLock(StatementContext statementContext, int agentInstanceId) {
        return AgentInstanceUtil.newLock(statementContext);
    }

    public StatementAgentInstanceFactorySelectResult newContext(AgentInstanceContext agentInstanceContext, boolean isRecoveringResilient) {
        List<AgentInstanceStopCallback> stopCallbacks = new ArrayList<>();
        List<StatementAgentInstancePreload> preloadList = new ArrayList<StatementAgentInstancePreload>();
        int numStreams = viewableActivators.length;

        // root activations
        ViewableActivationResult[] activationResults = new ViewableActivationResult[numStreams];
        Viewable[] eventStreamParentViewable = new Viewable[numStreams];
        EvalRootState[] patternRoots = new EvalRootState[numStreams];
        EvalRootMatchRemover evalRootMatchRemover = null;
        boolean suppressSameEventMatches = false;
        boolean discardPartialsOnMatch = false;

        for (int stream = 0; stream < numStreams; stream++) {
            ViewableActivationResult activationResult = viewableActivators[stream].activate(agentInstanceContext, false, isRecoveringResilient);
            stopCallbacks.add(activationResult.getStopCallback());
            activationResults[stream] = activationResult;
            eventStreamParentViewable[stream] = activationResult.getViewable();
            patternRoots[stream] = activationResult.getOptionalPatternRoot();
            suppressSameEventMatches = activationResult.isSuppressSameEventMatches();
            discardPartialsOnMatch = activationResult.isDiscardPartialsOnMatch();

            if (stream == 0) {
                evalRootMatchRemover = activationResult.getOptEvalRootMatchRemover();
            }
        }

        // create view factory chain context: holds stream-specific services
        AgentInstanceViewFactoryChainContext[] viewFactoryChainContexts = new AgentInstanceViewFactoryChainContext[numStreams];
        PriorEvalStrategy[] priorEvalStrategies = new PriorEvalStrategy[numStreams];
        PreviousGetterStrategy[] previousGetterStrategies = new PreviousGetterStrategy[numStreams];
        RowRecogPreviousStrategy rowRecogPreviousStrategy = null;

        for (int i = 0; i < numStreams; i++) {
            viewFactoryChainContexts[i] = AgentInstanceViewFactoryChainContext.create(viewFactories[i], agentInstanceContext, viewResourceDelegates[i]);
            priorEvalStrategies[i] = PriorHelper.toStrategy(viewFactoryChainContexts[i]);
            previousGetterStrategies[i] = viewFactoryChainContexts[i].getPreviousNodeGetter();
        }

        // materialize views
        Viewable[] topViews = new Viewable[numStreams];
        Viewable[] streamViews = new Viewable[numStreams];
        for (int stream = 0; stream < numStreams; stream++) {
            ViewablePair viewables = ViewFactoryUtil.materialize(viewFactories[stream], eventStreamParentViewable[stream], viewFactoryChainContexts[stream], stopCallbacks);
            topViews[stream] = viewables.getTop();
            streamViews[stream] = viewables.getLast();
        }

        // determine match-recognize "previous"-node strategy (none if not present, or one handling and number of nodes)
        RowRecogNFAViewService matchRecognize = RowRecogHelper.recursiveFindRegexService(topViews[0]);
        if (matchRecognize != null) {
            rowRecogPreviousStrategy = matchRecognize.getPreviousEvaluationStrategy();
            stopCallbacks.add(matchRecognize);
        }

        // start subselects
        Map<Integer, SubSelectFactoryResult> subselectActivations = SubSelectHelperStart.startSubselects(subselects, agentInstanceContext, stopCallbacks, isRecoveringResilient);

        // start table-access
        Map<Integer, ExprTableEvalStrategy> tableAccessEvals = ExprTableEvalHelperStart.startTableAccess(tableAccesses, agentInstanceContext);

        // result-set-processing
        Pair<ResultSetProcessor, AggregationService> processorPair = StatementAgentInstanceFactoryUtil.startResultSetAndAggregation(resultSetProcessorFactoryProvider, agentInstanceContext, false, null);
        stopCallbacks.add(new SelectStopCallback(processorPair));

        // join versus non-join
        JoinSetComposer joinSetComposer;
        JoinPreloadMethod joinPreloadMethod;
        OutputProcessView outputProcessView;
        if (streamViews.length == 1) {
            outputProcessView = handleSimpleSelect(streamViews, processorPair.getFirst(), evalRootMatchRemover, suppressSameEventMatches, discardPartialsOnMatch, agentInstanceContext);
            joinSetComposer = null;
            joinPreloadMethod = null;
        } else {
            JoinPlanResult joinPlanResult = handleJoin(streamViews, processorPair.getFirst(),
                    agentInstanceContext, stopCallbacks, isRecoveringResilient);
            outputProcessView = joinPlanResult.getViewable();
            joinSetComposer = joinPlanResult.getJoinSetComposerDesc().getJoinSetComposer();
            joinPreloadMethod = joinPlanResult.getPreloadMethod();
        }
        stopCallbacks.add(outputProcessView);

        // handle preloads
        if (!isRecoveringResilient) {
            boolean aggregated = resultSetProcessorFactoryProvider.getResultSetProcessorType().isAggregated();
            handlePreloads(preloadList, aggregated, joinPreloadMethod, activationResults, agentInstanceContext, processorPair.getFirst());
        }

        AgentInstanceStopCallback stopCallback = AgentInstanceUtil.finalizeSafeStopCallbacks(stopCallbacks);

        // clean up empty holder
        if (CollectionUtil.isArrayAllNull(priorEvalStrategies)) {
            priorEvalStrategies = PriorEvalStrategy.EMPTY_ARRAY;
        }
        if (CollectionUtil.isAllNullArray(previousGetterStrategies)) {
            previousGetterStrategies = PreviousGetterStrategy.EMPTY_ARRAY;
        }
        if (CollectionUtil.isAllNullArray(patternRoots)) {
            patternRoots = EvalRootState.EMPTY_ARRAY;
        }
        if (CollectionUtil.isArraySameReferences(topViews, eventStreamParentViewable)) {
            topViews = eventStreamParentViewable;
        }

        return new StatementAgentInstanceFactorySelectResult(outputProcessView, stopCallback, agentInstanceContext, processorPair.getSecond(),
                subselectActivations, priorEvalStrategies, previousGetterStrategies, rowRecogPreviousStrategy, tableAccessEvals, preloadList, patternRoots,
                joinSetComposer, topViews, eventStreamParentViewable, activationResults, processorPair.getFirst());
    }

    public EventType getStatementEventType() {
        return resultSetProcessorFactoryProvider.getResultEventType();
    }

    public AIRegistryRequirements getRegistryRequirements() {
        boolean hasPrior = false;
        boolean hasPrevious = false;
        for (int i = 0; i < viewResourceDelegates.length; i++) {
            if (viewResourceDelegates[i].getPriorRequests() != null && !viewResourceDelegates[i].getPriorRequests().isEmpty()) {
                hasPrior = true;
            }
            hasPrevious |= viewResourceDelegates[i].isHasPrevious();
        }

        boolean[] prior = null;
        if (hasPrior) {
            prior = new boolean[viewResourceDelegates.length];
            for (int i = 0; i < viewResourceDelegates.length; i++) {
                if (viewResourceDelegates[i].getPriorRequests() != null && !viewResourceDelegates[i].getPriorRequests().isEmpty()) {
                    prior[i] = true;
                }
            }
        }

        boolean[] previous = null;
        if (hasPrevious) {
            previous = new boolean[viewResourceDelegates.length];
            for (int i = 0; i < viewResourceDelegates.length; i++) {
                previous[i] = viewResourceDelegates[i].isHasPrevious();
            }
        }

        AIRegistryRequirementSubquery[] subqueries = AIRegistryRequirements.getSubqueryRequirements(subselects);

        boolean hasRowRecogWithPrevious = false;
        for (ViewFactory viewFactory : viewFactories[0]) {
            if (viewFactory instanceof RowRecogNFAViewFactory) {
                RowRecogNFAViewFactory recog = (RowRecogNFAViewFactory) viewFactory;
                hasRowRecogWithPrevious = recog.getDesc().getPreviousRandomAccessIndexes() != null;
            }
        }

        return new AIRegistryRequirements(prior, previous, subqueries, tableAccesses == null ? 0 : tableAccesses.size(), hasRowRecogWithPrevious);
    }

    private OutputProcessView handleSimpleSelect(Viewable[] streamViews, ResultSetProcessor resultSetProcessor, EvalRootMatchRemover evalRootMatchRemover, boolean suppressSameEventMatches, boolean discardPartialsOnMatch, AgentInstanceContext agentInstanceContext) {
        Deque<EPStatementDispatch> dispatches = null;
        Viewable finalView = streamViews[0];

        // where-clause
        if (whereClauseEvaluator != null) {
            FilterExprView filterView = new FilterExprView(whereClauseEvaluator, agentInstanceContext, whereClauseEvaluatorTextForAudit);
            finalView.setChild(filterView);
            filterView.setParent(finalView);
            finalView = filterView;
        }

        if (evalRootMatchRemover != null && (suppressSameEventMatches || discardPartialsOnMatch)) {
            PatternRemoveDispatchView v = new PatternRemoveDispatchView(evalRootMatchRemover, suppressSameEventMatches, discardPartialsOnMatch);
            dispatches = new ArrayDeque<>(2);
            dispatches.add(v);
            finalView.setChild(v);
            v.setParent(finalView);
            finalView = v;
        }

        // for ordered deliver without output limit/buffer
        if (orderByWithoutOutputRateLimit) {
            SingleStreamDispatchView bf = new SingleStreamDispatchView();
            if (dispatches == null) {
                dispatches = new ArrayDeque<>(1);
            }
            dispatches.add(bf);
            finalView.setChild(bf);
            bf.setParent(finalView);
            finalView = bf;
        }

        if (dispatches != null) {
            EPStatementAgentInstanceHandle handle = agentInstanceContext.getEpStatementAgentInstanceHandle();
            if (dispatches.size() == 1) {
                handle.setOptionalDispatchable(dispatches.getFirst());
            } else {
                final EPStatementDispatch[] dispatchArray = dispatches.toArray(new EPStatementDispatch[dispatches.size()]);
                handle.setOptionalDispatchable(new EPStatementDispatch() {
                    public void execute() {
                        for (EPStatementDispatch dispatch : dispatchArray) {
                            dispatch.execute();
                        }
                    }
                });
            }
        }

        OutputProcessView outputProcessView = outputProcessViewFactoryProvider.getOutputProcessViewFactory().makeView(resultSetProcessor, agentInstanceContext);
        finalView.setChild(outputProcessView);
        outputProcessView.setParent(finalView);

        return outputProcessView;
    }

    private JoinPlanResult handleJoin(Viewable[] streamViews,
                                      ResultSetProcessor resultSetProcessor,
                                      AgentInstanceContext agentInstanceContext,
                                      List<AgentInstanceStopCallback> stopCallbacks,
                                      boolean isRecoveringResilient) {
        final JoinSetComposerDesc joinSetComposerDesc = joinSetComposerPrototype.create(streamViews, false, agentInstanceContext, isRecoveringResilient);

        stopCallbacks.add(new AgentInstanceStopCallback() {
            public void stop(AgentInstanceStopServices services) {
                joinSetComposerDesc.getJoinSetComposer().destroy();
            }
        });

        OutputProcessView outputProcessView = outputProcessViewFactoryProvider.getOutputProcessViewFactory().makeView(resultSetProcessor, agentInstanceContext);

        // Create strategy for join execution
        JoinExecutionStrategy execution = new JoinExecutionStrategyImpl(joinSetComposerDesc.getJoinSetComposer(), joinSetComposerDesc.getPostJoinFilterEvaluator(), outputProcessView, agentInstanceContext);

        // The view needs a reference to the join execution to pull iterator values
        outputProcessView.setJoinExecutionStrategy(execution);

        // Hook up dispatchable with buffer and execution strategy
        JoinExecStrategyDispatchable joinStatementDispatch = new JoinExecStrategyDispatchable(execution, streamViews.length, agentInstanceContext);
        agentInstanceContext.getEpStatementAgentInstanceHandle().setOptionalDispatchable(joinStatementDispatch);

        JoinPreloadMethod preloadMethod;
        if (unidirectionalJoin || !joinSetComposerDesc.getJoinSetComposer().allowsInit()) {
            preloadMethod = new JoinPreloadMethodNull();
        } else {
            preloadMethod = new JoinPreloadMethodImpl(streamNames.length, joinSetComposerDesc.getJoinSetComposer());
        }

        for (int i = 0; i < streamViews.length; i++) {
            BufferView buffer = new BufferView(i);
            streamViews[i].setChild(buffer);
            buffer.setObserver(joinStatementDispatch);
            preloadMethod.setBuffer(buffer, i);
        }

        return new JoinPlanResult(outputProcessView, preloadMethod, joinSetComposerDesc);
    }

    private void handlePreloads(List<StatementAgentInstancePreload> preloadList, boolean isAggregated, JoinPreloadMethod joinPreloadMethod, ViewableActivationResult[] activationResults, AgentInstanceContext agentInstanceContext, ResultSetProcessor resultSetProcessor) {
        boolean hasNamedWindow = false;

        for (int stream = 0; stream < activationResults.length; stream++) {
            ViewableActivationResult activationResult = activationResults[stream];
            if (!(activationResult.getViewable() instanceof NamedWindowConsumerView)) {
                continue;
            }

            hasNamedWindow = true;
            NamedWindowConsumerView consumer = (NamedWindowConsumerView) activationResult.getViewable();
            if (consumer.getConsumerCallback().isParentBatchWindow()) {
                continue;
            }

            ViewableActivatorNamedWindow nwActivator = (ViewableActivatorNamedWindow) viewableActivators[stream];
            preloadList.add(new NamedWindowConsumerPreload(nwActivator, consumer, agentInstanceContext, joinPreloadMethod));

            if (streamNames.length == 1) {
                preloadList.add(new NamedWindowConsumerPreloadDispatchNonJoin(agentInstanceContext));
            } else {
                preloadList.add(new NamedWindowConsumerPreloadDispatchJoin(joinPreloadMethod, stream, agentInstanceContext));
            }
        }

        // last, for aggregation we need to send the current join results to the result set processor
        if (hasNamedWindow && joinPreloadMethod != null && isAggregated) {
            preloadList.add(new NamedWindowConsumerPreloadAggregationJoin(joinPreloadMethod, resultSetProcessor));
        }
    }

    private static class JoinPlanResult {
        private final OutputProcessView outputProcessView;
        private final JoinPreloadMethod preloadMethod;
        private final JoinSetComposerDesc joinSetComposerDesc;

        private JoinPlanResult(OutputProcessView viewable, JoinPreloadMethod preloadMethod, JoinSetComposerDesc joinSetComposerDesc) {
            this.outputProcessView = viewable;
            this.preloadMethod = preloadMethod;
            this.joinSetComposerDesc = joinSetComposerDesc;
        }

        public OutputProcessView getViewable() {
            return outputProcessView;
        }

        public JoinPreloadMethod getPreloadMethod() {
            return preloadMethod;
        }

        public JoinSetComposerDesc getJoinSetComposerDesc() {
            return joinSetComposerDesc;
        }
    }

    private static class NamedWindowConsumerPreload implements StatementAgentInstancePreload {
        private final ViewableActivatorNamedWindow nwActivator;
        private final NamedWindowConsumerView consumer;
        private final AgentInstanceContext agentInstanceContext;
        private final JoinPreloadMethod joinPreloadMethod;

        public NamedWindowConsumerPreload(ViewableActivatorNamedWindow nwActivator, NamedWindowConsumerView consumer, AgentInstanceContext agentInstanceContext, JoinPreloadMethod joinPreloadMethod) {
            this.nwActivator = nwActivator;
            this.consumer = consumer;
            this.agentInstanceContext = agentInstanceContext;
            this.joinPreloadMethod = joinPreloadMethod;
        }

        public void executePreload() {
            if (nwActivator.getNamedWindowContextName() != null &&
                    !nwActivator.getNamedWindowContextName().equals(agentInstanceContext.getStatementContext().getContextName())) {
                return;
            }

            Collection<EventBean> snapshot = consumer.getConsumerCallback().snapshot(nwActivator.getFilterQueryGraph(), agentInstanceContext.getAnnotations());

            EventBean[] events;
            if (consumer.getFilter() == null) {
                events = CollectionUtil.toArrayEvents(snapshot);
            } else {
                List<EventBean> eventsInWindow = new ArrayList<>(snapshot.size());
                ExprNodeUtilityEvaluate.applyFilterExpressionIterable(snapshot.iterator(), consumer.getFilter(), agentInstanceContext, eventsInWindow);
                events = eventsInWindow.toArray(new EventBean[eventsInWindow.size()]);
            }

            if (events.length == 0) {
                return;
            }
            consumer.update(events, null);

            if (joinPreloadMethod != null && !joinPreloadMethod.isPreloading() && agentInstanceContext.getEpStatementAgentInstanceHandle().getOptionalDispatchable() != null) {
                agentInstanceContext.getEpStatementAgentInstanceHandle().getOptionalDispatchable().execute();
            }
        }
    }

    private static class NamedWindowConsumerPreloadDispatchNonJoin implements StatementAgentInstancePreload {
        private final AgentInstanceContext agentInstanceContext;

        public NamedWindowConsumerPreloadDispatchNonJoin(AgentInstanceContext agentInstanceContext) {
            this.agentInstanceContext = agentInstanceContext;
        }

        public void executePreload() {
            if (agentInstanceContext.getEpStatementAgentInstanceHandle().getOptionalDispatchable() != null) {
                agentInstanceContext.getEpStatementAgentInstanceHandle().getOptionalDispatchable().execute();
            }
        }
    }

    private static class NamedWindowConsumerPreloadDispatchJoin implements StatementAgentInstancePreload {
        private final JoinPreloadMethod joinPreloadMethod;
        private final int stream;
        private final AgentInstanceContext agentInstanceContext;

        public NamedWindowConsumerPreloadDispatchJoin(JoinPreloadMethod joinPreloadMethod, int stream, AgentInstanceContext agentInstanceContext) {
            this.joinPreloadMethod = joinPreloadMethod;
            this.stream = stream;
            this.agentInstanceContext = agentInstanceContext;
        }

        public void executePreload() {
            joinPreloadMethod.preloadFromBuffer(stream, agentInstanceContext);
        }
    }

    private static class NamedWindowConsumerPreloadAggregationJoin implements StatementAgentInstancePreload {
        private final JoinPreloadMethod joinPreloadMethod;
        private final ResultSetProcessor resultSetProcessor;

        public NamedWindowConsumerPreloadAggregationJoin(JoinPreloadMethod joinPreloadMethod, ResultSetProcessor resultSetProcessor) {
            this.joinPreloadMethod = joinPreloadMethod;
            this.resultSetProcessor = resultSetProcessor;
        }

        public void executePreload() {
            joinPreloadMethod.preloadAggregation(resultSetProcessor);
        }
    }

    private static class SelectStopCallback implements AgentInstanceStopCallback {
        private final ResultSetProcessor resultSetProcessor;
        private final AggregationService aggregationService;

        public SelectStopCallback(Pair<ResultSetProcessor, AggregationService> processorPair) {
            resultSetProcessor = processorPair.getFirst();
            aggregationService = processorPair.getSecond();
        }

        public void stop(AgentInstanceStopServices services) {
            resultSetProcessor.stop();
            aggregationService.stop();
        }
    }
}
