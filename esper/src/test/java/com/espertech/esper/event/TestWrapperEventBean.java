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
package com.espertech.esper.event;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.core.support.SupportEventAdapterService;
import com.espertech.esper.supportunit.bean.SupportBeanCombinedProps;
import com.espertech.esper.supportunit.bean.SupportBeanSimple;
import junit.framework.TestCase;

import java.util.HashMap;
import java.util.Map;

public class TestWrapperEventBean extends TestCase {
    private EventBean eventBeanSimple;
    private EventBean eventBeanCombined;
    private Map<String, Object> properties;
    private EventType eventTypeSimple;
    private EventType eventTypeCombined;
    private EventAdapterService eventService;

    protected void setUp() {
        eventService = SupportEventAdapterService.getService();
        EventType underlyingEventTypeSimple = eventService.addBeanType("underlyingSimpleBean", SupportBeanSimple.class, true, true, true);
        EventType underlyingEventTypeCombined = eventService.addBeanType("underlyingCombinedBean", SupportBeanCombinedProps.class, true, true, true);

        Map<String, Object> typeMap = new HashMap<String, Object>();
        typeMap.put("string", String.class);
        typeMap.put("int", Integer.class);

        EventTypeMetadata meta = EventTypeMetadata.createWrapper("test", true, false, false);
        eventTypeSimple = new WrapperEventType(meta, "mytype", 1, underlyingEventTypeSimple, typeMap, eventService);
        eventTypeCombined = new WrapperEventType(meta, "mytype", 1, underlyingEventTypeCombined, typeMap, eventService);
        properties = new HashMap<String, Object>();
        properties.put("string", "xx");
        properties.put("int", 11);

        EventBean wrappedSimple = eventService.adapterForBean(new SupportBeanSimple("eventString", 0));
        eventBeanSimple = eventService.adapterForTypedWrapper(wrappedSimple, properties, eventTypeSimple);

        EventBean wrappedCombined = eventService.adapterForBean(SupportBeanCombinedProps.makeDefaultBean());
        eventBeanCombined = eventService.adapterForTypedWrapper(wrappedCombined, properties, eventTypeCombined);
    }

    public void testGetSimple() {
        assertEquals("eventString", eventBeanSimple.get("myString"));
        assertEquals(0, eventBeanSimple.get("myInt"));
        assertMap(eventBeanSimple);
    }

    public void testGetCombined() {
        assertEquals("0ma0", eventBeanCombined.get("indexed[0].mapped('0ma').value"));
        assertEquals("0ma1", eventBeanCombined.get("indexed[0].mapped('0mb').value"));
        assertEquals("1ma0", eventBeanCombined.get("indexed[1].mapped('1ma').value"));
        assertEquals("1ma1", eventBeanCombined.get("indexed[1].mapped('1mb').value"));

        assertEquals("0ma0", eventBeanCombined.get("array[0].mapped('0ma').value"));
        assertEquals("1ma1", eventBeanCombined.get("array[1].mapped('1mb').value"));

        assertMap(eventBeanCombined);
    }

    private void assertMap(EventBean eventBean) {
        assertEquals("xx", eventBean.get("string"));
        assertEquals(11, eventBean.get("int"));
    }
}
