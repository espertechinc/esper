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

import com.espertech.esper.client.EventType;
import com.espertech.esper.client.annotation.HookType;
import com.espertech.esper.client.annotation.IterableUnbound;
import com.espertech.esper.client.hook.SQLColumnTypeConversion;
import com.espertech.esper.client.hook.SQLOutputRowConversion;
import com.espertech.esper.core.context.activator.ViewableActivator;
import com.espertech.esper.core.context.activator.ViewableActivatorFactory;
import com.espertech.esper.core.context.factory.StatementAgentInstanceFactorySelect;
import com.espertech.esper.core.context.subselect.SubSelectActivationCollection;
import com.espertech.esper.core.context.subselect.SubSelectStrategyCollection;
import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.core.context.util.ContextPropertyRegistry;
import com.espertech.esper.core.context.util.EPStatementAgentInstanceHandle;
import com.espertech.esper.core.service.*;
import com.espertech.esper.epl.spec.PatternStreamSpecCompiled;
import com.espertech.esper.core.service.speccompiled.StatementSpecCompiled;
import com.espertech.esper.epl.annotation.AnnotationUtil;
import com.espertech.esper.epl.core.engineimport.EngineImportUtil;
import com.espertech.esper.epl.core.poll.MethodPollingViewableFactory;
import com.espertech.esper.epl.core.resultset.core.ResultSetProcessorFactoryDesc;
import com.espertech.esper.epl.core.resultset.core.ResultSetProcessorFactoryFactory;
import com.espertech.esper.epl.core.select.SelectExprProcessorDeliveryCallback;
import com.espertech.esper.epl.core.streamtype.StreamTypeService;
import com.espertech.esper.epl.core.streamtype.StreamTypeServiceImpl;
import com.espertech.esper.epl.core.viewres.ViewResourceDelegateUnverified;
import com.espertech.esper.epl.core.viewres.ViewResourceDelegateVerified;
import com.espertech.esper.epl.db.DatabasePollingViewableFactory;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.util.ExprNodeUtilityRich;
import com.espertech.esper.epl.expression.core.ExprValidationException;
import com.espertech.esper.epl.join.base.HistoricalViewableDesc;
import com.espertech.esper.epl.join.base.JoinSetComposerPrototype;
import com.espertech.esper.epl.join.base.JoinSetComposerPrototypeFactory;
import com.espertech.esper.epl.join.plan.OuterJoinAnalyzer;
import com.espertech.esper.epl.named.NamedWindowMgmtService;
import com.espertech.esper.epl.named.NamedWindowProcessor;
import com.espertech.esper.epl.spec.*;
import com.espertech.esper.epl.table.mgmt.TableMetadata;
import com.espertech.esper.epl.util.EPLValidationUtil;
import com.espertech.esper.epl.view.OutputProcessViewCallback;
import com.espertech.esper.epl.view.OutputProcessViewFactory;
import com.espertech.esper.epl.view.OutputProcessViewFactoryFactory;
import com.espertech.esper.epl.virtualdw.VirtualDWView;
import com.espertech.esper.epl.virtualdw.VirtualDWViewProviderForAgentInstance;
import com.espertech.esper.filterspec.FilterSpecCompiled;
import com.espertech.esper.metrics.instrumentation.InstrumentationAgent;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.pattern.EvalRootFactoryNode;
import com.espertech.esper.pattern.PatternContext;
import com.espertech.esper.rowregex.EventRowRegexNFAViewFactory;
import com.espertech.esper.type.OuterJoinType;
import com.espertech.esper.util.StopCallback;
import com.espertech.esper.view.HistoricalEventViewable;
import com.espertech.esper.view.ViewFactoryChain;

import java.util.LinkedList;
import java.util.List;

/**
 * Starts and provides the stop method for EPL statements.
 */
public class EPStatementStartMethodSelectUtil {
    public static EPStatementStartMethodSelectDesc prepare(StatementSpecCompiled statementSpec,
                                                           EPServicesContext services,
                                                           StatementContext statementContext,
                                                           boolean recoveringResilient,
                                                           AgentInstanceContext defaultAgentInstanceContext,
                                                           boolean queryPlanLogging,
                                                           ViewableActivatorFactory optionalViewableActivatorFactory,
                                                           OutputProcessViewCallback optionalOutputProcessViewCallback,
                                                           SelectExprProcessorDeliveryCallback selectExprProcessorDeliveryCallback)
            throws ExprValidationException {

        // define stop and destroy
        final List<StopCallback> stopCallbacks = new LinkedList<StopCallback>();
        EPStatementDestroyCallbackList destroyCallbacks = new EPStatementDestroyCallbackList();

        // determine context
        final String contextName = statementSpec.getOptionalContextName();
        final ContextPropertyRegistry contextPropertyRegistry = (contextName != null) ? services.getContextManagementService().getContextDescriptor(contextName).getContextPropertyRegistry() : null;

        // Determine stream names for each stream - some streams may not have a name given
        String[] streamNames = EPStatementStartMethodHelperUtil.determineStreamNames(statementSpec.getStreamSpecs());
        int numStreams = streamNames.length;
        if (numStreams == 0) {
            throw new ExprValidationException("The from-clause is required but has not been specified");
        }
        final boolean isJoin = statementSpec.getStreamSpecs().length > 1;
        final boolean hasContext = statementSpec.getOptionalContextName() != null;

        // First we create streams for subselects, if there are any
        SubSelectActivationCollection subSelectStreamDesc = EPStatementStartMethodHelperSubselect.createSubSelectActivation(services, statementSpec, statementContext, destroyCallbacks);

        // Create streams and views
        ViewableActivator[] eventStreamParentViewableActivators = new ViewableActivator[numStreams];
        ViewFactoryChain[] unmaterializedViewChain = new ViewFactoryChain[numStreams];
        String[] eventTypeNames = new String[numStreams];
        boolean[] isNamedWindow = new boolean[numStreams];
        HistoricalEventViewable[] historicalEventViewables = new HistoricalEventViewable[numStreams];

        // verify for joins that required views are present
        StreamJoinAnalysisResult joinAnalysisResult = verifyJoinViews(statementSpec, statementContext.getNamedWindowMgmtService());
        final ExprEvaluatorContextStatement evaluatorContextStmt = new ExprEvaluatorContextStatement(statementContext, false);

        for (int i = 0; i < statementSpec.getStreamSpecs().length; i++) {
            StreamSpecCompiled streamSpec = statementSpec.getStreamSpecs()[i];

            boolean isCanIterateUnbound = streamSpec.getViewSpecs().length == 0 &&
                    (services.getConfigSnapshot().getEngineDefaults().getViewResources().isIterableUnbound() ||
                            AnnotationUtil.findAnnotation(statementSpec.getAnnotations(), IterableUnbound.class) != null);

            // Create view factories and parent view based on a filter specification
            if (streamSpec instanceof FilterStreamSpecCompiled) {
                final FilterStreamSpecCompiled filterStreamSpec = (FilterStreamSpecCompiled) streamSpec;
                eventTypeNames[i] = filterStreamSpec.getFilterSpec().getFilterForEventTypeName();

                // Since only for non-joins we get the existing stream's lock and try to reuse it's views
                final boolean filterSubselectSameStream = EPStatementStartMethodHelperUtil.determineSubquerySameStream(statementSpec, filterStreamSpec);

                // create activator
                ViewableActivator activatorDeactivator;
                if (optionalViewableActivatorFactory != null) {
                    activatorDeactivator = optionalViewableActivatorFactory.createActivatorSimple(filterStreamSpec);
                    if (activatorDeactivator == null) {
                        throw new IllegalStateException("Viewable activate is null for " + filterStreamSpec.getFilterSpec().getFilterForEventType().getName());
                    }
                } else {
                    if (!hasContext) {
                        activatorDeactivator = services.getViewableActivatorFactory().createStreamReuseView(services, statementContext, statementSpec, filterStreamSpec, isJoin, evaluatorContextStmt, filterSubselectSameStream, i, isCanIterateUnbound);
                    } else {
                        InstrumentationAgent instrumentationAgentFilter = null;
                        if (InstrumentationHelper.ENABLED) {
                            final String eventTypeName = filterStreamSpec.getFilterSpec().getFilterForEventType().getName();
                            final int streamNumber = i;
                            instrumentationAgentFilter = new InstrumentationAgent() {
                                public void indicateQ() {
                                    InstrumentationHelper.get().qFilterActivationStream(eventTypeName, streamNumber);
                                }

                                public void indicateA() {
                                    InstrumentationHelper.get().aFilterActivationStream();
                                }
                            };
                        }

                        activatorDeactivator = services.getViewableActivatorFactory().createFilterProxy(services, filterStreamSpec.getFilterSpec(), statementSpec.getAnnotations(), false, instrumentationAgentFilter, isCanIterateUnbound, i);
                    }
                }
                eventStreamParentViewableActivators[i] = activatorDeactivator;

                EventType resultEventType = filterStreamSpec.getFilterSpec().getResultEventType();
                unmaterializedViewChain[i] = services.getViewService().createFactories(i, resultEventType, streamSpec.getViewSpecs(), streamSpec.getOptions(), statementContext, false, -1);
            } else if (streamSpec instanceof PatternStreamSpecCompiled) {
                // Create view factories and parent view based on a pattern expression
                PatternStreamSpecCompiled patternStreamSpec = (PatternStreamSpecCompiled) streamSpec;
                boolean usedByChildViews = streamSpec.getViewSpecs().length > 0 || (statementSpec.getInsertIntoDesc() != null);
                String patternTypeName = statementContext.getStatementId() + "_pattern_" + i;
                final EventType eventType = services.getEventAdapterService().createSemiAnonymousMapType(patternTypeName, patternStreamSpec.getTaggedEventTypes(), patternStreamSpec.getArrayEventTypes(), usedByChildViews);
                unmaterializedViewChain[i] = services.getViewService().createFactories(i, eventType, streamSpec.getViewSpecs(), streamSpec.getOptions(), statementContext, false, -1);

                final EvalRootFactoryNode rootFactoryNode = services.getPatternNodeFactory().makeRootNode(patternStreamSpec.getEvalFactoryNode());
                final PatternContext patternContext = statementContext.getPatternContextFactory().createContext(statementContext, i, rootFactoryNode, patternStreamSpec.getMatchedEventMapMeta(), true);

                // create activator
                ViewableActivator patternActivator = services.getViewableActivatorFactory().createPattern(patternContext, rootFactoryNode, eventType, EPStatementStartMethodHelperUtil.isConsumingFilters(patternStreamSpec.getEvalFactoryNode()), patternStreamSpec.isSuppressSameEventMatches(), patternStreamSpec.isDiscardPartialsOnMatch(), isCanIterateUnbound);
                eventStreamParentViewableActivators[i] = patternActivator;
            } else if (streamSpec instanceof DBStatementStreamSpec) {
                // Create view factories and parent view based on a database SQL statement
                validateNoViews(streamSpec, "Historical data");
                DBStatementStreamSpec sqlStreamSpec = (DBStatementStreamSpec) streamSpec;
                SQLColumnTypeConversion typeConversionHook = (SQLColumnTypeConversion) EngineImportUtil.getAnnotationHook(statementSpec.getAnnotations(), HookType.SQLCOL, SQLColumnTypeConversion.class, statementContext.getEngineImportService());
                SQLOutputRowConversion outputRowConversionHook = (SQLOutputRowConversion) EngineImportUtil.getAnnotationHook(statementSpec.getAnnotations(), HookType.SQLROW, SQLOutputRowConversion.class, statementContext.getEngineImportService());
                EPStatementAgentInstanceHandle epStatementAgentInstanceHandle = defaultAgentInstanceContext.getEpStatementAgentInstanceHandle();
                HistoricalEventViewable historicalEventViewable = DatabasePollingViewableFactory.createDBStatementView(statementContext.getStatementId(), i, sqlStreamSpec, services.getDatabaseRefService(), services.getEventAdapterService(), epStatementAgentInstanceHandle, typeConversionHook, outputRowConversionHook,
                        statementContext.getConfigSnapshot().getEngineDefaults().getLogging().isEnableJDBC(), services.getDataCacheFactory(), statementContext);
                historicalEventViewables[i] = historicalEventViewable;
                unmaterializedViewChain[i] = ViewFactoryChain.fromTypeNoViews(historicalEventViewable.getEventType());
                eventStreamParentViewableActivators[i] = services.getViewableActivatorFactory().makeHistorical(historicalEventViewable);
                stopCallbacks.add(historicalEventViewable);
            } else if (streamSpec instanceof MethodStreamSpec) {
                validateNoViews(streamSpec, "Method data");
                MethodStreamSpec methodStreamSpec = (MethodStreamSpec) streamSpec;
                EPStatementAgentInstanceHandle epStatementAgentInstanceHandle = defaultAgentInstanceContext.getEpStatementAgentInstanceHandle();
                HistoricalEventViewable historicalEventViewable = MethodPollingViewableFactory.createPollMethodView(i, methodStreamSpec, services.getEventAdapterService(), epStatementAgentInstanceHandle, statementContext.getEngineImportService(), statementContext.getSchedulingService(), statementContext.getScheduleBucket(), evaluatorContextStmt, statementContext.getVariableService(), statementContext.getContextName(), services.getDataCacheFactory(), statementContext);
                historicalEventViewables[i] = historicalEventViewable;
                unmaterializedViewChain[i] = ViewFactoryChain.fromTypeNoViews(historicalEventViewable.getEventType());
                eventStreamParentViewableActivators[i] = services.getViewableActivatorFactory().makeHistorical(historicalEventViewable);
                stopCallbacks.add(historicalEventViewable);
            } else if (streamSpec instanceof TableQueryStreamSpec) {
                validateNoViews(streamSpec, "Table data");
                TableQueryStreamSpec tableStreamSpec = (TableQueryStreamSpec) streamSpec;
                if (isJoin && tableStreamSpec.getFilterExpressions().size() > 0) {
                    throw new ExprValidationException("Joins with tables do not allow table filter expressions, please add table filters to the where-clause instead");
                }
                TableMetadata metadata = services.getTableService().getTableMetadata(tableStreamSpec.getTableName());
                ExprEvaluator[] tableFilterEvals = null;
                if (tableStreamSpec.getFilterExpressions().size() > 0) {
                    tableFilterEvals = ExprNodeUtilityRich.getEvaluatorsMayCompile(tableStreamSpec.getFilterExpressions(), statementContext.getEngineImportService(), EPStatementStartMethodSelectUtil.class, false, statementContext.getStatementName());
                }
                EPLValidationUtil.validateContextName(true, metadata.getTableName(), metadata.getContextName(), statementSpec.getOptionalContextName(), false);
                eventStreamParentViewableActivators[i] = services.getViewableActivatorFactory().createTable(metadata, tableFilterEvals);
                unmaterializedViewChain[i] = ViewFactoryChain.fromTypeNoViews(metadata.getInternalEventType());
                eventTypeNames[i] = tableStreamSpec.getTableName();
                joinAnalysisResult.setTablesForStream(i, metadata);
                if (tableStreamSpec.getOptions().isUnidirectional()) {
                    throw new ExprValidationException("Tables cannot be marked as unidirectional");
                }
                if (tableStreamSpec.getOptions().isRetainIntersection() || tableStreamSpec.getOptions().isRetainUnion()) {
                    throw new ExprValidationException("Tables cannot be marked with retain");
                }
                if (isJoin) {
                    destroyCallbacks.addCallback(new EPStatementDestroyCallbackTableIdxRef(services.getTableService(), metadata, statementContext.getStatementName()));
                }
                services.getStatementVariableRefService().addReferences(statementContext.getStatementName(), metadata.getTableName());
            } else if (streamSpec instanceof NamedWindowConsumerStreamSpec) {
                final NamedWindowConsumerStreamSpec namedSpec = (NamedWindowConsumerStreamSpec) streamSpec;
                final NamedWindowProcessor processor = services.getNamedWindowMgmtService().getProcessor(namedSpec.getWindowName());
                EventType namedWindowType = processor.getTailView().getEventType();
                if (namedSpec.getOptPropertyEvaluator() != null) {
                    namedWindowType = namedSpec.getOptPropertyEvaluator().getFragmentEventType();
                }

                eventStreamParentViewableActivators[i] = services.getViewableActivatorFactory().createNamedWindow(processor, namedSpec, statementContext);
                services.getNamedWindowConsumerMgmtService().addConsumer(statementContext, namedSpec);
                unmaterializedViewChain[i] = services.getViewService().createFactories(i, namedWindowType, namedSpec.getViewSpecs(), namedSpec.getOptions(), statementContext, false, -1);
                joinAnalysisResult.setNamedWindow(i);
                eventTypeNames[i] = namedSpec.getWindowName();
                isNamedWindow[i] = true;

                // Consumers to named windows cannot declare a data window view onto the named window to avoid duplicate remove streams
                EPStatementStartMethodHelperValidate.validateNoDataWindowOnNamedWindow(unmaterializedViewChain[i].getViewFactoryChain());
            } else {
                throw new ExprValidationException("Unknown stream specification type: " + streamSpec);
            }
        }

        // handle match-recognize pattern
        if (statementSpec.getMatchRecognizeSpec() != null) {
            if (isJoin) {
                throw new ExprValidationException("Joins are not allowed when using match-recognize");
            }
            if (joinAnalysisResult.getTablesPerStream()[0] != null) {
                throw new ExprValidationException("Tables cannot be used with match-recognize");
            }
            boolean isUnbound = (unmaterializedViewChain[0].getViewFactoryChain().isEmpty()) && (!(statementSpec.getStreamSpecs()[0] instanceof NamedWindowConsumerStreamSpec));
            EventRowRegexNFAViewFactory factory = services.getRegexHandlerFactory().makeViewFactory(unmaterializedViewChain[0], statementSpec.getMatchRecognizeSpec(), defaultAgentInstanceContext, isUnbound, statementSpec.getAnnotations(), services.getConfigSnapshot().getEngineDefaults().getMatchRecognize());
            unmaterializedViewChain[0].getViewFactoryChain().add(factory);

            EPStatementStartMethodHelperAssignExpr.assignAggregations(factory.getAggregationService(), factory.getAggregationExpressions());
        }

        // Obtain event types from view factory chains
        EventType[] streamEventTypes = new EventType[statementSpec.getStreamSpecs().length];
        for (int i = 0; i < unmaterializedViewChain.length; i++) {
            streamEventTypes[i] = unmaterializedViewChain[i].getEventType();
        }

        // Add uniqueness information useful for joins
        joinAnalysisResult.addUniquenessInfo(unmaterializedViewChain, statementSpec.getAnnotations());

        // Validate sub-select views
        SubSelectStrategyCollection subSelectStrategyCollection = EPStatementStartMethodHelperSubselect.planSubSelect(services, statementContext, queryPlanLogging, subSelectStreamDesc, streamNames, streamEventTypes, eventTypeNames, statementSpec.getDeclaredExpressions(), contextPropertyRegistry);

        // Construct type information per stream
        boolean optionalStreamsIfAny = OuterJoinAnalyzer.optionalStreamsIfAny(statementSpec.getOuterJoinDescList());
        StreamTypeService typeService = new StreamTypeServiceImpl(streamEventTypes, streamNames, EPStatementStartMethodHelperUtil.getHasIStreamOnly(isNamedWindow, unmaterializedViewChain), services.getEngineURI(), false, optionalStreamsIfAny);
        ViewResourceDelegateUnverified viewResourceDelegateUnverified = new ViewResourceDelegateUnverified();

        // Validate views that require validation, specifically streams that don't have
        // sub-views such as DB SQL joins
        HistoricalViewableDesc historicalViewableDesc = new HistoricalViewableDesc(numStreams);
        for (int stream = 0; stream < historicalEventViewables.length; stream++) {
            HistoricalEventViewable historicalEventViewable = historicalEventViewables[stream];
            if (historicalEventViewable == null) {
                continue;
            }
            historicalEventViewable.validate(services.getEngineImportService(),
                    typeService,
                    statementContext.getTimeProvider(),
                    statementContext.getVariableService(), statementContext.getTableService(), evaluatorContextStmt,
                    services.getConfigSnapshot(), services.getSchedulingService(), services.getEngineURI(),
                    statementSpec.getSqlParameters(),
                    statementContext.getEventAdapterService(), statementContext);
            historicalViewableDesc.setHistorical(stream, historicalEventViewable.getRequiredStreams());
            if (historicalEventViewable.getRequiredStreams().contains(stream)) {
                throw new ExprValidationException("Parameters for historical stream " + stream + " indicate that the stream is subordinate to itself as stream parameters originate in the same stream");
            }
        }

        // unidirectional is not supported with into-table
        if (joinAnalysisResult.isUnidirectional() && statementSpec.getIntoTableSpec() != null) {
            throw new ExprValidationException("Into-table does not allow unidirectional joins");
        }

        // Validate where-clause filter tree, outer join clause and output limit expression
        EPStatementStartMethodHelperValidate.validateNodes(statementSpec, statementContext, typeService, viewResourceDelegateUnverified);

        // Construct a processor for results posted by views and joins, which takes care of aggregation if required.
        // May return null if we don't need to post-process results posted by views or joins.
        ResultSetProcessorFactoryDesc resultSetProcessorPrototypeDesc = ResultSetProcessorFactoryFactory.getProcessorPrototype(
                statementSpec, statementContext, typeService, viewResourceDelegateUnverified, joinAnalysisResult.getUnidirectionalInd(), true, contextPropertyRegistry, selectExprProcessorDeliveryCallback, services.getConfigSnapshot(), services.getResultSetProcessorHelperFactory(), false, false);

        // Handle 'prior' function nodes in terms of view requirements
        ViewResourceDelegateVerified viewResourceDelegateVerified = EPStatementStartMethodHelperViewResources.verifyPreviousAndPriorRequirements(unmaterializedViewChain, viewResourceDelegateUnverified);

        // handle join
        JoinSetComposerPrototype joinSetComposerPrototype = null;
        if (numStreams > 1) {
            boolean selectsRemoveStream = statementSpec.getSelectStreamSelectorEnum().isSelectsRStream() ||
                    statementSpec.getOutputLimitSpec() != null;
            boolean hasAggregations = !resultSetProcessorPrototypeDesc.getAggregationServiceFactoryDesc().getExpressions().isEmpty();
            joinSetComposerPrototype = JoinSetComposerPrototypeFactory.makeComposerPrototype(
                    statementContext.getStatementName(), statementContext.getStatementId(),
                    statementSpec.getOuterJoinDescList(), statementSpec.getFilterRootNode(), typeService.getEventTypes(), streamNames,
                    joinAnalysisResult, queryPlanLogging, statementContext, historicalViewableDesc, defaultAgentInstanceContext,
                    selectsRemoveStream, hasAggregations, services.getTableService(), false, services.getEventTableIndexService().allowInitIndex(recoveringResilient));
        }

        // obtain factory for output limiting
        OutputProcessViewFactory outputViewFactory = OutputProcessViewFactoryFactory.make(statementSpec, services.getInternalEventRouter(), statementContext, resultSetProcessorPrototypeDesc.getResultEventType(), optionalOutputProcessViewCallback, services.getTableService(), resultSetProcessorPrototypeDesc.getResultSetProcessorType(), services.getResultSetProcessorHelperFactory(), services.getStatementVariableRefService());

        // Factory for statement-context instances
        StatementAgentInstanceFactorySelect factory = new StatementAgentInstanceFactorySelect(
                numStreams, eventStreamParentViewableActivators,
                statementContext, statementSpec, services,
                typeService, unmaterializedViewChain, resultSetProcessorPrototypeDesc, joinAnalysisResult, recoveringResilient,
                joinSetComposerPrototype, subSelectStrategyCollection, viewResourceDelegateVerified, outputViewFactory);

        final EPStatementStopMethod stopMethod = new EPStatementStopMethodImpl(statementContext, stopCallbacks);
        return new EPStatementStartMethodSelectDesc(factory, subSelectStrategyCollection, viewResourceDelegateUnverified, resultSetProcessorPrototypeDesc, stopMethod, destroyCallbacks);
    }

    private static void validateNoViews(StreamSpecCompiled streamSpec, String conceptName)
            throws ExprValidationException {
        if (streamSpec.getViewSpecs().length > 0) {
            throw new ExprValidationException(conceptName + " joins do not allow views onto the data, view '"
                    + streamSpec.getViewSpecs()[0].getObjectName() + "' is not valid in this context");
        }
    }

    private static StreamJoinAnalysisResult verifyJoinViews(StatementSpecCompiled statementSpec, NamedWindowMgmtService namedWindowMgmtService)
            throws ExprValidationException {
        StreamSpecCompiled[] streamSpecs = statementSpec.getStreamSpecs();
        StreamJoinAnalysisResult analysisResult = new StreamJoinAnalysisResult(streamSpecs.length);
        if (streamSpecs.length < 2) {
            return analysisResult;
        }

        // Determine if any stream has a unidirectional keyword

        // inspect unidirectional indicator and named window flags
        for (int i = 0; i < statementSpec.getStreamSpecs().length; i++) {
            StreamSpecCompiled streamSpec = statementSpec.getStreamSpecs()[i];
            if (streamSpec.getOptions().isUnidirectional()) {
                analysisResult.setUnidirectionalInd(i);
            }
            if (streamSpec.getViewSpecs().length > 0) {
                analysisResult.setHasChildViews(i);
            }
            if (streamSpec instanceof NamedWindowConsumerStreamSpec) {
                NamedWindowConsumerStreamSpec nwSpec = (NamedWindowConsumerStreamSpec) streamSpec;
                if (nwSpec.getOptPropertyEvaluator() != null && !streamSpec.getOptions().isUnidirectional()) {
                    throw new ExprValidationException("Failed to validate named window use in join, contained-event is only allowed for named windows when marked as unidirectional");
                }
                analysisResult.setNamedWindow(i);
                final NamedWindowProcessor processor = namedWindowMgmtService.getProcessor(nwSpec.getWindowName());
                String[][] uniqueIndexes = processor.getUniqueIndexes();
                analysisResult.getUniqueKeys()[i] = uniqueIndexes;
                if (processor.isVirtualDataWindow()) {
                    analysisResult.getViewExternal()[i] = new VirtualDWViewProviderForAgentInstance() {
                        public VirtualDWView getView(AgentInstanceContext agentInstanceContext) {
                            return processor.getProcessorInstance(agentInstanceContext).getRootViewInstance().getVirtualDataWindow();
                        }
                    };
                }
            }
        }

        // non-outer-join: verify unidirectional can be on a single stream only
        if (statementSpec.getStreamSpecs().length > 1 && analysisResult.isUnidirectional()) {
            verifyJoinUnidirectional(analysisResult, statementSpec);
        }

        // count streams that provide data, excluding streams that poll data (DB and method)
        int countProviderNonpolling = 0;
        for (int i = 0; i < statementSpec.getStreamSpecs().length; i++) {
            StreamSpecCompiled streamSpec = statementSpec.getStreamSpecs()[i];
            if ((streamSpec instanceof MethodStreamSpec) ||
                    (streamSpec instanceof DBStatementStreamSpec) ||
                    (streamSpec instanceof TableQueryStreamSpec)) {
                continue;
            }
            countProviderNonpolling++;
        }

        // if there is only one stream providing data, the analysis is done
        if (countProviderNonpolling == 1) {
            return analysisResult;
        }
        // there are multiple driving streams, verify the presence of a view for insert/remove stream

        // validation of join views works differently for unidirectional as there can be self-joins that don't require a view
        // see if this is a self-join in which all streams are filters and filter specification is the same.
        FilterSpecCompiled unidirectionalFilterSpec = null;
        FilterSpecCompiled lastFilterSpec = null;
        boolean pureSelfJoin = true;
        for (StreamSpecCompiled streamSpec : statementSpec.getStreamSpecs()) {
            if (!(streamSpec instanceof FilterStreamSpecCompiled)) {
                pureSelfJoin = false;
                continue;
            }

            FilterSpecCompiled filterSpec = ((FilterStreamSpecCompiled) streamSpec).getFilterSpec();
            if ((lastFilterSpec != null) && (!lastFilterSpec.equalsTypeAndFilter(filterSpec))) {
                pureSelfJoin = false;
            }
            if (streamSpec.getViewSpecs().length > 0) {
                pureSelfJoin = false;
            }
            lastFilterSpec = filterSpec;

            if (streamSpec.getOptions().isUnidirectional()) {
                unidirectionalFilterSpec = filterSpec;
            }
        }

        // self-join without views and not unidirectional
        if (pureSelfJoin && (unidirectionalFilterSpec == null)) {
            analysisResult.setPureSelfJoin(true);
            return analysisResult;
        }

        // weed out filter and pattern streams that don't have a view in a join
        for (int i = 0; i < statementSpec.getStreamSpecs().length; i++) {
            StreamSpecCompiled streamSpec = statementSpec.getStreamSpecs()[i];
            if (streamSpec.getViewSpecs().length > 0) {
                continue;
            }

            String name = streamSpec.getOptionalStreamName();
            if ((name == null) && (streamSpec instanceof FilterStreamSpecCompiled)) {
                name = ((FilterStreamSpecCompiled) streamSpec).getFilterSpec().getFilterForEventTypeName();
            }
            if ((name == null) && (streamSpec instanceof PatternStreamSpecCompiled)) {
                name = "pattern event stream";
            }

            if (streamSpec.getOptions().isUnidirectional()) {
                continue;
            }
            // allow a self-join without a child view, in that the filter spec is the same as the unidirection's stream filter
            if ((unidirectionalFilterSpec != null) &&
                    (streamSpec instanceof FilterStreamSpecCompiled) &&
                    (((FilterStreamSpecCompiled) streamSpec).getFilterSpec().equalsTypeAndFilter(unidirectionalFilterSpec))) {
                analysisResult.setUnidirectionalNonDriving(i);
                continue;
            }
            if ((streamSpec instanceof FilterStreamSpecCompiled) ||
                    (streamSpec instanceof PatternStreamSpecCompiled)) {
                throw new ExprValidationException("Joins require that at least one view is specified for each stream, no view was specified for " + name);
            }
        }

        return analysisResult;
    }

    private static void verifyJoinUnidirectional(StreamJoinAnalysisResult analysisResult, StatementSpecCompiled statementSpec) throws ExprValidationException {
        int numUnidirectionalStreams = analysisResult.getUnidirectionalCount();
        int numStreams = statementSpec.getStreamSpecs().length;

        // only a single stream is unidirectional (applies to all but all-full-outer-join)
        if (!isFullOuterJoinAllStreams(statementSpec)) {
            if (numUnidirectionalStreams > 1) {
                throw new ExprValidationException("The unidirectional keyword can only apply to one stream in a join");
            }
        } else {
            // verify full-outer-join: requires unidirectional for all streams
            if (numUnidirectionalStreams > 1 && numUnidirectionalStreams < numStreams) {
                throw new ExprValidationException("The unidirectional keyword must either apply to a single stream or all streams in a full outer join");
            }
        }

        // verify no-child-view for unidirectional
        for (int i = 0; i < statementSpec.getStreamSpecs().length; i++) {
            if (analysisResult.getUnidirectionalInd()[i]) {
                if (analysisResult.getHasChildViews()[i]) {
                    throw new ExprValidationException("The unidirectional keyword requires that no views are declared onto the stream (applies to stream " + i + ")");
                }
            }
        }
    }

    private static boolean isFullOuterJoinAllStreams(StatementSpecCompiled statementSpec) {
        if (statementSpec.getOuterJoinDescList() == null || statementSpec.getOuterJoinDescList().length == 0) {
            return false;
        }
        for (int stream = 0; stream < statementSpec.getStreamSpecs().length - 1; stream++) {
            if (statementSpec.getOuterJoinDescList()[stream].getOuterJoinType() != OuterJoinType.FULL) {
                return false;
            }
        }
        return true;
    }
}
