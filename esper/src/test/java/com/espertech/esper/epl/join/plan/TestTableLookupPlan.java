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

import com.espertech.esper.epl.join.exec.base.ExecNode;
import com.espertech.esper.epl.join.exec.base.FullTableScanLookupStrategy;
import com.espertech.esper.epl.join.exec.base.TableLookupExecNode;
import com.espertech.esper.epl.join.table.EventTable;
import com.espertech.esper.epl.join.table.UnindexedEventTableImpl;
import com.espertech.esper.epl.virtualdw.VirtualDWView;
import com.espertech.esper.view.Viewable;
import junit.framework.TestCase;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

public class TestTableLookupPlan extends TestCase {
    public void testMakeExec() {
        Map<TableLookupIndexReqKey, EventTable>[] indexesPerStream = new Map[2];
        indexesPerStream[1] = new HashMap<TableLookupIndexReqKey, EventTable>();
        indexesPerStream[1].put(new TableLookupIndexReqKey("idx1"), new UnindexedEventTableImpl(0));

        TableLookupNode spec = new TableLookupNode(new FullTableScanLookupPlan(0, 1, new TableLookupIndexReqKey("idx1")));
        ExecNode execNode = spec.makeExec("ABC", 1, null, indexesPerStream, null, new Viewable[2], null, new VirtualDWView[2], new ReentrantLock[2]);
        TableLookupExecNode exec = (TableLookupExecNode) execNode;

        assertSame(indexesPerStream[1].get(new TableLookupIndexReqKey("idx1")), ((FullTableScanLookupStrategy) exec.getLookupStrategy()).getEventIndex());
        assertEquals(1, exec.getIndexedStream());
    }
}
