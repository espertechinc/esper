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
import com.espertech.esper.epl.expression.ops.ExprNotNode;
import com.espertech.esper.epl.expression.ops.ExprOrNode;
import com.espertech.esper.support.epl.SupportExprNodeUtil;
import junit.framework.TestCase;
import com.espertech.esper.support.epl.SupportExprNode;
import com.espertech.esper.support.epl.SupportBoolExprNode;
import com.espertech.esper.type.MinMaxTypeEnum;

public class TestExprNotNode extends TestCase
{
    private ExprNotNode notNode;

    public void setUp()
    {
        notNode = new ExprNotNode();
    }

    public void testGetType()
    {
        assertEquals(Boolean.class, notNode.getType());
    }

    public void testValidate() throws Exception
    {
        // fails with zero expressions
        try
        {
            notNode.validate(ExprValidationContextFactory.makeEmpty());
            fail();
        }
        catch (ExprValidationException ex)
        {
            // Expected
        }

        // fails with too many sub-expressions
        notNode.addChildNode(new SupportExprNode(Boolean.class));
        notNode.addChildNode(new SupportExprNode(Boolean.class));
        try
        {
            notNode.validate(ExprValidationContextFactory.makeEmpty());
            fail();
        }
        catch (ExprValidationException ex)
        {
            // Expected
        }

        // test failure, type mismatch
        notNode = new ExprNotNode();
        notNode.addChildNode(new SupportExprNode(String.class));
        try
        {
            notNode.validate(ExprValidationContextFactory.makeEmpty());
            fail();
        }
        catch (ExprValidationException ex)
        {
            // Expected
        }

        // validates
        notNode = new ExprNotNode();
        notNode.addChildNode(new SupportExprNode(Boolean.class));
        notNode.validate(ExprValidationContextFactory.makeEmpty());
    }

    public void testEvaluate() throws Exception
    {
        notNode.addChildNode(new SupportBoolExprNode(true));
        SupportExprNodeUtil.validate(notNode);
        assertFalse( (Boolean) notNode.evaluate(null, false, null));

        notNode = new ExprNotNode();
        notNode.addChildNode(new SupportBoolExprNode(false));
        SupportExprNodeUtil.validate(notNode);
        assertTrue( (Boolean) notNode.evaluate(null, false, null));
    }

    public void testToExpressionString() throws Exception
    {
        notNode.addChildNode(new SupportExprNode(true));
        assertEquals("not true", ExprNodeUtility.toExpressionStringMinPrecedenceSafe(notNode));
    }

    public void testEqualsNode() throws Exception
    {
        assertTrue(notNode.equalsNode(notNode));
        assertFalse(notNode.equalsNode(new ExprMinMaxRowNode(MinMaxTypeEnum.MIN)));
        assertFalse(notNode.equalsNode(new ExprOrNode()));
        assertTrue(notNode.equalsNode(new ExprNotNode()));
    }
}
