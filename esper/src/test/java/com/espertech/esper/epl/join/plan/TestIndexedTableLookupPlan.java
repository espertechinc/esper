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
import com.espertech.esper.epl.expression.core.ExprIdentNodeImpl;
import com.espertech.esper.epl.join.exec.base.IndexedTableLookupStrategy;
import com.espertech.esper.epl.join.exec.base.JoinExecTableLookupStrategy;
import com.espertech.esper.epl.join.table.EventTable;
import com.espertech.esper.epl.join.table.PropertyIndexedEventTable;
import com.espertech.esper.epl.join.table.PropertyIndexedEventTableFactory;
import com.espertech.esper.epl.virtualdw.VirtualDWView;
import com.espertech.esper.supportunit.bean.SupportBean;
import com.espertech.esper.supportunit.event.SupportEventTypeFactory;
import junit.framework.TestCase;

import java.util.*;

public class TestIndexedTableLookupPlan extends TestCase {
    private PropertyIndexedEventTable propertyMapEventIndex;
    private EventType[] types;

    public void setUp() {
        types = new EventType[]{SupportEventTypeFactory.createBeanType(SupportBean.class)};

        PropertyIndexedEventTableFactory factory = new PropertyIndexedEventTableFactory(1, types[0], new String[]{"intBoxed"}, false, null);
        propertyMapEventIndex = (PropertyIndexedEventTable) factory.makeEventTables(null, null)[0];
    }

    public void testLookup() {
        List<QueryGraphValueEntryHashKeyed> keys = new ArrayList<QueryGraphValueEntryHashKeyed>();
        keys.add(new QueryGraphValueEntryHashKeyedProp(new ExprIdentNodeImpl(types[0], "intBoxed", 0), "intBoxed"));
        IndexedTableLookupPlanMulti spec = new IndexedTableLookupPlanMulti(0, 1, new TableLookupIndexReqKey("idx1"), keys);

        Map<TableLookupIndexReqKey, EventTable>[] indexes = new Map[2];
        indexes[0] = new HashMap<TableLookupIndexReqKey, EventTable>();
        indexes[1] = new HashMap<TableLookupIndexReqKey, EventTable>();
        indexes[1].put(new TableLookupIndexReqKey("idx1"), propertyMapEventIndex);

        JoinExecTableLookupStrategy lookupStrategy = spec.makeStrategy("ABC", 1, null, indexes, types, new VirtualDWView[2]);

        IndexedTableLookupStrategy strategy = (IndexedTableLookupStrategy) lookupStrategy;
        assertEquals(types[0], strategy.getEventType());
        assertEquals(propertyMapEventIndex, strategy.getIndex());
        assertTrue(Arrays.equals(new String[]{"intBoxed"}, strategy.getProperties()));
    }
}
