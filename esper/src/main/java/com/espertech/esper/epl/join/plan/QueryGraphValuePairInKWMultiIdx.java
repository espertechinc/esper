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

public class QueryGraphValuePairInKWMultiIdx {
    private final ExprNode[] indexed;
    private final QueryGraphValueEntryInKeywordMultiIdx key;

    public QueryGraphValuePairInKWMultiIdx(ExprNode[] indexed, QueryGraphValueEntryInKeywordMultiIdx key) {
        this.indexed = indexed;
        this.key = key;
    }

    public ExprNode[] getIndexed() {
        return indexed;
    }

    public QueryGraphValueEntryInKeywordMultiIdx getKey() {
        return key;
    }
}

