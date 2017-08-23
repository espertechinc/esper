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
package com.espertech.esper.epl.join.plan;

import com.espertech.esper.client.EventType;
import com.espertech.esper.client.annotation.HintEnum;
import com.espertech.esper.core.service.StreamJoinAnalysisResult;
import com.espertech.esper.epl.core.engineimport.EngineImportService;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.expression.core.ExprValidationException;
import com.espertech.esper.epl.join.base.HistoricalViewableDesc;
import com.espertech.esper.epl.join.table.HistoricalStreamIndexList;
import com.espertech.esper.epl.spec.OuterJoinDesc;
import com.espertech.esper.type.OuterJoinType;
import com.espertech.esper.util.AuditPath;
import com.espertech.esper.util.DependencyGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;

/**
 * Build a query plan based on filtering information.
 */
public class QueryPlanBuilder {
    private static final Logger QUERY_PLAN_LOG = LoggerFactory.getLogger(AuditPath.QUERYPLAN_LOG);

    /**
     * Build query plan using the filter.
     *
     * @param typesPerStream             - event types for each stream
     * @param outerJoinDescList          - list of outer join criteria, or null if there are no outer joins
     * @param queryGraph                 - relationships between streams based on filter expressions and outer-join on-criteria
     * @param streamNames                - names of streams
     * @param dependencyGraph            - dependencies between historical streams
     * @param historicalStreamIndexLists - index management, populated for the query plan
     * @param streamJoinAnalysisResult   stream join analysis metadata
     * @param historicalViewableDesc     historicals
     * @param isQueryPlanLogging         for logging
     * @param exprEvaluatorContext       context
     * @param annotations                annotations
     * @param isFireAndForget            fire-and-forget flag
     * @param engineImportService        engine imports
     * @return query plan
     * @throws ExprValidationException if the query plan fails
     */
    public static QueryPlan getPlan(EventType[] typesPerStream,
                                    OuterJoinDesc[] outerJoinDescList,
                                    QueryGraph queryGraph,
                                    String[] streamNames,
                                    HistoricalViewableDesc historicalViewableDesc,
                                    DependencyGraph dependencyGraph,
                                    HistoricalStreamIndexList[] historicalStreamIndexLists,
                                    StreamJoinAnalysisResult streamJoinAnalysisResult,
                                    boolean isQueryPlanLogging,
                                    Annotation[] annotations,
                                    ExprEvaluatorContext exprEvaluatorContext,
                                    EngineImportService engineImportService,
                                    boolean isFireAndForget)
            throws ExprValidationException {
        String methodName = ".getPlan ";

        int numStreams = typesPerStream.length;
        if (numStreams < 2) {
            throw new IllegalArgumentException("Number of join stream types is less then 2");
        }
        if (outerJoinDescList.length >= numStreams) {
            throw new IllegalArgumentException("Too many outer join descriptors found");
        }

        if (numStreams == 2) {
            OuterJoinType outerJoinType = null;
            if (outerJoinDescList.length > 0) {
                outerJoinType = outerJoinDescList[0].getOuterJoinType();
            }

            QueryPlan queryPlan = TwoStreamQueryPlanBuilder.build(typesPerStream, queryGraph, outerJoinType, streamJoinAnalysisResult.getUniqueKeys(), streamJoinAnalysisResult.getTablesPerStream());
            removeUnidirectionalAndTable(queryPlan, streamJoinAnalysisResult);

            if (log.isDebugEnabled()) {
                log.debug(methodName + "2-Stream queryPlan=" + queryPlan);
            }
            return queryPlan;
        }

        boolean hasPreferMergeJoin = HintEnum.PREFER_MERGE_JOIN.getHint(annotations) != null;
        boolean hasForceNestedIter = HintEnum.FORCE_NESTED_ITER.getHint(annotations) != null;
        boolean isAllInnerJoins = outerJoinDescList.length == 0 || OuterJoinDesc.consistsOfAllInnerJoins(outerJoinDescList);

        if (isAllInnerJoins && !hasPreferMergeJoin) {
            QueryPlan queryPlan = NStreamQueryPlanBuilder.build(queryGraph, typesPerStream,
                    historicalViewableDesc, dependencyGraph, historicalStreamIndexLists,
                    hasForceNestedIter, streamJoinAnalysisResult.getUniqueKeys(),
                    streamJoinAnalysisResult.getTablesPerStream());

            if (queryPlan != null) {
                removeUnidirectionalAndTable(queryPlan, streamJoinAnalysisResult);

                if (log.isDebugEnabled()) {
                    log.debug(methodName + "N-Stream inner-join queryPlan=" + queryPlan);
                }
                return queryPlan;
            }

            if (isQueryPlanLogging && QUERY_PLAN_LOG.isInfoEnabled()) {
                log.info("Switching to Outer-NStream algorithm for query plan");
            }
        }

        QueryPlan queryPlan = NStreamOuterQueryPlanBuilder.build(queryGraph, outerJoinDescList, streamNames, typesPerStream,
                historicalViewableDesc, dependencyGraph, historicalStreamIndexLists, exprEvaluatorContext, streamJoinAnalysisResult.getUniqueKeys(),
                streamJoinAnalysisResult.getTablesPerStream(), engineImportService, isFireAndForget);
        removeUnidirectionalAndTable(queryPlan, streamJoinAnalysisResult);
        return queryPlan;
    }

    // Remove plans for non-unidirectional streams
    private static void removeUnidirectionalAndTable(QueryPlan queryPlan, StreamJoinAnalysisResult streamJoinAnalysisResult) {
        boolean allUnidirectional = streamJoinAnalysisResult.isUnidirectionalAll();
        for (int streamNum = 0; streamNum < queryPlan.getExecNodeSpecs().length; streamNum++) {
            if (allUnidirectional) {
                queryPlan.getExecNodeSpecs()[streamNum] = new QueryPlanNodeAllUnidirectionalOuter(streamNum);
            } else {
                boolean unidirectional = streamJoinAnalysisResult.isUnidirectional() && !streamJoinAnalysisResult.getUnidirectionalInd()[streamNum];
                boolean table = streamJoinAnalysisResult.getTablesPerStream()[streamNum] != null;
                if (unidirectional || table) {
                    queryPlan.getExecNodeSpecs()[streamNum] = new QueryPlanNodeNoOp();
                }
            }
        }
    }

    private static final Logger log = LoggerFactory.getLogger(QueryPlanBuilder.class);
}
