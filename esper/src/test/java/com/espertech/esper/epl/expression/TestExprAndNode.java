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
import com.espertech.esper.epl.expression.core.ExprValidationException;
import com.espertech.esper.epl.expression.ops.ExprAndNode;
import com.espertech.esper.epl.expression.ops.ExprAndNodeImpl;
import com.espertech.esper.epl.expression.ops.ExprOrNode;
import com.espertech.esper.supportunit.epl.SupportExprNodeUtil;
import com.espertech.esper.util.support.SupportExprValidationContextFactory;
import junit.framework.TestCase;
import com.espertech.esper.supportunit.epl.SupportExprNode;
import com.espertech.esper.supportunit.epl.SupportBoolExprNode;

public class TestExprAndNode extends TestCase
{
    private ExprAndNode andNode;

    public void setUp()
    {
        andNode = new ExprAndNodeImpl();
    }

    public void testGetType()
    {
        assertEquals(Boolean.class, andNode.getType());
    }

    public void testValidate() throws Exception
    {
        // test success
        andNode.addChildNode(new SupportExprNode(Boolean.class));
        andNode.addChildNode(new SupportExprNode(Boolean.class));
        andNode.validate(SupportExprValidationContextFactory.makeEmpty());

        // test failure, type mismatch
        andNode.addChildNode(new SupportExprNode(String.class));
        try
        {
            andNode.validate(SupportExprValidationContextFactory.makeEmpty());
            fail();
        }
        catch (ExprValidationException ex)
        {
            // Expected
        }

        // test failed - with just one child
        andNode = new ExprAndNodeImpl();
        andNode.addChildNode(new SupportExprNode(Boolean.class));
        try
        {
            andNode.validate(SupportExprValidationContextFactory.makeEmpty());
            fail();
        }
        catch (ExprValidationException ex)
        {
            // Expected
        }
    }

    public void testEvaluate() throws Exception
    {
        andNode.addChildNode(new SupportBoolExprNode(true));
        andNode.addChildNode(new SupportBoolExprNode(true));
        SupportExprNodeUtil.validate(andNode);
        assertTrue( (Boolean) andNode.evaluate(null, false, null));

        andNode = new ExprAndNodeImpl();
        andNode.addChildNode(new SupportBoolExprNode(true));
        andNode.addChildNode(new SupportBoolExprNode(false));
        SupportExprNodeUtil.validate(andNode);
        assertFalse( (Boolean) andNode.evaluate(null, false, null));
    }

    public void testToExpressionString() throws Exception
    {
        andNode.addChildNode(new SupportExprNode(true));
        andNode.addChildNode(new SupportExprNode(false));

        assertEquals("true and false", ExprNodeUtility.toExpressionStringMinPrecedenceSafe(andNode));
    }

    public void testEqualsNode()
    {
        assertTrue(andNode.equalsNode(new ExprAndNodeImpl()));
        assertFalse(andNode.equalsNode(new ExprOrNode()));
    }
}
