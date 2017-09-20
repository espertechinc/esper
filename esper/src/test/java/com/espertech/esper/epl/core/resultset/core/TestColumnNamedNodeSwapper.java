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
package com.espertech.esper.epl.core.resultset.core;

import com.espertech.esper.epl.expression.core.ExprIdentNode;
import com.espertech.esper.epl.expression.core.ExprIdentNodeImpl;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.epl.expression.ops.ExprEqualsNode;
import com.espertech.esper.epl.expression.ops.ExprEqualsNodeImpl;
import com.espertech.esper.supportunit.epl.SupportExprNodeFactory;
import junit.framework.TestCase;

public class TestColumnNamedNodeSwapper extends TestCase {
    ExprNode exprTree;
    String alias;
    ExprNode fullExpr;
    ExprNode resultingTree;

    public void setUp() throws Exception {
        fullExpr = new ExprIdentNodeImpl("full expression");
    }

    public void testWholeReplaced() throws Exception {
        exprTree = new ExprIdentNodeImpl("swapped");
        alias = "swapped";
        resultingTree = ColumnNamedNodeSwapper.swap(exprTree, alias, fullExpr);
        assertTrue(resultingTree == fullExpr);
    }

    public void testPartReplaced() throws Exception {
        exprTree = makeEqualsNode();
        alias = "intPrimitive";
        resultingTree = ColumnNamedNodeSwapper.swap(exprTree, alias, fullExpr);

        assertTrue(resultingTree == exprTree);
        ExprNode[] childNodes = resultingTree.getChildNodes();
        ExprNode[] oldChildNodes = exprTree.getChildNodes();
        assertTrue(childNodes.length == 2);
        assertTrue(childNodes[0] == fullExpr);
        assertTrue(childNodes[1] == oldChildNodes[1]);

        exprTree = resultingTree;
        alias = "intBoxed";
        resultingTree = ColumnNamedNodeSwapper.swap(exprTree, alias, fullExpr);
        childNodes = resultingTree.getChildNodes();
        assertTrue(childNodes.length == 2);
        assertTrue(childNodes[0] == fullExpr);
        assertTrue(childNodes[1] == fullExpr);

        exprTree = resultingTree;
        ExprNode newFullExpr = new ExprIdentNodeImpl("new full expr");
        alias = "full expression";
        resultingTree = ColumnNamedNodeSwapper.swap(exprTree, alias, newFullExpr);
        childNodes = resultingTree.getChildNodes();
        assertTrue(childNodes.length == 2);
        assertTrue(childNodes[0] == newFullExpr);
        assertTrue(childNodes[1] == newFullExpr);
    }

    public static ExprEqualsNode makeEqualsNode() throws Exception {
        ExprEqualsNode topNode = new ExprEqualsNodeImpl(false, false);
        ExprIdentNode i1_1 = new ExprIdentNodeImpl("intPrimitive");
        ExprIdentNode i1_2 = new ExprIdentNodeImpl("intBoxed");
        topNode.addChildNode(i1_1);
        topNode.addChildNode(i1_2);

        SupportExprNodeFactory.validate1StreamBean(topNode);

        return topNode;
    }

}
