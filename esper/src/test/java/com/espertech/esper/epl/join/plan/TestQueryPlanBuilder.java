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
import com.espertech.esper.core.service.StreamJoinAnalysisResult;
import com.espertech.esper.core.support.SupportEngineImportServiceFactory;
import com.espertech.esper.core.support.SupportEventAdapterService;
import com.espertech.esper.epl.core.engineimport.EngineImportService;
import com.espertech.esper.epl.join.base.HistoricalViewableDesc;
import com.espertech.esper.epl.spec.OuterJoinDesc;
import com.espertech.esper.supportunit.bean.SupportBean_S0;
import com.espertech.esper.supportunit.bean.SupportBean_S1;
import com.espertech.esper.supportunit.epl.SupportExprNodeFactory;
import com.espertech.esper.supportunit.epl.SupportOuterJoinDescFactory;
import com.espertech.esper.type.OuterJoinType;
import com.espertech.esper.util.DependencyGraph;
import junit.framework.TestCase;

public class TestQueryPlanBuilder extends TestCase {
    private EventType[] typesPerStream;
    private DependencyGraph dependencyGraph;

    public void setUp() {
        typesPerStream = new EventType[]{
                SupportEventAdapterService.getService().addBeanType(SupportBean_S0.class.getName(), SupportBean_S0.class, true, true, true),
                SupportEventAdapterService.getService().addBeanType(SupportBean_S1.class.getName(), SupportBean_S1.class, true, true, true)
        };
        dependencyGraph = new DependencyGraph(2, false);
    }

    public void testGetPlan() throws Exception {
        OuterJoinDesc[] descList = new OuterJoinDesc[]{
                SupportOuterJoinDescFactory.makeDesc("intPrimitive", "s0", "intBoxed", "s1", OuterJoinType.LEFT)
        };

        QueryGraph queryGraph = new QueryGraph(2, null, false);
        EngineImportService engineImportService = SupportEngineImportServiceFactory.make();
        QueryPlan plan = QueryPlanBuilder.getPlan(typesPerStream, new OuterJoinDesc[0], queryGraph, null, new HistoricalViewableDesc(5), dependencyGraph, null, new StreamJoinAnalysisResult(2), true, null, null, engineImportService, false);
        assertPlan(plan);

        plan = QueryPlanBuilder.getPlan(typesPerStream, descList, queryGraph, null, new HistoricalViewableDesc(5), dependencyGraph, null, new StreamJoinAnalysisResult(2), true, null, null, engineImportService, false);
        assertPlan(plan);

        FilterExprAnalyzer.analyze(SupportExprNodeFactory.makeEqualsNode(), queryGraph, false);
        plan = QueryPlanBuilder.getPlan(typesPerStream, descList, queryGraph, null, new HistoricalViewableDesc(5), dependencyGraph, null, new StreamJoinAnalysisResult(2), true, null, null, engineImportService, false);
        assertPlan(plan);

        plan = QueryPlanBuilder.getPlan(typesPerStream, new OuterJoinDesc[0], queryGraph, null, new HistoricalViewableDesc(5), dependencyGraph, null, new StreamJoinAnalysisResult(2), true, null, null, engineImportService, false);
        assertPlan(plan);
    }

    private void assertPlan(QueryPlan plan) {
        assertEquals(2, plan.getExecNodeSpecs().length);
        assertEquals(2, plan.getExecNodeSpecs().length);
    }
}
