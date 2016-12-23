/*
 * *************************************************************************************
 *  Copyright (C) 2006-2015 EsperTech, Inc. All rights reserved.                       *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 * *************************************************************************************
 */

package com.espertech.esper.epl.expression;

import com.espertech.esper.epl.expression.core.ExprNodeOrigin;
import com.espertech.esper.epl.expression.core.ExprNodeUtility;
import com.espertech.esper.supportunit.epl.SupportExprNode;
import com.espertech.esper.supportunit.epl.SupportExprNodeFactory;
import com.espertech.esper.util.support.SupportExprValidationContextFactory;
import junit.framework.TestCase;

public class TestExprNode extends TestCase
{
    public void testGetValidatedSubtree() throws Exception
    {
        SupportExprNode.setValidateCount(0);

        // Confirms all child nodes validated
        // Confirms depth-first validation
        SupportExprNode topNode = new SupportExprNode(Boolean.class);

        SupportExprNode parent_1 = new SupportExprNode(Boolean.class);
        SupportExprNode parent_2 = new SupportExprNode(Boolean.class);

        topNode.addChildNode(parent_1);
        topNode.addChildNode(parent_2);

        SupportExprNode supportNode1_1 = new SupportExprNode(Boolean.class);
        SupportExprNode supportNode1_2 = new SupportExprNode(Boolean.class);
        SupportExprNode supportNode2_1 = new SupportExprNode(Boolean.class);
        SupportExprNode supportNode2_2 = new SupportExprNode(Boolean.class);

        parent_1.addChildNode(supportNode1_1);
        parent_1.addChildNode(supportNode1_2);
        parent_2.addChildNode(supportNode2_1);
        parent_2.addChildNode(supportNode2_2);

        ExprNodeUtility.getValidatedSubtree(ExprNodeOrigin.SELECT, topNode, SupportExprValidationContextFactory.makeEmpty());

        assertEquals(1, supportNode1_1.getValidateCountSnapshot());
        assertEquals(2, supportNode1_2.getValidateCountSnapshot());
        assertEquals(3, parent_1.getValidateCountSnapshot());
        assertEquals(4, supportNode2_1.getValidateCountSnapshot());
        assertEquals(5, supportNode2_2.getValidateCountSnapshot());
        assertEquals(6, parent_2.getValidateCountSnapshot());
        assertEquals(7, topNode.getValidateCountSnapshot());
    }

    public void testDeepEquals() throws Exception
    {
        assertFalse(ExprNodeUtility.deepEquals(SupportExprNodeFactory.make2SubNodeAnd(), SupportExprNodeFactory.make3SubNodeAnd()));
        assertFalse(ExprNodeUtility.deepEquals(SupportExprNodeFactory.makeEqualsNode(), SupportExprNodeFactory.makeMathNode()));
        assertTrue(ExprNodeUtility.deepEquals(SupportExprNodeFactory.makeMathNode(), SupportExprNodeFactory.makeMathNode()));
        assertFalse(ExprNodeUtility.deepEquals(SupportExprNodeFactory.makeMathNode(), SupportExprNodeFactory.make2SubNodeAnd()));
        assertTrue(ExprNodeUtility.deepEquals(SupportExprNodeFactory.make3SubNodeAnd(), SupportExprNodeFactory.make3SubNodeAnd()));
    }

    public void testParseMappedProp()
    {
        ExprNodeUtility.MappedPropertyParseResult result = ExprNodeUtility.parseMappedProperty("a.b('c')");
        assertEquals("a", result.getClassName());
        assertEquals("b", result.getMethodName());
        assertEquals("c", result.getArgString());

        result = ExprNodeUtility.parseMappedProperty("SupportStaticMethodLib.delimitPipe('POLYGON ((100.0 100, \", 100 100, 400 400))')");
        assertEquals("SupportStaticMethodLib", result.getClassName());
        assertEquals("delimitPipe", result.getMethodName());
        assertEquals("POLYGON ((100.0 100, \", 100 100, 400 400))", result.getArgString());

        result = ExprNodeUtility.parseMappedProperty("a.b.c.d.e('f.g.h,u.h')");
        assertEquals("a.b.c.d", result.getClassName());
        assertEquals("e", result.getMethodName());
        assertEquals("f.g.h,u.h", result.getArgString());

        result = ExprNodeUtility.parseMappedProperty("a.b.c.d.E(\"hfhf f f f \")");
        assertEquals("a.b.c.d", result.getClassName());
        assertEquals("E", result.getMethodName());
        assertEquals("hfhf f f f ", result.getArgString());

        result = ExprNodeUtility.parseMappedProperty("c.d.getEnumerationSource(\"kf\"kf'kf\")");
        assertEquals("c.d", result.getClassName());
        assertEquals("getEnumerationSource", result.getMethodName());
        assertEquals("kf\"kf'kf", result.getArgString());

        result = ExprNodeUtility.parseMappedProperty("c.d.getEnumerationSource('kf\"kf'kf\"')");
        assertEquals("c.d", result.getClassName());
        assertEquals("getEnumerationSource", result.getMethodName());
        assertEquals("kf\"kf'kf\"", result.getArgString());

        result = ExprNodeUtility.parseMappedProperty("f('a')");
        assertEquals(null, result.getClassName());
        assertEquals("f", result.getMethodName());
        assertEquals("a", result.getArgString());

        assertNull(ExprNodeUtility.parseMappedProperty("('a')"));
        assertNull(ExprNodeUtility.parseMappedProperty(""));
    }
}
