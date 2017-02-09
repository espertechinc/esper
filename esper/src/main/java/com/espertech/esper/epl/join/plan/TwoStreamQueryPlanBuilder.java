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
import com.espertech.esper.epl.table.mgmt.TableMetadata;
import com.espertech.esper.type.OuterJoinType;

/**
 * Builds a query plan for the simple 2-stream scenario.
 */
public class TwoStreamQueryPlanBuilder {
    /**
     * Build query plan.
     *
     * @param queryGraph            - navigability info
     * @param optionalOuterJoinType - outer join type, null if not an outer join
     * @param typesPerStream        - event types for each stream
     * @param tablesPerStream       - table info
     * @param uniqueIndexProps      props of unique indexes
     * @return query plan
     */
    public static QueryPlan build(EventType[] typesPerStream, QueryGraph queryGraph, OuterJoinType optionalOuterJoinType, String[][][] uniqueIndexProps, TableMetadata[] tablesPerStream) {
        QueryPlanIndex[] indexSpecs = QueryPlanIndexBuilder.buildIndexSpec(queryGraph, typesPerStream, uniqueIndexProps);

        QueryPlanNode[] execNodeSpecs = new QueryPlanNode[2];
        TableLookupPlan[] lookupPlans = new TableLookupPlan[2];

        // plan lookup from 1 to zero
        lookupPlans[1] = NStreamQueryPlanBuilder.createLookupPlan(queryGraph, 1, 0, indexSpecs[0], typesPerStream, tablesPerStream[0]);

        // plan lookup from zero to 1
        lookupPlans[0] = NStreamQueryPlanBuilder.createLookupPlan(queryGraph, 0, 1, indexSpecs[1], typesPerStream, tablesPerStream[1]);
        execNodeSpecs[0] = new TableLookupNode(lookupPlans[0]);
        execNodeSpecs[1] = new TableLookupNode(lookupPlans[1]);

        if (optionalOuterJoinType != null) {
            if ((optionalOuterJoinType.equals(OuterJoinType.LEFT)) ||
                    (optionalOuterJoinType.equals(OuterJoinType.FULL))) {
                execNodeSpecs[0] = new TableOuterLookupNode(lookupPlans[0]);
            }
            if ((optionalOuterJoinType.equals(OuterJoinType.RIGHT)) ||
                    (optionalOuterJoinType.equals(OuterJoinType.FULL))) {
                execNodeSpecs[1] = new TableOuterLookupNode(lookupPlans[1]);
            }
        }

        return new QueryPlan(indexSpecs, execNodeSpecs);
    }
}
