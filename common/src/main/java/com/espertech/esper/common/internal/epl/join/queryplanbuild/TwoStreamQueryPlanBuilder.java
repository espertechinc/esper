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
import com.espertech.esper.common.internal.context.aifactory.select.StreamJoinAnalysisResultCompileTime;
import com.espertech.esper.common.internal.epl.join.querygraph.QueryGraphForge;
import com.espertech.esper.common.internal.epl.join.queryplan.*;
import com.espertech.esper.common.internal.epl.table.compiletime.TableMetaData;
import com.espertech.esper.common.internal.type.OuterJoinType;

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
     * @return query plan
     */
    public static QueryPlanForge build(EventType[] typesPerStream, QueryGraphForge queryGraph, OuterJoinType optionalOuterJoinType, StreamJoinAnalysisResultCompileTime streamJoinAnalysisResult) {
        String[][][] uniqueIndexProps = streamJoinAnalysisResult.getUniqueKeys();
        TableMetaData[] tablesPerStream = streamJoinAnalysisResult.getTablesPerStream();

        QueryPlanIndexForge[] indexSpecs = QueryPlanIndexBuilder.buildIndexSpec(queryGraph, typesPerStream, uniqueIndexProps);

        QueryPlanNodeForge[] execNodeSpecs = new QueryPlanNodeForge[2];
        TableLookupPlanForge[] lookupPlans = new TableLookupPlanForge[2];

        // plan lookup from 1 to zero
        lookupPlans[1] = NStreamQueryPlanBuilder.createLookupPlan(queryGraph, 1, 0, streamJoinAnalysisResult.isVirtualDW(0), indexSpecs[0], typesPerStream, tablesPerStream[0]);

        // plan lookup from zero to 1
        lookupPlans[0] = NStreamQueryPlanBuilder.createLookupPlan(queryGraph, 0, 1, streamJoinAnalysisResult.isVirtualDW(1), indexSpecs[1], typesPerStream, tablesPerStream[1]);
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

        return new QueryPlanForge(indexSpecs, execNodeSpecs);
    }
}
