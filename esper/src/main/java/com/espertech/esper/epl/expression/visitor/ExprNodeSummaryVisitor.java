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

import com.espertech.esper.epl.expression.baseagg.ExprAggregateNode;
import com.espertech.esper.epl.expression.core.ExprIdentNode;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.epl.expression.core.ExprStreamUnderlyingNode;
import com.espertech.esper.epl.expression.prev.ExprPreviousNode;
import com.espertech.esper.epl.expression.prior.ExprPriorNode;
import com.espertech.esper.epl.expression.subquery.ExprSubselectNode;

/**
 * Visitor for compiling usage informaton of special expressions within an expression tree.
 */
public class ExprNodeSummaryVisitor implements ExprNodeVisitor {
    private boolean hasProperties;
    private boolean hasAggregation;
    private boolean hasSubselect;
    private boolean hasStreamSelect;
    private boolean hasPreviousPrior;

    public boolean isVisit(ExprNode exprNode) {
        return true;
    }

    public void visit(ExprNode exprNode) {
        if (exprNode instanceof ExprIdentNode) {
            hasProperties = true;
        } else if (exprNode instanceof ExprSubselectNode) {
            hasSubselect = true;
        } else if (exprNode instanceof ExprAggregateNode) {
            hasAggregation = true;
        } else if (exprNode instanceof ExprStreamUnderlyingNode) {
            hasStreamSelect = true;
        } else if ((exprNode instanceof ExprPriorNode) || (exprNode instanceof ExprPreviousNode)) {
            hasPreviousPrior = true;
        }
    }

    /**
     * Returns true if the expression is a plain-value expression, without any of the following:
     * properties, aggregation, subselect, stream select, previous or prior
     *
     * @return true for plain
     */
    public boolean isPlain() {
        return !(hasProperties | hasAggregation | hasSubselect | hasStreamSelect | hasPreviousPrior);
    }

    public boolean isHasProperties() {
        return hasProperties;
    }

    public boolean isHasAggregation() {
        return hasAggregation;
    }

    public boolean isHasSubselect() {
        return hasSubselect;
    }

    public boolean isHasStreamSelect() {
        return hasStreamSelect;
    }

    public boolean isHasPreviousPrior() {
        return hasPreviousPrior;
    }

    /**
     * Returns a message if the expression contains special-instruction expressions.
     *
     * @return message
     */
    public String getMessage() {
        if (hasProperties) {
            return "event properties";
        } else if (hasAggregation) {
            return "aggregation functions";
        } else if (hasSubselect) {
            return "sub-selects";
        } else if (hasStreamSelect) {
            return "stream selects or event instance methods";
        } else if (hasPreviousPrior) {
            return "previous or prior functions";
        }
        return null;
    }
}
