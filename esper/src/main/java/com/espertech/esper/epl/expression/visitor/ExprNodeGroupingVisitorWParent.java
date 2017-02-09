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
import com.espertech.esper.epl.expression.core.ExprGroupingIdNode;
import com.espertech.esper.epl.expression.core.ExprGroupingNode;
import com.espertech.esper.epl.expression.core.ExprNode;

import java.util.ArrayList;
import java.util.List;

public class ExprNodeGroupingVisitorWParent implements ExprNodeVisitorWithParent {
    private final List<Pair<ExprNode, ExprGroupingIdNode>> groupingIdNodes;
    private final List<Pair<ExprNode, ExprGroupingNode>> groupingNodes;

    /**
     * Ctor.
     */
    public ExprNodeGroupingVisitorWParent() {
        this.groupingIdNodes = new ArrayList<Pair<ExprNode, ExprGroupingIdNode>>(2);
        this.groupingNodes = new ArrayList<Pair<ExprNode, ExprGroupingNode>>(2);
    }

    public boolean isVisit(ExprNode exprNode) {
        return true;
    }

    public List<Pair<ExprNode, ExprGroupingIdNode>> getGroupingIdNodes() {
        return groupingIdNodes;
    }

    public List<Pair<ExprNode, ExprGroupingNode>> getGroupingNodes() {
        return groupingNodes;
    }

    public void visit(ExprNode exprNode, ExprNode parentExprNode) {
        if (exprNode instanceof ExprGroupingIdNode) {
            groupingIdNodes.add(new Pair<ExprNode, ExprGroupingIdNode>(parentExprNode, (ExprGroupingIdNode) exprNode));
        }
        if (exprNode instanceof ExprGroupingNode) {
            groupingNodes.add(new Pair<ExprNode, ExprGroupingNode>(parentExprNode, (ExprGroupingNode) exprNode));
        }
    }
}