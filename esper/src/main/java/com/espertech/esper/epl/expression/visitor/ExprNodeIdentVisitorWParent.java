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
package com.espertech.esper.epl.expression.visitor;

import com.espertech.esper.collection.Pair;
import com.espertech.esper.epl.expression.core.ExprIdentNode;
import com.espertech.esper.epl.expression.core.ExprNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Visitor for getting a list of identifier nodes with their parent node, which can be null if there is no parent node.
 */
public class ExprNodeIdentVisitorWParent implements ExprNodeVisitorWithParent {
    private List<Pair<ExprNode, ExprIdentNode>> identNodes = new ArrayList<Pair<ExprNode, ExprIdentNode>>();

    public boolean isVisit(ExprNode exprNode) {
        return true;
    }

    public void visit(ExprNode exprNode, ExprNode parentExprNode) {
        if (exprNode instanceof ExprIdentNode) {
            identNodes.add(new Pair<ExprNode, ExprIdentNode>(parentExprNode, (ExprIdentNode) exprNode));
        }
    }

    public List<Pair<ExprNode, ExprIdentNode>> getIdentNodes() {
        return identNodes;
    }
}
