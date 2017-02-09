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
import com.espertech.esper.supportunit.bean.SupportBeanComplexProps;
import com.espertech.esper.supportunit.event.SupportEventBeanFactory;
import junit.framework.TestCase;
import net.sf.cglib.reflect.FastClass;
import net.sf.cglib.reflect.FastMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

public class TestCGLibPropertyGetter extends TestCase {
    EventBean unitTestBean;

    public void setUp() {
        SupportBean testEvent = new SupportBean();
        testEvent.setIntPrimitive(10);
        testEvent.setTheString("a");
        testEvent.setDoubleBoxed(null);

        unitTestBean = SupportEventBeanFactory.createObject(testEvent);
    }

    public void testGetter() throws Exception {
        CGLibPropertyGetter getter = makeGetter(SupportBean.class, "getIntPrimitive");
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
        CGLibPropertyGetter getter = makeGetter(SupportBean.class, "getIntPrimitive");

        log.info(".testPerformance Starting test");

        for (int i = 0; i < 10; i++)   // Change to 1E8 for performance testing
        {
            int value = (Integer) getter.get(unitTestBean);
            assertEquals(10, value);
        }

        log.info(".testPerformance Done test");
    }

    private CGLibPropertyGetter makeGetter(Class clazz, String methodName) throws Exception {
        FastClass fastClass = FastClass.create(Thread.currentThread().getContextClassLoader(), clazz);
        Method method = clazz.getMethod(methodName, new Class[]{});
        FastMethod fastMethod = fastClass.getMethod(method);

        CGLibPropertyGetter getter = new CGLibPropertyGetter(method, fastMethod, SupportEventAdapterService.getService());

        return getter;
    }

    public void testGetterSpecial() throws Exception {
        Class clazz = SupportBeanComplexProps.class;
        FastClass fastClass = FastClass.create(Thread.currentThread().getContextClassLoader(), clazz);

        // set up bean
        SupportBeanComplexProps bean = SupportBeanComplexProps.makeDefaultBean();

        // try mapped property
        Method method = clazz.getMethod("getMapped", new Class[]{String.class});
        FastMethod fastMethod = fastClass.getMethod(method);
        Object result = fastMethod.invoke(bean, new Object[]{"keyOne"});
        assertEquals("valueOne", result);
        result = fastMethod.invoke(bean, new Object[]{"keyTwo"});
        assertEquals("valueTwo", result);

        // try index property
        method = clazz.getMethod("getIndexed", new Class[]{int.class});
        fastMethod = fastClass.getMethod(method);
        result = fastMethod.invoke(bean, new Object[]{0});
        assertEquals(1, result);
        result = fastMethod.invoke(bean, new Object[]{1});
        assertEquals(2, result);

        // try nested property
        method = clazz.getMethod("getNested", new Class[]{});
        fastMethod = fastClass.getMethod(method);
        SupportBeanComplexProps.SupportBeanSpecialGetterNested nested = (SupportBeanComplexProps.SupportBeanSpecialGetterNested) fastMethod.invoke(bean, new Object[]{});

        Class nestedClazz = SupportBeanComplexProps.SupportBeanSpecialGetterNested.class;
        Method methodNested = nestedClazz.getMethod("getNestedValue", new Class[]{});
        FastClass fastClassNested = FastClass.create(Thread.currentThread().getContextClassLoader(), nestedClazz);
        fastClassNested.getMethod(methodNested);
    }

    private static final Logger log = LoggerFactory.getLogger(TestCGLibPropertyGetter.class);
}
