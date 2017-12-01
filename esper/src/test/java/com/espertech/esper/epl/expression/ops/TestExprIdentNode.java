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

import com.espertech.esper.client.EventBean;
import com.espertech.esper.epl.core.streamtype.StreamTypeService;
import com.espertech.esper.epl.expression.core.ExprNodeUtilityCore;
import com.espertech.esper.epl.expression.core.ExprIdentNode;
import com.espertech.esper.epl.expression.core.ExprIdentNodeImpl;
import com.espertech.esper.epl.expression.core.ExprValidationException;
import com.espertech.esper.support.SupportExprValidationContextFactory;
import com.espertech.esper.supportunit.bean.SupportBean;
import com.espertech.esper.supportunit.epl.SupportStreamTypeSvc3Stream;
import com.espertech.esper.supportunit.event.SupportEventBeanFactory;
import junit.framework.TestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestExprIdentNode extends TestCase {
    private ExprIdentNode identNodes[];
    private StreamTypeService streamTypeService;

    public void setUp() {
        identNodes = new ExprIdentNode[4];
        identNodes[0] = new ExprIdentNodeImpl("mapped('a')");
        identNodes[1] = new ExprIdentNodeImpl("nestedValue", "nested");
        identNodes[2] = new ExprIdentNodeImpl("indexed[1]", "s2");
        identNodes[3] = new ExprIdentNodeImpl("intPrimitive", "s0");

        streamTypeService = new SupportStreamTypeSvc3Stream();
    }

    public void testValidateInvalid() throws Exception {
        try {
            identNodes[0].getStreamId();
            fail();
        } catch (IllegalStateException ex) {
            // expected
        }

        try {
            assertNull(identNodes[0].getForge().getExprEvaluator());
            fail();
        } catch (IllegalStateException ex) {
            // expected
        }

        try {
            identNodes[0].getResolvedStreamName();
            fail();
        } catch (IllegalStateException ex) {
            // expected
        }

        try {
            identNodes[0].getResolvedPropertyName();
            fail();
        } catch (IllegalStateException ex) {
            // expected
        }
    }

    public void testValidate() throws Exception {
        identNodes[0].validate(SupportExprValidationContextFactory.make(streamTypeService));
        assertEquals(2, identNodes[0].getStreamId());
        assertEquals(String.class, identNodes[0].getForge().getEvaluationType());
        assertEquals("mapped('a')", identNodes[0].getResolvedPropertyName());

        identNodes[1].validate(SupportExprValidationContextFactory.make(streamTypeService));
        assertEquals(2, identNodes[1].getStreamId());
        assertEquals(String.class, identNodes[1].getForge().getEvaluationType());
        assertEquals("nested.nestedValue", identNodes[1].getResolvedPropertyName());

        identNodes[2].validate(SupportExprValidationContextFactory.make(streamTypeService));
        assertEquals(2, identNodes[2].getStreamId());
        assertEquals(Integer.class, identNodes[2].getForge().getEvaluationType());
        assertEquals("indexed[1]", identNodes[2].getResolvedPropertyName());

        identNodes[3].validate(SupportExprValidationContextFactory.make(streamTypeService));
        assertEquals(0, identNodes[3].getStreamId());
        assertEquals(Integer.class, identNodes[3].getForge().getEvaluationType());
        assertEquals("intPrimitive", identNodes[3].getResolvedPropertyName());

        tryInvalidValidate(new ExprIdentNodeImpl(""));
        tryInvalidValidate(new ExprIdentNodeImpl("dummy"));
        tryInvalidValidate(new ExprIdentNodeImpl("nested", "s0"));
        tryInvalidValidate(new ExprIdentNodeImpl("dummy", "s2"));
        tryInvalidValidate(new ExprIdentNodeImpl("intPrimitive", "s2"));
        tryInvalidValidate(new ExprIdentNodeImpl("intPrimitive", "s3"));
    }

    public void testGetType() throws Exception {
        // test success
        identNodes[0].validate(SupportExprValidationContextFactory.make(streamTypeService));
        assertEquals(String.class, identNodes[0].getForge().getEvaluationType());
    }

    public void testEvaluate() throws Exception {
        EventBean[] events = new EventBean[]{makeEvent(10)};

        identNodes[3].validate(SupportExprValidationContextFactory.make(streamTypeService));
        assertEquals(10, identNodes[3].getForge().getExprEvaluator().evaluate(events, false, null));
        assertNull(identNodes[3].getForge().getExprEvaluator().evaluate(new EventBean[2], false, null));
    }

    public void testEvaluatePerformance() throws Exception {
        // test performance of evaluate for indexed events
        // fails if the getter is not in place

        EventBean[] events = SupportStreamTypeSvc3Stream.getSampleEvents();
        identNodes[2].validate(SupportExprValidationContextFactory.make(streamTypeService));

        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 100000; i++) {
            identNodes[2].getForge().getExprEvaluator().evaluate(events, false, null);
        }
        long endTime = System.currentTimeMillis();
        long delta = endTime - startTime;
        log.info(".testEvaluate delta=" + delta);
        assertTrue(delta < 500);
    }

    public void testToExpressionString() throws Exception {
        for (int i = 0; i < identNodes.length; i++) {
            identNodes[i].validate(SupportExprValidationContextFactory.make(streamTypeService));
        }
        assertEquals("mapped('a')", ExprNodeUtilityCore.toExpressionStringMinPrecedenceSafe(identNodes[0]));
        assertEquals("nested.nestedValue", ExprNodeUtilityCore.toExpressionStringMinPrecedenceSafe(identNodes[1]));
        assertEquals("s2.indexed[1]", ExprNodeUtilityCore.toExpressionStringMinPrecedenceSafe(identNodes[2]));
        assertEquals("s0.intPrimitive", ExprNodeUtilityCore.toExpressionStringMinPrecedenceSafe(identNodes[3]));
    }

    public void testEqualsNode() throws Exception {
        identNodes[0].validate(SupportExprValidationContextFactory.make(streamTypeService));
        identNodes[2].validate(SupportExprValidationContextFactory.make(streamTypeService));
        identNodes[3].validate(SupportExprValidationContextFactory.make(streamTypeService));
        assertTrue(identNodes[3].equalsNode(identNodes[3], false));
        assertFalse(identNodes[0].equalsNode(identNodes[2], false));
    }

    protected static EventBean makeEvent(int intPrimitive) {
        SupportBean theEvent = new SupportBean();
        theEvent.setIntPrimitive(intPrimitive);
        return SupportEventBeanFactory.createObject(theEvent);
    }

    private void tryInvalidValidate(ExprIdentNode identNode) {
        try {
            identNode.validate(SupportExprValidationContextFactory.make(streamTypeService));
            fail();
        } catch (ExprValidationException ex) {
            // expected
        }
    }

    private static final Logger log = LoggerFactory.getLogger(TestExprIdentNode.class);
}
