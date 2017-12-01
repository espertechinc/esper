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
import com.espertech.esper.epl.expression.core.ExprValidationException;
import com.espertech.esper.epl.expression.methodagg.ExprMinMaxAggrNode;
import com.espertech.esper.epl.expression.methodagg.ExprSumNode;
import com.espertech.esper.supportunit.epl.SupportExprNode;
import com.espertech.esper.supportunit.epl.SupportExprNodeFactory;
import com.espertech.esper.supportunit.epl.SupportStreamTypeSvc3Stream;
import com.espertech.esper.type.MathArithTypeEnum;
import com.espertech.esper.epl.expression.core.MinMaxTypeEnum;
import com.espertech.esper.support.SupportExprValidationContextFactory;

public class TestExprMinMaxAggrNode extends TestExprAggregateNodeAdapter {
    private ExprMinMaxAggrNode maxNode;
    private ExprMinMaxAggrNode minNode;

    public void setUp() throws Exception {
        maxNode = new ExprMinMaxAggrNode(false, MinMaxTypeEnum.MAX, false, false);
        minNode = new ExprMinMaxAggrNode(false, MinMaxTypeEnum.MIN, false, false);

        super.validatedNodeToTest = makeNode(MinMaxTypeEnum.MAX, 5, Integer.class);
    }

    public void testGetType() throws Exception {
        maxNode.addChildNode(new SupportExprNode(Integer.class));
        SupportExprNodeFactory.validate3Stream(maxNode);
        assertEquals(Integer.class, maxNode.getEvaluationType());

        minNode.addChildNode(new SupportExprNode(Float.class));
        SupportExprNodeFactory.validate3Stream(minNode);
        assertEquals(Float.class, minNode.getEvaluationType());

        maxNode = new ExprMinMaxAggrNode(false, MinMaxTypeEnum.MAX, false, false);
        maxNode.addChildNode(new SupportExprNode(Short.class));
        SupportExprNodeFactory.validate3Stream(maxNode);
        assertEquals(Short.class, maxNode.getEvaluationType());
    }

    public void testToExpressionString() throws Exception {
        // Build sum(4-2)
        ExprMathNode arithNodeChild = new ExprMathNode(MathArithTypeEnum.SUBTRACT, false, false);
        arithNodeChild.addChildNode(new SupportExprNode(4));
        arithNodeChild.addChildNode(new SupportExprNode(2));

        maxNode.addChildNode(arithNodeChild);
        assertEquals("max(4-2)", ExprNodeUtilityCore.toExpressionStringMinPrecedenceSafe(maxNode));
        minNode.addChildNode(arithNodeChild);
        assertEquals("min(4-2)", ExprNodeUtilityCore.toExpressionStringMinPrecedenceSafe(minNode));
    }

    public void testValidate() {
        // Must have exactly 1 subnodes
        try {
            minNode.validate(SupportExprValidationContextFactory.makeEmpty());
            fail();
        } catch (ExprValidationException ex) {
            // Expected
        }

        // Must have only number-type subnodes
        minNode = new ExprMinMaxAggrNode(false, MinMaxTypeEnum.MIN, true, false);
        minNode.addChildNode(new SupportExprNode(String.class));
        minNode.addChildNode(new SupportExprNode(Integer.class));
        try {
            minNode.validate(SupportExprValidationContextFactory.make(new SupportStreamTypeSvc3Stream()));
            fail();
        } catch (ExprValidationException ex) {
            // Expected
        }
    }

    public void testEqualsNode() throws Exception {
        assertTrue(minNode.equalsNode(minNode, false));
        assertFalse(maxNode.equalsNode(minNode, false));
        assertFalse(minNode.equalsNode(new ExprSumNode(false), false));
    }

    private ExprMinMaxAggrNode makeNode(MinMaxTypeEnum minMaxType, Object value, Class type) throws Exception {
        ExprMinMaxAggrNode minMaxNode = new ExprMinMaxAggrNode(false, minMaxType, false, false);
        minMaxNode.addChildNode(new SupportExprNode(value, type));
        SupportExprNodeFactory.validate3Stream(minMaxNode);
        return minMaxNode;
    }
}
