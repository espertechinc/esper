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
package com.espertech.esper.common.internal.epl.join.querygraph;

import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;

public class QueryGraphValueEntryRangeRelOp extends QueryGraphValueEntryRange {

    private final ExprEvaluator expression;
    private final boolean isBetweenPart; // indicate that this is part of a between-clause or in-clause

    public QueryGraphValueEntryRangeRelOp(QueryGraphRangeEnum type, ExprEvaluator expression, boolean isBetweenPart) {
        super(type);
        if (type.isRange()) {
            throw new IllegalArgumentException("Invalid ctor for use with ranges");
        }
        this.expression = expression;
        this.isBetweenPart = isBetweenPart;
    }

    public ExprEvaluator getExpression() {
        return expression;
    }

    public boolean isBetweenPart() {
        return isBetweenPart;
    }

    public ExprEvaluator[] getExpressions() {
        return new ExprEvaluator[]{expression};
    }
}
