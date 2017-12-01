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
package com.espertech.esper.epl.expression.ops;

import com.espertech.esper.epl.agg.aggregator.AggregatorAvg;
import com.espertech.esper.epl.expression.core.ExprNodeUtilityCore;
import com.espertech.esper.epl.expression.methodagg.ExprAvgNode;
import com.espertech.esper.epl.expression.methodagg.ExprSumNode;
import com.espertech.esper.supportunit.epl.SupportExprNode;
import com.espertech.esper.supportunit.epl.SupportExprNodeFactory;

public class TestExprAvgNode extends TestExprAggregateNodeAdapter {
    private ExprAvgNode avgNodeDistinct;

    public void setUp() throws Exception {
        super.validatedNodeToTest = makeNode(5, Integer.class, false);
        this.avgNodeDistinct = makeNode(6, Integer.class, true);
    }

    public void testAggregation() {
        AggregatorAvg agg = new AggregatorAvg();
        assertEquals(null, agg.getValue());

        agg.enter(5);
        assertEquals(5d, agg.getValue());

        agg.enter(10);
        assertEquals(7.5d, agg.getValue());

        agg.leave(5);
        assertEquals(10d, agg.getValue());
    }

    public void testGetType() throws Exception {
        assertEquals(Double.class, validatedNodeToTest.getEvaluationType());
    }

    public void testToExpressionString() throws Exception {
        assertEquals("avg(5)", ExprNodeUtilityCore.toExpressionStringMinPrecedenceSafe(validatedNodeToTest));
        assertEquals("avg(distinct 6)", ExprNodeUtilityCore.toExpressionStringMinPrecedenceSafe(avgNodeDistinct));
    }

    public void testEqualsNode() throws Exception {
        assertTrue(validatedNodeToTest.equalsNode(validatedNodeToTest, false));
        assertFalse(validatedNodeToTest.equalsNode(new ExprSumNode(false), false));
    }

    private ExprAvgNode makeNode(Object value, Class type, boolean isDistinct) throws Exception {
        ExprAvgNode avgNode = new ExprAvgNode(isDistinct);
        avgNode.addChildNode(new SupportExprNode(value, type));
        SupportExprNodeFactory.validate3Stream(avgNode);
        return avgNode;
    }
}
