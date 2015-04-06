/*
 * *************************************************************************************
 *  Copyright (C) 2006-2015 EsperTech, Inc. All rights reserved.                       *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 * *************************************************************************************
 */

package com.espertech.esper.event.arr;

import com.espertech.esper.client.EventType;
import com.espertech.esper.client.PropertyAccessException;
import com.espertech.esper.support.bean.SupportBeanComplexProps;
import com.espertech.esper.support.event.SupportEventAdapterService;
import junit.framework.TestCase;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.LinkedHashMap;
import java.util.Map;

public class TestObjectArrayEventBean extends TestCase
{
    private String[] testProps;
    private Object[] testTypes;
    private Object[] testValues;

    private EventType eventType;
    private ObjectArrayEventBean eventBean;

    private SupportBeanComplexProps supportBean = SupportBeanComplexProps.makeDefaultBean();

    public void setUp()
    {
        testProps = new String[] {"aString", "anInt", "myComplexBean"};
        testTypes = new Object[] {String.class, Integer.class, SupportBeanComplexProps.class};
        Map<String, Object> typeRep = new LinkedHashMap<String, Object>();
        for (int i = 0; i < testProps.length; i++) {
            typeRep.put(testProps[i], testTypes[i]);
        }

        testValues = new Object[] {"test", 10, supportBean};

        eventType = new ObjectArrayEventType(null, "", 1, SupportEventAdapterService.getService(), typeRep, null, null, null);
        eventBean = new ObjectArrayEventBean(testValues, eventType);
    }

    public void testGet()
    {
        assertEquals(eventType, eventBean.getEventType());
        assertEquals(testValues, eventBean.getUnderlying());

        assertEquals("test", eventBean.get("aString"));
        assertEquals(10, eventBean.get("anInt"));

        assertEquals("nestedValue", eventBean.get("myComplexBean.nested.nestedValue"));

        // test wrong property name
        try
        {
            eventBean.get("dummy");
            assertTrue(false);
        }
        catch (PropertyAccessException ex)
        {
            // Expected
            log.debug(".testGetter Expected exception, msg=" + ex.getMessage());
        }
    }

    private static final Log log = LogFactory.getLog(TestObjectArrayEventBean.class);
}
