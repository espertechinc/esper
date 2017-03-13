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

import com.espertech.esper.epl.expression.TestExprAggregateNodeAdapter;
import com.espertech.esper.epl.expression.core.ExprNodeUtility;
import com.espertech.esper.epl.expression.core.ExprValidationException;
import com.espertech.esper.epl.expression.methodagg.ExprSumNode;
import com.espertech.esper.epl.expression.ops.ExprMathNode;
import com.espertech.esper.epl.expression.ops.ExprOrNode;
import com.espertech.esper.supportunit.epl.SupportExprNode;
import com.espertech.esper.supportunit.epl.SupportExprNodeFactory;
import com.espertech.esper.type.MathArithTypeEnum;
import com.espertech.esper.util.support.SupportExprValidationContextFactory;

public class TestExprSumNode extends TestExprAggregateNodeAdapter {
    private ExprSumNode sumNode;

    public void setUp() throws Exception {
        sumNode = new ExprSumNode(false);

        super.validatedNodeToTest = makeNode(5, Integer.class);
    }

    public void testGetType() throws Exception {
        sumNode.addChildNode(new SupportExprNode(Integer.class));
        SupportExprNodeFactory.validate3Stream(sumNode);
        assertEquals(Integer.class, sumNode.getType());

        sumNode = new ExprSumNode(false);
        sumNode.addChildNode(new SupportExprNode(Float.class));
        SupportExprNodeFactory.validate3Stream(sumNode);
        assertEquals(Float.class, sumNode.getType());

        sumNode = new ExprSumNode(false);
        sumNode.addChildNode(new SupportExprNode(Short.class));
        SupportExprNodeFactory.validate3Stream(sumNode);
        assertEquals(Integer.class, sumNode.getType());
    }

    public void testToExpressionString() throws Exception {
        // Build sum(4-2)
        ExprMathNode arithNodeChild = new ExprMathNode(MathArithTypeEnum.SUBTRACT, false, false);
        arithNodeChild.addChildNode(new SupportExprNode(4));
        arithNodeChild.addChildNode(new SupportExprNode(2));

        sumNode = new ExprSumNode(false);
        sumNode.addChildNode(arithNodeChild);

        assertEquals("sum(4-2)", ExprNodeUtility.toExpressionStringMinPrecedenceSafe(sumNode));
    }

    public void testValidate() {
        // Must have exactly 1 subnodes
        try {
            sumNode.validate(SupportExprValidationContextFactory.makeEmpty());
            fail();
        } catch (ExprValidationException ex) {
            // Expected
        }

        // Must have only number-type subnodes
        sumNode.addChildNode(new SupportExprNode(String.class));
        sumNode.addChildNode(new SupportExprNode(Integer.class));
        try {
            sumNode.validate(SupportExprValidationContextFactory.makeEmpty());
            fail();
        } catch (ExprValidationException ex) {
            // Expected
        }
    }

    public void testEqualsNode() throws Exception {
        assertTrue(sumNode.equalsNode(sumNode));
        assertFalse(sumNode.equalsNode(new ExprOrNode()));
    }

    private ExprSumNode makeNode(Object value, Class type) throws Exception {
        ExprSumNode sumNode = new ExprSumNode(false);
        sumNode.addChildNode(new SupportExprNode(value, type));
        SupportExprNodeFactory.validate3Stream(sumNode);
        return sumNode;
    }
}
