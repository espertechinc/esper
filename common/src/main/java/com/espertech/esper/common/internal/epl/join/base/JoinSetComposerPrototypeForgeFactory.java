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
package com.espertech.esper.common.internal.epl.join.base;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.collection.Pair;
import com.espertech.esper.common.internal.compile.stage1.spec.OuterJoinDesc;
import com.espertech.esper.common.internal.compile.stage2.StatementRawInfo;
import com.espertech.esper.common.internal.compile.stage2.StatementSpecCompiled;
import com.espertech.esper.common.internal.compile.stage3.StatementCompileTimeServices;
import com.espertech.esper.common.internal.context.aifactory.select.StreamJoinAnalysisResultCompileTime;
import com.espertech.esper.common.internal.epl.expression.core.*;
import com.espertech.esper.common.internal.epl.expression.ops.ExprAndNode;
import com.espertech.esper.common.internal.epl.expression.ops.ExprAndNodeImpl;
import com.espertech.esper.common.internal.epl.historical.common.HistoricalStreamIndexListForge;
import com.espertech.esper.common.internal.epl.historical.common.HistoricalViewableDesc;
import com.espertech.esper.common.internal.epl.historical.indexingstrategy.*;
import com.espertech.esper.common.internal.epl.historical.lookupstrategy.*;
import com.espertech.esper.common.internal.epl.join.analyze.FilterExprAnalyzer;
import com.espertech.esper.common.internal.epl.join.analyze.OuterJoinAnalyzer;
import com.espertech.esper.common.internal.epl.join.hint.ExcludePlanHint;
import com.espertech.esper.common.internal.epl.join.querygraph.*;
import com.espertech.esper.common.internal.epl.join.queryplan.*;
import com.espertech.esper.common.internal.epl.join.queryplanbuild.QueryPlanBuilder;
import com.espertech.esper.common.internal.epl.join.support.QueryPlanIndexDescHistorical;
import com.espertech.esper.common.internal.epl.join.support.QueryPlanIndexHook;
import com.espertech.esper.common.internal.epl.join.support.QueryPlanIndexHookUtil;
import com.espertech.esper.common.internal.epl.streamtype.StreamTypeService;
import com.espertech.esper.common.internal.metrics.audit.AuditPath;
import com.espertech.esper.common.internal.type.OuterJoinType;
import com.espertech.esper.common.internal.util.DependencyGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class JoinSetComposerPrototypeForgeFactory {
    private static final Logger QUERY_PLAN_LOG = LoggerFactory.getLogger(AuditPath.QUERYPLAN_LOG);
    private static final Logger log = LoggerFactory.getLogger(JoinSetComposerPrototypeForgeFactory.class);

    public static JoinSetComposerPrototypeForge makeComposerPrototype(StatementSpecCompiled spec,
                                                                      StreamJoinAnalysisResultCompileTime joinAnalysisResult,
                                                                      StreamTypeService typeService,
                                                                      HistoricalViewableDesc historicalViewableDesc,
                                                                      boolean isOnDemandQuery,
                                                                      boolean hasAggregations,
                                                                      StatementRawInfo statementRawInfo,
                                                                      StatementCompileTimeServices compileTimeServices)
            throws ExprValidationException {

        EventType[] streamTypes = typeService.getEventTypes();
        String[] streamNames = typeService.getStreamNames();
        ExprNode whereClause = spec.getRaw().getWhereClause();
        boolean queryPlanLogging = compileTimeServices.getConfiguration().getCommon().getLogging().isEnableQueryPlan();

        // Determine if there is a historical stream, and what dependencies exist
        DependencyGraph historicalDependencyGraph = new DependencyGraph(streamTypes.length, false);
        for (int i = 0; i < streamTypes.length; i++) {
            if (historicalViewableDesc.getHistorical()[i]) {
                SortedSet<Integer> streamsThisStreamDependsOn = historicalViewableDesc.getDependenciesPerHistorical()[i];
                historicalDependencyGraph.addDependency(i, streamsThisStreamDependsOn);
            }
        }

        if (log.isDebugEnabled()) {
            log.debug("Dependency graph: " + historicalDependencyGraph);
        }

        // Handle a join with a database or other historical data source for 2 streams
        OuterJoinDesc[] outerJoinDescs = OuterJoinDesc.toArray(spec.getRaw().getOuterJoinDescList());
        if ((historicalViewableDesc.isHasHistorical()) && (streamTypes.length == 2)) {
            return makeComposerHistorical2Stream(outerJoinDescs, whereClause, streamTypes, streamNames, historicalViewableDesc, queryPlanLogging, statementRawInfo, compileTimeServices);
        }

        boolean isOuterJoins = !OuterJoinDesc.consistsOfAllInnerJoins(outerJoinDescs);

        // Query graph for graph relationships between streams/historicals
        // For outer joins the query graph will just contain outer join relationships
        ExcludePlanHint hint = ExcludePlanHint.getHint(typeService.getStreamNames(), statementRawInfo, compileTimeServices);
        QueryGraphForge queryGraph = new QueryGraphForge(streamTypes.length, hint, false);
        if (outerJoinDescs.length > 0) {
            OuterJoinAnalyzer.analyze(outerJoinDescs, queryGraph);
            if (log.isDebugEnabled()) {
                log.debug(".makeComposer After outer join filterQueryGraph=\n" + queryGraph);
            }
        }

        // Let the query graph reflect the where-clause
        if (whereClause != null) {
            // Analyze relationships between streams using the optional filter expression.
            // Relationships are properties in AND and EQUALS nodes of joins.
            FilterExprAnalyzer.analyze(whereClause, queryGraph, isOuterJoins);
            if (log.isDebugEnabled()) {
                log.debug(".makeComposer After filter expression filterQueryGraph=\n" + queryGraph);
            }

            // Add navigation entries based on key and index property equivalency (a=b, b=c follows a=c)
            QueryGraphForge.fillEquivalentNav(streamTypes, queryGraph);
            if (log.isDebugEnabled()) {
                log.debug(".makeComposer After fill equiv. nav. filterQueryGraph=\n" + queryGraph);
            }
        }

        // Historical index lists
        HistoricalStreamIndexListForge[] historicalStreamIndexLists = new HistoricalStreamIndexListForge[streamTypes.length];

        QueryPlanForge queryPlan = QueryPlanBuilder.getPlan(streamTypes, outerJoinDescs, queryGraph, typeService.getStreamNames(),
                historicalViewableDesc, historicalDependencyGraph, historicalStreamIndexLists,
                joinAnalysisResult, queryPlanLogging, statementRawInfo, compileTimeServices);

        // remove unused indexes - consider all streams or all unidirectional
        HashSet<TableLookupIndexReqKey> usedIndexes = new HashSet<TableLookupIndexReqKey>();
        QueryPlanIndexForge[] indexSpecs = queryPlan.getIndexSpecs();
        for (int streamNum = 0; streamNum < queryPlan.getExecNodeSpecs().length; streamNum++) {
            QueryPlanNodeForge planNode = queryPlan.getExecNodeSpecs()[streamNum];
            if (planNode != null) {
                planNode.addIndexes(usedIndexes);
            }
        }
        for (QueryPlanIndexForge indexSpec : indexSpecs) {
            if (indexSpec == null) {
                continue;
            }
            Map<TableLookupIndexReqKey, QueryPlanIndexItemForge> items = indexSpec.getItems();
            TableLookupIndexReqKey[] indexNames = items.keySet().toArray(new TableLookupIndexReqKey[items.size()]);
            for (TableLookupIndexReqKey indexName : indexNames) {
                if (!usedIndexes.contains(indexName)) {
                    items.remove(indexName);
                }
            }
        }

        QueryPlanIndexHook hook = QueryPlanIndexHookUtil.getHook(spec.getAnnotations(), compileTimeServices.getClasspathImportServiceCompileTime());
        if (queryPlanLogging && (QUERY_PLAN_LOG.isInfoEnabled() || hook != null)) {
            QUERY_PLAN_LOG.info("Query plan: " + queryPlan.toQueryPlan());
            if (hook != null) {
                hook.join(queryPlan);
            }
        }

        boolean selectsRemoveStream = spec.getRaw().getSelectStreamSelectorEnum().isSelectsRStream() || spec.getRaw().getOutputLimitSpec() != null;
        boolean joinRemoveStream = selectsRemoveStream || hasAggregations;

        ExprNode postJoinEvaluator;
        if (JoinSetComposerUtil.isNonUnidirectionalNonSelf(isOuterJoins, joinAnalysisResult.isUnidirectional(), joinAnalysisResult.isPureSelfJoin())) {
            postJoinEvaluator = getFilterExpressionInclOnClause(spec.getRaw().getWhereClause(), outerJoinDescs, statementRawInfo, compileTimeServices);
        } else {
            postJoinEvaluator = spec.getRaw().getWhereClause();
        }

        return new JoinSetComposerPrototypeGeneralForge(typeService.getEventTypes(), postJoinEvaluator, outerJoinDescs.length > 0, queryPlan, joinAnalysisResult, typeService.getStreamNames(), joinRemoveStream, historicalViewableDesc.isHasHistorical());
    }

    private static JoinSetComposerPrototypeForge makeComposerHistorical2Stream(OuterJoinDesc[] outerJoinDescs,
                                                                               ExprNode whereClause,
                                                                               EventType[] streamTypes,
                                                                               String[] streamNames,
                                                                               HistoricalViewableDesc historicalViewableDesc,
                                                                               boolean queryPlanLogging,
                                                                               StatementRawInfo statementRawInfo,
                                                                               StatementCompileTimeServices services)
            throws ExprValidationException {
        int polledViewNum = 0;
        int streamViewNum = 1;
        if (historicalViewableDesc.getHistorical()[1]) {
            streamViewNum = 0;
            polledViewNum = 1;
        }

        // if all-historical join, check dependency
        boolean isAllHistoricalNoSubordinate = false;
        if ((historicalViewableDesc.getHistorical()[0]) && historicalViewableDesc.getHistorical()[1]) {
            DependencyGraph graph = new DependencyGraph(2, false);
            graph.addDependency(0, historicalViewableDesc.getDependenciesPerHistorical()[0]);
            graph.addDependency(1, historicalViewableDesc.getDependenciesPerHistorical()[1]);
            if (graph.getFirstCircularDependency() != null) {
                throw new ExprValidationException("Circular dependency detected between historical streams");
            }

            // if both streams are independent
            if (graph.getRootNodes().size() == 2) {
                isAllHistoricalNoSubordinate = true; // No parameters used by either historical
            } else {
                if (graph.getDependenciesForStream(0).size() == 0) {
                    streamViewNum = 0;
                    polledViewNum = 1;
                } else {
                    streamViewNum = 1;
                    polledViewNum = 0;
                }
            }
        }

        // Build an outer join expression node
        boolean isOuterJoin = false;
        ExprNode outerJoinEqualsNode = null;
        boolean isInnerJoinOnly = false;
        boolean[] outerJoinPerStream = new boolean[2];
        if (outerJoinDescs != null && outerJoinDescs.length > 0) {
            OuterJoinDesc outerJoinDesc = outerJoinDescs[0];
            isInnerJoinOnly = outerJoinDesc.getOuterJoinType().equals(OuterJoinType.INNER);

            if (isAllHistoricalNoSubordinate) {
                if (outerJoinDesc.getOuterJoinType().equals(OuterJoinType.FULL)) {
                    isOuterJoin = true;
                    outerJoinPerStream[0] = true;
                    outerJoinPerStream[1] = true;
                } else if (outerJoinDesc.getOuterJoinType().equals(OuterJoinType.LEFT)) {
                    isOuterJoin = true;
                    outerJoinPerStream[0] = true;
                } else if (outerJoinDesc.getOuterJoinType().equals(OuterJoinType.RIGHT)) {
                    isOuterJoin = true;
                    outerJoinPerStream[1] = true;
                }
            } else {
                if (outerJoinDesc.getOuterJoinType().equals(OuterJoinType.FULL)) {
                    isOuterJoin = true;
                    outerJoinPerStream[0] = true;
                    outerJoinPerStream[1] = true;
                } else if ((outerJoinDesc.getOuterJoinType().equals(OuterJoinType.LEFT)) &&
                        (streamViewNum == 0)) {
                    isOuterJoin = true;
                    outerJoinPerStream[0] = true;
                } else if ((outerJoinDesc.getOuterJoinType().equals(OuterJoinType.RIGHT)) &&
                        (streamViewNum == 1)) {
                    isOuterJoin = true;
                    outerJoinPerStream[1] = true;
                }
            }

            outerJoinEqualsNode = outerJoinDesc.makeExprNode(statementRawInfo, services);
        }

        // Determine filter for indexing purposes
        ExprNode filterForIndexing = null;
        if ((outerJoinEqualsNode != null) && (whereClause != null) && isInnerJoinOnly) {
            // both filter and outer join, add
            filterForIndexing = new ExprAndNodeImpl();
            filterForIndexing.addChildNode(whereClause);
            filterForIndexing.addChildNode(outerJoinEqualsNode);
        } else if ((outerJoinEqualsNode == null) && (whereClause != null)) {
            filterForIndexing = whereClause;
        } else if (outerJoinEqualsNode != null) {
            filterForIndexing = outerJoinEqualsNode;
        }

        Pair<HistoricalIndexLookupStrategyForge, PollResultIndexingStrategyForge> indexStrategies =
                determineIndexing(filterForIndexing, streamTypes[polledViewNum], streamTypes[streamViewNum], polledViewNum, streamViewNum, streamNames, statementRawInfo, services);

        QueryPlanIndexHook hook = QueryPlanIndexHookUtil.getHook(statementRawInfo.getAnnotations(), services.getClasspathImportServiceCompileTime());
        if (queryPlanLogging && (QUERY_PLAN_LOG.isInfoEnabled() || hook != null)) {
            QUERY_PLAN_LOG.info("historical lookup strategy: " + indexStrategies.getFirst().toQueryPlan());
            QUERY_PLAN_LOG.info("historical index strategy: " + indexStrategies.getSecond().toQueryPlan());
            if (hook != null) {
                hook.historical(new QueryPlanIndexDescHistorical(indexStrategies.getFirst().getClass().getSimpleName(), indexStrategies.getSecond().getClass().getSimpleName()));
            }
        }

        return new JoinSetComposerPrototypeHistorical2StreamForge(streamTypes, whereClause, isOuterJoin, polledViewNum, streamViewNum, outerJoinEqualsNode,
                indexStrategies.getFirst(), indexStrategies.getSecond(), isAllHistoricalNoSubordinate, outerJoinPerStream);
    }

    private static ExprNode getFilterExpressionInclOnClause(ExprNode whereClause, OuterJoinDesc[] outerJoinDescList, StatementRawInfo rawInfo, StatementCompileTimeServices services) {
        if (whereClause == null) {   // no need to add as query planning is fully based on on-clause
            return null;
        }
        if (outerJoinDescList.length == 0) {  // not an outer-join syntax
            return whereClause;
        }
        if (!OuterJoinDesc.consistsOfAllInnerJoins(outerJoinDescList)) {    // all-inner joins
            return whereClause;
        }

        boolean hasOnClauses = OuterJoinDesc.hasOnClauses(outerJoinDescList);
        if (!hasOnClauses) {
            return whereClause;
        }

        List<ExprNode> expressions = new ArrayList<>();
        expressions.add(whereClause);

        for (OuterJoinDesc outerJoinDesc : outerJoinDescList) {
            if (outerJoinDesc.getOptLeftNode() != null) {
                expressions.add(outerJoinDesc.makeExprNode(rawInfo, services));
            }
        }

        ExprAndNode andNode = ExprNodeUtilityMake.connectExpressionsByLogicalAnd(expressions);
        try {
            andNode.validate(null);
        } catch (ExprValidationException ex) {
            throw new RuntimeException("Unexpected exception validating expression: " + ex.getMessage(), ex);
        }
        return andNode;
    }

    private static Pair<HistoricalIndexLookupStrategyForge, PollResultIndexingStrategyForge> determineIndexing(ExprNode filterForIndexing,
                                                                                                               EventType polledViewType,
                                                                                                               EventType streamViewType,
                                                                                                               int polledViewStreamNum,
                                                                                                               int streamViewStreamNum,
                                                                                                               String[] streamNames,
                                                                                                               StatementRawInfo rawInfo,
                                                                                                               StatementCompileTimeServices services)
            throws ExprValidationException {
        // No filter means unindexed event tables
        if (filterForIndexing == null) {
            return new Pair<>(HistoricalIndexLookupStrategyNoIndexForge.INSTANCE, PollResultIndexingStrategyNoIndexForge.INSTANCE);
        }

        // analyze query graph; Whereas stream0=named window, stream1=delete-expr filter
        ExcludePlanHint hint = ExcludePlanHint.getHint(streamNames, rawInfo, services);
        QueryGraphForge queryGraph = new QueryGraphForge(2, hint, false);
        FilterExprAnalyzer.analyze(filterForIndexing, queryGraph, false);

        return determineIndexing(queryGraph, polledViewType, streamViewType, polledViewStreamNum, streamViewStreamNum);
    }

    /**
     * Constructs indexing and lookup strategy for a given relationship that a historical stream may have with another
     * stream (historical or not) that looks up into results of a poll of a historical stream.
     * <p>
     * The term "polled" refers to the assumed-historical stream.
     *
     * @param queryGraph          relationship representation of where-clause filter and outer join on-expressions
     * @param polledViewType      the event type of the historical that is indexed
     * @param streamViewType      the event type of the stream looking up in indexes
     * @param polledViewStreamNum the stream number of the historical that is indexed
     * @param streamViewStreamNum the stream number of the historical that is looking up
     * @return indexing and lookup strategy pair
     */
    public static Pair<HistoricalIndexLookupStrategyForge, PollResultIndexingStrategyForge> determineIndexing(QueryGraphForge queryGraph,
                                                                                                              EventType polledViewType,
                                                                                                              EventType streamViewType,
                                                                                                              int polledViewStreamNum,
                                                                                                              int streamViewStreamNum) {
        QueryGraphValueForge queryGraphValue = queryGraph.getGraphValue(streamViewStreamNum, polledViewStreamNum);
        QueryGraphValuePairHashKeyIndexForge hashKeysAndIndes = queryGraphValue.getHashKeyProps();
        QueryGraphValuePairRangeIndexForge rangeKeysAndIndex = queryGraphValue.getRangeProps();

        // index and key property names
        List<QueryGraphValueEntryHashKeyedForge> hashKeys = hashKeysAndIndes.getKeys();
        String[] hashIndexes = hashKeysAndIndes.getIndexed();
        List<QueryGraphValueEntryRangeForge> rangeKeys = rangeKeysAndIndex.getKeys();
        String[] rangeIndexes = rangeKeysAndIndex.getIndexed();

        // If the analysis revealed no join columns, must use the brute-force full table scan
        if (hashKeys.isEmpty() && rangeKeys.isEmpty()) {
            QueryGraphValuePairInKWSingleIdxForge inKeywordSingles = queryGraphValue.getInKeywordSingles();
            if (inKeywordSingles != null && inKeywordSingles.getIndexed().length != 0) {
                String indexed = inKeywordSingles.getIndexed()[0];
                QueryGraphValueEntryInKeywordSingleIdxForge lookup = inKeywordSingles.getKey().get(0);
                HistoricalIndexLookupStrategyInKeywordSingleForge strategy = new HistoricalIndexLookupStrategyInKeywordSingleForge(streamViewStreamNum, lookup.getKeyExprs());
                PollResultIndexingStrategyHashForge indexing = new PollResultIndexingStrategyHashForge(polledViewStreamNum, polledViewType, new String[]{indexed}, null);
                return new Pair<>(strategy, indexing);
            }

            List<QueryGraphValuePairInKWMultiIdx> multis = queryGraphValue.getInKeywordMulti();
            if (!multis.isEmpty()) {
                QueryGraphValuePairInKWMultiIdx multi = multis.get(0);
                HistoricalIndexLookupStrategyInKeywordMultiForge strategy = new HistoricalIndexLookupStrategyInKeywordMultiForge(streamViewStreamNum, multi.getKey().getKeyExpr());
                PollResultIndexingStrategyInKeywordMultiForge indexing = new PollResultIndexingStrategyInKeywordMultiForge(polledViewStreamNum, polledViewType, ExprNodeUtilityQuery.getIdentResolvedPropertyNames(multi.getIndexed()));
                return new Pair<>(strategy, indexing);
            }

            return new Pair<>(
                    HistoricalIndexLookupStrategyNoIndexForge.INSTANCE, PollResultIndexingStrategyNoIndexForge.INSTANCE);
        }

        CoercionDesc keyCoercionTypes = CoercionUtil.getCoercionTypesHash(new EventType[]{streamViewType, polledViewType}, 0, 1, hashKeys, hashIndexes);

        if (rangeKeys.isEmpty()) {
            ExprForge[] hashEvals = QueryGraphValueEntryHashKeyedForge.getForges(hashKeys.toArray(new QueryGraphValueEntryHashKeyedForge[0]));
            HistoricalIndexLookupStrategyHashForge lookup = new HistoricalIndexLookupStrategyHashForge(streamViewStreamNum, hashEvals, keyCoercionTypes.getCoercionTypes());
            PollResultIndexingStrategyHashForge indexing = new PollResultIndexingStrategyHashForge(polledViewStreamNum, polledViewType,
                    hashIndexes, keyCoercionTypes.getCoercionTypes());
            return new Pair<>(lookup, indexing);
        } else {
            CoercionDesc rangeCoercionTypes = CoercionUtil.getCoercionTypesRange(new EventType[]{streamViewType, polledViewType}, 1, rangeIndexes, rangeKeys);

            if (rangeKeys.size() == 1 && hashKeys.size() == 0) {
                Class rangeCoercionType = rangeCoercionTypes.getCoercionTypes()[0];
                PollResultIndexingStrategySortedForge indexing = new PollResultIndexingStrategySortedForge(polledViewStreamNum, polledViewType, rangeIndexes[0], rangeCoercionType);
                HistoricalIndexLookupStrategySortedForge lookup = new HistoricalIndexLookupStrategySortedForge(streamViewStreamNum, rangeKeys.get(0), rangeCoercionType);
                return new Pair<>(lookup, indexing);
            } else {
                PollResultIndexingStrategyCompositeForge indexing = new PollResultIndexingStrategyCompositeForge(polledViewStreamNum, polledViewType, hashIndexes, keyCoercionTypes.getCoercionTypes(), rangeIndexes, rangeCoercionTypes.getCoercionTypes());
                ExprForge[] hashEvals = QueryGraphValueEntryHashKeyedForge.getForges(hashKeys.toArray(new QueryGraphValueEntryHashKeyedForge[0]));
                HistoricalIndexLookupStrategyForge strategy = new HistoricalIndexLookupStrategyCompositeForge(streamViewStreamNum, hashEvals, rangeKeys.toArray(new QueryGraphValueEntryRangeForge[0]));
                return new Pair<>(strategy, indexing);
            }
        }
    }
}
