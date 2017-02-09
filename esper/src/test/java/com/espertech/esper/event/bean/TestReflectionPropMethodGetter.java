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
package com.espertech.esper.event.bean;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.PropertyAccessException;
import com.espertech.esper.core.support.SupportEventAdapterService;
import com.espertech.esper.supportunit.bean.SupportBean;
import com.espertech.esper.supportunit.event.SupportEventBeanFactory;
import junit.framework.TestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

public class TestReflectionPropMethodGetter extends TestCase {
    EventBean unitTestBean;

    public void setUp() {
        SupportBean testEvent = new SupportBean();
        testEvent.setIntPrimitive(10);
        testEvent.setTheString("a");
        testEvent.setDoubleBoxed(null);

        unitTestBean = SupportEventBeanFactory.createObject(testEvent);
    }

    public void testGetter() throws Exception {
        ReflectionPropMethodGetter getter = makeGetter(SupportBean.class, "getIntPrimitive");
        assertEquals(10, getter.get(unitTestBean));

        getter = makeGetter(SupportBean.class, "getTheString");
        assertEquals("a", getter.get(unitTestBean));

        getter = makeGetter(SupportBean.class, "getDoubleBoxed");
        assertEquals(null, getter.get(unitTestBean));

        try {
            EventBean eventBean = SupportEventBeanFactory.createObject(new Object());
            getter.get(eventBean);
            assertTrue(false);
        } catch (PropertyAccessException ex) {
            // Expected
            log.debug(".testGetter Expected exception, msg=" + ex.getMessage());
        }
    }

    public void testPerformance() throws Exception {
        ReflectionPropMethodGetter getter = makeGetter(SupportBean.class, "getIntPrimitive");

        log.info(".testPerformance Starting test");

        for (int i = 0; i < 10; i++)   // Change to 1E8 for performance testing
        {
            int value = (Integer) getter.get(unitTestBean);
            assertEquals(10, value);
        }

        log.info(".testPerformance Done test");
    }

    private ReflectionPropMethodGetter makeGetter(Class clazz, String methodName) throws Exception {
        Method method = clazz.getMethod(methodName, new Class[]{});

        ReflectionPropMethodGetter getter = new ReflectionPropMethodGetter(method, SupportEventAdapterService.getService());

        return getter;
    }

    private static final Logger log = LoggerFactory.getLogger(TestReflectionPropMethodGetter.class);
}
