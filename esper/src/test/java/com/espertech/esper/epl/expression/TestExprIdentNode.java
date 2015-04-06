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

import com.espertech.esper.client.EventBean;
import com.espertech.esper.epl.core.StreamTypeService;
import com.espertech.esper.epl.expression.core.ExprIdentNode;
import com.espertech.esper.epl.expression.core.ExprIdentNodeImpl;
import com.espertech.esper.epl.expression.core.ExprNodeUtility;
import com.espertech.esper.epl.expression.core.ExprValidationException;
import com.espertech.esper.support.bean.SupportBean;
import com.espertech.esper.support.epl.SupportStreamTypeSvc3Stream;
import com.espertech.esper.support.event.SupportEventBeanFactory;
import junit.framework.TestCase;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class TestExprIdentNode extends TestCase
{
    private ExprIdentNode identNodes[];
    private StreamTypeService streamTypeService;

    public void setUp()
    {
        identNodes = new ExprIdentNode[4];
        identNodes[0] = new ExprIdentNodeImpl("mapped('a')");
        identNodes[1] = new ExprIdentNodeImpl("nestedValue", "nested");
        identNodes[2] = new ExprIdentNodeImpl("indexed[1]", "s2");
        identNodes[3] = new ExprIdentNodeImpl("intPrimitive", "s0");

        streamTypeService = new SupportStreamTypeSvc3Stream();
    }

    public void testValidateInvalid() throws Exception
    {
        try
        {
            identNodes[0].getStreamId();
            fail();
        }
        catch (IllegalStateException ex)
        {
            // expected
        }

        assertNull(identNodes[0].getExprEvaluator());

        try
        {
            identNodes[0].getResolvedStreamName();
            fail();
        }
        catch (IllegalStateException ex)
        {
            // expected
        }

        try
        {
            identNodes[0].getResolvedPropertyName();
            fail();
        }
        catch (IllegalStateException ex)
        {
            // expected
        }
    }

    public void testValidate() throws Exception
    {
        identNodes[0].validate(ExprValidationContextFactory.make(streamTypeService));
        assertEquals(2, identNodes[0].getStreamId());
        assertEquals(String.class, identNodes[0].getExprEvaluator().getType());
        assertEquals("mapped('a')", identNodes[0].getResolvedPropertyName());

        identNodes[1].validate(ExprValidationContextFactory.make(streamTypeService));
        assertEquals(2, identNodes[1].getStreamId());
        assertEquals(String.class, identNodes[1].getExprEvaluator().getType());
        assertEquals("nested.nestedValue", identNodes[1].getResolvedPropertyName());

        identNodes[2].validate(ExprValidationContextFactory.make(streamTypeService));
        assertEquals(2, identNodes[2].getStreamId());
        assertEquals(int.class, identNodes[2].getExprEvaluator().getType());
        assertEquals("indexed[1]", identNodes[2].getResolvedPropertyName());

        identNodes[3].validate(ExprValidationContextFactory.make(streamTypeService));
        assertEquals(0, identNodes[3].getStreamId());
        assertEquals(int.class, identNodes[3].getExprEvaluator().getType());
        assertEquals("intPrimitive", identNodes[3].getResolvedPropertyName());

        tryInvalidValidate(new ExprIdentNodeImpl(""));
        tryInvalidValidate(new ExprIdentNodeImpl("dummy"));
        tryInvalidValidate(new ExprIdentNodeImpl("nested", "s0"));
        tryInvalidValidate(new ExprIdentNodeImpl("dummy", "s2"));
        tryInvalidValidate(new ExprIdentNodeImpl("intPrimitive", "s2"));
        tryInvalidValidate(new ExprIdentNodeImpl("intPrimitive", "s3"));
    }

    public void testGetType() throws Exception
    {
        // test success
        identNodes[0].validate(ExprValidationContextFactory.make(streamTypeService));
        assertEquals(String.class, identNodes[0].getExprEvaluator().getType());
    }

    public void testEvaluate() throws Exception
    {
        EventBean[] events = new EventBean[] {makeEvent(10)};

        identNodes[3].validate(ExprValidationContextFactory.make(streamTypeService));
        assertEquals(10, identNodes[3].getExprEvaluator().evaluate(events, false, null));
        assertNull(identNodes[3].getExprEvaluator().evaluate(new EventBean[2], false, null));
    }

    public void testEvaluatePerformance() throws Exception
    {
        // test performance of evaluate for indexed events
        // fails if the getter is not in place

        EventBean[] events = SupportStreamTypeSvc3Stream.getSampleEvents();
        identNodes[2].validate(ExprValidationContextFactory.make(streamTypeService));

        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 100000; i++)
        {
            identNodes[2].getExprEvaluator().evaluate(events, false, null);
        }
        long endTime = System.currentTimeMillis();
        long delta = endTime - startTime;
        log.info(".testEvaluate delta=" + delta);
        assertTrue(delta < 500);
    }

    public void testToExpressionString() throws Exception
    {
        for (int i = 0; i < identNodes.length; i++)
        {
            identNodes[i].validate(ExprValidationContextFactory.make(streamTypeService));
        }
        assertEquals("mapped('a')", ExprNodeUtility.toExpressionStringMinPrecedenceSafe(identNodes[0]));
        assertEquals("nested.nestedValue", ExprNodeUtility.toExpressionStringMinPrecedenceSafe(identNodes[1]));
        assertEquals("s2.indexed[1]", ExprNodeUtility.toExpressionStringMinPrecedenceSafe(identNodes[2]));
        assertEquals("s0.intPrimitive", ExprNodeUtility.toExpressionStringMinPrecedenceSafe(identNodes[3]));
    }

    public void testEqualsNode() throws Exception
    {
        identNodes[0].validate(ExprValidationContextFactory.make(streamTypeService));
        identNodes[2].validate(ExprValidationContextFactory.make(streamTypeService));
        identNodes[3].validate(ExprValidationContextFactory.make(streamTypeService));
        assertTrue(identNodes[3].equalsNode(identNodes[3]));
        assertFalse(identNodes[0].equalsNode(identNodes[2]));
    }

    protected static EventBean makeEvent(int intPrimitive)
    {
        SupportBean theEvent = new SupportBean();
        theEvent.setIntPrimitive(intPrimitive);
        return SupportEventBeanFactory.createObject(theEvent);
    }

    private void tryInvalidValidate(ExprIdentNode identNode)
    {
        try
        {
            identNode.validate(ExprValidationContextFactory.make(streamTypeService));
            fail();
        }
        catch(ExprValidationException ex)
        {
            // expected
        }
    }

    private static final Log log = LogFactory.getLog(TestExprIdentNode.class);
}
