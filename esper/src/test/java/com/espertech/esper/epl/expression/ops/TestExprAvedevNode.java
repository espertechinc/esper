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

import com.espertech.esper.epl.agg.aggregator.AggregationMethod;
import com.espertech.esper.epl.agg.service.common.AggregationMethodFactory;
import com.espertech.esper.epl.expression.core.ExprNodeUtilityCore;
import com.espertech.esper.epl.expression.methodagg.ExprAvedevNode;
import com.espertech.esper.epl.expression.methodagg.ExprStddevNode;
import com.espertech.esper.supportunit.epl.SupportExprNode;
import com.espertech.esper.supportunit.epl.SupportExprNodeFactory;

public class TestExprAvedevNode extends TestExprAggregateNodeAdapter {
    public void setUp() throws Exception {
        super.validatedNodeToTest = makeNode(5, Integer.class);
    }

    public void testGetType() throws Exception {
        assertEquals(Double.class, validatedNodeToTest.getEvaluationType());
    }

    public void testToExpressionString() throws Exception {
        assertEquals("avedev(5)", ExprNodeUtilityCore.toExpressionStringMinPrecedenceSafe(validatedNodeToTest));
    }

    public void testEqualsNode() throws Exception {
        assertTrue(validatedNodeToTest.equalsNode(validatedNodeToTest, false));
        assertFalse(validatedNodeToTest.equalsNode(new ExprStddevNode(false), false));
    }

    public void testAggregateFunction() {
        AggregationMethodFactory aggFactory = validatedNodeToTest.getFactory();
        AggregationMethod agg = aggFactory.make();

        assertNull(agg.getValue());

        agg.enter(82);
        assertEquals(0D, agg.getValue());

        agg.enter(78);
        assertEquals(2D, agg.getValue());

        agg.enter(70);
        double result = (Double) agg.getValue();
        assertEquals("4.4444", Double.toString(result).substring(0, 6));

        agg.enter(58);
        assertEquals(8D, agg.getValue());

        agg.enter(42);
        assertEquals(12.8D, agg.getValue());

        agg.leave(82);
        assertEquals(12D, agg.getValue());

        agg.leave(58);
        result = (Double) agg.getValue();
        assertEquals("14.2222", Double.toString(result).substring(0, 7));
    }

    private ExprAvedevNode makeNode(Object value, Class type) throws Exception {
        ExprAvedevNode avedevNode = new ExprAvedevNode(false);
        avedevNode.addChildNode(new SupportExprNode(value, type));
        SupportExprNodeFactory.validate3Stream(avedevNode);
        return avedevNode;
    }
}
