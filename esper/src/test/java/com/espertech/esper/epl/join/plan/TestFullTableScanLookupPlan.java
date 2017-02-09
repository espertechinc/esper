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

import com.espertech.esper.epl.join.exec.base.FullTableScanLookupStrategy;
import com.espertech.esper.epl.join.exec.base.JoinExecTableLookupStrategy;
import com.espertech.esper.epl.join.table.EventTable;
import com.espertech.esper.epl.join.table.UnindexedEventTable;
import com.espertech.esper.epl.join.table.UnindexedEventTableImpl;
import com.espertech.esper.epl.virtualdw.VirtualDWView;
import junit.framework.TestCase;

import java.util.HashMap;
import java.util.Map;

public class TestFullTableScanLookupPlan extends TestCase {
    private UnindexedEventTable unindexedEventIndex;

    public void setUp() {
        unindexedEventIndex = new UnindexedEventTableImpl(0);
    }

    public void testLookup() {
        FullTableScanLookupPlan spec = new FullTableScanLookupPlan(0, 1, new TableLookupIndexReqKey("idx2"));

        Map<TableLookupIndexReqKey, EventTable>[] indexes = new Map[2];
        indexes[0] = new HashMap<TableLookupIndexReqKey, EventTable>();
        indexes[1] = new HashMap<TableLookupIndexReqKey, EventTable>();
        indexes[1].put(new TableLookupIndexReqKey("idx2"), unindexedEventIndex);

        JoinExecTableLookupStrategy lookupStrategy = spec.makeStrategy("ABC", 1, null, indexes, null, new VirtualDWView[2]);

        FullTableScanLookupStrategy strategy = (FullTableScanLookupStrategy) lookupStrategy;
        assertEquals(unindexedEventIndex, strategy.getEventIndex());
    }
}
