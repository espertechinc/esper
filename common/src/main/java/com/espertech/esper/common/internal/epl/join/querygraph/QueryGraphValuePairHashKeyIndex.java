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

public class QueryGraphValuePairHashKeyIndex {
    private final String[] indexed;
    private final List<QueryGraphValueEntryHashKeyed> key;

    public QueryGraphValuePairHashKeyIndex(String[] indexed, List<QueryGraphValueEntryHashKeyed> key) {
        this.indexed = indexed;
        this.key = key;
    }

    public String[] getIndexed() {
        return indexed;
    }

    public List<QueryGraphValueEntryHashKeyed> getKeys() {
        return key;
    }
}

