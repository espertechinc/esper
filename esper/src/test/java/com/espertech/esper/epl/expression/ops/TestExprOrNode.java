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

public class TestExprOrNode extends TestCase {
    private ExprOrNode orNode;

    public void setUp() {
        orNode = new ExprOrNode();
    }

    public void testGetType() {
        assertEquals(Boolean.class, orNode.getForge().getEvaluationType());
    }

    public void testValidate() throws Exception {
        // test success
        orNode.addChildNode(new SupportExprNode(Boolean.class));
        orNode.addChildNode(new SupportExprNode(Boolean.class));
        orNode.validate(SupportExprValidationContextFactory.makeEmpty());

        // test failure, type mismatch
        orNode.addChildNode(new SupportExprNode(String.class));
        try {
            orNode.validate(SupportExprValidationContextFactory.makeEmpty());
            fail();
        } catch (ExprValidationException ex) {
            // Expected
        }

        // test failed - with just one child
        orNode = new ExprOrNode();
        orNode.addChildNode(new SupportExprNode(Boolean.class));
        try {
            orNode.validate(SupportExprValidationContextFactory.makeEmpty());
            fail();
        } catch (ExprValidationException ex) {
            // Expected
        }
    }

    public void testEvaluate() throws Exception {
        orNode.addChildNode(new SupportBoolExprNode(true));
        orNode.addChildNode(new SupportBoolExprNode(false));
        SupportExprNodeUtil.validate(orNode);
        assertTrue((Boolean) orNode.getForge().getExprEvaluator().evaluate(null, false, null));

        orNode = new ExprOrNode();
        orNode.addChildNode(new SupportBoolExprNode(false));
        orNode.addChildNode(new SupportBoolExprNode(false));
        SupportExprNodeUtil.validate(orNode);
        assertFalse((Boolean) orNode.getForge().getExprEvaluator().evaluate(null, false, null));

        orNode = new ExprOrNode();
        orNode.addChildNode(new SupportExprNode(null, Boolean.class));
        orNode.addChildNode(new SupportExprNode(false));
        SupportExprNodeUtil.validate(orNode);
        assertNull(orNode.getForge().getExprEvaluator().evaluate(null, false, null));
    }

    public void testToExpressionString() throws Exception {
        orNode.addChildNode(new SupportExprNode(true));
        orNode.addChildNode(new SupportExprNode(false));
        assertEquals("true or false", ExprNodeUtilityCore.toExpressionStringMinPrecedenceSafe(orNode));
    }

    public void testEqualsNode() throws Exception {
        assertTrue(orNode.equalsNode(orNode, false));
        assertFalse(orNode.equalsNode(new ExprMinMaxRowNode(MinMaxTypeEnum.MIN), false));
        assertTrue(orNode.equalsNode(new ExprOrNode(), false));
    }
}
