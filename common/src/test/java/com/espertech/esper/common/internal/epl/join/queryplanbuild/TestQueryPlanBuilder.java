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
import com.espertech.esper.common.internal.compile.stage1.spec.OuterJoinDesc;
import com.espertech.esper.common.internal.context.aifactory.select.StreamJoinAnalysisResultCompileTime;
import com.espertech.esper.common.internal.epl.historical.common.HistoricalViewableDesc;
import com.espertech.esper.common.internal.epl.join.analyze.FilterExprAnalyzer;
import com.espertech.esper.common.internal.epl.join.querygraph.QueryGraphForge;
import com.espertech.esper.common.internal.epl.join.queryplan.QueryPlanForge;
import com.espertech.esper.common.internal.epl.join.queryplan.QueryPlanForgeDesc;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.common.internal.support.SupportBean_S1;
import com.espertech.esper.common.internal.supportunit.event.SupportEventTypeFactory;
import com.espertech.esper.common.internal.supportunit.util.SupportExprNodeFactory;
import com.espertech.esper.common.internal.supportunit.util.SupportOuterJoinDescFactory;
import com.espertech.esper.common.internal.type.OuterJoinType;
import com.espertech.esper.common.internal.util.DependencyGraph;
import junit.framework.TestCase;

public class TestQueryPlanBuilder extends TestCase {
    private EventType[] typesPerStream;
    private DependencyGraph dependencyGraph;

    public void setUp() {
        typesPerStream = new EventType[]{
                SupportEventTypeFactory.createBeanType(SupportBean_S0.class),
                SupportEventTypeFactory.createBeanType(SupportBean_S1.class)
        };
        dependencyGraph = new DependencyGraph(2, false);
    }

    public void testGetPlan() throws Exception {
        OuterJoinDesc[] descList = new OuterJoinDesc[]{
                SupportOuterJoinDescFactory.makeDesc("intPrimitive", "s0", "intBoxed", "s1", OuterJoinType.LEFT)
        };

        QueryGraphForge queryGraph = new QueryGraphForge(2, null, false);
        QueryPlanForgeDesc plan = QueryPlanBuilder.getPlan(typesPerStream, new OuterJoinDesc[0], queryGraph, null, new HistoricalViewableDesc(5), dependencyGraph, null, new StreamJoinAnalysisResultCompileTime(2), true, null, null);
        assertPlan(plan.getForge());

        plan = QueryPlanBuilder.getPlan(typesPerStream, descList, queryGraph, null, new HistoricalViewableDesc(5), dependencyGraph, null, new StreamJoinAnalysisResultCompileTime(2), true, null, null);
        assertPlan(plan.getForge());

        FilterExprAnalyzer.analyze(SupportExprNodeFactory.makeEqualsNode(), queryGraph, false);
        plan = QueryPlanBuilder.getPlan(typesPerStream, descList, queryGraph, null, new HistoricalViewableDesc(5), dependencyGraph, null, new StreamJoinAnalysisResultCompileTime(2), true, null, null);
        assertPlan(plan.getForge());

        plan = QueryPlanBuilder.getPlan(typesPerStream, new OuterJoinDesc[0], queryGraph, null, new HistoricalViewableDesc(5), dependencyGraph, null, new StreamJoinAnalysisResultCompileTime(2), true, null, null);
        assertPlan(plan.getForge());
    }

    private void assertPlan(QueryPlanForge plan) {
        assertEquals(2, plan.getExecNodeSpecs().length);
        assertEquals(2, plan.getExecNodeSpecs().length);
    }
}
