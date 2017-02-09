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

import com.espertech.esper.epl.enummethod.dot.ExprLambdaGoesNode;
import com.espertech.esper.epl.expression.baseagg.ExprAggregateNode;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.epl.expression.core.ExprStreamUnderlyingNode;
import com.espertech.esper.epl.expression.dot.ExprDotNode;

/**
 * Visitor that collects event property identifier information under expression nodes.
 * The visitor can be configued to not visit aggregation nodes thus ignoring
 * properties under aggregation nodes such as sum, avg, min/max etc.
 */
public class ExprNodeStreamSelectVisitor implements ExprNodeVisitor {
    private final boolean isVisitAggregateNodes;
    private boolean hasStreamSelect;

    /**
     * Ctor.
     *
     * @param visitAggregateNodes true to indicate that the visitor should visit aggregate nodes, or false
     *                            if the visitor ignores aggregate nodes
     */
    public ExprNodeStreamSelectVisitor(boolean visitAggregateNodes) {
        this.isVisitAggregateNodes = visitAggregateNodes;
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

    public boolean hasStreamSelect() {
        return hasStreamSelect;
    }

    public void visit(ExprNode exprNode) {
        if (exprNode instanceof ExprStreamUnderlyingNode) {
            hasStreamSelect = true;
        }
        if (exprNode instanceof ExprDotNode) {
            ExprDotNode streamRef = (ExprDotNode) exprNode;
            if (streamRef.getStreamReferencedIfAny() != null) {
                hasStreamSelect = true;
            }
        }
    }
}
