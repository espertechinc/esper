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
import com.espertech.esper.epl.expression.core.ExprConstantNode;
import com.espertech.esper.epl.expression.core.ExprConstantNodeImpl;
import com.espertech.esper.support.SupportExprValidationContextFactory;
import junit.framework.TestCase;

public class TestExprConstantNode extends TestCase {
    private ExprConstantNode constantNode;

    public void setUp() {
        constantNode = new ExprConstantNodeImpl("5");
    }

    public void testGetType() throws Exception {
        assertEquals(String.class, constantNode.getConstantType());

        constantNode = new ExprConstantNodeImpl(null);
        assertNull(constantNode.getConstantType());
    }

    public void testValidate() throws Exception {
        constantNode.validate(SupportExprValidationContextFactory.makeEmpty());
    }

    public void testEvaluate() {
        assertEquals("5", constantNode.getConstantValue(null));
    }

    public void testToExpressionString() throws Exception {
        constantNode = new ExprConstantNodeImpl("5");
        assertEquals("\"5\"", ExprNodeUtilityCore.toExpressionStringMinPrecedenceSafe(constantNode));

        constantNode = new ExprConstantNodeImpl(10);
        assertEquals("10", ExprNodeUtilityCore.toExpressionStringMinPrecedenceSafe(constantNode));
    }

    public void testEqualsNode() {
        assertTrue(constantNode.equalsNode(new ExprConstantNodeImpl("5"), false));
        assertFalse(constantNode.equalsNode(new ExprOrNode(), false));
        assertFalse(constantNode.equalsNode(new ExprConstantNodeImpl(null), false));
        assertFalse(constantNode.equalsNode(new ExprConstantNodeImpl(3), false));

        constantNode = new ExprConstantNodeImpl(null);
        assertTrue(constantNode.equalsNode(new ExprConstantNodeImpl(null), false));
    }
}
