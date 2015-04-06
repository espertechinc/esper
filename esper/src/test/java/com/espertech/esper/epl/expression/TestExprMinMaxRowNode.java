/*
 * *************************************************************************************
 *  Copyright (C) 2006-2015 EsperTech, Inc. All rights reserved.                       *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 * *************************************************************************************
 */

package com.espertech.esper.epl.expression;

import com.espertech.esper.epl.expression.core.ExprNodeUtility;
import com.espertech.esper.epl.expression.core.ExprValidationException;
import com.espertech.esper.epl.expression.funcs.ExprMinMaxRowNode;
import com.espertech.esper.epl.expression.ops.ExprOrNode;
import junit.framework.TestCase;
import com.espertech.esper.support.epl.SupportExprNode;
import com.espertech.esper.type.MinMaxTypeEnum;

public class TestExprMinMaxRowNode extends TestCase
{
    private ExprMinMaxRowNode minMaxNode;

    public void setUp()
    {
        minMaxNode = new ExprMinMaxRowNode(MinMaxTypeEnum.MAX);
    }

    public void testGetType() throws Exception
    {
        minMaxNode.addChildNode(new SupportExprNode(Double.class));
        minMaxNode.addChildNode(new SupportExprNode(Integer.class));
        minMaxNode.validate(ExprValidationContextFactory.makeEmpty());
        assertEquals(Double.class, minMaxNode.getType());

        minMaxNode.addChildNode(new SupportExprNode(Double.class));
        minMaxNode.validate(ExprValidationContextFactory.makeEmpty());
        assertEquals(Double.class, minMaxNode.getType());
    }

    public void testToExpressionString() throws Exception
    {
        minMaxNode.addChildNode(new SupportExprNode(9d));
        minMaxNode.addChildNode(new SupportExprNode(6));
        assertEquals("max(9.0,6)", ExprNodeUtility.toExpressionStringMinPrecedenceSafe(minMaxNode));
        minMaxNode.addChildNode(new SupportExprNode(0.5d));
        assertEquals("max(9.0,6,0.5)", ExprNodeUtility.toExpressionStringMinPrecedenceSafe(minMaxNode));
    }

    public void testValidate()
    {
        // Must have 2 or more subnodes
        try
        {
            minMaxNode.validate(ExprValidationContextFactory.makeEmpty());
            fail();
        }
        catch (ExprValidationException ex)
        {
            // Expected
        }

        // Must have only number-type subnodes
        minMaxNode.addChildNode(new SupportExprNode(String.class));
        minMaxNode.addChildNode(new SupportExprNode(Integer.class));
        try
        {
            minMaxNode.validate(ExprValidationContextFactory.makeEmpty());
            fail();
        }
        catch (ExprValidationException ex)
        {
            // Expected
        }
    }

    public void testEvaluate() throws Exception
    {
        minMaxNode = new ExprMinMaxRowNode(MinMaxTypeEnum.MAX);
        setupNode(minMaxNode, 10, 1.5, null);
        assertEquals(10d, minMaxNode.evaluate(null, false, null));

        minMaxNode = new ExprMinMaxRowNode(MinMaxTypeEnum.MAX);
        setupNode(minMaxNode, 1, 1.5, null);
        assertEquals(1.5d, minMaxNode.evaluate(null, false, null));

        minMaxNode = new ExprMinMaxRowNode(MinMaxTypeEnum.MIN);
        setupNode(minMaxNode, 1, 1.5, null);
        assertEquals(1d, minMaxNode.evaluate(null, false, null));

        minMaxNode = new ExprMinMaxRowNode(MinMaxTypeEnum.MAX);
        setupNode(minMaxNode, 1, 1.5, 2.0f);
        assertEquals(2.0d, minMaxNode.evaluate(null, false, null));

        minMaxNode = new ExprMinMaxRowNode(MinMaxTypeEnum.MIN);
        setupNode(minMaxNode, 6, 3.5, 2.0f);
        assertEquals(2.0d, minMaxNode.evaluate(null, false, null));

        minMaxNode = makeNode(null, Integer.class, 5, Integer.class, 6, Integer.class);
        assertNull(minMaxNode.evaluate(null, false, null));
        minMaxNode = makeNode(7, Integer.class, null, Integer.class, 6, Integer.class);
        assertNull(minMaxNode.evaluate(null, false, null));
        minMaxNode = makeNode(3, Integer.class, 5, Integer.class, null, Integer.class);
        assertNull(minMaxNode.evaluate(null, false, null));
        minMaxNode = makeNode(null, Integer.class, null, Integer.class, null, Integer.class);
        assertNull(minMaxNode.evaluate(null, false, null));
    }

    public void testEqualsNode() throws Exception
    {
        assertTrue(minMaxNode.equalsNode(minMaxNode));
        assertFalse(minMaxNode.equalsNode(new ExprMinMaxRowNode(MinMaxTypeEnum.MIN)));
        assertFalse(minMaxNode.equalsNode(new ExprOrNode()));
    }

    private static void setupNode(ExprMinMaxRowNode nodeMin, int intValue, double doubleValue, Float floatValue) throws Exception
    {
        nodeMin.addChildNode(new SupportExprNode(new Integer(intValue)));
        nodeMin.addChildNode(new SupportExprNode(new Double(doubleValue)));
        if (floatValue != null)
        {
            nodeMin.addChildNode(new SupportExprNode(floatValue));
        }
        nodeMin.validate(ExprValidationContextFactory.makeEmpty());
    }

    private ExprMinMaxRowNode makeNode(Object valueOne, Class typeOne,
                                       Object valueTwo, Class typeTwo,
                                       Object valueThree, Class typeThree) throws Exception
    {
        ExprMinMaxRowNode maxNode = new ExprMinMaxRowNode(MinMaxTypeEnum.MAX);
        maxNode.addChildNode(new SupportExprNode(valueOne, typeOne));
        maxNode.addChildNode(new SupportExprNode(valueTwo, typeTwo));
        maxNode.addChildNode(new SupportExprNode(valueThree, typeThree));
        maxNode.validate(ExprValidationContextFactory.makeEmpty());
        return maxNode;
    }

}
