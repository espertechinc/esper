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
package com.espertech.esper.common.internal.epl.expression.ops;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityPrint;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportExprValidationContextFactory;
import com.espertech.esper.common.internal.supportunit.event.SupportEventBeanFactory;
import com.espertech.esper.common.internal.supportunit.util.SupportExprNode;
import com.espertech.esper.common.internal.supportunit.util.SupportExprNodeFactory;
import junit.framework.TestCase;

public class TestExprLikeNode extends TestCase {
    private ExprLikeNode likeNodeNormal;
    private ExprLikeNode likeNodeNot;
    private ExprLikeNode likeNodeNormalEscaped;

    public void setUp() throws Exception {
        likeNodeNormal = SupportExprNodeFactory.makeLikeNode(false, null);
        likeNodeNot = SupportExprNodeFactory.makeLikeNode(true, null);
        likeNodeNormalEscaped = SupportExprNodeFactory.makeLikeNode(false, "!");
    }

    public void testGetType() throws Exception {
        assertEquals(Boolean.class, likeNodeNormal.getType());
        assertEquals(Boolean.class, likeNodeNot.getType());
        assertEquals(Boolean.class, likeNodeNormalEscaped.getType());
    }

    public void testValidate() throws Exception {
        // No subnodes: Exception is thrown.
        tryInvalidValidate(new ExprLikeNode(true));

        // singe child node not possible, must be 2 at least
        likeNodeNormal = new ExprLikeNode(false);
        likeNodeNormal.addChildNode(new SupportExprNode(new Integer(4)));
        tryInvalidValidate(likeNodeNormal);

        // test a type mismatch
        likeNodeNormal = new ExprLikeNode(true);
        likeNodeNormal.addChildNode(new SupportExprNode("sx"));
        likeNodeNormal.addChildNode(new SupportExprNode(4));
        tryInvalidValidate(likeNodeNormal);

        // test numeric supported
        likeNodeNormal = new ExprLikeNode(false);
        likeNodeNormal.addChildNode(new SupportExprNode(new Integer(4)));
        likeNodeNormal.addChildNode(new SupportExprNode("sx"));

        // test invalid escape char
        likeNodeNormal = new ExprLikeNode(false);
        likeNodeNormal.addChildNode(new SupportExprNode(new Integer(4)));
        likeNodeNormal.addChildNode(new SupportExprNode("sx"));
        likeNodeNormal.addChildNode(new SupportExprNode(5));
    }

    public void testEvaluate() throws Exception {
        // Build :      s0.string like "%abc__"  (with or witout escape)
        assertFalse((Boolean) likeNodeNormal.getForge().getExprEvaluator().evaluate(makeEvent("abcx"), false, null));
        assertTrue((Boolean) likeNodeNormal.getForge().getExprEvaluator().evaluate(makeEvent("dskfsljkdfabcxx"), false, null));
        assertTrue((Boolean) likeNodeNot.getForge().getExprEvaluator().evaluate(makeEvent("abcx"), false, null));
        assertFalse((Boolean) likeNodeNot.getForge().getExprEvaluator().evaluate(makeEvent("dskfsljkdfabcxx"), false, null));
    }

    public void testEquals() throws Exception {
        ExprLikeNode otherLikeNodeNot = SupportExprNodeFactory.makeLikeNode(true, "@");
        ExprLikeNode otherLikeNodeNot2 = SupportExprNodeFactory.makeLikeNode(true, "!");

        assertTrue(likeNodeNot.equalsNode(otherLikeNodeNot2, false));
        assertTrue(otherLikeNodeNot2.equalsNode(otherLikeNodeNot, false)); // Escape char itself is an expression
        assertFalse(likeNodeNormal.equalsNode(otherLikeNodeNot, false));
    }

    public void testToExpressionString() throws Exception {
        assertEquals("s0.theString like \"%abc__\"", ExprNodeUtilityPrint.toExpressionStringMinPrecedenceSafe(likeNodeNormal));
        assertEquals("s0.theString not like \"%abc__\"", ExprNodeUtilityPrint.toExpressionStringMinPrecedenceSafe(likeNodeNot));
        assertEquals("s0.theString like \"%abc__\" escape \"!\"", ExprNodeUtilityPrint.toExpressionStringMinPrecedenceSafe(likeNodeNormalEscaped));
    }

    private EventBean[] makeEvent(String stringValue) {
        SupportBean theEvent = new SupportBean();
        theEvent.setTheString(stringValue);
        return new EventBean[]{SupportEventBeanFactory.createObject(theEvent)};
    }

    private void tryInvalidValidate(ExprLikeNode exprLikeRegexpNode) throws Exception {
        try {
            exprLikeRegexpNode.validate(SupportExprValidationContextFactory.makeEmpty());
            fail();
        } catch (ExprValidationException ex) {
            // expected
        }
    }
}
