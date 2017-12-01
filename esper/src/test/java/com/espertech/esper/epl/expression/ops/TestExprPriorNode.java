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
import com.espertech.esper.epl.expression.core.ExprNodeUtilityCore;
import com.espertech.esper.epl.expression.core.ExprValidationException;
import com.espertech.esper.epl.expression.prior.ExprPriorEvalStrategyRandomAccess;
import com.espertech.esper.epl.expression.prior.ExprPriorNode;
import com.espertech.esper.support.SupportExprValidationContextFactory;
import com.espertech.esper.supportunit.bean.SupportBean;
import com.espertech.esper.supportunit.epl.SupportExprNode;
import com.espertech.esper.supportunit.epl.SupportExprNodeFactory;
import com.espertech.esper.supportunit.event.SupportEventBeanFactory;
import com.espertech.esper.view.internal.PriorEventBufferUnbound;
import junit.framework.TestCase;

public class TestExprPriorNode extends TestCase {
    private ExprPriorNode priorNode;

    public void setUp() throws Exception {
        priorNode = SupportExprNodeFactory.makePriorNode();
    }

    public void testGetType() throws Exception {
        assertEquals(Double.class, priorNode.getEvaluationType());
    }

    public void testValidate() throws Exception {
        priorNode = new ExprPriorNode();

        // No subnodes: Exception is thrown.
        tryInvalidValidate(priorNode);

        // singe child node not possible, must be 2 at least
        priorNode.addChildNode(new SupportExprNode(new Integer(4)));
        tryInvalidValidate(priorNode);
    }

    public void testEvaluate() throws Exception {
        PriorEventBufferUnbound buffer = new PriorEventBufferUnbound(10);
        priorNode.setPriorStrategy(new ExprPriorEvalStrategyRandomAccess(buffer));
        EventBean[] events = new EventBean[]{makeEvent(1d), makeEvent(10d)};
        buffer.update(events, null);

        assertEquals(1d, priorNode.evaluate(events, true, null));
    }

    public void testEquals() throws Exception {
        ExprPriorNode node1 = new ExprPriorNode();
        assertTrue(node1.equalsNode(priorNode, false));
    }

    public void testToExpressionString() throws Exception {
        assertEquals("prior(1,s0.doublePrimitive)", ExprNodeUtilityCore.toExpressionStringMinPrecedenceSafe(priorNode));
    }

    private EventBean makeEvent(double doublePrimitive) {
        SupportBean theEvent = new SupportBean();
        theEvent.setDoublePrimitive(doublePrimitive);
        return SupportEventBeanFactory.createObject(theEvent);
    }

    private void tryInvalidValidate(ExprPriorNode exprPriorNode) throws Exception {
        try {
            exprPriorNode.validate(SupportExprValidationContextFactory.makeEmpty());
            fail();
        } catch (ExprValidationException ex) {
            // expected
        }
    }
}
