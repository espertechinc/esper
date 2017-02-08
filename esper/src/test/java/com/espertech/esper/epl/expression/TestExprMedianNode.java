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
package com.espertech.esper.epl.expression;

import com.espertech.esper.epl.expression.core.ExprNodeUtility;
import com.espertech.esper.epl.expression.methodagg.ExprMedianNode;
import com.espertech.esper.epl.expression.methodagg.ExprSumNode;
import com.espertech.esper.supportunit.epl.SupportExprNode;
import com.espertech.esper.supportunit.epl.SupportExprNodeFactory;

public class TestExprMedianNode extends TestExprAggregateNodeAdapter
{
    public void setUp() throws Exception
    {
        super.validatedNodeToTest = makeNode(5, Integer.class);
    }

    public void testGetType() throws Exception
    {
        assertEquals(Double.class, validatedNodeToTest.getType());
    }

    public void testToExpressionString() throws Exception
    {
        assertEquals("median(5)", ExprNodeUtility.toExpressionStringMinPrecedenceSafe(validatedNodeToTest));
    }

    public void testEqualsNode() throws Exception
    {
        assertTrue(validatedNodeToTest.equalsNode(validatedNodeToTest));
        assertFalse(validatedNodeToTest.equalsNode(new ExprSumNode(false)));
    }

    private ExprMedianNode makeNode(Object value, Class type) throws Exception
    {
        ExprMedianNode medianNode = new ExprMedianNode(false);
        medianNode.addChildNode(new SupportExprNode(value, type));
        SupportExprNodeFactory.validate3Stream(medianNode);
        return medianNode;
    }
}
