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

import java.io.Serializable;

public class QueryGraphValueEntryInKeywordMultiIdx implements QueryGraphValueEntry, Serializable {
    private static final long serialVersionUID = 3018879286841740917L;
    private final ExprNode keyExpr;

    protected QueryGraphValueEntryInKeywordMultiIdx(ExprNode keyExpr) {
        this.keyExpr = keyExpr;
    }

    public ExprNode getKeyExpr() {
        return keyExpr;
    }

    public String toQueryPlan() {
        return "in-keyword multi-indexed single keyed lookup " + ExprNodeUtilityCore.toExpressionStringMinPrecedenceSafe(keyExpr);
    }
}

