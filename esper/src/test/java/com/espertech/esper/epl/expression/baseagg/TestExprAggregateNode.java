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
package com.espertech.esper.epl.expression.baseagg;

import com.espertech.esper.epl.expression.baseagg.ExprAggregateNode;
import com.espertech.esper.epl.expression.baseagg.ExprAggregateNodeUtil;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.supportunit.epl.SupportAggregateExprNode;
import com.espertech.esper.supportunit.epl.SupportExprNode;
import junit.framework.TestCase;

import java.util.LinkedList;
import java.util.List;

public class TestExprAggregateNode extends TestCase {
    public void testGetAggregatesBottomUp() {
        /*
                                    top (ag)
                  c1                            c2
           c1_1 (ag)   c1_2 (ag)            c2_1     c2_2
                                    c2_1_1 (ag)    c2_1_2 (ag)

        */

        ExprNode top = new SupportAggregateExprNode(null);
        ExprNode c1 = new SupportExprNode(null);
        ExprNode c2 = new SupportExprNode(null);
        top.addChildNode(c1);
        top.addChildNode(c2);

        ExprNode c1_1 = new SupportAggregateExprNode(null);
        ExprNode c1_2 = new SupportAggregateExprNode(null);
        c1.addChildNode(c1_1);
        c1.addChildNode(c1_2);
        c1_1.addChildNode(new SupportExprNode(null));
        c1_2.addChildNode(new SupportExprNode(null));

        ExprNode c2_1 = new SupportExprNode(null);
        ExprNode c2_2 = new SupportExprNode(null);
        c2.addChildNode(c2_1);
        c2.addChildNode(c2_2);
        c2_2.addChildNode(new SupportExprNode(null));

        ExprNode c2_1_1 = new SupportAggregateExprNode(null);
        ExprNode c2_1_2 = new SupportAggregateExprNode(null);
        c2_1.addChildNode(c2_1_1);
        c2_1.addChildNode(c2_1_2);

        List<ExprAggregateNode> aggregates = new LinkedList<ExprAggregateNode>();
        ExprAggregateNodeUtil.getAggregatesBottomUp(top, aggregates);

        assertEquals(5, aggregates.size());
        assertSame(c2_1_1, aggregates.get(0));
        assertSame(c2_1_2, aggregates.get(1));
        assertSame(c1_1, aggregates.get(2));
        assertSame(c1_2, aggregates.get(3));
        assertSame(top, aggregates.get(4));

        // Test no aggregates
        aggregates.clear();
        ExprAggregateNodeUtil.getAggregatesBottomUp(new SupportExprNode(null), aggregates);
        assertTrue(aggregates.isEmpty());
    }
}
