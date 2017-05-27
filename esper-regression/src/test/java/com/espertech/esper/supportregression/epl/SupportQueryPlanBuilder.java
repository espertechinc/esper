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
package com.espertech.esper.supportregression.epl;

import com.espertech.esper.epl.join.plan.*;

import java.util.Arrays;
import java.util.LinkedHashMap;

public class SupportQueryPlanBuilder {

    private QueryPlan queryPlan;

    public static SupportQueryPlanBuilder start(int numStreams) {
        return new SupportQueryPlanBuilder(numStreams);
    }

    public static SupportQueryPlanBuilder start(QueryPlan existing) {
        return new SupportQueryPlanBuilder(existing);
    }

    private SupportQueryPlanBuilder(int numStreams) {
        QueryPlanIndex[] indexes = new QueryPlanIndex[numStreams];
        for (int i = 0; i < indexes.length; i++) {
            indexes[i] = new QueryPlanIndex(new LinkedHashMap<TableLookupIndexReqKey, QueryPlanIndexItem>());
        }
        queryPlan = new QueryPlan(indexes, new QueryPlanNode[numStreams]);
    }

    public SupportQueryPlanBuilder(QueryPlan queryPlan) {
        this.queryPlan = queryPlan;
    }

    public QueryPlan get() {
        return queryPlan;
    }

    public SupportQueryPlanBuilder setIndexFullTableScan(int stream, String indexName) {
        QueryPlanIndex index = queryPlan.getIndexSpecs()[stream];
        index.getItems().put(new TableLookupIndexReqKey(indexName), new QueryPlanIndexItem(null, null, null, null, false, null));
        return this;
    }

    public SupportQueryPlanBuilder addIndexHashSingleNonUnique(int stream, String indexName, String property) {
        QueryPlanIndex index = queryPlan.getIndexSpecs()[stream];
        index.getItems().put(new TableLookupIndexReqKey(indexName), new QueryPlanIndexItem(new String[]{property}, null, new String[0], null, false, null));
        return this;
    }

    public SupportQueryPlanBuilder addIndexBtreeSingle(int stream, String indexName, String property) {
        QueryPlanIndex index = queryPlan.getIndexSpecs()[stream];
        index.getItems().put(new TableLookupIndexReqKey(indexName), new QueryPlanIndexItem(new String[0], null, new String[]{property}, null, false, null));
        return this;
    }

    public SupportQueryPlanBuilder setLookupPlanInner(int stream, TableLookupPlan plan) {
        queryPlan.getExecNodeSpecs()[stream] = new TableLookupNode(plan);
        return this;
    }

    public SupportQueryPlanBuilder setLookupPlanOuter(int stream, TableLookupPlan plan) {
        queryPlan.getExecNodeSpecs()[stream] = new TableOuterLookupNode(plan);
        return this;
    }

    public SupportQueryPlanBuilder setLookupPlanInstruction(int stream, String streamName, LookupInstructionPlan[] instructions) {
        queryPlan.getExecNodeSpecs()[stream] = new LookupInstructionQueryPlanNode(stream, streamName, queryPlan.getIndexSpecs().length,
                null, Arrays.asList(instructions), null);
        return this;
    }
}
