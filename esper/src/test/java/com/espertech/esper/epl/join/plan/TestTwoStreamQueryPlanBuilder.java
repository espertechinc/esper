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
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.core.support.SupportEventAdapterService;
import com.espertech.esper.epl.expression.core.ExprIdentNode;
import com.espertech.esper.epl.expression.core.ExprIdentNodeImpl;
import com.espertech.esper.epl.table.mgmt.TableMetadata;
import com.espertech.esper.supportunit.bean.SupportBean_S0;
import com.espertech.esper.supportunit.bean.SupportBean_S1;
import com.espertech.esper.type.OuterJoinType;
import junit.framework.TestCase;

public class TestTwoStreamQueryPlanBuilder extends TestCase {
    private EventType[] typesPerStream;

    public void setUp() {
        typesPerStream = new EventType[]{
                SupportEventAdapterService.getService().addBeanType(SupportBean_S0.class.getName(), SupportBean_S0.class, true, true, true),
                SupportEventAdapterService.getService().addBeanType(SupportBean_S1.class.getName(), SupportBean_S1.class, true, true, true)
        };
    }

    public void testBuildNoOuter() {
        QueryGraph graph = makeQueryGraph();
        QueryPlan spec = TwoStreamQueryPlanBuilder.build(typesPerStream, graph, null, new String[2][][], new TableMetadata[2]);

        EPAssertionUtil.assertEqualsExactOrder(new String[]{"p01", "p02"}, spec.getIndexSpecs()[0].getIndexProps()[0]);
        EPAssertionUtil.assertEqualsExactOrder(new String[]{"p11", "p12"}, spec.getIndexSpecs()[1].getIndexProps()[0]);
        assertEquals(2, spec.getExecNodeSpecs().length);
    }

    public void testBuildOuter() {
        QueryGraph graph = makeQueryGraph();
        QueryPlan spec = TwoStreamQueryPlanBuilder.build(typesPerStream, graph, OuterJoinType.LEFT, new String[2][][], new TableMetadata[2]);

        EPAssertionUtil.assertEqualsExactOrder(new String[]{"p01", "p02"}, spec.getIndexSpecs()[0].getIndexProps()[0]);
        EPAssertionUtil.assertEqualsExactOrder(new String[]{"p11", "p12"}, spec.getIndexSpecs()[1].getIndexProps()[0]);
        assertEquals(2, spec.getExecNodeSpecs().length);
        assertEquals(TableOuterLookupNode.class, spec.getExecNodeSpecs()[0].getClass());
        assertEquals(TableLookupNode.class, spec.getExecNodeSpecs()[1].getClass());
    }

    private QueryGraph makeQueryGraph() {
        QueryGraph graph = new QueryGraph(2, null, false);
        graph.addStrictEquals(0, "p01", make(0, "p01"), 1, "p11", make(1, "p11"));
        graph.addStrictEquals(0, "p02", make(0, "p02"), 1, "p12", make(1, "p12"));
        return graph;
    }

    private ExprIdentNode make(int stream, String p) {
        return new ExprIdentNodeImpl(typesPerStream[stream], p, stream);
    }
}
