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

public class QueryGraphValueDesc {
    private final ExprNode[] indexExprs;
    private final QueryGraphValueEntry entry;

    public QueryGraphValueDesc(ExprNode[] indexExprs, QueryGraphValueEntry entry) {
        this.indexExprs = indexExprs;
        this.entry = entry;
    }

    public ExprNode[] getIndexExprs() {
        return indexExprs;
    }

    public QueryGraphValueEntry getEntry() {
        return entry;
    }
}

