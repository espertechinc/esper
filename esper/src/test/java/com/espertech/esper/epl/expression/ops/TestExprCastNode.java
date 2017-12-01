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
import com.espertech.esper.epl.expression.funcs.ExprCastNode;
import com.espertech.esper.supportunit.epl.SupportExprNode;
import com.espertech.esper.support.SupportExprValidationContextFactory;
import junit.framework.TestCase;

public class TestExprCastNode extends TestCase {
    private ExprCastNode[] castNodes;

    public void setUp() {
        castNodes = new ExprCastNode[2];

        castNodes[0] = new ExprCastNode("long");
        castNodes[0].addChildNode(new SupportExprNode(10L, Long.class));

        castNodes[1] = new ExprCastNode("java.lang.Integer");
        castNodes[1].addChildNode(new SupportExprNode(0x10, byte.class));
    }

    public void testGetType() throws Exception {
        for (int i = 0; i < castNodes.length; i++) {
            castNodes[i].validate(SupportExprValidationContextFactory.makeEmpty());
        }

        assertEquals(Long.class, castNodes[0].getTargetType());
        assertEquals(Integer.class, castNodes[1].getTargetType());
    }

    public void testValidate() throws Exception {
        ExprCastNode castNode = new ExprCastNode("int");

        // Test too few nodes under this node
        try {
            castNode.validate(SupportExprValidationContextFactory.makeEmpty());
            fail();
        } catch (ExprValidationException ex) {
            // Expected
        }
    }

    public void testEvaluate() throws Exception {
        for (int i = 0; i < castNodes.length; i++) {
            castNodes[i].validate(SupportExprValidationContextFactory.makeEmpty());
        }

        assertEquals(10L, castNodes[0].getForge().getExprEvaluator().evaluate(null, false, null));
        assertEquals(16, castNodes[1].getForge().getExprEvaluator().evaluate(null, false, null));
    }

    public void testEquals() throws Exception {
        assertFalse(castNodes[0].equalsNode(new ExprEqualsNodeImpl(true, false), false));
        assertFalse(castNodes[0].equalsNode(castNodes[1], false));
        assertFalse(castNodes[0].equalsNode(new ExprCastNode("java.lang.Integer"), false));
    }

    public void testToExpressionString() throws Exception {
        castNodes[0].validate(SupportExprValidationContextFactory.makeEmpty());
        assertEquals("cast(10,long)", ExprNodeUtilityCore.toExpressionStringMinPrecedenceSafe(castNodes[0]));
    }
}
