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
import com.espertech.esper.client.annotation.HintEnum;
import com.espertech.esper.collection.Pair;
import com.espertech.esper.core.context.activator.ViewableActivationResult;
import com.espertech.esper.core.context.activator.ViewableActivator;
import com.espertech.esper.core.context.subselect.*;
import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.core.context.util.ContextPropertyRegistry;
import com.espertech.esper.core.service.EPServicesContext;
import com.espertech.esper.core.service.ExprEvaluatorContextStatement;
import com.espertech.esper.core.service.StatementContext;
import com.espertech.esper.core.service.speccompiled.SelectClauseStreamCompiledSpec;
import com.espertech.esper.core.service.speccompiled.StatementSpecCompiled;
import com.espertech.esper.epl.agg.codegen.AggregationServiceFactoryCompiler;
import com.espertech.esper.epl.agg.service.common.*;
import com.espertech.esper.epl.core.streamtype.StreamTypeService;
import com.espertech.esper.epl.core.streamtype.StreamTypeServiceImpl;
import com.espertech.esper.epl.core.viewres.ViewResourceDelegateUnverified;
import com.espertech.esper.epl.core.viewres.ViewResourceDelegateVerified;
import com.espertech.esper.epl.declexpr.ExprDeclaredNode;
import com.espertech.esper.epl.expression.baseagg.ExprAggregateNode;
import com.espertech.esper.epl.expression.baseagg.ExprAggregateNodeGroupKey;
import com.espertech.esper.epl.expression.baseagg.ExprAggregateNodeUtil;
import com.espertech.esper.epl.expression.codegen.ExprNodeCompiler;
import com.espertech.esper.epl.expression.core.*;
import com.espertech.esper.epl.expression.prev.ExprPreviousNode;
import com.espertech.esper.epl.expression.prior.ExprPriorNode;
import com.espertech.esper.epl.expression.subquery.ExprSubselectNode;
import com.espertech.esper.epl.expression.subquery.ExprSubselectStrategy;
import com.espertech.esper.epl.expression.visitor.ExprNodeIdentifierVisitor;
import com.espertech.esper.epl.expression.visitor.ExprNodeSubselectDeclaredNoTraverseVisitor;
import com.espertech.esper.epl.join.hint.ExcludePlanHint;
import com.espertech.esper.epl.join.hint.IndexHint;
import com.espertech.esper.epl.join.plan.CoercionDesc;
import com.espertech.esper.epl.join.plan.CoercionUtil;
import com.espertech.esper.epl.join.plan.QueryPlanIndexBuilder;
import com.espertech.esper.epl.join.table.EventTableFactory;
import com.espertech.esper.epl.join.util.IndexNameAndDescPair;
import com.espertech.esper.epl.join.util.QueryPlanIndexDescSubquery;
import com.espertech.esper.epl.join.util.QueryPlanIndexHook;
import com.espertech.esper.epl.join.util.QueryPlanIndexHookUtil;
import com.espertech.esper.epl.lookup.*;
import com.espertech.esper.epl.named.NamedWindowProcessor;
import com.espertech.esper.epl.spec.*;
import com.espertech.esper.epl.subquery.SubselectAggregationPreprocessorBase;
import com.espertech.esper.epl.table.mgmt.TableMetadata;
import com.espertech.esper.epl.util.ExprNodeUtilityRich;
import com.espertech.esper.metrics.instrumentation.InstrumentationAgent;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.util.AuditPath;
import com.espertech.esper.util.CollectionUtil;
import com.espertech.esper.util.JavaClassHelper;
import com.espertech.esper.util.StopCallback;
import com.espertech.esper.view.StoppableView;
import com.espertech.esper.view.ViewFactoryChain;
import com.espertech.esper.view.ViewProcessingException;
import com.espertech.esper.view.ViewServiceHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.util.*;

import static com.espertech.esper.epl.util.ExprNodeUtilityRich.getSubqueryInfoText;

public class EPStatementStartMethodHelperSubselect {
    private static final Logger QUERY_PLAN_LOG = LoggerFactory.getLogger(AuditPath.QUERYPLAN_LOG);
    private final static String MSG_SUBQUERY_REQUIRES_WINDOW = "Subqueries require one or more views to limit the stream, consider declaring a length or time window (applies to correlated or non-fully-aggregated subqueries)";

    protected static SubSelectActivationCollection createSubSelectActivation(EPServicesContext services, StatementSpecCompiled statementSpecContainer, StatementContext statementContext, EPStatementDestroyCallbackList destroyCallbacks)
            throws ExprValidationException, ViewProcessingException {
        SubSelectActivationCollection subSelectStreamDesc = new SubSelectActivationCollection();
        int subselectStreamNumber = 1024;

        // Process all subselect expression nodes
        for (ExprSubselectNode subselect : statementSpecContainer.getSubSelectExpressions()) {
            StatementSpecCompiled statementSpec = subselect.getStatementSpecCompiled();
            StreamSpecCompiled streamSpec = statementSpec.getStreamSpecs()[0];

            if (streamSpec instanceof FilterStreamSpecCompiled) {
                FilterStreamSpecCompiled filterStreamSpec = (FilterStreamSpecCompiled) statementSpec.getStreamSpecs()[0];

                subselectStreamNumber++;

                InstrumentationAgent instrumentationAgentSubquery = null;
                if (InstrumentationHelper.ENABLED) {
                    final String eventTypeName = filterStreamSpec.getFilterSpec().getFilterForEventType().getName();
                    final ExprSubselectNode exprSubselectNode = subselect;
                    instrumentationAgentSubquery = new InstrumentationAgent() {
                        public void indicateQ() {
                            InstrumentationHelper.get().qFilterActivationSubselect(eventTypeName, exprSubselectNode);
                        }

                        public void indicateA() {
                            InstrumentationHelper.get().aFilterActivationSubselect();
                        }
                    };
                }

                // Register filter, create view factories
                ViewableActivator activatorDeactivator = services.getViewableActivatorFactory().createFilterProxy(services, filterStreamSpec.getFilterSpec(), statementSpec.getAnnotations(), true, instrumentationAgentSubquery, false, null);
                ViewFactoryChain viewFactoryChain = services.getViewService().createFactories(subselectStreamNumber, filterStreamSpec.getFilterSpec().getResultEventType(), filterStreamSpec.getViewSpecs(), filterStreamSpec.getOptions(), statementContext, true, subselect.getSubselectNumber());
                subselect.setRawEventType(viewFactoryChain.getEventType());

                // Add lookup to list, for later starts
                subSelectStreamDesc.add(subselect, new SubSelectActivationHolder(subselectStreamNumber, filterStreamSpec.getFilterSpec().getResultEventType(), viewFactoryChain, activatorDeactivator, streamSpec));
            } else if (streamSpec instanceof TableQueryStreamSpec) {
                TableQueryStreamSpec table = (TableQueryStreamSpec) streamSpec;
                TableMetadata metadata = services.getTableService().getTableMetadata(table.getTableName());
                ViewFactoryChain viewFactoryChain = ViewFactoryChain.fromTypeNoViews(metadata.getInternalEventType());
                ViewableActivator viewableActivator = services.getViewableActivatorFactory().createTable(metadata, null);
                subSelectStreamDesc.add(subselect, new SubSelectActivationHolder(subselectStreamNumber, metadata.getInternalEventType(), viewFactoryChain, viewableActivator, streamSpec));
                subselect.setRawEventType(metadata.getInternalEventType());
                destroyCallbacks.addCallback(new EPStatementDestroyCallbackTableIdxRef(services.getTableService(), metadata, statementContext.getStatementName()));
                services.getStatementVariableRefService().addReferences(statementContext.getStatementName(), metadata.getTableName());
            } else {
                NamedWindowConsumerStreamSpec namedSpec = (NamedWindowConsumerStreamSpec) statementSpec.getStreamSpecs()[0];
                NamedWindowProcessor processor = services.getNamedWindowMgmtService().getProcessor(namedSpec.getWindowName());

                EventType namedWindowType = processor.getTailView().getEventType();
                if (namedSpec.getOptPropertyEvaluator() != null) {
                    namedWindowType = namedSpec.getOptPropertyEvaluator().getFragmentEventType();
                }

                // if named-window index sharing is disabled (the default) or filter expressions are provided then consume the insert-remove stream
                boolean disableIndexShare = HintEnum.DISABLE_WINDOW_SUBQUERY_INDEXSHARE.getHint(statementSpecContainer.getAnnotations()) != null;
                if (disableIndexShare && processor.isVirtualDataWindow()) {
                    disableIndexShare = false;
                }
                if (!namedSpec.getFilterExpressions().isEmpty() || !processor.isEnableSubqueryIndexShare() || disableIndexShare) {
                    ViewableActivator activatorNamedWindow = services.getViewableActivatorFactory().createNamedWindow(processor, namedSpec, statementContext);
                    ViewFactoryChain viewFactoryChain = services.getViewService().createFactories(0, namedWindowType, namedSpec.getViewSpecs(), namedSpec.getOptions(), statementContext, true, subselect.getSubselectNumber());
                    subselect.setRawEventType(viewFactoryChain.getEventType());
                    subSelectStreamDesc.add(subselect, new SubSelectActivationHolder(subselectStreamNumber, namedWindowType, viewFactoryChain, activatorNamedWindow, streamSpec));
                    services.getNamedWindowConsumerMgmtService().addConsumer(statementContext, namedSpec);
                } else {
                    // else if there are no named window stream filter expressions and index sharing is enabled
                    ViewFactoryChain viewFactoryChain = services.getViewService().createFactories(0, processor.getNamedWindowType(), namedSpec.getViewSpecs(), namedSpec.getOptions(), statementContext, true, subselect.getSubselectNumber());
                    subselect.setRawEventType(processor.getNamedWindowType());
                    ViewableActivator activator = services.getViewableActivatorFactory().makeSubqueryNWIndexShare();
                    subSelectStreamDesc.add(subselect, new SubSelectActivationHolder(subselectStreamNumber, namedWindowType, viewFactoryChain, activator, streamSpec));
                    services.getStatementVariableRefService().addReferences(statementContext.getStatementName(), processor.getNamedWindowType().getName());
                }
            }
        }

        return subSelectStreamDesc;
    }

    protected static SubSelectStrategyCollection planSubSelect(EPServicesContext services,
                                                               StatementContext statementContext,
                                                               boolean queryPlanLogging,
                                                               SubSelectActivationCollection subSelectStreamDesc,
                                                               String[] outerStreamNames,
                                                               EventType[] outerEventTypesSelect,
                                                               String[] outerEventTypeNamees,
                                                               ExprDeclaredNode[] declaredExpressions,
                                                               ContextPropertyRegistry contextPropertyRegistry)
            throws ExprValidationException, ViewProcessingException {
        int subqueryNum = -1;
        SubSelectStrategyCollection collection = new SubSelectStrategyCollection();

        Map<ExprDeclaredNode, List<ExprDeclaredNode>> declaredExpressionCallHierarchy = null;
        if (declaredExpressions.length > 0) {
            declaredExpressionCallHierarchy = ExprNodeUtilityRich.getDeclaredExpressionCallHierarchy(declaredExpressions);
        }

        for (Map.Entry<ExprSubselectNode, SubSelectActivationHolder> entry : subSelectStreamDesc.getSubqueries().entrySet()) {
            subqueryNum++;
            ExprSubselectNode subselect = entry.getKey();
            SubSelectActivationHolder subSelectActivation = entry.getValue();

            try {
                SubSelectStrategyFactoryDesc factoryDesc = planSubSelectInternal(subqueryNum, subselect, subSelectActivation,
                        services, statementContext, queryPlanLogging, subSelectStreamDesc,
                        outerStreamNames, outerEventTypesSelect, outerEventTypeNamees,
                        declaredExpressions, contextPropertyRegistry, declaredExpressionCallHierarchy);
                collection.add(subselect, factoryDesc);
            } catch (Exception ex) {
                throw new ExprValidationException("Failed to plan " + getSubqueryInfoText(subqueryNum, subselect) + ": " + ex.getMessage(), ex);
            }
        }

        return collection;
    }

    public static Map<ExprSubselectNode, SubSelectStrategyHolder> startSubselects(
            EPServicesContext services,
            SubSelectStrategyCollection subSelectStrategyCollection,
            final AgentInstanceContext agentInstanceContext,
            List<StopCallback> stopCallbackList,
            boolean isRecoveringResilient) {

        Map<ExprSubselectNode, SubSelectStrategyHolder> subselectStrategies = new HashMap<ExprSubselectNode, SubSelectStrategyHolder>();

        for (Map.Entry<ExprSubselectNode, SubSelectStrategyFactoryDesc> subselectEntry : subSelectStrategyCollection.getSubqueries().entrySet()) {

            ExprSubselectNode subselectNode = subselectEntry.getKey();
            SubSelectStrategyFactoryDesc factoryDesc = subselectEntry.getValue();
            SubSelectActivationHolder holder = factoryDesc.getSubSelectActivationHolder();

            // activate viewable
            ViewableActivationResult subselectActivationResult = holder.getActivator().activate(agentInstanceContext, true, isRecoveringResilient);
            stopCallbackList.add(subselectActivationResult.getStopCallback());

            // apply returning the strategy instance
            SubSelectStrategyRealization result = factoryDesc.getFactory().instantiate(services, subselectActivationResult.getViewable(), agentInstanceContext, stopCallbackList, factoryDesc.getSubqueryNumber(), isRecoveringResilient);

            // handle stoppable view
            if (result.getSubselectView() instanceof StoppableView) {
                stopCallbackList.add((StoppableView) result.getSubselectView());
            }
            if (result.getSubselectAggregationService() != null) {
                final AggregationService subselectAggregationService = result.getSubselectAggregationService();
                stopCallbackList.add(new StopCallback() {
                    public void stop() {
                        subselectAggregationService.stop();
                    }
                });
            }

            // set aggregation
            final SubordTableLookupStrategy lookupStrategy = result.getStrategy();
            final SubselectAggregationPreprocessorBase aggregationPreprocessor = result.getSubselectAggregationPreprocessor();

            // determine strategy
            ExprSubselectStrategy strategy;
            if (aggregationPreprocessor != null) {
                strategy = new ExprSubselectStrategy() {
                    public Collection<EventBean> evaluateMatching(EventBean[] eventsPerStream, ExprEvaluatorContext exprEvaluatorContext) {
                        Collection<EventBean> matchingEvents = lookupStrategy.lookup(eventsPerStream, exprEvaluatorContext);
                        aggregationPreprocessor.evaluate(eventsPerStream, matchingEvents, exprEvaluatorContext);
                        return CollectionUtil.SINGLE_NULL_ROW_EVENT_SET;
                    }
                };
            } else {
                strategy = new ExprSubselectStrategy() {
                    public Collection<EventBean> evaluateMatching(EventBean[] eventsPerStream, ExprEvaluatorContext exprEvaluatorContext) {
                        return lookupStrategy.lookup(eventsPerStream, exprEvaluatorContext);
                    }
                };
            }

            SubSelectStrategyHolder instance = new SubSelectStrategyHolder(strategy,
                    result.getSubselectAggregationService(),
                    result.getPriorNodeStrategies(),
                    result.getPreviousNodeStrategies(),
                    result.getSubselectView(),
                    result.getPostLoad(),
                    subselectActivationResult);
            subselectStrategies.put(subselectNode, instance);
        }

        return subselectStrategies;
    }

    private static Pair<EventTableFactory, SubordTableLookupStrategyFactory> determineSubqueryIndexFactory(ExprNode filterExpr,
                                                                                                           EventType viewableEventType,
                                                                                                           EventType[] outerEventTypes,
                                                                                                           StreamTypeService subselectTypeService,
                                                                                                           boolean fullTableScan,
                                                                                                           boolean queryPlanLogging,
                                                                                                           Set<String> optionalUniqueProps,
                                                                                                           StatementContext statementContext,
                                                                                                           int subqueryNum)
            throws ExprValidationException {
        Pair<EventTableFactory, SubordTableLookupStrategyFactory> result = determineSubqueryIndexInternalFactory(filterExpr, viewableEventType, outerEventTypes, subselectTypeService, fullTableScan, optionalUniqueProps, statementContext);

        QueryPlanIndexHook hook = QueryPlanIndexHookUtil.getHook(statementContext.getAnnotations(), statementContext.getEngineImportService());
        if (queryPlanLogging && (QUERY_PLAN_LOG.isInfoEnabled() || hook != null)) {
            QUERY_PLAN_LOG.info("local index");
            QUERY_PLAN_LOG.info("strategy " + result.getSecond().toQueryPlan());
            QUERY_PLAN_LOG.info("table " + result.getFirst().toQueryPlan());
            if (hook != null) {
                String strategyName = result.getSecond().getClass().getSimpleName();
                hook.subquery(new QueryPlanIndexDescSubquery(
                    new IndexNameAndDescPair[]{
                        new IndexNameAndDescPair(null, result.getFirst().getEventTableClass().getSimpleName())
                    }, subqueryNum, strategyName));
            }
        }

        return result;
    }

    private static Pair<EventTableFactory, SubordTableLookupStrategyFactory> determineSubqueryIndexInternalFactory(ExprNode filterExpr,
                                                                                                                   EventType viewableEventType,
                                                                                                                   EventType[] outerEventTypes,
                                                                                                                   StreamTypeService subselectTypeService,
                                                                                                                   boolean fullTableScan,
                                                                                                                   Set<String> optionalUniqueProps,
                                                                                                                   StatementContext statementContext)
            throws ExprValidationException {
        // No filter expression means full table scan
        if ((filterExpr == null) || fullTableScan) {
            EventTableFactory tableFactory = statementContext.getEventTableIndexService().createUnindexed(0, null, false);
            SubordFullTableScanLookupStrategyFactory strategy = new SubordFullTableScanLookupStrategyFactory();
            return new Pair<EventTableFactory, SubordTableLookupStrategyFactory>(tableFactory, strategy);
        }

        // Build a list of streams and indexes
        ExcludePlanHint excludePlanHint = ExcludePlanHint.getHint(subselectTypeService.getStreamNames(), statementContext);
        SubordPropPlan joinPropDesc = QueryPlanIndexBuilder.getJoinProps(filterExpr, outerEventTypes.length, subselectTypeService.getEventTypes(), excludePlanHint);
        Map<String, SubordPropHashKey> hashKeys = joinPropDesc.getHashProps();
        Map<String, SubordPropRangeKey> rangeKeys = joinPropDesc.getRangeProps();
        List<SubordPropHashKey> hashKeyList = new ArrayList<SubordPropHashKey>(hashKeys.values());
        List<SubordPropRangeKey> rangeKeyList = new ArrayList<SubordPropRangeKey>(rangeKeys.values());
        boolean unique = false;
        ExprNode[] inKeywordSingleIdxKeys = null;
        ExprNode inKeywordMultiIdxKey = null;

        // If this is a unique-view and there are unique criteria, use these
        if (optionalUniqueProps != null && !optionalUniqueProps.isEmpty()) {
            boolean found = true;
            for (String uniqueProp : optionalUniqueProps) {
                if (!hashKeys.containsKey(uniqueProp)) {
                    found = false;
                    break;
                }
            }
            if (found) {
                String[] hashKeysArray = hashKeys.keySet().toArray(new String[hashKeys.keySet().size()]);
                for (String hashKey : hashKeysArray) {
                    if (!optionalUniqueProps.contains(hashKey)) {
                        hashKeys.remove(hashKey);
                    }
                }
                hashKeyList = new ArrayList<SubordPropHashKey>(hashKeys.values());
                unique = true;
                rangeKeyList.clear();
                rangeKeys.clear();
            }
        }

        // build table (local table)
        EventTableFactory eventTableFactory;
        CoercionDesc hashCoercionDesc;
        CoercionDesc rangeCoercionDesc;
        if (hashKeys.size() != 0 && rangeKeys.isEmpty()) {
            String[] indexedProps = hashKeys.keySet().toArray(new String[hashKeys.keySet().size()]);
            hashCoercionDesc = CoercionUtil.getCoercionTypesHash(viewableEventType, indexedProps, hashKeyList);
            rangeCoercionDesc = new CoercionDesc(false, null);

            if (hashKeys.size() == 1) {
                if (!hashCoercionDesc.isCoerce()) {
                    eventTableFactory = statementContext.getEventTableIndexService().createSingle(0, viewableEventType, indexedProps[0], unique, null, null, false);
                } else {
                    eventTableFactory = statementContext.getEventTableIndexService().createSingleCoerceAdd(0, viewableEventType, indexedProps[0], hashCoercionDesc.getCoercionTypes()[0], null, false);
                }
            } else {
                if (!hashCoercionDesc.isCoerce()) {
                    eventTableFactory = statementContext.getEventTableIndexService().createMultiKey(0, viewableEventType, indexedProps, unique, null, null, false);
                } else {
                    eventTableFactory = statementContext.getEventTableIndexService().createMultiKeyCoerceAdd(0, viewableEventType, indexedProps, hashCoercionDesc.getCoercionTypes(), false);
                }
            }
        } else if (hashKeys.isEmpty() && rangeKeys.isEmpty()) {
            hashCoercionDesc = new CoercionDesc(false, null);
            rangeCoercionDesc = new CoercionDesc(false, null);
            if (joinPropDesc.getInKeywordSingleIndex() != null) {
                eventTableFactory = statementContext.getEventTableIndexService().createSingle(0, viewableEventType, joinPropDesc.getInKeywordSingleIndex().getIndexedProp(), unique, null, null, false);
                inKeywordSingleIdxKeys = joinPropDesc.getInKeywordSingleIndex().getExpressions();
            } else if (joinPropDesc.getInKeywordMultiIndex() != null) {
                eventTableFactory = statementContext.getEventTableIndexService().createInArray(0, viewableEventType, joinPropDesc.getInKeywordMultiIndex().getIndexedProp(), unique);
                inKeywordMultiIdxKey = joinPropDesc.getInKeywordMultiIndex().getExpression();
            } else {
                eventTableFactory = statementContext.getEventTableIndexService().createUnindexed(0, null, false);
            }
        } else if (hashKeys.isEmpty() && rangeKeys.size() == 1) {
            String indexedProp = rangeKeys.keySet().iterator().next();
            CoercionDesc coercionRangeTypes = CoercionUtil.getCoercionTypesRange(viewableEventType, rangeKeys, outerEventTypes);
            if (!coercionRangeTypes.isCoerce()) {
                eventTableFactory = statementContext.getEventTableIndexService().createSorted(0, viewableEventType, indexedProp, false);
            } else {
                eventTableFactory = statementContext.getEventTableIndexService().createSortedCoerce(0, viewableEventType, indexedProp, coercionRangeTypes.getCoercionTypes()[0], false);
            }
            hashCoercionDesc = new CoercionDesc(false, null);
            rangeCoercionDesc = coercionRangeTypes;
        } else {
            String[] indexedKeyProps = hashKeys.keySet().toArray(new String[hashKeys.keySet().size()]);
            Class[] coercionKeyTypes = SubordPropUtil.getCoercionTypes(hashKeys.values());
            String[] indexedRangeProps = rangeKeys.keySet().toArray(new String[rangeKeys.keySet().size()]);
            CoercionDesc coercionRangeTypes = CoercionUtil.getCoercionTypesRange(viewableEventType, rangeKeys, outerEventTypes);
            eventTableFactory = statementContext.getEventTableIndexService().createComposite(0, viewableEventType, indexedKeyProps, coercionKeyTypes, indexedRangeProps, coercionRangeTypes.getCoercionTypes(), false);
            hashCoercionDesc = CoercionUtil.getCoercionTypesHash(viewableEventType, indexedKeyProps, hashKeyList);
            rangeCoercionDesc = coercionRangeTypes;
        }

        SubordTableLookupStrategyFactory subqTableLookupStrategyFactory = SubordinateTableLookupStrategyUtil.getLookupStrategy(outerEventTypes,
                hashKeyList, hashCoercionDesc, rangeKeyList, rangeCoercionDesc, inKeywordSingleIdxKeys, inKeywordMultiIdxKey, false);

        return new Pair<EventTableFactory, SubordTableLookupStrategyFactory>(eventTableFactory, subqTableLookupStrategyFactory);
    }

    private static StreamTypeService getDeclaredExprTypeService(ExprDeclaredNode[] declaredExpressions,
                                                                Map<ExprDeclaredNode, List<ExprDeclaredNode>> declaredExpressionCallHierarchy,
                                                                String[] outerStreamNames,
                                                                EventType[] outerEventTypesSelect,
                                                                String engineURI,
                                                                ExprSubselectNode subselect,
                                                                String subexpressionStreamName,
                                                                EventType eventType)
            throws ExprValidationException {
        // Find that subselect within that any of the expression declarations
        for (ExprDeclaredNode declaration : declaredExpressions) {
            ExprNodeSubselectDeclaredNoTraverseVisitor visitor = new ExprNodeSubselectDeclaredNoTraverseVisitor(declaration);
            visitor.reset();
            declaration.accept(visitor);
            if (!visitor.getSubselects().contains(subselect)) {
                continue;
            }

            // no type service for "alias"
            if (declaration.getPrototype().isAlias()) {
                return null;
            }

            // subselect found - compute outer stream names
            // initialize from the outermost provided stream names
            Map<String, Integer> outerStreamNamesMap = new LinkedHashMap<String, Integer>();
            int count = 0;
            for (String outerStreamName : outerStreamNames) {
                outerStreamNamesMap.put(outerStreamName, count++);
            }

            // give each declared expression a chance to change the names (unless alias expression)
            Map<String, Integer> outerStreamNamesForSubselect = outerStreamNamesMap;
            List<ExprDeclaredNode> callers = declaredExpressionCallHierarchy.get(declaration);
            for (ExprDeclaredNode caller : callers) {
                outerStreamNamesForSubselect = caller.getOuterStreamNames(outerStreamNamesForSubselect);
            }
            outerStreamNamesForSubselect = declaration.getOuterStreamNames(outerStreamNamesForSubselect);

            // compile a new StreamTypeService for use in validating that particular subselect
            EventType[] eventTypes = new EventType[outerStreamNamesForSubselect.size() + 1];
            String[] streamNames = new String[outerStreamNamesForSubselect.size() + 1];
            eventTypes[0] = eventType;
            streamNames[0] = subexpressionStreamName;
            count = 0;
            for (Map.Entry<String, Integer> entry : outerStreamNamesForSubselect.entrySet()) {
                eventTypes[count + 1] = outerEventTypesSelect[entry.getValue()];
                streamNames[count + 1] = entry.getKey();
                count++;
            }

            StreamTypeServiceImpl availableTypes = new StreamTypeServiceImpl(eventTypes, streamNames, new boolean[eventTypes.length], engineURI, false, false);
            availableTypes.setRequireStreamNames(true);
            return availableTypes;
        }
        return null;
    }

    private static SubSelectStrategyFactoryDesc planSubSelectInternal(int subqueryNum,
                                                                      ExprSubselectNode subselect,
                                                                      SubSelectActivationHolder subSelectActivation,
                                                                      EPServicesContext services,
                                                                      StatementContext statementContext,
                                                                      boolean queryPlanLogging,
                                                                      SubSelectActivationCollection subSelectStreamDesc,
                                                                      String[] outerStreamNames,
                                                                      EventType[] outerEventTypesSelect,
                                                                      String[] outerEventTypeNamees,
                                                                      ExprDeclaredNode[] declaredExpressions,
                                                                      ContextPropertyRegistry contextPropertyRegistry,
                                                                      Map<ExprDeclaredNode, List<ExprDeclaredNode>> declaredExpressionCallHierarchy)
            throws ExprValidationException {
        if (queryPlanLogging && QUERY_PLAN_LOG.isInfoEnabled()) {
            QUERY_PLAN_LOG.info("For statement '" + statementContext.getStatementName() + "' subquery " + subqueryNum);
        }

        Annotation[] annotations = statementContext.getAnnotations();
        IndexHint indexHint = IndexHint.getIndexHint(statementContext.getAnnotations());
        StatementSpecCompiled statementSpec = subselect.getStatementSpecCompiled();
        StreamSpecCompiled filterStreamSpec = statementSpec.getStreamSpecs()[0];

        String subselecteventTypeName = null;
        if (filterStreamSpec instanceof FilterStreamSpecCompiled) {
            subselecteventTypeName = ((FilterStreamSpecCompiled) filterStreamSpec).getFilterSpec().getFilterForEventTypeName();
        } else if (filterStreamSpec instanceof NamedWindowConsumerStreamSpec) {
            subselecteventTypeName = ((NamedWindowConsumerStreamSpec) filterStreamSpec).getWindowName();
        } else if (filterStreamSpec instanceof TableQueryStreamSpec) {
            subselecteventTypeName = ((TableQueryStreamSpec) filterStreamSpec).getTableName();
        }

        ViewFactoryChain viewFactoryChain = subSelectStreamDesc.getViewFactoryChain(subselect);
        EventType eventType = viewFactoryChain.getEventType();

        // determine a stream name unless one was supplied
        String subexpressionStreamName = filterStreamSpec.getOptionalStreamName();
        int subselectStreamNumber = subSelectStreamDesc.getStreamNumber(subselect);
        if (subexpressionStreamName == null) {
            subexpressionStreamName = "$subselect_" + subselectStreamNumber;
        }
        String[] allStreamNames = new String[outerStreamNames.length + 1];
        System.arraycopy(outerStreamNames, 0, allStreamNames, 1, outerStreamNames.length);
        allStreamNames[0] = subexpressionStreamName;

        // Named windows don't allow data views
        if (filterStreamSpec instanceof NamedWindowConsumerStreamSpec || filterStreamSpec instanceof TableQueryStreamSpec) {
            EPStatementStartMethodHelperValidate.validateNoDataWindowOnNamedWindow(viewFactoryChain.getViewFactoryChain());
        }

        // Expression declarations are copies of a predefined expression body with their own stream context.
        // Should only be invoked if the subselect belongs to that instance.
        StreamTypeService subselectTypeService = null;
        EventType[] outerEventTypes = null;

        // determine subselect type information from the enclosing declared expression, if possibly enclosed
        if (declaredExpressions.length > 0) {
            subselectTypeService = getDeclaredExprTypeService(declaredExpressions, declaredExpressionCallHierarchy, outerStreamNames, outerEventTypesSelect, services.getEngineURI(), subselect, subexpressionStreamName, eventType);
            if (subselectTypeService != null) {
                outerEventTypes = new EventType[subselectTypeService.getEventTypes().length - 1];
                System.arraycopy(subselectTypeService.getEventTypes(), 1, outerEventTypes, 0, subselectTypeService.getEventTypes().length - 1);
            }
        }

        // Use the override provided by the subselect if present
        if (subselectTypeService == null) {
            if (subselect.getFilterSubqueryStreamTypes() != null) {
                subselectTypeService = subselect.getFilterSubqueryStreamTypes();
                outerEventTypes = new EventType[subselectTypeService.getEventTypes().length - 1];
                System.arraycopy(subselectTypeService.getEventTypes(), 1, outerEventTypes, 0, subselectTypeService.getEventTypes().length - 1);
            } else {
                // Streams event types are the original stream types with the stream zero the subselect stream
                LinkedHashMap<String, Pair<EventType, String>> namesAndTypes = new LinkedHashMap<String, Pair<EventType, String>>();
                namesAndTypes.put(subexpressionStreamName, new Pair<EventType, String>(eventType, subselecteventTypeName));
                for (int i = 0; i < outerEventTypesSelect.length; i++) {
                    Pair<EventType, String> pair = new Pair<EventType, String>(outerEventTypesSelect[i], outerEventTypeNamees[i]);
                    namesAndTypes.put(outerStreamNames[i], pair);
                }
                subselectTypeService = new StreamTypeServiceImpl(namesAndTypes, services.getEngineURI(), true, true);
                outerEventTypes = outerEventTypesSelect;
            }
        }

        // Validate select expression
        ViewResourceDelegateUnverified viewResourceDelegateSubselect = new ViewResourceDelegateUnverified();
        SelectClauseSpecCompiled selectClauseSpec = subselect.getStatementSpecCompiled().getSelectClauseSpec();
        AggregationServiceFactoryDesc aggregationServiceFactoryDesc = null;
        List<ExprNode> selectExpressions = new ArrayList<ExprNode>();
        List<String> assignedNames = new ArrayList<String>();
        boolean isWildcard = false;
        boolean isStreamWildcard = false;
        ExprEvaluator[] groupByEvaluators = null;
        boolean hasNonAggregatedProperties;

        ExprEvaluatorContextStatement evaluatorContextStmt = new ExprEvaluatorContextStatement(statementContext, false);
        ExprValidationContext validationContext = new ExprValidationContext(subselectTypeService, statementContext.getEngineImportService(), statementContext.getStatementExtensionServicesContext(), viewResourceDelegateSubselect, statementContext.getSchedulingService(), statementContext.getVariableService(), statementContext.getTableService(), evaluatorContextStmt, statementContext.getEventAdapterService(), statementContext.getStatementName(), statementContext.getStatementId(), statementContext.getAnnotations(), statementContext.getContextDescriptor(), false, false, true, false, null, false);
        List<ExprAggregateNode> aggExprNodesSelect = new ArrayList<>(2);

        for (int i = 0; i < selectClauseSpec.getSelectExprList().length; i++) {
            SelectClauseElementCompiled element = selectClauseSpec.getSelectExprList()[i];

            if (element instanceof SelectClauseExprCompiledSpec) {
                // validate
                SelectClauseExprCompiledSpec compiled = (SelectClauseExprCompiledSpec) element;
                ExprNode selectExpression = compiled.getSelectExpression();
                selectExpression = ExprNodeUtilityRich.getValidatedSubtree(ExprNodeOrigin.SELECT, selectExpression, validationContext);

                selectExpressions.add(selectExpression);
                if (compiled.getAssignedName() == null) {
                    assignedNames.add(ExprNodeUtilityCore.toExpressionStringMinPrecedenceSafe(selectExpression));
                } else {
                    assignedNames.add(compiled.getAssignedName());
                }

                // handle aggregation
                ExprAggregateNodeUtil.getAggregatesBottomUp(selectExpression, aggExprNodesSelect);

                // This stream (stream 0) properties must either all be under aggregation, or all not be.
                if (aggExprNodesSelect.size() > 0) {
                    List<Pair<Integer, String>> propertiesNotAggregated = ExprNodeUtilityRich.getExpressionProperties(selectExpression, false);
                    for (Pair<Integer, String> pair : propertiesNotAggregated) {
                        if (pair.getFirst() == 0) {
                            throw new ExprValidationException("Subselect properties must all be within aggregation functions");
                        }
                    }
                }
            } else if (element instanceof SelectClauseElementWildcard) {
                isWildcard = true;
            } else if (element instanceof SelectClauseStreamCompiledSpec) {
                isStreamWildcard = true;
            }
        }   // end of for loop

        // validate having-clause and collect aggregations
        List<ExprAggregateNode> aggExpressionNodesHaving = Collections.emptyList();
        if (statementSpec.getHavingExprRootNode() != null) {
            ExprNode validatedHavingClause = ExprNodeUtilityRich.getValidatedSubtree(ExprNodeOrigin.HAVING, statementSpec.getHavingExprRootNode(), validationContext);
            if (JavaClassHelper.getBoxedType(validatedHavingClause.getForge().getEvaluationType()) != Boolean.class) {
                throw new ExprValidationException("Subselect having-clause expression must return a boolean value");
            }
            aggExpressionNodesHaving = new ArrayList<>();
            ExprAggregateNodeUtil.getAggregatesBottomUp(validatedHavingClause, aggExpressionNodesHaving);
            validateAggregationPropsAndLocalGroup(aggExpressionNodesHaving);

            // if the having-clause does not have aggregations, it becomes part of the filter
            if (aggExpressionNodesHaving.isEmpty()) {
                ExprNode filter = statementSpec.getFilterRootNode();
                if (filter == null) {
                    statementSpec.setFilterExprRootNode(statementSpec.getHavingExprRootNode());
                } else {
                    statementSpec.setFilterExprRootNode(ExprNodeUtilityRich.connectExpressionsByLogicalAnd(Arrays.asList(statementSpec.getFilterRootNode(), statementSpec.getHavingExprRootNode())));
                }
                statementSpec.setHavingExprRootNode(null);
            } else {
                ExprEvaluator havingEvaluator = ExprNodeCompiler.allocateEvaluator(validatedHavingClause.getForge(), statementContext.getEngineImportService(), EPStatementStartMethodHelperSubselect.class, false, statementContext.getStatementName());
                subselect.setHavingExpr(havingEvaluator);
                ExprNodePropOrStreamSet nonAggregatedPropsHaving = ExprNodeUtilityRich.getNonAggregatedProps(validationContext.getStreamTypeService().getEventTypes(), Collections.singletonList(validatedHavingClause), contextPropertyRegistry);
                for (ExprNodePropOrStreamPropDesc prop : nonAggregatedPropsHaving.getProperties()) {
                    if (prop.getStreamNum() == 0) {
                        throw new ExprValidationException("Subselect having-clause requires that all properties are under aggregation, consider using the 'first' aggregation function instead");
                    }
                }
            }
        }

        // Figure out all non-aggregated event properties in the select clause (props not under a sum/avg/max aggregation node)
        ExprNodePropOrStreamSet nonAggregatedPropsSelect = ExprNodeUtilityRich.getNonAggregatedProps(validationContext.getStreamTypeService().getEventTypes(), selectExpressions, contextPropertyRegistry);
        hasNonAggregatedProperties = !nonAggregatedPropsSelect.isEmpty();

        // Validate and set select-clause names and expressions
        if (!selectExpressions.isEmpty()) {
            if (isWildcard || isStreamWildcard) {
                throw new ExprValidationException("Subquery multi-column select does not allow wildcard or stream wildcard when selecting multiple columns.");
            }
            if (selectExpressions.size() > 1 && !subselect.isAllowMultiColumnSelect()) {
                throw new ExprValidationException("Subquery multi-column select is not allowed in this context.");
            }
            if (statementSpec.getGroupByExpressions() == null && selectExpressions.size() > 1 &&
                    aggExprNodesSelect.size() > 0 && hasNonAggregatedProperties) {
                throw new ExprValidationException("Subquery with multi-column select requires that either all or none of the selected columns are under aggregation, unless a group-by clause is also specified");
            }
            subselect.setSelectClause(selectExpressions.toArray(new ExprNode[selectExpressions.size()]), statementContext.getEngineImportService(), statementContext.getStatementName());
            subselect.setSelectAsNames(assignedNames.toArray(new String[assignedNames.size()]));
        }

        // Handle aggregation
        ExprNodePropOrStreamSet propertiesGroupBy = null;
        if (aggExprNodesSelect.size() > 0 || aggExpressionNodesHaving.size() > 0) {
            if (statementSpec.getGroupByExpressions() != null && statementSpec.getGroupByExpressions().getGroupByRollupLevels() != null) {
                throw new ExprValidationException("Group-by expressions in a subselect may not have rollups");
            }
            ExprNode[] theGroupBy = statementSpec.getGroupByExpressions() == null ? null : statementSpec.getGroupByExpressions().getGroupByNodes();
            boolean hasGroupBy = theGroupBy != null && theGroupBy.length > 0;
            if (hasGroupBy) {
                ExprNode[] groupByNodes = statementSpec.getGroupByExpressions().getGroupByNodes();
                groupByEvaluators = new ExprEvaluator[groupByNodes.length];

                // validate group-by
                for (int i = 0; i < groupByNodes.length; i++) {
                    groupByNodes[i] = ExprNodeUtilityRich.getValidatedSubtree(ExprNodeOrigin.GROUPBY, groupByNodes[i], validationContext);
                    groupByEvaluators[i] = ExprNodeCompiler.allocateEvaluator(groupByNodes[i].getForge(), statementContext.getEngineImportService(), EPStatementStartMethodHelperSubselect.class, false, statementContext.getStatementName());
                    String minimal = ExprNodeUtilityRich.isMinimalExpression(groupByNodes[i]);
                    if (minimal != null) {
                        throw new ExprValidationException("Group-by expressions in a subselect may not have " + minimal);
                    }
                }

                // Get a list of event properties being aggregated in the select clause, if any
                propertiesGroupBy = ExprNodeUtilityRich.getGroupByPropertiesValidateHasOne(groupByNodes);

                // Validated all group-by properties come from stream itself
                ExprNodePropOrStreamDesc firstNonZeroGroupBy = propertiesGroupBy.getFirstWithStreamNumNotZero();
                if (firstNonZeroGroupBy != null) {
                    throw new ExprValidationException("Subselect with group-by requires that group-by properties are provided by the subselect stream only (" + firstNonZeroGroupBy.getTextual() + " is not)");
                }

                // Validate that this is a grouped full-aggregated case
                String reasonMessage = propertiesGroupBy.notContainsAll(nonAggregatedPropsSelect);
                boolean allInGroupBy = reasonMessage == null;
                if (!allInGroupBy) {
                    throw new ExprValidationException("Subselect with group-by requires non-aggregated properties in the select-clause to also appear in the group-by clause");
                }
            }

            // Other stream properties, if there is aggregation, cannot be under aggregation.
            validateAggregationPropsAndLocalGroup(aggExprNodesSelect);

            // determine whether select-clause has grouped-by expressions
            List<ExprAggregateNodeGroupKey> groupKeyExpressions = null;
            ExprNode[] groupByExpressions = ExprNodeUtilityCore.EMPTY_EXPR_ARRAY;
            if (hasGroupBy) {
                groupByExpressions = statementSpec.getGroupByExpressions().getGroupByNodes();
                for (int i = 0; i < selectExpressions.size(); i++) {
                    ExprNode selectExpression = selectExpressions.get(i);
                    boolean revalidate = false;
                    for (int j = 0; j < groupByExpressions.length; j++) {
                        List<Pair<ExprNode, ExprNode>> foundPairs = ExprNodeUtilityCore.findExpression(selectExpression, groupByExpressions[j]);
                        for (Pair<ExprNode, ExprNode> pair : foundPairs) {
                            ExprAggregateNodeGroupKey replacement = new ExprAggregateNodeGroupKey(j, groupByExpressions[j].getForge().getEvaluationType());
                            if (pair.getFirst() == null) {
                                selectExpressions.set(i, replacement);
                            } else {
                                ExprNodeUtilityCore.replaceChildNode(pair.getFirst(), pair.getSecond(), replacement);
                                revalidate = true;
                            }
                            if (groupKeyExpressions == null) {
                                groupKeyExpressions = new ArrayList<ExprAggregateNodeGroupKey>();
                            }
                            groupKeyExpressions.add(replacement);
                        }
                    }

                    // if the select-clause expression changed, revalidate it
                    if (revalidate) {
                        selectExpression = ExprNodeUtilityRich.getValidatedSubtree(ExprNodeOrigin.SELECT, selectExpression, validationContext);
                        selectExpressions.set(i, selectExpression);
                    }
                }   // end of for loop
            }

            AggregationServiceForgeDesc forge = AggregationServiceFactoryFactory.getService(aggExprNodesSelect, Collections.<ExprNode, String>emptyMap(), Collections.<ExprDeclaredNode>emptyList(), groupByExpressions, aggExpressionNodesHaving, Collections.<ExprAggregateNode>emptyList(), groupKeyExpressions, hasGroupBy, annotations, statementContext.getVariableService(), false, true, statementSpec.getFilterRootNode(), statementSpec.getHavingExprRootNode(), statementContext.getAggregationServiceFactoryService(), subselectTypeService.getEventTypes(), null, statementSpec.getOptionalContextName(), null, null, false, false, false, statementContext.getEngineImportService(), statementContext.getStatementName(), statementContext.getTimeAbacus());
            AggregationServiceFactory aggregationServiceFactory = AggregationServiceFactoryCompiler.allocate(forge.getAggregationServiceFactoryForge(), statementContext, false);
            aggregationServiceFactoryDesc = new AggregationServiceFactoryDesc(aggregationServiceFactory, forge.getExpressions(), forge.getGroupKeyExpressions());

            // assign select-clause
            if (!selectExpressions.isEmpty()) {
                subselect.setSelectClause(selectExpressions.toArray(new ExprNode[selectExpressions.size()]), statementContext.getEngineImportService(), statementContext.getStatementName());
                subselect.setSelectAsNames(assignedNames.toArray(new String[assignedNames.size()]));
            }
        }

        // no aggregation functions allowed in filter
        if (statementSpec.getFilterRootNode() != null) {
            List<ExprAggregateNode> aggExprNodesFilter = new LinkedList<ExprAggregateNode>();
            ExprAggregateNodeUtil.getAggregatesBottomUp(statementSpec.getFilterRootNode(), aggExprNodesFilter);
            if (aggExprNodesFilter.size() > 0) {
                throw new ExprValidationException("Aggregation functions are not supported within subquery filters, consider using a having-clause or insert-into instead");
            }
        }

        // validate filter expression, if there is one
        ExprNode filterExpr = statementSpec.getFilterRootNode();

        // add the table filter for tables
        if (filterStreamSpec instanceof TableQueryStreamSpec) {
            TableQueryStreamSpec table = (TableQueryStreamSpec) filterStreamSpec;
            filterExpr = ExprNodeUtilityRich.connectExpressionsByLogicalAnd(table.getFilterExpressions(), filterExpr);
        }

        // determine correlated
        boolean correlatedSubquery = false;
        if (filterExpr != null) {
            filterExpr = ExprNodeUtilityRich.getValidatedSubtree(ExprNodeOrigin.FILTER, filterExpr, validationContext);
            if (JavaClassHelper.getBoxedType(filterExpr.getForge().getEvaluationType()) != Boolean.class) {
                throw new ExprValidationException("Subselect filter expression must return a boolean value");
            }

            // check the presence of a correlated filter, not allowed with aggregation
            ExprNodeIdentifierVisitor visitor = new ExprNodeIdentifierVisitor(true);
            filterExpr.accept(visitor);
            List<Pair<Integer, String>> propertiesNodes = visitor.getExprProperties();
            for (Pair<Integer, String> pair : propertiesNodes) {
                if (pair.getFirst() != 0) {
                    correlatedSubquery = true;
                    break;
                }
            }
        }

        ViewResourceDelegateVerified viewResourceDelegateVerified = EPStatementStartMethodHelperViewResources.verifyPreviousAndPriorRequirements(new ViewFactoryChain[]{viewFactoryChain}, viewResourceDelegateSubselect);
        List<ExprPriorNode> priorNodes = viewResourceDelegateVerified.getPerStream()[0].getPriorRequestsAsList();
        List<ExprPreviousNode> previousNodes = viewResourceDelegateVerified.getPerStream()[0].getPreviousRequests();

        // Set the aggregated flag
        // This must occur here as some analysis of return type depends on aggregated or not.
        if (aggregationServiceFactoryDesc == null) {
            subselect.setSubselectAggregationType(ExprSubselectNode.SubqueryAggregationType.NONE);
        } else {
            subselect.setSubselectAggregationType(hasNonAggregatedProperties ? ExprSubselectNode.SubqueryAggregationType.FULLY_AGGREGATED_WPROPS : ExprSubselectNode.SubqueryAggregationType.FULLY_AGGREGATED_NOPROPS);
        }

        // Set the filter.
        ExprEvaluator filterExprEval = (filterExpr == null) ? null : ExprNodeCompiler.allocateEvaluator(filterExpr.getForge(), statementContext.getEngineImportService(), EPStatementStartMethodHelperSubselect.class, false, statementContext.getStatementName());
        ExprEvaluator assignedFilterExpr = aggregationServiceFactoryDesc != null ? null : filterExprEval;
        subselect.setFilterExpr(assignedFilterExpr);

        // validation for correlated subqueries against named windows contained-event syntax
        if (filterStreamSpec instanceof NamedWindowConsumerStreamSpec && correlatedSubquery) {
            NamedWindowConsumerStreamSpec namedSpec = (NamedWindowConsumerStreamSpec) filterStreamSpec;
            if (namedSpec.getOptPropertyEvaluator() != null) {
                throw new ExprValidationException("Failed to validate named window use in subquery, contained-event is only allowed for named windows when not correlated");
            }
        }

        // Validate presence of a data window
        validateSubqueryDataWindow(subselect, correlatedSubquery, hasNonAggregatedProperties, propertiesGroupBy, nonAggregatedPropsSelect);

        // Determine strategy factories
        //

        // handle named window index share first
        if (filterStreamSpec instanceof NamedWindowConsumerStreamSpec) {
            NamedWindowConsumerStreamSpec namedSpec = (NamedWindowConsumerStreamSpec) filterStreamSpec;
            if (namedSpec.getFilterExpressions().isEmpty()) {
                NamedWindowProcessor processor = services.getNamedWindowMgmtService().getProcessor(namedSpec.getWindowName());
                if (processor == null) {
                    throw new ExprValidationException("A named window by name '" + namedSpec.getWindowName() + "' does not exist");
                }

                boolean disableIndexShare = HintEnum.DISABLE_WINDOW_SUBQUERY_INDEXSHARE.getHint(annotations) != null;
                if (disableIndexShare && processor.isVirtualDataWindow()) {
                    disableIndexShare = false;
                }

                if (!disableIndexShare && processor.isEnableSubqueryIndexShare()) {
                    validateContextAssociation(statementContext, processor.getContextName(), "named window '" + processor.getNamedWindowName() + "'");
                    if (queryPlanLogging && QUERY_PLAN_LOG.isInfoEnabled()) {
                        QUERY_PLAN_LOG.info("prefering shared index");
                    }
                    boolean fullTableScan = HintEnum.SET_NOINDEX.getHint(annotations) != null;
                    ExcludePlanHint excludePlanHint = ExcludePlanHint.getHint(allStreamNames, statementContext);
                    SubordPropPlan joinedPropPlan = QueryPlanIndexBuilder.getJoinProps(filterExpr, outerEventTypes.length, subselectTypeService.getEventTypes(), excludePlanHint);
                    SubSelectStrategyFactory factory = new SubSelectStrategyFactoryIndexShare(statementContext.getStatementName(), statementContext.getStatementId(), subqueryNum, outerEventTypesSelect,
                            processor, null, fullTableScan, indexHint, joinedPropPlan, filterExprEval, aggregationServiceFactoryDesc, groupByEvaluators, services.getTableService(), statementContext.getAnnotations(), statementContext.getStatementStopService(), statementContext.getEngineImportService());
                    return new SubSelectStrategyFactoryDesc(subSelectActivation, factory, aggregationServiceFactoryDesc, priorNodes, previousNodes, subqueryNum);
                }
            }
        }

        // handle table-subselect
        if (filterStreamSpec instanceof TableQueryStreamSpec) {
            TableQueryStreamSpec tableSpec = (TableQueryStreamSpec) filterStreamSpec;
            TableMetadata metadata = services.getTableService().getTableMetadata(tableSpec.getTableName());
            if (metadata == null) {
                throw new ExprValidationException("A table by name '" + tableSpec.getTableName() + "' does not exist");
            }

            validateContextAssociation(statementContext, metadata.getContextName(), "table '" + tableSpec.getTableName() + "'");
            boolean fullTableScan = HintEnum.SET_NOINDEX.getHint(annotations) != null;
            ExcludePlanHint excludePlanHint = ExcludePlanHint.getHint(allStreamNames, statementContext);
            SubordPropPlan joinedPropPlan = QueryPlanIndexBuilder.getJoinProps(filterExpr, outerEventTypes.length, subselectTypeService.getEventTypes(), excludePlanHint);
            SubSelectStrategyFactory factory = new SubSelectStrategyFactoryIndexShare(statementContext.getStatementName(), statementContext.getStatementId(), subqueryNum, outerEventTypesSelect,
                    null, metadata, fullTableScan, indexHint, joinedPropPlan, filterExprEval, aggregationServiceFactoryDesc, groupByEvaluators, services.getTableService(), statementContext.getAnnotations(), statementContext.getStatementStopService(), statementContext.getEngineImportService());
            return new SubSelectStrategyFactoryDesc(subSelectActivation, factory, aggregationServiceFactoryDesc, priorNodes, previousNodes, subqueryNum);
        }

        // determine unique keys, if any
        Set<String> optionalUniqueProps = null;
        if (viewFactoryChain.getDataWindowViewFactoryCount() > 0) {
            optionalUniqueProps = ViewServiceHelper.getUniqueCandidateProperties(viewFactoryChain.getViewFactoryChain(), annotations);
        }
        if (filterStreamSpec instanceof NamedWindowConsumerStreamSpec) {
            NamedWindowConsumerStreamSpec namedSpec = (NamedWindowConsumerStreamSpec) filterStreamSpec;
            NamedWindowProcessor processor = services.getNamedWindowMgmtService().getProcessor(namedSpec.getWindowName());
            optionalUniqueProps = processor.getOptionalUniqueKeyProps();
        }

        // handle local stream + named-window-stream
        boolean fullTableScan = HintEnum.SET_NOINDEX.getHint(annotations) != null;
        Pair<EventTableFactory, SubordTableLookupStrategyFactory> indexPair = determineSubqueryIndexFactory(filterExpr, eventType,
                outerEventTypes, subselectTypeService, fullTableScan, queryPlanLogging, optionalUniqueProps, statementContext, subqueryNum);

        SubSelectStrategyFactory factory = new SubSelectStrategyFactoryLocalViewPreloaded(subqueryNum, subSelectActivation, indexPair, filterExpr, filterExprEval, correlatedSubquery, aggregationServiceFactoryDesc, viewResourceDelegateVerified, groupByEvaluators);
        return new SubSelectStrategyFactoryDesc(subSelectActivation, factory, aggregationServiceFactoryDesc, priorNodes, previousNodes, subqueryNum);
    }

    private static String validateContextAssociation(StatementContext statementContext, String entityDeclaredContextName, String entityDesc)
            throws ExprValidationException {
        String optionalProvidedContextName = statementContext.getContextDescriptor() == null ? null : statementContext.getContextDescriptor().getContextName();
        if (entityDeclaredContextName != null) {
            if (optionalProvidedContextName == null || !optionalProvidedContextName.equals(entityDeclaredContextName)) {
                throw new ExprValidationException("Mismatch in context specification, the context for the " + entityDesc + " is '" +
                        entityDeclaredContextName + "' and the query specifies " +
                        (optionalProvidedContextName == null ? "no context " :
                                "context '" + optionalProvidedContextName + "'"));
            }
        }
        return null;
    }

    private static void validateSubqueryDataWindow(ExprSubselectNode subselectNode, boolean correlatedSubquery, boolean hasNonAggregatedProperties, ExprNodePropOrStreamSet propertiesGroupBy, ExprNodePropOrStreamSet nonAggregatedPropsSelect)
            throws ExprValidationException {
        // validation applies only to type+filter subqueries that have no data window
        StreamSpecCompiled streamSpec = subselectNode.getStatementSpecCompiled().getStreamSpecs()[0];
        if (!(streamSpec instanceof FilterStreamSpecCompiled) || streamSpec.getViewSpecs().length > 0) {
            return;
        }

        if (correlatedSubquery) {
            throw new ExprValidationException(MSG_SUBQUERY_REQUIRES_WINDOW);
        }

        // we have non-aggregated properties
        if (hasNonAggregatedProperties) {
            if (propertiesGroupBy == null) {
                throw new ExprValidationException(MSG_SUBQUERY_REQUIRES_WINDOW);
            }

            String reason = nonAggregatedPropsSelect.notContainsAll(propertiesGroupBy);
            if (reason != null) {
                throw new ExprValidationException(MSG_SUBQUERY_REQUIRES_WINDOW);
            }
        }
    }

    private static void validateAggregationPropsAndLocalGroup(List<ExprAggregateNode> aggregateNodes) throws ExprValidationException {
        for (ExprAggregateNode aggNode : aggregateNodes) {
            List<Pair<Integer, String>> propertiesNodesAggregated = ExprNodeUtilityRich.getExpressionProperties(aggNode, true);
            for (Pair<Integer, String> pair : propertiesNodesAggregated) {
                if (pair.getFirst() != 0) {
                    throw new ExprValidationException("Subselect aggregation functions cannot aggregate across correlated properties");
                }
            }

            if (aggNode.getOptionalLocalGroupBy() != null) {
                throw new ExprValidationException("Subselect aggregations functions cannot specify a group-by");
            }
        }
    }
}
