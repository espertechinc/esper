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

import com.espertech.esper.epl.expression.core.ExprNodeUtilityCore;
import com.espertech.esper.epl.expression.core.ExprValidationException;
import com.espertech.esper.support.SupportExprValidationContextFactory;
import com.espertech.esper.supportunit.epl.SupportExprNode;
import com.espertech.esper.supportunit.epl.SupportExprNodeUtil;
import com.espertech.esper.type.RelationalOpEnum;
import junit.framework.TestCase;

public class TestExprRelationalOpNode extends TestCase {
    private ExprRelationalOpNode opNode;

    public void setUp() {
        opNode = new ExprRelationalOpNodeImpl(RelationalOpEnum.GE);
    }

    public void testGetType() throws Exception {
        opNode.addChildNode(new SupportExprNode(Long.class));
        opNode.addChildNode(new SupportExprNode(int.class));
        opNode.validate(SupportExprValidationContextFactory.makeEmpty());
        assertEquals(Boolean.class, opNode.getForge().getEvaluationType());
    }

    public void testValidate() throws Exception {
        // Test success
        opNode.addChildNode(new SupportExprNode(String.class));
        opNode.addChildNode(new SupportExprNode(String.class));
        opNode.validate(SupportExprValidationContextFactory.makeEmpty());

        opNode.setChildNodes(new SupportExprNode(String.class));

        // Test too few nodes under this node
        try {
            opNode.validate(SupportExprValidationContextFactory.makeEmpty());
            fail();
        } catch (IllegalStateException ex) {
            // Expected
        }

        // Test mismatch type
        opNode.addChildNode(new SupportExprNode(Integer.class));
        try {
            opNode.validate(SupportExprValidationContextFactory.makeEmpty());
            fail();
        } catch (ExprValidationException ex) {
            // Expected
        }

        // Test type cannot be compared
        opNode.setChildNodes(new SupportExprNode(Boolean.class));
        opNode.addChildNode(new SupportExprNode(Boolean.class));

        try {
            opNode.validate(SupportExprValidationContextFactory.makeEmpty());
            fail();
        } catch (ExprValidationException ex) {
            // Expected
        }
    }

    public void testEvaluate() throws Exception {
        SupportExprNode childOne = new SupportExprNode("d");
        SupportExprNode childTwo = new SupportExprNode("c");
        opNode.addChildNode(childOne);
        opNode.addChildNode(childTwo);
        opNode.validate(SupportExprValidationContextFactory.makeEmpty());       // Type initialization

        assertEquals(true, opNode.getForge().getExprEvaluator().evaluate(null, false, null));

        childOne.setValue("c");
        assertEquals(true, opNode.getForge().getExprEvaluator().evaluate(null, false, null));

        childOne.setValue("b");
        assertEquals(false, opNode.getForge().getExprEvaluator().evaluate(null, false, null));

        opNode = makeNode(null, Integer.class, 2, Integer.class);
        assertEquals(null, opNode.getForge().getExprEvaluator().evaluate(null, false, null));
        opNode = makeNode(1, Integer.class, null, Integer.class);
        assertEquals(null, opNode.getForge().getExprEvaluator().evaluate(null, false, null));
        opNode = makeNode(null, Integer.class, null, Integer.class);
        assertEquals(null, opNode.getForge().getExprEvaluator().evaluate(null, false, null));
    }

    public void testToExpressionString() throws Exception {
        opNode.addChildNode(new SupportExprNode(10));
        opNode.addChildNode(new SupportExprNode(5));
        assertEquals("10>=5", ExprNodeUtilityCore.toExpressionStringMinPrecedenceSafe(opNode));
    }

    private ExprRelationalOpNode makeNode(Object valueLeft, Class typeLeft, Object valueRight, Class typeRight) throws Exception {
        ExprRelationalOpNode relOpNode = new ExprRelationalOpNodeImpl(RelationalOpEnum.GE);
        relOpNode.addChildNode(new SupportExprNode(valueLeft, typeLeft));
        relOpNode.addChildNode(new SupportExprNode(valueRight, typeRight));
        SupportExprNodeUtil.validate(relOpNode);
        return relOpNode;
    }

    public void testEqualsNode() throws Exception {
        assertTrue(opNode.equalsNode(opNode, false));
        assertFalse(opNode.equalsNode(new ExprRelationalOpNodeImpl(RelationalOpEnum.LE), false));
        assertFalse(opNode.equalsNode(new ExprOrNode(), false));
    }
}
