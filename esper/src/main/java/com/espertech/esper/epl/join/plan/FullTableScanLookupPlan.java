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
import com.espertech.esper.epl.join.exec.base.FullTableScanLookupStrategy;
import com.espertech.esper.epl.join.exec.base.JoinExecTableLookupStrategy;
import com.espertech.esper.epl.join.table.EventTable;
import com.espertech.esper.epl.join.table.UnindexedEventTable;

import java.util.Collections;

/**
 * Plan for a full table scan.
 */
public class FullTableScanLookupPlan extends TableLookupPlan {
    /**
     * Ctor.
     *
     * @param lookupStream  - stream that generates event to look up for
     * @param indexedStream - stream to full table scan
     * @param indexNum      - index number for the table containing the full unindexed contents
     */
    public FullTableScanLookupPlan(int lookupStream, int indexedStream, TableLookupIndexReqKey indexNum) {
        super(lookupStream, indexedStream, new TableLookupIndexReqKey[]{indexNum});
    }

    public TableLookupKeyDesc getKeyDescriptor() {
        return new TableLookupKeyDesc(Collections.<QueryGraphValueEntryHashKeyed>emptyList(), Collections.<QueryGraphValueEntryRange>emptyList());
    }

    public JoinExecTableLookupStrategy makeStrategyInternal(EventTable[] eventTable, EventType[] eventTypes) {
        UnindexedEventTable index = (UnindexedEventTable) eventTable[0];
        return new FullTableScanLookupStrategy(index);
    }

    public String toString() {
        return "FullTableScanLookupPlan " +
                super.toString();
    }

}
