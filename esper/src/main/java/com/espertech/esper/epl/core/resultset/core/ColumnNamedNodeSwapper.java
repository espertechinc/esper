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
import com.espertech.esper.epl.expression.core.ExprNode;

/**
 * A utility class for replacing select-clause column names with their
 * definitions in expression node trees.
 */
public class ColumnNamedNodeSwapper {
    /**
     * Replace all instances of the node representing the colum name with
     * the full expression.
     *
     * @param exprTree   - the expression node tree to make the changes in
     * @param columnName - the select-clause name that is to be expanded
     * @param fullExpr   - the full expression that the column name represents
     * @return exprTree with the appropriate swaps performed, or fullExpr,
     * if all of exprTree needed to be swapped
     */
    public static ExprNode swap(ExprNode exprTree, String columnName, ExprNode fullExpr) {
        if (fullExpr == null) {
            throw new NullPointerException();
        }

        if (isColumnNameNode(exprTree, columnName)) {
            return fullExpr;
        } else {
            visitChildren(exprTree, columnName, fullExpr);
        }

        return exprTree;
    }

    /**
     * A recursive function that works on the child nodes of a given
     * node, replacing any instances of the node representing the name,
     * and visiting the children of all other nodes.
     *
     * @param node     - the node whose children are to be examined for names
     * @param name     - the name to replace
     * @param fullExpr - the full expression corresponding to the name
     */
    private static void visitChildren(ExprNode node, String name, ExprNode fullExpr) {
        ExprNode[] childNodes = node.getChildNodes();

        for (int i = 0; i < childNodes.length; i++) {
            ExprNode childNode = childNodes[i];
            if (isColumnNameNode(childNode, name)) {
                node.setChildNode(i, fullExpr);
            } else {
                visitChildren(childNode, name, fullExpr);
            }
        }
    }

    private static boolean isColumnNameNode(ExprNode node, String name) {
        if (node instanceof ExprIdentNode) {
            if (node.getChildNodes().length > 0) {
                throw new IllegalStateException("Ident node has unexpected child nodes");
            }
            ExprIdentNode identNode = (ExprIdentNode) node;
            return identNode.getUnresolvedPropertyName().equals(name) && identNode.getStreamOrPropertyName() == null;
        } else {
            return false;
        }
    }
}
