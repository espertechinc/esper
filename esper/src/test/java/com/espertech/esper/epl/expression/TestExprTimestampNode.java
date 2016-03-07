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

import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.expression.core.ExprNodeUtility;
import com.espertech.esper.epl.expression.core.ExprValidationContext;
import com.espertech.esper.epl.expression.core.ExprValidationException;
import com.espertech.esper.epl.expression.ops.ExprEqualsNodeImpl;
import com.espertech.esper.epl.expression.time.ExprTimestampNode;
import com.espertech.esper.schedule.TimeProvider;
import com.espertech.esper.support.core.SupportExprEvaluatorContext;
import com.espertech.esper.support.epl.SupportExprNode;
import junit.framework.TestCase;

public class TestExprTimestampNode extends TestCase
{
    private ExprTimestampNode node;
    private ExprEvaluatorContext context;

    public void setUp()
    {
        node = new ExprTimestampNode();
    }

    public void testGetType() throws Exception
    {
        assertEquals(Long.class, node.getType());
    }

    public void testValidate() throws Exception
    {
        // Test too many nodes
        node.addChildNode(new SupportExprNode(1));
        try
        {
            node.validate(ExprValidationContextFactory.makeEmpty());
            fail();
        }
        catch (ExprValidationException ex)
        {
            // Expected
        }
    }

    public void testEvaluate() throws Exception
    {
        final TimeProvider provider = new TimeProvider()
        {
            public long getTime()
            {
                return 99;
            }
        };
        context = new SupportExprEvaluatorContext(provider);
        node.validate(new ExprValidationContext(null, null, null, provider, null, null, null, null, null, 1, null, null, false, false, false, false, null, false));
        assertEquals(99L, node.evaluate(null, false, context));
    }

    public void testEquals() throws Exception
    {
        assertFalse(node.equalsNode(new ExprEqualsNodeImpl(true, false)));
        assertTrue(node.equalsNode(new ExprTimestampNode()));
    }

    public void testToExpressionString() throws Exception
    {
        assertEquals("current_timestamp()", ExprNodeUtility.toExpressionStringMinPrecedenceSafe(node));
    }
}
