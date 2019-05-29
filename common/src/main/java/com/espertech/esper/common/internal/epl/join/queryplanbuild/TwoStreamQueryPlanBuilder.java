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
import com.espertech.esper.common.internal.compile.stage2.StatementRawInfo;
import com.espertech.esper.common.internal.compile.stage3.StmtClassForgeableFactory;
import com.espertech.esper.common.internal.context.aifactory.select.StreamJoinAnalysisResultCompileTime;
import com.espertech.esper.common.internal.epl.join.querygraph.QueryGraphForge;
import com.espertech.esper.common.internal.epl.join.queryplan.*;
import com.espertech.esper.common.internal.epl.table.compiletime.TableMetaData;
import com.espertech.esper.common.internal.serde.compiletime.resolve.SerdeCompileTimeResolverNonHA;
import com.espertech.esper.common.internal.type.OuterJoinType;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds a query plan for the simple 2-stream scenario.
 */
public class TwoStreamQueryPlanBuilder {
    /**
     * Build query plan.
     *
     * @param queryGraph               - navigability info
     * @param optionalOuterJoinType    - outer join type, null if not an outer join
     * @param typesPerStream           - event types for each stream
     * @param streamJoinAnalysisResult stream join analysis
     * @param raw raw info
     * @return query plan
     */
    public static QueryPlanForgeDesc build(EventType[] typesPerStream, QueryGraphForge queryGraph, OuterJoinType optionalOuterJoinType, StreamJoinAnalysisResultCompileTime streamJoinAnalysisResult, StatementRawInfo raw) {
        String[][][] uniqueIndexProps = streamJoinAnalysisResult.getUniqueKeys();
        TableMetaData[] tablesPerStream = streamJoinAnalysisResult.getTablesPerStream();
        List<StmtClassForgeableFactory> additionalForgeable = new ArrayList<>(2);

        QueryPlanIndexForge[] indexSpecs = QueryPlanIndexBuilder.buildIndexSpec(queryGraph, typesPerStream, uniqueIndexProps);

        QueryPlanNodeForge[] execNodeSpecs = new QueryPlanNodeForge[2];
        TableLookupPlanForge[] lookupPlans = new TableLookupPlanForge[2];

        // plan lookup from 1 to zero
        TableLookupPlanDesc plan1to0 = NStreamQueryPlanBuilder.createLookupPlan(queryGraph, 1, 0, streamJoinAnalysisResult.isVirtualDW(0), indexSpecs[0], typesPerStream, tablesPerStream[0], raw, SerdeCompileTimeResolverNonHA.INSTANCE);
        lookupPlans[1] = plan1to0.getForge();
        additionalForgeable.addAll(plan1to0.getAdditionalForgeables());

        // plan lookup from zero to 1
        TableLookupPlanDesc plan0to1 = NStreamQueryPlanBuilder.createLookupPlan(queryGraph, 0, 1, streamJoinAnalysisResult.isVirtualDW(1), indexSpecs[1], typesPerStream, tablesPerStream[1], raw, SerdeCompileTimeResolverNonHA.INSTANCE);
        lookupPlans[0] = plan0to1.getForge();
        additionalForgeable.addAll(plan0to1.getAdditionalForgeables());

        execNodeSpecs[0] = new TableLookupNodeForge(lookupPlans[0]);
        execNodeSpecs[1] = new TableLookupNodeForge(lookupPlans[1]);

        if (optionalOuterJoinType != null) {
            if ((optionalOuterJoinType.equals(OuterJoinType.LEFT)) ||
                    (optionalOuterJoinType.equals(OuterJoinType.FULL))) {
                execNodeSpecs[0] = new TableOuterLookupNodeForge(lookupPlans[0]);
            }
            if ((optionalOuterJoinType.equals(OuterJoinType.RIGHT)) ||
                    (optionalOuterJoinType.equals(OuterJoinType.FULL))) {
                execNodeSpecs[1] = new TableOuterLookupNodeForge(lookupPlans[1]);
            }
        }

        QueryPlanForge forge = new QueryPlanForge(indexSpecs, execNodeSpecs);
        return new QueryPlanForgeDesc(forge, additionalForgeable);
    }
}
