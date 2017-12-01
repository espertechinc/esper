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

import com.espertech.esper.epl.expression.core.ExprNodeUtilityCore;
import com.espertech.esper.epl.expression.methodagg.ExprStddevNode;
import com.espertech.esper.epl.expression.methodagg.ExprSumNode;
import com.espertech.esper.supportunit.epl.SupportExprNode;
import com.espertech.esper.supportunit.epl.SupportExprNodeFactory;

public class TestExprStddevNode extends TestExprAggregateNodeAdapter {
    public void setUp() throws Exception {
        super.validatedNodeToTest = makeNode(5, Integer.class);
    }

    public void testGetType() throws Exception {
        assertEquals(Double.class, validatedNodeToTest.getEvaluationType());
    }

    public void testToExpressionString() throws Exception {
        assertEquals("stddev(5)", ExprNodeUtilityCore.toExpressionStringMinPrecedenceSafe(validatedNodeToTest));
    }

    public void testEqualsNode() throws Exception {
        assertTrue(validatedNodeToTest.equalsNode(validatedNodeToTest, false));
        assertFalse(validatedNodeToTest.equalsNode(new ExprSumNode(false), false));
    }

    private ExprStddevNode makeNode(Object value, Class type) throws Exception {
        ExprStddevNode stddevNode = new ExprStddevNode(false);
        stddevNode.addChildNode(new SupportExprNode(value, type));
        SupportExprNodeFactory.validate3Stream(stddevNode);
        return stddevNode;
    }
}
