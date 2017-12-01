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
import com.espertech.esper.epl.expression.funcs.ExprCaseNode;
import com.espertech.esper.support.SupportExprValidationContextFactory;
import com.espertech.esper.supportunit.bean.SupportBean;
import com.espertech.esper.supportunit.epl.SupportExprNode;
import com.espertech.esper.supportunit.epl.SupportExprNodeFactory;
import com.espertech.esper.supportunit.event.SupportEventBeanFactory;
import junit.framework.TestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestExprCaseNode extends TestCase {
    public void testGetType() throws Exception {
        // Template expression is:
        // case when (so.floatPrimitive>s1.shortBoxed) then count(5) when (so.LongPrimitive>s1.intPrimitive) then (25 + 130.5) else (3*3) end
        ExprCaseNode caseNode = SupportExprNodeFactory.makeCaseSyntax1Node();
        assertEquals(String.class, caseNode.getForge().getEvaluationType());

        // case when (2.5>2) then count(5) when (1>3) then (25 + 130.5) else (3*3) end
        // First when node is true, case node type is the first when node type.
        caseNode = SupportExprNodeFactory.makeCaseSyntax2Node();
        assertEquals(String.class, caseNode.getForge().getEvaluationType());
    }

    public void testValidate() throws Exception {
        ExprCaseNode caseNode = SupportExprNodeFactory.makeCaseSyntax1Node();
        caseNode.validate(SupportExprValidationContextFactory.makeEmpty());

        caseNode = SupportExprNodeFactory.makeCaseSyntax2Node();
        caseNode.validate(SupportExprValidationContextFactory.makeEmpty());

        // No subnodes: Exception is thrown.
        tryInvalidValidate(new ExprCaseNode(false));
        tryInvalidValidate(new ExprCaseNode(true));

        // singe child node not possible, must be 2 at least
        caseNode = new ExprCaseNode(false);
        caseNode.addChildNode(new SupportExprNode(new Integer(4)));
        tryInvalidValidate(caseNode);

        // in a case 1 expression (e.g. case when a=b then 1 else 2) the when child nodes must return boolean
        caseNode.addChildNode(new SupportExprNode(new Integer(2)));
        tryInvalidValidate(caseNode);

        // in a case 2 expression (e.g. case a when b then 1 else 2) then a and b types must be comparable
        caseNode = new ExprCaseNode(true);
        caseNode.addChildNode(new SupportExprNode("a"));
        caseNode.addChildNode(new SupportExprNode(1));
        caseNode.addChildNode(new SupportExprNode(2));
        tryInvalidValidate(caseNode);
    }

    public void testEvaluate() throws Exception {
        ExprCaseNode caseNode = SupportExprNodeFactory.makeCaseSyntax1Node();
        caseNode.validate(SupportExprValidationContextFactory.makeEmpty());

        assertEquals("a", caseNode.getForge().getExprEvaluator().evaluate(makeEvent(1), false, null));
        assertEquals("b", caseNode.getForge().getExprEvaluator().evaluate(makeEvent(2), false, null));
        assertEquals("c", caseNode.getForge().getExprEvaluator().evaluate(makeEvent(3), false, null));

        caseNode = SupportExprNodeFactory.makeCaseSyntax2Node();
        caseNode.validate(SupportExprValidationContextFactory.makeEmpty());

        assertEquals("a", caseNode.getForge().getExprEvaluator().evaluate(makeEvent(1), false, null));
        assertEquals("b", caseNode.getForge().getExprEvaluator().evaluate(makeEvent(2), false, null));
        assertEquals("c", caseNode.getForge().getExprEvaluator().evaluate(makeEvent(3), false, null));
    }

    public void testEquals() throws Exception {
        ExprCaseNode caseNode = SupportExprNodeFactory.makeCaseSyntax1Node();
        ExprCaseNode otherCaseNode = SupportExprNodeFactory.makeCaseSyntax1Node();
        ExprCaseNode caseNodeSyntax2 = SupportExprNodeFactory.makeCaseSyntax2Node();
        ExprCaseNode otherCaseNodeSyntax2 = SupportExprNodeFactory.makeCaseSyntax2Node();

        assertTrue(caseNode.equalsNode(otherCaseNode, false));
        assertTrue(otherCaseNode.equalsNode(caseNode, false));
        assertFalse(caseNode.equalsNode(caseNodeSyntax2, false));
        assertFalse(caseNodeSyntax2.equalsNode(caseNode, false));
        assertTrue(caseNodeSyntax2.equalsNode(otherCaseNodeSyntax2, false));
    }

    public void testToExpressionString() throws Exception {
        ExprCaseNode _caseNode = SupportExprNodeFactory.makeCaseSyntax1Node();
        assertEquals("case when s0.intPrimitive=1 then \"a\" when s0.intPrimitive=2 then \"b\" else \"c\" end", ExprNodeUtilityCore.toExpressionStringMinPrecedenceSafe(_caseNode));

        _caseNode = SupportExprNodeFactory.makeCaseSyntax2Node();
        assertEquals("case s0.intPrimitive when 1 then \"a\" when 2 then \"b\" else \"c\" end", ExprNodeUtilityCore.toExpressionStringMinPrecedenceSafe(_caseNode));
    }

    private void tryInvalidValidate(ExprCaseNode exprCaseNode) throws Exception {
        try {
            exprCaseNode.validate(SupportExprValidationContextFactory.makeEmpty());
            fail();
        } catch (ExprValidationException ex) {
            // expected
        }
    }

    private EventBean[] makeEvent(int intPrimitive) {
        SupportBean theEvent = new SupportBean();
        theEvent.setIntPrimitive(intPrimitive);
        return new EventBean[]{SupportEventBeanFactory.createObject(theEvent)};
    }

    private static final Logger log = LoggerFactory.getLogger(TestExprCaseNode.class);
}
