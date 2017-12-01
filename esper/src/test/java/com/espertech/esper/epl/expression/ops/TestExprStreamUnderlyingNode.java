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
import com.espertech.esper.epl.expression.core.ExprStreamUnderlyingNode;
import com.espertech.esper.epl.expression.core.ExprStreamUnderlyingNodeImpl;
import com.espertech.esper.epl.expression.core.ExprValidationException;
import com.espertech.esper.supportunit.bean.SupportBean;
import com.espertech.esper.supportunit.epl.SupportStreamTypeSvc3Stream;
import com.espertech.esper.supportunit.event.SupportEventBeanFactory;
import com.espertech.esper.support.SupportExprValidationContextFactory;
import junit.framework.TestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestExprStreamUnderlyingNode extends TestCase {
    private ExprStreamUnderlyingNodeImpl node;
    private StreamTypeService streamTypeService;

    public void setUp() {
        node = new ExprStreamUnderlyingNodeImpl("s0", false);
        streamTypeService = new SupportStreamTypeSvc3Stream();
    }

    public void testValidateInvalid() throws Exception {
        try {
            node.getStreamId();
            fail();
        } catch (IllegalStateException ex) {
            // expected
        }

        try {
            node.getForge().getEvaluationType();
            fail();
        } catch (IllegalStateException ex) {
            // expected
        }
    }

    public void testValidate() throws Exception {
        node.validate(SupportExprValidationContextFactory.make(streamTypeService));
        assertEquals(0, node.getStreamId());
        assertEquals(SupportBean.class, node.getEvaluationType());

        tryInvalidValidate(new ExprStreamUnderlyingNodeImpl("", false));
        tryInvalidValidate(new ExprStreamUnderlyingNodeImpl("dummy", false));
    }

    public void testEvaluate() throws Exception {
        EventBean theEvent = makeEvent(10);
        EventBean[] events = new EventBean[]{theEvent};

        node.validate(SupportExprValidationContextFactory.make(streamTypeService));
        assertEquals(theEvent.getUnderlying(), node.evaluate(events, false, null));
    }

    public void testEqualsNode() throws Exception {
        node.validate(SupportExprValidationContextFactory.make(streamTypeService));
        assertTrue(node.equalsNode(new ExprStreamUnderlyingNodeImpl("s0", false), false));
        assertFalse(node.equalsNode(new ExprStreamUnderlyingNodeImpl("xxx", false), false));
    }

    protected static EventBean makeEvent(int intPrimitive) {
        SupportBean theEvent = new SupportBean();
        theEvent.setIntPrimitive(intPrimitive);
        return SupportEventBeanFactory.createObject(theEvent);
    }

    private void tryInvalidValidate(ExprStreamUnderlyingNode node) {
        try {
            node.validate(SupportExprValidationContextFactory.make(streamTypeService));
            fail();
        } catch (ExprValidationException ex) {
            // expected
        }
    }

    private static final Logger log = LoggerFactory.getLogger(TestExprStreamUnderlyingNode.class);
}
