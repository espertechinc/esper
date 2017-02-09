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

import java.util.List;

public class QueryGraphValuePairInKWSingleIdx {
    private final String[] indexed;
    private final List<QueryGraphValueEntryInKeywordSingleIdx> key;

    public QueryGraphValuePairInKWSingleIdx(String[] indexed, List<QueryGraphValueEntryInKeywordSingleIdx> key) {
        this.indexed = indexed;
        this.key = key;
    }

    public String[] getIndexed() {
        return indexed;
    }

    public List<QueryGraphValueEntryInKeywordSingleIdx> getKey() {
        return key;
    }
}

