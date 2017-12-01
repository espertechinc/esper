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
import com.espertech.esper.epl.expression.core.ExprWildcardImpl;
import com.espertech.esper.epl.expression.methodagg.ExprCountNode;
import com.espertech.esper.epl.expression.methodagg.ExprSumNode;
import com.espertech.esper.supportunit.epl.SupportExprNode;
import com.espertech.esper.supportunit.epl.SupportExprNodeFactory;

public class TestExprCountNode extends TestExprAggregateNodeAdapter {
    private ExprCountNode wildcardCount;

    public void setUp() throws Exception {
        super.validatedNodeToTest = makeNode(5, Integer.class);

        wildcardCount = new ExprCountNode(false);
        wildcardCount.addChildNode(new ExprWildcardImpl());
        SupportExprNodeFactory.validate3Stream(wildcardCount);
    }

    public void testGetType() throws Exception {
        assertEquals(Long.class, validatedNodeToTest.getEvaluationType());
        assertEquals(Long.class, wildcardCount.getEvaluationType());
    }

    public void testToExpressionString() throws Exception {
        assertEquals("count(5)", ExprNodeUtilityCore.toExpressionStringMinPrecedenceSafe(validatedNodeToTest));
        assertEquals("count(*)", ExprNodeUtilityCore.toExpressionStringMinPrecedenceSafe(wildcardCount));
    }

    public void testEqualsNode() throws Exception {
        assertTrue(validatedNodeToTest.equalsNode(validatedNodeToTest, false));
        assertFalse(validatedNodeToTest.equalsNode(new ExprSumNode(false), false));
        assertTrue(wildcardCount.equalsNode(wildcardCount, false));
    }

    private ExprCountNode makeNode(Object value, Class type) throws Exception {
        ExprCountNode countNode = new ExprCountNode(false);
        countNode.addChildNode(new SupportExprNode(value, type));
        SupportExprNodeFactory.validate3Stream(countNode);
        return countNode;
    }
}
