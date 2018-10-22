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

public class QueryGraphValueDesc {
    private final String[] indexExprs;
    private final QueryGraphValueEntry entry;

    public QueryGraphValueDesc(String[] indexExprs, QueryGraphValueEntry entry) {
        this.indexExprs = indexExprs;
        this.entry = entry;
    }

    public String[] getIndexExprs() {
        return indexExprs;
    }

    public QueryGraphValueEntry getEntry() {
        return entry;
    }
}

