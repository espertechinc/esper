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

import java.util.List;

public class QueryGraphValuePairHashKeyIndexForge {
    private final String[] indexed;
    private final List<QueryGraphValueEntryHashKeyedForge> key;
    private final String[] strictKeys;  // those with direct lookup, can contain null elements

    public QueryGraphValuePairHashKeyIndexForge(String[] indexed, List<QueryGraphValueEntryHashKeyedForge> key, String[] strictKeys) {
        this.indexed = indexed;
        this.key = key;
        this.strictKeys = strictKeys;
    }

    public String[] getIndexed() {
        return indexed;
    }

    public List<QueryGraphValueEntryHashKeyedForge> getKeys() {
        return key;
    }

    public String[] getStrictKeys() {
        return strictKeys;
    }
}

