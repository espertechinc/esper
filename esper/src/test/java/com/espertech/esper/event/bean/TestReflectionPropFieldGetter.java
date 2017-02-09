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
import com.espertech.esper.supportunit.bean.SupportLegacyBean;
import com.espertech.esper.supportunit.event.SupportEventBeanFactory;
import junit.framework.TestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;

public class TestReflectionPropFieldGetter extends TestCase {
    EventBean unitTestBean;

    public void setUp() {
        SupportLegacyBean testEvent = new SupportLegacyBean("a");
        unitTestBean = SupportEventBeanFactory.createObject(testEvent);
    }

    public void testGetter() throws Exception {
        ReflectionPropFieldGetter getter = makeGetter(SupportLegacyBean.class, "fieldLegacyVal");
        assertEquals("a", getter.get(unitTestBean));

        try {
            EventBean eventBean = SupportEventBeanFactory.createObject(new Object());
            getter.get(eventBean);
            assertTrue(false);
        } catch (PropertyAccessException ex) {
            // Expected
            log.debug(".testGetter Expected exception, msg=" + ex.getMessage());
        }
    }

    private ReflectionPropFieldGetter makeGetter(Class clazz, String fieldName) throws Exception {
        Field field = clazz.getField(fieldName);
        ReflectionPropFieldGetter getter = new ReflectionPropFieldGetter(field, SupportEventAdapterService.getService());
        return getter;
    }

    private static final Logger log = LoggerFactory.getLogger(TestReflectionPropFieldGetter.class);
}
