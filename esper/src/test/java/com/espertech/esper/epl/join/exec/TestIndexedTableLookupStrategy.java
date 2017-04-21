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
import com.espertech.esper.epl.join.table.PropertyIndexedEventTable;
import com.espertech.esper.epl.join.table.PropertyIndexedEventTableFactory;
import com.espertech.esper.supportunit.bean.SupportBean;
import com.espertech.esper.supportunit.event.SupportEventBeanFactory;
import com.espertech.esper.supportunit.event.SupportEventTypeFactory;
import junit.framework.TestCase;

import java.util.Set;

public class TestIndexedTableLookupStrategy extends TestCase {
    private EventType eventType;
    private IndexedTableLookupStrategy lookupStrategy;
    private PropertyIndexedEventTable propertyMapEventIndex;

    public void setUp() {
        eventType = SupportEventTypeFactory.createBeanType(SupportBean.class);

        PropertyIndexedEventTableFactory factory = new PropertyIndexedEventTableFactory(0, eventType, new String[]{"theString", "intPrimitive"}, false, null);
        propertyMapEventIndex = (PropertyIndexedEventTable) factory.makeEventTables(null, null)[0];
        lookupStrategy = new IndexedTableLookupStrategy(eventType, new String[]{"theString", "intPrimitive"}, propertyMapEventIndex);

        propertyMapEventIndex.add(new EventBean[]{SupportEventBeanFactory.createObject(new SupportBean("a", 1))}, null);
    }

    public void testLookup() {
        Set<EventBean> events = lookupStrategy.lookup(SupportEventBeanFactory.createObject(new SupportBean("a", 1)), null, null);

        assertEquals(1, events.size());
    }

    public void testInvalid() {
        try {
            new IndexedTableLookupStrategy(eventType, new String[]{"theString", "xxx"}, propertyMapEventIndex);
            fail();
        } catch (IllegalArgumentException ex) {
            // expected
        }
    }
}
