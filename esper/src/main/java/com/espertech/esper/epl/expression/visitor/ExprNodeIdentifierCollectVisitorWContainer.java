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
 * Visitor that collects event property identifier information under expression nodes.
 */
public class ExprNodeIdentifierCollectVisitorWContainer implements ExprNodeVisitorWithParent {
    private final List<Pair<ExprNode, ExprIdentNode>> exprProperties;

    /**
     * Ctor.
     */
    public ExprNodeIdentifierCollectVisitorWContainer() {
        this.exprProperties = new ArrayList<Pair<ExprNode, ExprIdentNode>>(2);
    }

    public boolean isVisit(ExprNode exprNode) {
        return true;
    }

    /**
     * Returns list of event property stream numbers and names that uniquely identify which
     * property is from whcih stream, and the name of each.
     *
     * @return list of event property statement-unique info
     */
    public List<Pair<ExprNode, ExprIdentNode>> getExprProperties() {
        return exprProperties;
    }

    public void visit(ExprNode exprNode, ExprNode containerExprNode) {
        if (!(exprNode instanceof ExprIdentNode)) {
            return;
        }
        ExprIdentNode identNode = (ExprIdentNode) exprNode;
        exprProperties.add(new Pair<ExprNode, ExprIdentNode>(containerExprNode, identNode));
    }
}