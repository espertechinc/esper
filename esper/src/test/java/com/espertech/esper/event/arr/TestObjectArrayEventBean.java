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
package com.espertech.esper.event.arr;

import com.espertech.esper.client.EventType;
import com.espertech.esper.client.PropertyAccessException;
import com.espertech.esper.core.support.SupportEventAdapterService;
import com.espertech.esper.event.EventTypeMetadata;
import com.espertech.esper.supportunit.bean.SupportBeanComplexProps;
import junit.framework.TestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;

public class TestObjectArrayEventBean extends TestCase {
    private String[] testProps;
    private Object[] testTypes;
    private Object[] testValues;

    private EventType eventType;
    private ObjectArrayEventBean eventBean;

    private SupportBeanComplexProps supportBean = SupportBeanComplexProps.makeDefaultBean();

    public void setUp() {
        testProps = new String[]{"aString", "anInt", "myComplexBean"};
        testTypes = new Object[]{String.class, Integer.class, SupportBeanComplexProps.class};
        Map<String, Object> typeRep = new LinkedHashMap<String, Object>();
        for (int i = 0; i < testProps.length; i++) {
            typeRep.put(testProps[i], testTypes[i]);
        }

        testValues = new Object[]{"test", 10, supportBean};

        EventTypeMetadata metadata = EventTypeMetadata.createNonPojoApplicationType(EventTypeMetadata.ApplicationType.OBJECTARR, "testtype", true, true, true, false, false);
        eventType = new ObjectArrayEventType(metadata, "", 1, SupportEventAdapterService.getService(), typeRep, null, null, null);
        eventBean = new ObjectArrayEventBean(testValues, eventType);
    }

    public void testGet() {
        assertEquals(eventType, eventBean.getEventType());
        assertEquals(testValues, eventBean.getUnderlying());

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

    private static final Logger log = LoggerFactory.getLogger(TestObjectArrayEventBean.class);
}
