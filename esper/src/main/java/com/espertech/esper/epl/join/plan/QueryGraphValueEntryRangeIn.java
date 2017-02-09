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
package com.espertech.esper.epl.join.plan;

import com.espertech.esper.epl.expression.core.ExprNode;

public class QueryGraphValueEntryRangeIn extends QueryGraphValueEntryRange {

    private static final long serialVersionUID = 7727959298465977751L;

    private ExprNode exprStart;
    private ExprNode exprEnd;
    private boolean allowRangeReversal; // indicate whether "a between 60 and 50" should return no results (false, equivalent to a>= X and a <=Y) or should return results (true, equivalent to 'between' and 'in')

    public QueryGraphValueEntryRangeIn(QueryGraphRangeEnum rangeType, ExprNode exprStart, ExprNode exprEnd, boolean allowRangeReversal) {
        super(rangeType);
        if (!rangeType.isRange()) {
            throw new IllegalArgumentException("Range type expected but received " + rangeType.name());
        }
        this.exprStart = exprStart;
        this.exprEnd = exprEnd;
        this.allowRangeReversal = allowRangeReversal;
    }

    public boolean isAllowRangeReversal() {
        return allowRangeReversal;
    }

    public ExprNode getExprStart() {
        return exprStart;
    }

    public ExprNode getExprEnd() {
        return exprEnd;
    }

    public String toQueryPlan() {
        return this.getClass().getSimpleName();
    }

    public ExprNode[] getExpressions() {
        return new ExprNode[]{exprStart, exprEnd};
    }
}
