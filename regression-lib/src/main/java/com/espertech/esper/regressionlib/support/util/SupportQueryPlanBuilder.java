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
package com.espertech.esper.regressionlib.support.util;

import com.espertech.esper.common.internal.epl.join.queryplan.*;
import com.espertech.esper.common.internal.epl.join.queryplanouter.LookupInstructionPlanForge;

import java.util.Arrays;
import java.util.LinkedHashMap;

public class SupportQueryPlanBuilder {

    private QueryPlanForge queryPlan;

    public static SupportQueryPlanBuilder start(int numStreams) {
        return new SupportQueryPlanBuilder(numStreams);
    }

    public static SupportQueryPlanBuilder start(QueryPlanForge existing) {
        return new SupportQueryPlanBuilder(existing);
    }

    private SupportQueryPlanBuilder(int numStreams) {
        QueryPlanIndexForge[] indexes = new QueryPlanIndexForge[numStreams];
        for (int i = 0; i < indexes.length; i++) {
            indexes[i] = new QueryPlanIndexForge(new LinkedHashMap<TableLookupIndexReqKey, QueryPlanIndexItemForge>());
        }
        queryPlan = new QueryPlanForge(indexes, new QueryPlanNodeForge[numStreams]);
    }

    public SupportQueryPlanBuilder(QueryPlanForge queryPlan) {
        this.queryPlan = queryPlan;
    }

    public QueryPlanForge get() {
        return queryPlan;
    }

    public SupportQueryPlanBuilder setIndexFullTableScan(int stream, String indexName) {
        QueryPlanIndexForge index = queryPlan.getIndexSpecs()[stream];
        index.getItems().put(new TableLookupIndexReqKey(indexName, null), new QueryPlanIndexItemForge(new String[0], new Class[0], new String[0], new Class[0], false, null, null));
        return this;
    }

    public SupportQueryPlanBuilder addIndexHashSingleNonUnique(int stream, String indexName, String property) {
        QueryPlanIndexForge index = queryPlan.getIndexSpecs()[stream];
        index.getItems().put(new TableLookupIndexReqKey(indexName, null), new QueryPlanIndexItemForge(new String[]{property}, new Class[]{String.class}, new String[0], new Class[0], false, null, null));
        return this;
    }

    public SupportQueryPlanBuilder addIndexBtreeSingle(int stream, String indexName, String property) {
        QueryPlanIndexForge index = queryPlan.getIndexSpecs()[stream];
        index.getItems().put(new TableLookupIndexReqKey(indexName, null), new QueryPlanIndexItemForge(new String[0], new Class[0], new String[]{property}, new Class[]{String.class}, false, null, null));
        return this;
    }

    public SupportQueryPlanBuilder setLookupPlanInner(int stream, TableLookupPlanForge plan) {
        queryPlan.getExecNodeSpecs()[stream] = new TableLookupNodeForge(plan);
        return this;
    }

    public SupportQueryPlanBuilder setLookupPlanOuter(int stream, TableLookupPlanForge plan) {
        queryPlan.getExecNodeSpecs()[stream] = new TableOuterLookupNodeForge(plan);
        return this;
    }

    public SupportQueryPlanBuilder setLookupPlanInstruction(int stream, String streamName, LookupInstructionPlanForge[] instructions) {
        queryPlan.getExecNodeSpecs()[stream] = new LookupInstructionQueryPlanNodeForge(stream, streamName, queryPlan.getIndexSpecs().length,
            null, Arrays.asList(instructions), null);
        return this;
    }
}
