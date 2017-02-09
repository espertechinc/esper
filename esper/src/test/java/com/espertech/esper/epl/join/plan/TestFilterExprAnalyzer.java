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

import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.epl.expression.ops.ExprAndNode;
import com.espertech.esper.epl.expression.ops.ExprEqualsNode;
import com.espertech.esper.supportunit.epl.SupportExprNodeFactory;
import junit.framework.TestCase;

public class TestFilterExprAnalyzer extends TestCase {
    public void testAnalyzeEquals() throws Exception {
        // s0.intPrimitive = s1.intBoxed
        ExprEqualsNode equalsNode = SupportExprNodeFactory.makeEqualsNode();

        QueryGraph graph = new QueryGraph(2, null, false);
        FilterExprAnalyzer.analyzeEqualsNode(equalsNode, graph, false);

        assertTrue(graph.isNavigableAtAll(0, 1));
        EPAssertionUtil.assertEqualsExactOrder(new String[]{"intPrimitive"}, QueryGraphTestUtil.getStrictKeyProperties(graph, 0, 1));
        EPAssertionUtil.assertEqualsExactOrder(new String[]{"intPrimitive"}, QueryGraphTestUtil.getIndexProperties(graph, 1, 0));
        EPAssertionUtil.assertEqualsExactOrder(new String[]{"intBoxed"}, QueryGraphTestUtil.getStrictKeyProperties(graph, 1, 0));
        EPAssertionUtil.assertEqualsExactOrder(new String[]{"intBoxed"}, QueryGraphTestUtil.getIndexProperties(graph, 0, 1));
    }

    public void testAnalyzeAnd() throws Exception {
        ExprAndNode andNode = SupportExprNodeFactory.make2SubNodeAnd();

        QueryGraph graph = new QueryGraph(2, null, false);
        FilterExprAnalyzer.analyzeAndNode(andNode, graph, false);

        assertTrue(graph.isNavigableAtAll(0, 1));
        EPAssertionUtil.assertEqualsExactOrder(new String[]{"intPrimitive", "theString"}, QueryGraphTestUtil.getStrictKeyProperties(graph, 0, 1));
        EPAssertionUtil.assertEqualsExactOrder(new String[]{"intPrimitive", "theString"}, QueryGraphTestUtil.getIndexProperties(graph, 1, 0));
        EPAssertionUtil.assertEqualsExactOrder(new String[]{"intBoxed", "theString"}, QueryGraphTestUtil.getStrictKeyProperties(graph, 1, 0));
        EPAssertionUtil.assertEqualsExactOrder(new String[]{"intBoxed", "theString"}, QueryGraphTestUtil.getIndexProperties(graph, 0, 1));
    }
}
