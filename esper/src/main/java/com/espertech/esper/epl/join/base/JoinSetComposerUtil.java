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
package com.espertech.esper.epl.join.base;

import com.espertech.esper.epl.join.plan.TableLookupIndexReqKey;
import com.espertech.esper.epl.join.table.EventTable;

import java.util.Map;

public class JoinSetComposerUtil {
    private final static EventTable[] EMPTY = new EventTable[0];

    public static EventTable[][] toArray(Map<TableLookupIndexReqKey, EventTable>[] repositories) {
        return toArray(repositories, repositories.length);
    }

    public static EventTable[][] toArray(Map<TableLookupIndexReqKey, EventTable>[] repositories, int length) {
        if (repositories == null) {
            return getDefaultTablesArray(length);
        }
        EventTable[][] tables = new EventTable[repositories.length][];
        for (int i = 0; i < repositories.length; i++) {
            tables[i] = toArray(repositories[i]);
        }
        return tables;
    }

    private static EventTable[] toArray(Map<TableLookupIndexReqKey, EventTable> repository) {
        if (repository == null) {
            return EMPTY;
        }
        EventTable[] tables = new EventTable[repository.size()];
        int count = 0;
        for (Map.Entry<TableLookupIndexReqKey, EventTable> entries : repository.entrySet()) {
            tables[count] = entries.getValue();
            count++;
        }
        return tables;
    }

    private static EventTable[][] getDefaultTablesArray(int length) {
        EventTable[][] result = new EventTable[length][];
        for (int i = 0; i < result.length; i++) {
            result[i] = EMPTY;
        }
        return result;
    }
}