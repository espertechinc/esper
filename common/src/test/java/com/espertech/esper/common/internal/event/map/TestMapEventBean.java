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
package com.espertech.esper.common.internal.event.map;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.PropertyAccessException;
import com.espertech.esper.common.client.meta.EventTypeApplicationType;
import com.espertech.esper.common.client.meta.EventTypeIdPair;
import com.espertech.esper.common.client.meta.EventTypeMetadata;
import com.espertech.esper.common.client.meta.EventTypeTypeClass;
import com.espertech.esper.common.client.util.EventTypeBusModifier;
import com.espertech.esper.common.client.util.NameAccessModifier;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportBeanComplexProps;
import com.espertech.esper.common.internal.support.SupportBean_A;
import com.espertech.esper.common.internal.supportunit.event.SupportEventTypeFactory;
import junit.framework.TestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class TestMapEventBean extends TestCase {
    private Map<String, Object> testTypesMap;
    private Map<String, Object> testValuesMap;

    private EventType eventType;
    private MapEventBean eventBean;

    private SupportBeanComplexProps supportBean = SupportBeanComplexProps.makeDefaultBean();

    public void setUp() {
        testTypesMap = new HashMap<String, Object>();
        testTypesMap.put("aString", String.class);
        testTypesMap.put("anInt", Integer.class);
        testTypesMap.put("myComplexBean", SupportBeanComplexProps.class);

        testValuesMap = new HashMap<String, Object>();
        testValuesMap.put("aString", "test");
        testValuesMap.put("anInt", 10);
        testValuesMap.put("myComplexBean", supportBean);

        EventTypeMetadata metadata = new EventTypeMetadata("MyType", null, EventTypeTypeClass.STREAM, EventTypeApplicationType.MAP, NameAccessModifier.PROTECTED, EventTypeBusModifier.NONBUS, false, EventTypeIdPair.unassigned());
        eventType = new MapEventType(metadata, testTypesMap, null, null, null, null, SupportEventTypeFactory.BEAN_EVENT_TYPE_FACTORY);
        eventBean = new MapEventBean(testValuesMap, eventType);
    }

    public void testGet() {
        assertEquals(eventType, eventBean.getEventType());
        assertEquals(testValuesMap, eventBean.getUnderlying());

        assertEquals("test", eventBean.get("aString"));
        assertEquals(10, eventBean.get("anInt"));

        assertEquals("nestedValue", eventBean.get("myComplexBean.nested.nestedValue"));

        // test wrong property name
        try {
            eventBean.get("dummy");
            assertTrue(false);
        } catch (PropertyAccessException ex) {
            // Expected
            log.debug(".testGetter Expected exception, msg=" + ex.getMessage());
        }
    }

    public void testCreateUnderlying() {
        SupportBean beanOne = new SupportBean();
        SupportBean_A beanTwo = new SupportBean_A("a");

        // Set up event type
        testTypesMap.clear();
        testTypesMap.put("a", SupportBean.class);
        testTypesMap.put("b", SupportBean_A.class);
        EventTypeMetadata metadata = new EventTypeMetadata("MyType", null, EventTypeTypeClass.STREAM, EventTypeApplicationType.MAP, NameAccessModifier.PROTECTED, EventTypeBusModifier.NONBUS, false, EventTypeIdPair.unassigned());
        EventType eventType = new MapEventType(metadata, testTypesMap, null, null, null, null, SupportEventTypeFactory.BEAN_EVENT_TYPE_FACTORY);

        Map<String, Object> events = new HashMap<String, Object>();
        events.put("a", beanOne);
        events.put("b", beanTwo);

        MapEventBean theEvent = new MapEventBean(events, eventType);
        assertSame(theEvent.get("a"), beanOne);
        assertSame(theEvent.get("b"), beanTwo);
    }

    private static final Logger log = LoggerFactory.getLogger(TestMapEventBean.class);
}
