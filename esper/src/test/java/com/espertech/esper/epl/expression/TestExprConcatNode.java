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
import com.espertech.esper.epl.expression.ops.ExprConcatNode;
import com.espertech.esper.epl.expression.ops.ExprMathNode;
import com.espertech.esper.support.epl.SupportExprNodeUtil;
import junit.framework.TestCase;
import com.espertech.esper.support.epl.SupportExprNode;
import com.espertech.esper.type.MathArithTypeEnum;

public class TestExprConcatNode extends TestCase
{
    private ExprConcatNode concatNode;

    public void setUp()
    {
        concatNode = new ExprConcatNode();
    }

    public void testGetType() throws Exception
    {
        assertEquals(String.class, concatNode.getType());
    }

    public void testToExpressionString() throws Exception
    {
        concatNode = new ExprConcatNode();
        concatNode.addChildNode(new SupportExprNode("a"));
        concatNode.addChildNode(new SupportExprNode("b"));
        assertEquals("\"a\"||\"b\"", ExprNodeUtility.toExpressionStringMinPrecedenceSafe(concatNode));
        concatNode.addChildNode(new SupportExprNode("c"));
        assertEquals("\"a\"||\"b\"||\"c\"", ExprNodeUtility.toExpressionStringMinPrecedenceSafe(concatNode));
    }

    public void testValidate()
    {
        // Must have 2 or more String subnodes
        try
        {
            concatNode.validate(ExprValidationContextFactory.makeEmpty());
            fail();
        }
        catch (ExprValidationException ex)
        {
            // Expected
        }

        // Must have only string-type subnodes
        concatNode.addChildNode(new SupportExprNode(String.class));
        concatNode.addChildNode(new SupportExprNode(Integer.class));
        try
        {
            concatNode.validate(ExprValidationContextFactory.makeEmpty());
            fail();
        }
        catch (ExprValidationException ex)
        {
            // Expected
        }
    }

    public void testEvaluate() throws Exception
    {
        concatNode.addChildNode(new SupportExprNode("x"));
        concatNode.addChildNode(new SupportExprNode("y"));
        SupportExprNodeUtil.validate(concatNode);
        assertEquals("xy", concatNode.evaluate(null, false, null));

        concatNode.addChildNode(new SupportExprNode("z"));
        SupportExprNodeUtil.validate(concatNode);
        assertEquals("xyz", concatNode.evaluate(null, false, null));
    }

    public void testEqualsNode() throws Exception
    {
        assertTrue(concatNode.equalsNode(concatNode));
        assertFalse(concatNode.equalsNode(new ExprMathNode(MathArithTypeEnum.DIVIDE, false, false)));
    }
}
