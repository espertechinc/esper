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
import com.espertech.esper.epl.expression.core.MinMaxTypeEnum;
import com.espertech.esper.epl.expression.funcs.ExprMinMaxRowNode;
import com.espertech.esper.support.SupportExprValidationContextFactory;
import com.espertech.esper.supportunit.epl.SupportBoolExprNode;
import com.espertech.esper.supportunit.epl.SupportExprNode;
import com.espertech.esper.supportunit.epl.SupportExprNodeUtil;
import junit.framework.TestCase;

public class TestExprNotNode extends TestCase {
    private ExprNotNode notNode;

    public void setUp() {
        notNode = new ExprNotNode();
    }

    public void testGetType() {
        assertEquals(Boolean.class, notNode.getEvaluationType());
    }

    public void testValidate() throws Exception {
        // fails with zero expressions
        try {
            notNode.validate(SupportExprValidationContextFactory.makeEmpty());
            fail();
        } catch (ExprValidationException ex) {
            // Expected
        }

        // fails with too many sub-expressions
        notNode.addChildNode(new SupportExprNode(Boolean.class));
        notNode.addChildNode(new SupportExprNode(Boolean.class));
        try {
            notNode.validate(SupportExprValidationContextFactory.makeEmpty());
            fail();
        } catch (ExprValidationException ex) {
            // Expected
        }

        // test failure, type mismatch
        notNode = new ExprNotNode();
        notNode.addChildNode(new SupportExprNode(String.class));
        try {
            notNode.validate(SupportExprValidationContextFactory.makeEmpty());
            fail();
        } catch (ExprValidationException ex) {
            // Expected
        }

        // validates
        notNode = new ExprNotNode();
        notNode.addChildNode(new SupportExprNode(Boolean.class));
        notNode.validate(SupportExprValidationContextFactory.makeEmpty());
    }

    public void testEvaluate() throws Exception {
        notNode.addChildNode(new SupportBoolExprNode(true));
        SupportExprNodeUtil.validate(notNode);
        assertFalse((Boolean) notNode.evaluate(null, false, null));

        notNode = new ExprNotNode();
        notNode.addChildNode(new SupportBoolExprNode(false));
        SupportExprNodeUtil.validate(notNode);
        assertTrue((Boolean) notNode.evaluate(null, false, null));
    }

    public void testToExpressionString() throws Exception {
        notNode.addChildNode(new SupportExprNode(true));
        assertEquals("not true", ExprNodeUtilityCore.toExpressionStringMinPrecedenceSafe(notNode));
    }

    public void testEqualsNode() throws Exception {
        assertTrue(notNode.equalsNode(notNode, false));
        assertFalse(notNode.equalsNode(new ExprMinMaxRowNode(MinMaxTypeEnum.MIN), false));
        assertFalse(notNode.equalsNode(new ExprOrNode(), false));
        assertTrue(notNode.equalsNode(new ExprNotNode(), false));
    }
}
