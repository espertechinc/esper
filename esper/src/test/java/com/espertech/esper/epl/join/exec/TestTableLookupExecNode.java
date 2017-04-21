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
package com.espertech.esper.epl.join.exec;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.epl.join.exec.base.IndexedTableLookupStrategy;
import com.espertech.esper.epl.join.exec.base.TableLookupExecNode;
import com.espertech.esper.epl.join.table.PropertyIndexedEventTable;
import com.espertech.esper.epl.join.table.PropertyIndexedEventTableFactory;
import com.espertech.esper.supportunit.bean.SupportBean;
import com.espertech.esper.supportunit.bean.SupportMarketDataBean;
import com.espertech.esper.supportunit.event.SupportEventBeanFactory;
import com.espertech.esper.supportunit.event.SupportEventTypeFactory;
import junit.framework.TestCase;

import java.util.LinkedList;
import java.util.List;

public class TestTableLookupExecNode extends TestCase {
    private TableLookupExecNode exec;
    private PropertyIndexedEventTable index;

    public void setUp() {
        EventType eventTypeIndex = SupportEventTypeFactory.createBeanType(SupportBean.class);
        PropertyIndexedEventTableFactory factory = new PropertyIndexedEventTableFactory(0, eventTypeIndex, new String[]{"theString"}, false, null);
        index = (PropertyIndexedEventTable) factory.makeEventTables(null, null)[0];

        EventType eventTypeKeyGen = SupportEventTypeFactory.createBeanType(SupportMarketDataBean.class);

        exec = new TableLookupExecNode(1, new IndexedTableLookupStrategy(eventTypeKeyGen, new String[]{"symbol"}, index));
    }

    public void testFlow() {
        EventBean[] indexEvents = SupportEventBeanFactory.makeEvents(new String[]{"a1", "a2"});
        index.add(indexEvents, null);

        EventBean[] lookupEvents = SupportEventBeanFactory.makeMarketDataEvents(new String[]{"a2", "a3"});

        List<EventBean[]> result = new LinkedList<EventBean[]>();
        EventBean[] prefill = new EventBean[]{lookupEvents[0], null};
        exec.process(lookupEvents[0], prefill, result, null);

        // Test lookup found 1 row
        assertEquals(1, result.size());
        EventBean[] events = result.iterator().next();
        assertSame(indexEvents[1], events[1]);
        assertSame(lookupEvents[0], events[0]);

        // Test lookup found no rows
        result.clear();
        exec.process(lookupEvents[1], prefill, result, null);
        assertEquals(0, result.size());
    }
}
