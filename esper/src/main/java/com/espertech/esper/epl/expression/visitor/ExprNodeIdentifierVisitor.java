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
import com.espertech.esper.epl.enummethod.dot.ExprLambdaGoesNode;
import com.espertech.esper.epl.expression.baseagg.ExprAggregateNode;
import com.espertech.esper.epl.expression.core.ExprIdentNode;
import com.espertech.esper.epl.expression.core.ExprNode;

import java.util.LinkedList;
import java.util.List;

/**
 * Visitor that collects event property identifier information under expression nodes.
 * The visitor can be configued to not visit aggregation nodes thus ignoring
 * properties under aggregation nodes such as sum, avg, min/max etc.
 */
public class ExprNodeIdentifierVisitor implements ExprNodeVisitor {
    private final List<Pair<Integer, String>> exprProperties;
    private final boolean isVisitAggregateNodes;

    /**
     * Ctor.
     *
     * @param visitAggregateNodes true to indicate that the visitor should visit aggregate nodes, or false
     *                            if the visitor ignores aggregate nodes
     */
    public ExprNodeIdentifierVisitor(boolean visitAggregateNodes) {
        this.isVisitAggregateNodes = visitAggregateNodes;
        this.exprProperties = new LinkedList<Pair<Integer, String>>();
    }

    public boolean isVisit(ExprNode exprNode) {
        if (exprNode instanceof ExprLambdaGoesNode) {
            return false;
        }

        if (isVisitAggregateNodes) {
            return true;
        }

        return !(exprNode instanceof ExprAggregateNode);
    }

    /**
     * Returns list of event property stream numbers and names that uniquely identify which
     * property is from whcih stream, and the name of each.
     *
     * @return list of event property statement-unique info
     */
    public List<Pair<Integer, String>> getExprProperties() {
        return exprProperties;
    }

    public void visit(ExprNode exprNode) {
        if (!(exprNode instanceof ExprIdentNode)) {
            return;
        }

        ExprIdentNode identNode = (ExprIdentNode) exprNode;

        int streamId = identNode.getStreamId();
        String propertyName = identNode.getResolvedPropertyName();

        exprProperties.add(new Pair<Integer, String>(streamId, propertyName));
    }
}
