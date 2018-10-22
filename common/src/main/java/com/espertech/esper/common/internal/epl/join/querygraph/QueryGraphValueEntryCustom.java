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

import java.util.Map;

public class QueryGraphValueEntryCustom implements QueryGraphValueEntry {
    private Map<QueryGraphValueEntryCustomKey, QueryGraphValueEntryCustomOperation> operations;

    public void setOperations(Map<QueryGraphValueEntryCustomKey, QueryGraphValueEntryCustomOperation> operations) {
        this.operations = operations;
    }

    public Map<QueryGraphValueEntryCustomKey, QueryGraphValueEntryCustomOperation> getOperations() {
        return operations;
    }
}

