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
import com.espertech.esper.epl.join.exec.base.FullTableScanLookupStrategy;
import com.espertech.esper.epl.join.exec.base.TableOuterLookupExecNode;
import com.espertech.esper.epl.join.table.UnindexedEventTable;
import com.espertech.esper.epl.join.table.UnindexedEventTableImpl;
import com.espertech.esper.supportunit.event.SupportEventBeanFactory;
import junit.framework.TestCase;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class TestTableOuterLookupExecNode extends TestCase {
    private TableOuterLookupExecNode exec;
    private UnindexedEventTable index;

    public void setUp() {
        index = new UnindexedEventTableImpl(0);
        exec = new TableOuterLookupExecNode(1, new FullTableScanLookupStrategy(index));
    }

    public void testFlow() {
        EventBean[] lookupEvents = SupportEventBeanFactory.makeMarketDataEvents(new String[]{"a2"});
        List<EventBean[]> result = new LinkedList<EventBean[]>();
        EventBean[] prefill = new EventBean[]{lookupEvents[0], null};

        // Test lookup on empty index, expect 1 row
        exec.process(lookupEvents[0], prefill, result, null);
        assertEquals(1, result.size());
        EventBean[] events = result.iterator().next();
        assertNull(events[1]);
        assertSame(lookupEvents[0], events[0]);
        result.clear();

        // Test lookup on filled index, expect row2
        EventBean[] indexEvents = SupportEventBeanFactory.makeEvents(new String[]{"a1", "a2"});
        index.add(indexEvents, null);
        exec.process(lookupEvents[0], prefill, result, null);
        assertEquals(2, result.size());

        Iterator<EventBean[]> it = result.iterator();

        events = it.next();
        assertSame(lookupEvents[0], events[0]);
        assertTrue((indexEvents[0] == events[1]) || (indexEvents[1] == events[1]));

        events = it.next();
        assertSame(lookupEvents[0], events[0]);
        assertTrue((indexEvents[0] == events[1]) || (indexEvents[1] == events[1]));
    }
}
