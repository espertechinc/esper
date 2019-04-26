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
package com.espertech.esper.common.internal.epl.join.queryplanbuild;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.annotation.HintEnum;
import com.espertech.esper.common.internal.compile.stage1.spec.OuterJoinDesc;
import com.espertech.esper.common.internal.compile.stage2.StatementRawInfo;
import com.espertech.esper.common.internal.compile.stage3.StatementCompileTimeServices;
import com.espertech.esper.common.internal.context.aifactory.select.StreamJoinAnalysisResultCompileTime;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.common.internal.epl.historical.common.HistoricalStreamIndexListForge;
import com.espertech.esper.common.internal.epl.historical.common.HistoricalViewableDesc;
import com.espertech.esper.common.internal.epl.join.querygraph.QueryGraphForge;
import com.espertech.esper.common.internal.epl.join.queryplan.QueryPlanForge;
import com.espertech.esper.common.internal.epl.join.queryplan.QueryPlanForgeDesc;
import com.espertech.esper.common.internal.epl.join.queryplan.QueryPlanNodeForgeAllUnidirectionalOuter;
import com.espertech.esper.common.internal.epl.join.queryplan.QueryPlanNodeNoOpForge;
import com.espertech.esper.common.internal.metrics.audit.AuditPath;
import com.espertech.esper.common.internal.type.OuterJoinType;
import com.espertech.esper.common.internal.util.DependencyGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Build a query plan based on filtering information.
 */
public class QueryPlanBuilder {
    private static final Logger QUERY_PLAN_LOG = LoggerFactory.getLogger(AuditPath.QUERYPLAN_LOG);

    public static QueryPlanForgeDesc getPlan(EventType[] typesPerStream,
                                             OuterJoinDesc[] outerJoinDescList,
                                             QueryGraphForge queryGraph,
                                             String[] streamNames,
                                             HistoricalViewableDesc historicalViewableDesc,
                                             DependencyGraph dependencyGraph,
                                             HistoricalStreamIndexListForge[] historicalStreamIndexLists,
                                             StreamJoinAnalysisResultCompileTime streamJoinAnalysisResult,
                                             boolean isQueryPlanLogging,
                                             StatementRawInfo statementRawInfo,
                                             StatementCompileTimeServices services)
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

            QueryPlanForgeDesc queryPlan = TwoStreamQueryPlanBuilder.build(typesPerStream, queryGraph, outerJoinType, streamJoinAnalysisResult, statementRawInfo);
            removeUnidirectionalAndTable(queryPlan.getForge(), streamJoinAnalysisResult);

            if (log.isDebugEnabled()) {
                log.debug(methodName + "2-Stream queryPlan=" + queryPlan);
            }
            return queryPlan;
        }

        boolean hasPreferMergeJoin = HintEnum.PREFER_MERGE_JOIN.getHint(statementRawInfo.getAnnotations()) != null;
        boolean hasForceNestedIter = HintEnum.FORCE_NESTED_ITER.getHint(statementRawInfo.getAnnotations()) != null;
        boolean isAllInnerJoins = outerJoinDescList.length == 0 || OuterJoinDesc.consistsOfAllInnerJoins(outerJoinDescList);

        if (isAllInnerJoins && !hasPreferMergeJoin) {
            QueryPlanForgeDesc queryPlan = NStreamQueryPlanBuilder.build(queryGraph, typesPerStream,
                    historicalViewableDesc, dependencyGraph, historicalStreamIndexLists,
                    hasForceNestedIter, streamJoinAnalysisResult.getUniqueKeys(),
                    streamJoinAnalysisResult.getTablesPerStream(), streamJoinAnalysisResult, statementRawInfo, services.getSerdeResolver());

            if (queryPlan != null) {
                removeUnidirectionalAndTable(queryPlan.getForge(), streamJoinAnalysisResult);

                if (log.isDebugEnabled()) {
                    log.debug(methodName + "N-Stream inner-join queryPlan=" + queryPlan);
                }
                return queryPlan;
            }

            if (isQueryPlanLogging && QUERY_PLAN_LOG.isInfoEnabled()) {
                log.info("Switching to Outer-NStream algorithm for query plan");
            }
        }

        QueryPlanForgeDesc queryPlan = NStreamOuterQueryPlanBuilder.build(queryGraph, outerJoinDescList, streamNames, typesPerStream,
                historicalViewableDesc, dependencyGraph, historicalStreamIndexLists, streamJoinAnalysisResult.getUniqueKeys(),
                streamJoinAnalysisResult.getTablesPerStream(), streamJoinAnalysisResult, statementRawInfo, services);
        removeUnidirectionalAndTable(queryPlan.getForge(), streamJoinAnalysisResult);
        return queryPlan;
    }

    // Remove plans for non-unidirectional streams
    private static void removeUnidirectionalAndTable(QueryPlanForge queryPlan, StreamJoinAnalysisResultCompileTime streamJoinAnalysisResult) {
        boolean allUnidirectional = streamJoinAnalysisResult.isUnidirectionalAll();
        for (int streamNum = 0; streamNum < queryPlan.getExecNodeSpecs().length; streamNum++) {
            if (allUnidirectional) {
                queryPlan.getExecNodeSpecs()[streamNum] = new QueryPlanNodeForgeAllUnidirectionalOuter(streamNum);
            } else {
                boolean unidirectional = streamJoinAnalysisResult.isUnidirectional() && !streamJoinAnalysisResult.getUnidirectionalInd()[streamNum];
                boolean table = streamJoinAnalysisResult.getTablesPerStream()[streamNum] != null;
                if (unidirectional || table) {
                    queryPlan.getExecNodeSpecs()[streamNum] = QueryPlanNodeNoOpForge.INSTANCE;
                }
            }
        }
    }

    private static final Logger log = LoggerFactory.getLogger(QueryPlanBuilder.class);
}
