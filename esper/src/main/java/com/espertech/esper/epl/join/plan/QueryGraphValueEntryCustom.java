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

import java.util.HashMap;
import java.util.Map;

public class QueryGraphValueEntryCustom implements QueryGraphValueEntry {
    private final Map<QueryGraphValueEntryCustomKey, QueryGraphValueEntryCustomOperation> operations = new HashMap<>();

    public Map<QueryGraphValueEntryCustomKey, QueryGraphValueEntryCustomOperation> getOperations() {
        return operations;
    }

    public void mergeInto(Map<QueryGraphValueEntryCustomKey, QueryGraphValueEntryCustomOperation> customIndexOps) {
        for (Map.Entry<QueryGraphValueEntryCustomKey, QueryGraphValueEntryCustomOperation> operation : operations.entrySet()) {
            QueryGraphValueEntryCustomOperation existing = customIndexOps.get(operation.getKey());
            if (existing == null) {
                customIndexOps.put(operation.getKey(), operation.getValue());
                continue;
            }
            existing.getPositionalExpressions().putAll(operation.getValue().getPositionalExpressions());
        }
    }
}

