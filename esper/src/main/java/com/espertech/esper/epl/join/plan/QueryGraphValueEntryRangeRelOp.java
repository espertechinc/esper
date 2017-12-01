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

import com.espertech.esper.epl.expression.core.ExprNodeUtilityCore;
import com.espertech.esper.epl.expression.core.ExprNode;

public class QueryGraphValueEntryRangeRelOp extends QueryGraphValueEntryRange {

    private static final long serialVersionUID = -4616155064548498888L;

    private final ExprNode expression;
    private final boolean isBetweenPart; // indicate that this is part of a between-clause or in-clause

    public QueryGraphValueEntryRangeRelOp(QueryGraphRangeEnum type, ExprNode expression, boolean isBetweenPart) {
        super(type);
        if (type.isRange()) {
            throw new IllegalArgumentException("Invalid ctor for use with ranges");
        }
        this.expression = expression;
        this.isBetweenPart = isBetweenPart;
    }

    public ExprNode getExpression() {
        return expression;
    }

    public boolean isBetweenPart() {
        return isBetweenPart;
    }

    public String toQueryPlan() {
        return getType().getStringOp() + " on " + ExprNodeUtilityCore.toExpressionStringMinPrecedenceSafe(expression);
    }

    public ExprNode[] getExpressions() {
        return new ExprNode[]{expression};
    }
}
