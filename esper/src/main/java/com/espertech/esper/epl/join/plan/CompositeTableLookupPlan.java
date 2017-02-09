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
import com.espertech.esper.epl.join.exec.base.CompositeTableLookupStrategy;
import com.espertech.esper.epl.join.exec.base.JoinExecTableLookupStrategy;
import com.espertech.esper.epl.join.table.EventTable;
import com.espertech.esper.epl.join.table.PropertyCompositeEventTable;

import java.util.List;

/**
 * Plan to perform an indexed table lookup.
 */
public class CompositeTableLookupPlan extends TableLookupPlan {
    private final List<QueryGraphValueEntryHashKeyed> hashKeys;
    private final List<QueryGraphValueEntryRange> rangeKeyPairs;

    public CompositeTableLookupPlan(int lookupStream, int indexedStream, TableLookupIndexReqKey indexNum, List<QueryGraphValueEntryHashKeyed> hashKeys, List<QueryGraphValueEntryRange> rangeKeyPairs) {
        super(lookupStream, indexedStream, new TableLookupIndexReqKey[]{indexNum});
        this.hashKeys = hashKeys;
        this.rangeKeyPairs = rangeKeyPairs;
    }

    public TableLookupKeyDesc getKeyDescriptor() {
        return new TableLookupKeyDesc(hashKeys, rangeKeyPairs);
    }

    public JoinExecTableLookupStrategy makeStrategyInternal(EventTable[] eventTable, EventType[] eventTypes) {
        PropertyCompositeEventTable index = (PropertyCompositeEventTable) eventTable[0];
        return new CompositeTableLookupStrategy(eventTypes[this.getLookupStream()], this.getLookupStream(), hashKeys, rangeKeyPairs, index);
    }

    public String toString() {
        return "CompositeTableLookupPlan " +
                super.toString() +
                " directKeys=" + QueryGraphValueEntryHashKeyed.toQueryPlan(hashKeys) +
                " rangeKeys=" + QueryGraphValueEntryRange.toQueryPlan(rangeKeyPairs);
    }
}
