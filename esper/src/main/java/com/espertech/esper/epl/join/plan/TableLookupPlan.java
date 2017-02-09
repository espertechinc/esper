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

import com.espertech.esper.client.EventType;
import com.espertech.esper.epl.join.exec.base.JoinExecTableLookupStrategy;
import com.espertech.esper.epl.join.table.EventTable;
import com.espertech.esper.epl.virtualdw.VirtualDWView;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Map;

/**
 * Abstract specification on how to perform a table lookup.
 */
public abstract class TableLookupPlan {
    private int lookupStream;
    private int indexedStream;
    private TableLookupIndexReqKey[] indexNum;

    public abstract JoinExecTableLookupStrategy makeStrategyInternal(EventTable[] eventTable, EventType[] eventTypes);

    public abstract TableLookupKeyDesc getKeyDescriptor();

    public final JoinExecTableLookupStrategy makeStrategy(String statementName, int statementId, Annotation[] accessedByStmtAnnotations, Map<TableLookupIndexReqKey, EventTable>[] indexesPerStream, EventType[] eventTypes, VirtualDWView[] viewExternals) {
        EventTable[] eventTables = new EventTable[indexNum.length];
        for (int i = 0; i < indexNum.length; i++) {
            eventTables[i] = indexesPerStream[this.getIndexedStream()].get(getIndexNum()[i]);
        }
        if (viewExternals[indexedStream] != null) {
            return viewExternals[indexedStream].getJoinLookupStrategy(statementName, statementId, accessedByStmtAnnotations, eventTables, getKeyDescriptor(), lookupStream);
        }
        return makeStrategyInternal(eventTables, eventTypes);
    }

    /**
     * Ctor.
     *
     * @param lookupStream  - stream number of stream that supplies event to be used to look up
     * @param indexedStream - - stream number of stream that is being access via index/table
     * @param indexNum      - index to use for lookup
     */
    protected TableLookupPlan(int lookupStream, int indexedStream, TableLookupIndexReqKey[] indexNum) {
        this.lookupStream = lookupStream;
        this.indexedStream = indexedStream;
        this.indexNum = indexNum;
    }

    /**
     * Returns the lookup stream.
     *
     * @return lookup stream
     */
    public int getLookupStream() {
        return lookupStream;
    }

    /**
     * Returns indexed stream.
     *
     * @return indexed stream
     */
    public int getIndexedStream() {
        return indexedStream;
    }

    /**
     * Returns index number to use for looking up in.
     *
     * @return index number
     */
    public TableLookupIndexReqKey[] getIndexNum() {
        return indexNum;
    }

    public String toString() {
        return "lookupStream=" + lookupStream +
                " indexedStream=" + indexedStream +
                " indexNum=" + Arrays.toString(indexNum);
    }
}
