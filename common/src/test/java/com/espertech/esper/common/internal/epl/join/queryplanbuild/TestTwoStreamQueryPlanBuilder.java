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
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.internal.context.aifactory.select.StreamJoinAnalysisResultCompileTime;
import com.espertech.esper.common.internal.epl.expression.core.ExprIdentNode;
import com.espertech.esper.common.internal.epl.expression.core.ExprIdentNodeImpl;
import com.espertech.esper.common.internal.epl.join.querygraph.QueryGraphForge;
import com.espertech.esper.common.internal.epl.join.queryplan.QueryPlanForge;
import com.espertech.esper.common.internal.epl.join.queryplan.QueryPlanForgeDesc;
import com.espertech.esper.common.internal.epl.join.queryplan.TableLookupNodeForge;
import com.espertech.esper.common.internal.epl.join.queryplan.TableOuterLookupNodeForge;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.common.internal.support.SupportBean_S1;
import com.espertech.esper.common.internal.supportunit.event.SupportEventTypeFactory;
import com.espertech.esper.common.internal.type.OuterJoinType;
import junit.framework.TestCase;

public class TestTwoStreamQueryPlanBuilder extends TestCase {
    private EventType[] typesPerStream;

    public void setUp() {
        typesPerStream = new EventType[]{
                SupportEventTypeFactory.createBeanType(SupportBean_S0.class),
                SupportEventTypeFactory.createBeanType(SupportBean_S1.class)
        };
    }

    public void testBuildNoOuter() {
        QueryGraphForge graph = makeQueryGraph();
        QueryPlanForgeDesc specDesc = TwoStreamQueryPlanBuilder.build(typesPerStream, graph, null, new StreamJoinAnalysisResultCompileTime(2), null);
        QueryPlanForge spec = specDesc.getForge();

        EPAssertionUtil.assertEqualsExactOrder(new String[]{"p01", "p02"}, spec.getIndexSpecs()[0].getIndexProps()[0]);
        EPAssertionUtil.assertEqualsExactOrder(new String[]{"p11", "p12"}, spec.getIndexSpecs()[1].getIndexProps()[0]);
        assertEquals(2, spec.getExecNodeSpecs().length);
    }

    public void testBuildOuter() {
        QueryGraphForge graph = makeQueryGraph();
        QueryPlanForgeDesc specDesc = TwoStreamQueryPlanBuilder.build(typesPerStream, graph, OuterJoinType.LEFT, new StreamJoinAnalysisResultCompileTime(2), null);
        QueryPlanForge spec = specDesc.getForge();

        EPAssertionUtil.assertEqualsExactOrder(new String[]{"p01", "p02"}, spec.getIndexSpecs()[0].getIndexProps()[0]);
        EPAssertionUtil.assertEqualsExactOrder(new String[]{"p11", "p12"}, spec.getIndexSpecs()[1].getIndexProps()[0]);
        assertEquals(2, spec.getExecNodeSpecs().length);
        assertEquals(TableOuterLookupNodeForge.class, spec.getExecNodeSpecs()[0].getClass());
        assertEquals(TableLookupNodeForge.class, spec.getExecNodeSpecs()[1].getClass());
    }

    private QueryGraphForge makeQueryGraph() {
        QueryGraphForge graph = new QueryGraphForge(2, null, false);
        graph.addStrictEquals(0, "p01", make(0, "p01"), 1, "p11", make(1, "p11"));
        graph.addStrictEquals(0, "p02", make(0, "p02"), 1, "p12", make(1, "p12"));
        return graph;
    }

    private ExprIdentNode make(int stream, String p) {
        return new ExprIdentNodeImpl(typesPerStream[stream], p, stream);
    }
}
