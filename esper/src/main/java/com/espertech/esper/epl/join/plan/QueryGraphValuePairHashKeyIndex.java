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

public class QueryGraphValuePairHashKeyIndex {
    private final String[] indexed;
    private final List<QueryGraphValueEntryHashKeyed> key;
    private final String[] strictKeys;  // those with direct lookup, can contain null elements

    public QueryGraphValuePairHashKeyIndex(String[] indexed, List<QueryGraphValueEntryHashKeyed> key, String[] strictKeys) {
        this.indexed = indexed;
        this.key = key;
        this.strictKeys = strictKeys;
    }

    public String[] getIndexed() {
        return indexed;
    }

    public List<QueryGraphValueEntryHashKeyed> getKeys() {
        return key;
    }

    public String[] getStrictKeys() {
        return strictKeys;
    }
}

