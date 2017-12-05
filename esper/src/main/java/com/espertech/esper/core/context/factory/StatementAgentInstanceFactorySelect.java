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
package com.espertech.esper.core.context.factory;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.collection.Pair;
import com.espertech.esper.core.context.activator.ViewableActivationResult;
import com.espertech.esper.core.context.activator.ViewableActivator;
import com.espertech.esper.core.context.activator.ViewableActivatorStreamReuseView;
import com.espertech.esper.core.context.subselect.SubSelectStrategyCollection;
import com.espertech.esper.core.context.subselect.SubSelectStrategyHolder;
import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.core.context.util.AgentInstanceViewFactoryChainContext;
import com.espertech.esper.core.context.util.EPStatementAgentInstanceHandle;
import com.espertech.esper.core.context.util.StatementAgentInstanceUtil;
import com.espertech.esper.core.service.EPServicesContext;
import com.espertech.esper.core.service.EPStatementDispatch;
import com.espertech.esper.core.service.StatementContext;
import com.espertech.esper.core.service.StreamJoinAnalysisResult;
import com.espertech.esper.core.start.*;
import com.espertech.esper.epl.agg.service.common.AggregationService;
import com.espertech.esper.epl.core.resultset.core.ResultSetProcessor;
import com.espertech.esper.epl.core.resultset.core.ResultSetProcessorFactoryDesc;
import com.espertech.esper.epl.core.streamtype.StreamTypeService;
import com.espertech.esper.epl.core.streamtype.StreamTypeServiceImpl;
import com.espertech.esper.epl.core.viewres.ViewResourceDelegateVerified;
import com.espertech.esper.epl.core.viewres.ViewResourceDelegateVerifiedStream;
import com.espertech.esper.epl.expression.codegen.ExprNodeCompiler;
import com.espertech.esper.epl.expression.core.ExprNodeUtilityCore;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.epl.util.ExprNodeUtilityRich;
import com.espertech.esper.epl.expression.prev.ExprPreviousEvalStrategy;
import com.espertech.esper.epl.expression.prev.ExprPreviousMatchRecognizeNode;
import com.espertech.esper.epl.expression.prev.ExprPreviousNode;
import com.espertech.esper.epl.expression.prior.ExprPriorEvalStrategy;
import com.espertech.esper.epl.expression.prior.ExprPriorNode;
import com.espertech.esper.epl.expression.subquery.ExprSubselectNode;
import com.espertech.esper.epl.expression.table.ExprTableAccessEvalStrategy;
import com.espertech.esper.epl.expression.table.ExprTableAccessNode;
import com.espertech.esper.epl.join.base.*;
import com.espertech.esper.epl.join.plan.QueryGraph;
import com.espertech.esper.epl.named.NamedWindowConsumerView;
import com.espertech.esper.epl.named.NamedWindowProcessor;
import com.espertech.esper.epl.named.NamedWindowProcessorInstance;
import com.espertech.esper.epl.named.NamedWindowTailViewInstance;
import com.espertech.esper.epl.spec.NamedWindowConsumerStreamSpec;
import com.espertech.esper.core.service.speccompiled.StatementSpecCompiled;
import com.espertech.esper.epl.spec.StreamSpecCompiled;
import com.espertech.esper.epl.util.EPLValidationUtil;
import com.espertech.esper.epl.view.FilterExprView;
import com.espertech.esper.epl.view.OutputProcessViewBase;
import com.espertech.esper.epl.view.OutputProcessViewFactory;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.pattern.EvalRootMatchRemover;
import com.espertech.esper.pattern.EvalRootState;
import com.espertech.esper.rowregex.EventRowRegexHelper;
import com.espertech.esper.rowregex.EventRowRegexNFAViewService;
import com.espertech.esper.rowregex.RegexExprPreviousEvalStrategy;
import com.espertech.esper.util.StopCallback;
import com.espertech.esper.view.*;
import com.espertech.esper.view.internal.BufferView;
import com.espertech.esper.view.internal.PatternRemoveDispatchView;
import com.espertech.esper.view.internal.PriorEventViewFactory;
import com.espertech.esper.view.internal.SingleStreamDispatchView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class StatementAgentInstanceFactorySelect extends StatementAgentInstanceFactoryBase {

    private static final Logger log = LoggerFactory.getLogger(StatementAgentInstanceFactorySelect.class);

    protected final int numStreams;
    protected final ViewableActivator[] eventStreamParentViewableActivators;
    protected final StatementContext statementContext;
    protected final StatementSpecCompiled statementSpec;
    protected final EPServicesContext services;
    protected final StreamTypeService typeService;
    protected final ViewFactoryChain[] unmaterializedViewChain;
    protected final ResultSetProcessorFactoryDesc resultSetProcessorFactoryDesc;
    protected final StreamJoinAnalysisResult joinAnalysisResult;
    protected final JoinSetComposerPrototype joinSetComposerPrototype;
    protected final SubSelectStrategyCollection subSelectStrategyCollection;
    protected final ViewResourceDelegateVerified viewResourceDelegate;
    protected final OutputProcessViewFactory outputProcessViewFactory;
    protected final ExprEvaluator filterRootNodeEvaluator;

    public StatementAgentInstanceFactorySelect(int numStreams, ViewableActivator[] eventStreamParentViewableActivators, StatementContext statementContext, StatementSpecCompiled statementSpec, EPServicesContext services, StreamTypeService typeService, ViewFactoryChain[] unmaterializedViewChain, ResultSetProcessorFactoryDesc resultSetProcessorFactoryDesc, StreamJoinAnalysisResult joinAnalysisResult, boolean recoveringResilient, JoinSetComposerPrototype joinSetComposerPrototype, SubSelectStrategyCollection subSelectStrategyCollection, ViewResourceDelegateVerified viewResourceDelegate, OutputProcessViewFactory outputProcessViewFactory) {
        super(statementSpec.getAnnotations());
        this.numStreams = numStreams;
        this.eventStreamParentViewableActivators = eventStreamParentViewableActivators;
        this.statementContext = statementContext;
        this.statementSpec = statementSpec;
        this.services = services;
        this.typeService = typeService;
        this.unmaterializedViewChain = unmaterializedViewChain;
        this.resultSetProcessorFactoryDesc = resultSetProcessorFactoryDesc;
        this.joinAnalysisResult = joinAnalysisResult;
        this.joinSetComposerPrototype = joinSetComposerPrototype;
        this.subSelectStrategyCollection = subSelectStrategyCollection;
        this.viewResourceDelegate = viewResourceDelegate;
        this.outputProcessViewFactory = outputProcessViewFactory;

        if (statementSpec.getFilterRootNode() != null) {
            filterRootNodeEvaluator = ExprNodeCompiler.allocateEvaluator(statementSpec.getFilterRootNode().getForge(), statementContext.getEngineImportService(), StatementAgentInstanceFactorySelect.class, false, statementContext.getStatementName());
        } else {
            filterRootNodeEvaluator = null;
        }
    }

    public ViewResourceDelegateVerified getViewResourceDelegate() {
        return viewResourceDelegate;
    }

    public StatementAgentInstanceFactorySelectResult newContextInternal(final AgentInstanceContext agentInstanceContext, boolean isRecoveringResilient) {
        final List<StopCallback> stopCallbacks = new ArrayList<StopCallback>(2);

        Viewable finalView;
        ViewableActivationResult[] viewableActivationResult = new ViewableActivationResult[eventStreamParentViewableActivators.length];
        Map<ExprSubselectNode, SubSelectStrategyHolder> subselectStrategies;
        AggregationService aggregationService;
        Viewable[] streamViews;
        Viewable[] eventStreamParentViewable;
        Viewable[] topViews;
        Map<ExprPriorNode, ExprPriorEvalStrategy> priorNodeStrategies;
        Map<ExprPreviousNode, ExprPreviousEvalStrategy> previousNodeStrategies;
        Map<ExprTableAccessNode, ExprTableAccessEvalStrategy> tableAccessStrategies;
        RegexExprPreviousEvalStrategy regexExprPreviousEvalStrategy = null;
        List<StatementAgentInstancePreload> preloadList = new ArrayList<StatementAgentInstancePreload>();
        EvalRootState[] patternRoots;
        StatementAgentInstancePostLoad postLoadJoin = null;
        boolean suppressSameEventMatches = false;
        boolean discardPartialsOnMatch = false;
        EvalRootMatchRemover evalRootMatchRemover = null;

        try {
            // create root viewables
            eventStreamParentViewable = new Viewable[numStreams];
            patternRoots = new EvalRootState[numStreams];

            for (int stream = 0; stream < eventStreamParentViewableActivators.length; stream++) {
                ViewableActivationResult activationResult = eventStreamParentViewableActivators[stream].activate(agentInstanceContext, false, isRecoveringResilient);
                viewableActivationResult[stream] = activationResult;
                stopCallbacks.add(activationResult.getStopCallback());
                suppressSameEventMatches = activationResult.isSuppressSameEventMatches();
                discardPartialsOnMatch = activationResult.isDiscardPartialsOnMatch();

                // add stop callback for any stream-level viewable when applicable
                if (activationResult.getViewable() instanceof StopCallback) {
                    stopCallbacks.add((StopCallback) activationResult.getViewable());
                }

                eventStreamParentViewable[stream] = activationResult.getViewable();
                patternRoots[stream] = activationResult.getOptionalPatternRoot();
                if (stream == 0) {
                    evalRootMatchRemover = activationResult.getOptEvalRootMatchRemover();
                }

                if (activationResult.getOptionalLock() != null) {
                    agentInstanceContext.getEpStatementAgentInstanceHandle().setStatementAgentInstanceLock(activationResult.getOptionalLock());
                    statementContext.setDefaultAgentInstanceLock(activationResult.getOptionalLock());
                }
            }

            // compile view factories adding "prior" as necessary
            List<ViewFactory>[] viewFactoryChains = new List[numStreams];
            for (int i = 0; i < numStreams; i++) {
                List<ViewFactory> viewFactoryChain = unmaterializedViewChain[i].getViewFactoryChain();

                // add "prior" view factory
                boolean hasPrior = viewResourceDelegate.getPerStream()[i].getPriorRequests() != null && !viewResourceDelegate.getPerStream()[i].getPriorRequests().isEmpty();
                if (hasPrior) {
                    PriorEventViewFactory priorEventViewFactory = EPStatementStartMethodHelperPrior.getPriorEventViewFactory(agentInstanceContext.getStatementContext(), i, viewFactoryChain.isEmpty(), false, -1);
                    viewFactoryChain = new ArrayList<ViewFactory>(viewFactoryChain);
                    viewFactoryChain.add(priorEventViewFactory);
                }
                viewFactoryChains[i] = viewFactoryChain;
            }

            // create view factory chain context: holds stream-specific services
            AgentInstanceViewFactoryChainContext[] viewFactoryChainContexts = new AgentInstanceViewFactoryChainContext[numStreams];
            for (int i = 0; i < numStreams; i++) {
                viewFactoryChainContexts[i] = AgentInstanceViewFactoryChainContext.create(viewFactoryChains[i], agentInstanceContext, viewResourceDelegate.getPerStream()[i]);
            }

            // handle "prior" nodes and their strategies
            priorNodeStrategies = EPStatementStartMethodHelperPrior.compilePriorNodeStrategies(viewResourceDelegate, viewFactoryChainContexts);

            // handle "previous" nodes and their strategies
            previousNodeStrategies = EPStatementStartMethodHelperPrevious.compilePreviousNodeStrategies(viewResourceDelegate, viewFactoryChainContexts);

            // materialize views
            streamViews = new Viewable[numStreams];
            topViews = new Viewable[numStreams];
            for (int i = 0; i < numStreams; i++) {
                boolean hasPreviousNode = viewResourceDelegate.getPerStream()[i].getPreviousRequests() != null && !viewResourceDelegate.getPerStream()[i].getPreviousRequests().isEmpty();

                ViewServiceCreateResult createResult = services.getViewService().createViews(eventStreamParentViewable[i], viewFactoryChains[i], viewFactoryChainContexts[i], hasPreviousNode);
                topViews[i] = createResult.getTopViewable();
                streamViews[i] = createResult.getFinalViewable();

                boolean isReuseableView = eventStreamParentViewableActivators[i] instanceof ViewableActivatorStreamReuseView;
                if (isReuseableView) {
                    final List<View> viewsCreated = createResult.getNewViews();
                    StopCallback stopCallback = new StopCallback() {
                        public void stop() {
                            ViewServiceHelper.removeFirstUnsharedView(viewsCreated);
                        }
                    };
                    stopCallbacks.add(stopCallback);
                }

                // add views to stop callback if applicable
                addViewStopCallback(stopCallbacks, createResult.getNewViews());
            }

            // determine match-recognize "previous"-node strategy (none if not present, or one handling and number of nodes)
            EventRowRegexNFAViewService matchRecognize = EventRowRegexHelper.recursiveFindRegexService(topViews[0]);
            if (matchRecognize != null) {
                regexExprPreviousEvalStrategy = matchRecognize.getPreviousEvaluationStrategy();
                stopCallbacks.add(matchRecognize);
            }

            // start subselects
            subselectStrategies = EPStatementStartMethodHelperSubselect.startSubselects(services, subSelectStrategyCollection, agentInstanceContext, stopCallbacks, isRecoveringResilient);

            // plan table access
            tableAccessStrategies = EPStatementStartMethodHelperTableAccess.attachTableAccess(services, agentInstanceContext, statementSpec.getTableNodes(), false);

            // obtain result set processor and aggregation services
            Pair<ResultSetProcessor, AggregationService> processorPair = EPStatementStartMethodHelperUtil.startResultSetAndAggregation(resultSetProcessorFactoryDesc, agentInstanceContext, false, null);
            final ResultSetProcessor resultSetProcessor = processorPair.getFirst();
            aggregationService = processorPair.getSecond();
            stopCallbacks.add(aggregationService);
            stopCallbacks.add(resultSetProcessor);

            // for just 1 event stream without joins, handle the one-table process separately.
            final JoinPreloadMethod joinPreloadMethod;
            JoinSetComposerDesc joinSetComposer = null;
            if (streamViews.length == 1) {
                finalView = handleSimpleSelect(streamViews[0], resultSetProcessor, agentInstanceContext, evalRootMatchRemover, suppressSameEventMatches, discardPartialsOnMatch);
                joinPreloadMethod = null;
            } else {
                JoinPlanResult joinPlanResult = handleJoin(typeService.getStreamNames(), streamViews, resultSetProcessor,
                        agentInstanceContext, stopCallbacks, joinAnalysisResult, isRecoveringResilient);
                finalView = joinPlanResult.getViewable();
                joinPreloadMethod = joinPlanResult.getPreloadMethod();
                joinSetComposer = joinPlanResult.getJoinSetComposerDesc();
            }

            // for stoppable final views, add callback
            if (finalView instanceof StopCallback) {
                stopCallbacks.add((StopCallback) finalView);
            }

            // Replay any named window data, for later consumers of named data windows
            if (services.getEventTableIndexService().allowInitIndex(isRecoveringResilient)) {
                boolean hasNamedWindow = false;
                QueryGraph[] namedWindowPostloadFilters = new QueryGraph[statementSpec.getStreamSpecs().length];
                NamedWindowTailViewInstance[] namedWindowTailViews = new NamedWindowTailViewInstance[statementSpec.getStreamSpecs().length];
                List<ExprNode>[] namedWindowFilters = new List[statementSpec.getStreamSpecs().length];

                for (int i = 0; i < statementSpec.getStreamSpecs().length; i++) {
                    final int streamNum = i;
                    StreamSpecCompiled streamSpec = statementSpec.getStreamSpecs()[i];

                    if (streamSpec instanceof NamedWindowConsumerStreamSpec) {
                        hasNamedWindow = true;
                        final NamedWindowConsumerStreamSpec namedSpec = (NamedWindowConsumerStreamSpec) streamSpec;
                        NamedWindowProcessor processor = services.getNamedWindowMgmtService().getProcessor(namedSpec.getWindowName());
                        NamedWindowProcessorInstance processorInstance = processor.getProcessorInstance(agentInstanceContext);
                        if (processorInstance != null) {
                            final NamedWindowTailViewInstance consumerView = processorInstance.getTailViewInstance();
                            namedWindowTailViews[i] = consumerView;
                            final NamedWindowConsumerView view = (NamedWindowConsumerView) viewableActivationResult[i].getViewable();

                            // determine preload/postload filter for index access
                            if (!namedSpec.getFilterExpressions().isEmpty()) {
                                namedWindowFilters[streamNum] = namedSpec.getFilterExpressions();
                                String streamName = streamSpec.getOptionalStreamName() != null ? streamSpec.getOptionalStreamName() : consumerView.getEventType().getName();
                                StreamTypeServiceImpl types = new StreamTypeServiceImpl(consumerView.getEventType(), streamName, false, services.getEngineURI());
                                namedWindowPostloadFilters[i] = EPLValidationUtil.validateFilterGetQueryGraphSafe(ExprNodeUtilityRich.connectExpressionsByLogicalAndWhenNeeded(namedSpec.getFilterExpressions()), statementContext, types);
                            }

                            // preload view for stream unless the expiry policy is batch window
                            Iterator<EventBean> consumerViewIterator = consumerView.iterator();
                            boolean preload = !consumerView.getTailView().isParentBatchWindow() && consumerViewIterator.hasNext();
                            if (preload) {
                                if (isRecoveringResilient && numStreams < 2) {
                                    preload = false;
                                }
                            }
                            if (preload) {
                                final boolean yesRecoveringResilient = isRecoveringResilient;
                                final QueryGraph preloadFilterSpec = namedWindowPostloadFilters[i];
                                preloadList.add(new StatementAgentInstancePreload() {
                                    public void executePreload(ExprEvaluatorContext exprEvaluatorContext) {
                                        Collection<EventBean> snapshot = consumerView.snapshot(preloadFilterSpec, statementContext.getAnnotations());
                                        List<EventBean> eventsInWindow = new ArrayList<EventBean>(snapshot.size());
                                        ExprNodeUtilityCore.applyFilterExpressionsIterable(snapshot, namedSpec.getFilterExpressions(), agentInstanceContext, eventsInWindow);
                                        EventBean[] newEvents = eventsInWindow.toArray(new EventBean[eventsInWindow.size()]);
                                        view.update(newEvents, null);
                                        if (!yesRecoveringResilient && joinPreloadMethod != null && !joinPreloadMethod.isPreloading() && agentInstanceContext.getEpStatementAgentInstanceHandle().getOptionalDispatchable() != null) {
                                            agentInstanceContext.getEpStatementAgentInstanceHandle().getOptionalDispatchable().execute();
                                        }
                                    }
                                });
                            }
                        } else {
                            log.info("Named window access is out-of-context, the named window '" + namedSpec.getWindowName() + "' has been declared for a different context then the current statement, the aggregation and join state will not be initialized for statement expression [" + statementContext.getExpression() + "]");
                        }

                        preloadList.add(new StatementAgentInstancePreload() {
                            public void executePreload(ExprEvaluatorContext exprEvaluatorContext) {
                                // in a join, preload indexes, if any
                                if (joinPreloadMethod != null) {
                                    joinPreloadMethod.preloadFromBuffer(streamNum, exprEvaluatorContext);
                                } else {
                                    if (agentInstanceContext.getEpStatementAgentInstanceHandle().getOptionalDispatchable() != null) {
                                        agentInstanceContext.getEpStatementAgentInstanceHandle().getOptionalDispatchable().execute();
                                    }
                                }
                            }
                        });
                    }
                }

                // last, for aggregation we need to send the current join results to the result set processor
                if (hasNamedWindow && (joinPreloadMethod != null) && (!isRecoveringResilient) && resultSetProcessorFactoryDesc.getResultSetProcessorType().isAggregated()) {
                    preloadList.add(new StatementAgentInstancePreload() {
                        public void executePreload(ExprEvaluatorContext exprEvaluatorContext) {
                            joinPreloadMethod.preloadAggregation(resultSetProcessor);
                        }
                    });
                }

                if (isRecoveringResilient) {
                    postLoadJoin = new StatementAgentInstancePostLoadSelect(streamViews, joinSetComposer, namedWindowTailViews, namedWindowPostloadFilters, namedWindowFilters, statementContext.getAnnotations(), agentInstanceContext);
                } else if (joinSetComposer != null) {
                    postLoadJoin = new StatementAgentInstancePostLoadIndexVisiting(joinSetComposer.getJoinSetComposer());
                }
            }
        } catch (RuntimeException ex) {
            StopCallback stopCallback = StatementAgentInstanceUtil.getStopCallback(stopCallbacks, agentInstanceContext);
            StatementAgentInstanceUtil.stopSafe(stopCallback, statementContext);
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aContextPartitionAllocate();
            }
            throw ex;
        }

        StatementAgentInstanceFactorySelectResult selectResult = new StatementAgentInstanceFactorySelectResult(finalView, null, agentInstanceContext, aggregationService, subselectStrategies, priorNodeStrategies, previousNodeStrategies, regexExprPreviousEvalStrategy, tableAccessStrategies, preloadList, patternRoots, postLoadJoin, topViews, eventStreamParentViewable, viewableActivationResult);

        if (statementContext.getStatementExtensionServicesContext() != null) {
            statementContext.getStatementExtensionServicesContext().contributeStopCallback(selectResult, stopCallbacks);
        }
        StopCallback stopCallback = StatementAgentInstanceUtil.getStopCallback(stopCallbacks, agentInstanceContext);
        selectResult.setStopCallback(stopCallback);

        return selectResult;
    }

    protected static void addViewStopCallback(List<StopCallback> stopCallbacks, List<View> topViews) {
        for (View view : topViews) {
            if (view instanceof StoppableView) {
                stopCallbacks.add((StoppableView) view);
            }
        }
    }

    public void assignExpressions(StatementAgentInstanceFactoryResult result) {
        StatementAgentInstanceFactorySelectResult selectResult = (StatementAgentInstanceFactorySelectResult) result;
        EPStatementStartMethodHelperAssignExpr.assignAggregations(selectResult.getOptionalAggegationService(), resultSetProcessorFactoryDesc.getAggregationServiceFactoryDesc().getExpressions());
        EPStatementStartMethodHelperAssignExpr.assignSubqueryStrategies(subSelectStrategyCollection, result.getSubselectStrategies());
        EPStatementStartMethodHelperAssignExpr.assignPriorStrategies(result.getPriorNodeStrategies());
        EPStatementStartMethodHelperAssignExpr.assignPreviousStrategies(result.getPreviousNodeStrategies());
        Set<ExprPreviousMatchRecognizeNode> matchRecognizeNodes = viewResourceDelegate.getPerStream()[0].getMatchRecognizePreviousRequests();
        EPStatementStartMethodHelperAssignExpr.assignMatchRecognizePreviousStrategies(matchRecognizeNodes, result.getRegexExprPreviousEvalStrategy());
    }

    public void unassignExpressions() {
        EPStatementStartMethodHelperAssignExpr.unassignAggregations(resultSetProcessorFactoryDesc.getAggregationServiceFactoryDesc().getExpressions());
        EPStatementStartMethodHelperAssignExpr.unassignSubqueryStrategies(subSelectStrategyCollection.getSubqueries().keySet());
        for (int streamNum = 0; streamNum < viewResourceDelegate.getPerStream().length; streamNum++) {
            ViewResourceDelegateVerifiedStream viewResourceStream = viewResourceDelegate.getPerStream()[streamNum];
            SortedMap<Integer, List<ExprPriorNode>> callbacksPerIndex = viewResourceStream.getPriorRequests();
            for (Map.Entry<Integer, List<ExprPriorNode>> priorItem : callbacksPerIndex.entrySet()) {
                EPStatementStartMethodHelperAssignExpr.unassignPriorStrategies(priorItem.getValue());
            }
            EPStatementStartMethodHelperAssignExpr.unassignPreviousStrategies(viewResourceStream.getPreviousRequests());
        }
        Set<ExprPreviousMatchRecognizeNode> matchRecognizeNodes = viewResourceDelegate.getPerStream()[0].getMatchRecognizePreviousRequests();
        EPStatementStartMethodHelperAssignExpr.unassignMatchRecognizePreviousStrategies(matchRecognizeNodes);
    }

    public ViewableActivator[] getEventStreamParentViewableActivators() {
        return eventStreamParentViewableActivators;
    }

    public ViewFactoryChain[] getUnmaterializedViewChain() {
        return unmaterializedViewChain;
    }

    public int getNumStreams() {
        return numStreams;
    }

    public StatementContext getStatementContext() {
        return statementContext;
    }

    public StatementSpecCompiled getStatementSpec() {
        return statementSpec;
    }

    public EPServicesContext getServices() {
        return services;
    }

    public StreamTypeService getTypeService() {
        return typeService;
    }

    public ResultSetProcessorFactoryDesc getResultSetProcessorFactoryDesc() {
        return resultSetProcessorFactoryDesc;
    }

    public StreamJoinAnalysisResult getJoinAnalysisResult() {
        return joinAnalysisResult;
    }

    public JoinSetComposerPrototype getJoinSetComposerPrototype() {
        return joinSetComposerPrototype;
    }

    public SubSelectStrategyCollection getSubSelectStrategyCollection() {
        return subSelectStrategyCollection;
    }

    public OutputProcessViewFactory getOutputProcessViewFactory() {
        return outputProcessViewFactory;
    }

    private Viewable handleSimpleSelect(Viewable view,
                                        ResultSetProcessor resultSetProcessor,
                                        AgentInstanceContext agentInstanceContext,
                                        EvalRootMatchRemover evalRootMatchRemover,
                                        boolean suppressSameEventMatches,
                                        boolean discardPartialsOnMatch) {
        Viewable finalView = view;

        // Add filter view that evaluates the filter expression
        if (filterRootNodeEvaluator != null) {
            FilterExprView filterView = new FilterExprView(statementSpec.getFilterRootNode(), filterRootNodeEvaluator, agentInstanceContext);
            finalView.addView(filterView);
            finalView = filterView;
        }

        Deque<EPStatementDispatch> dispatches = null;

        if (evalRootMatchRemover != null && (suppressSameEventMatches || discardPartialsOnMatch)) {
            PatternRemoveDispatchView v = new PatternRemoveDispatchView(evalRootMatchRemover, suppressSameEventMatches, discardPartialsOnMatch);
            dispatches = new ArrayDeque<EPStatementDispatch>(2);
            dispatches.add(v);
            finalView.addView(v);
            finalView = v;
        }

        // for ordered deliver without output limit/buffer
        if (statementSpec.getOrderByList().length > 0 && (statementSpec.getOutputLimitSpec() == null)) {
            SingleStreamDispatchView bf = new SingleStreamDispatchView();
            if (dispatches == null) {
                dispatches = new ArrayDeque<EPStatementDispatch>(1);
            }
            dispatches.add(bf);
            finalView.addView(bf);
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

        com.espertech.esper.view.View selectView = outputProcessViewFactory.makeView(resultSetProcessor, agentInstanceContext);

        finalView.addView(selectView);
        finalView = selectView;

        return finalView;
    }

    private JoinPlanResult handleJoin(String[] streamNames,
                                      Viewable[] streamViews,
                                      ResultSetProcessor resultSetProcessor,
                                      AgentInstanceContext agentInstanceContext,
                                      List<StopCallback> stopCallbacks,
                                      StreamJoinAnalysisResult joinAnalysisResult,
                                      boolean isRecoveringResilient) {
        final JoinSetComposerDesc joinSetComposerDesc = joinSetComposerPrototype.create(streamViews, false, agentInstanceContext, isRecoveringResilient);

        stopCallbacks.add(new StopCallback() {
            public void stop() {
                joinSetComposerDesc.getJoinSetComposer().destroy();
            }
        });

        JoinSetFilter filter = new JoinSetFilter(joinSetComposerDesc.getPostJoinFilterEvaluator());
        OutputProcessViewBase indicatorView = outputProcessViewFactory.makeView(resultSetProcessor, agentInstanceContext);

        // Create strategy for join execution
        JoinExecutionStrategy execution = new JoinExecutionStrategyImpl(joinSetComposerDesc.getJoinSetComposer(), filter, indicatorView, agentInstanceContext);

        // The view needs a reference to the join execution to pull iterator values
        indicatorView.setJoinExecutionStrategy(execution);

        // Hook up dispatchable with buffer and execution strategy
        JoinExecStrategyDispatchable joinStatementDispatch = new JoinExecStrategyDispatchable(execution, statementSpec.getStreamSpecs().length);
        agentInstanceContext.getEpStatementAgentInstanceHandle().setOptionalDispatchable(joinStatementDispatch);

        JoinPreloadMethod preloadMethod;
        if (joinAnalysisResult.isUnidirectional()) {
            preloadMethod = new JoinPreloadMethodNull();
        } else {
            if (!joinSetComposerDesc.getJoinSetComposer().allowsInit()) {
                preloadMethod = new JoinPreloadMethodNull();
            } else {
                preloadMethod = new JoinPreloadMethodImpl(streamNames.length, joinSetComposerDesc.getJoinSetComposer());
            }
        }

        // Create buffer for each view. Point buffer to dispatchable for join.
        for (int i = 0; i < statementSpec.getStreamSpecs().length; i++) {
            BufferView buffer = new BufferView(i);
            streamViews[i].addView(buffer);
            buffer.setObserver(joinStatementDispatch);
            preloadMethod.setBuffer(buffer, i);
        }

        return new JoinPlanResult(indicatorView, preloadMethod, joinSetComposerDesc);
    }

    private static class JoinPlanResult {
        private final Viewable viewable;
        private final JoinPreloadMethod preloadMethod;
        private final JoinSetComposerDesc joinSetComposerDesc;

        private JoinPlanResult(Viewable viewable, JoinPreloadMethod preloadMethod, JoinSetComposerDesc joinSetComposerDesc) {
            this.viewable = viewable;
            this.preloadMethod = preloadMethod;
            this.joinSetComposerDesc = joinSetComposerDesc;
        }

        public Viewable getViewable() {
            return viewable;
        }

        public JoinPreloadMethod getPreloadMethod() {
            return preloadMethod;
        }

        public JoinSetComposerDesc getJoinSetComposerDesc() {
            return joinSetComposerDesc;
        }
    }
}
