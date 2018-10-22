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

import com.espertech.esper.common.internal.epl.expression.core.ExprNode;

public class QueryGraphValuePairInKWMultiIdx {
    private final ExprNode[] indexed;
    private final QueryGraphValueEntryInKeywordMultiIdxForge key;

    public QueryGraphValuePairInKWMultiIdx(ExprNode[] indexed, QueryGraphValueEntryInKeywordMultiIdxForge key) {
        this.indexed = indexed;
        this.key = key;
    }

    public ExprNode[] getIndexed() {
        return indexed;
    }

    public QueryGraphValueEntryInKeywordMultiIdxForge getKey() {
        return key;
    }
}

