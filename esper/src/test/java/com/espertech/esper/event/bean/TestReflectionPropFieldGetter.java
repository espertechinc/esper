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

package com.espertech.esper.event.bean;

import junit.framework.TestCase;
import com.espertech.esper.support.bean.SupportLegacyBean;
import com.espertech.esper.support.event.SupportEventBeanFactory;
import com.espertech.esper.core.support.SupportEventAdapterService;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.PropertyAccessException;

import java.lang.reflect.Field;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class TestReflectionPropFieldGetter extends TestCase
{
    EventBean unitTestBean;

    public void setUp()
    {
        SupportLegacyBean testEvent = new SupportLegacyBean("a");
        unitTestBean = SupportEventBeanFactory.createObject(testEvent);
    }

    public void testGetter() throws Exception
    {
        ReflectionPropFieldGetter getter = makeGetter(SupportLegacyBean.class, "fieldLegacyVal");
        assertEquals("a", getter.get(unitTestBean));

        try
        {
            EventBean eventBean = SupportEventBeanFactory.createObject(new Object());
            getter.get(eventBean);
            assertTrue(false);
        }
        catch (PropertyAccessException ex)
        {
            // Expected
            log.debug(".testGetter Expected exception, msg=" + ex.getMessage());
        }
    }

    private ReflectionPropFieldGetter makeGetter(Class clazz, String fieldName) throws Exception
    {
        Field field = clazz.getField(fieldName);
        ReflectionPropFieldGetter getter = new ReflectionPropFieldGetter(field, SupportEventAdapterService.getService());
        return getter;
    }

    private static final Log log = LogFactory.getLog(TestReflectionPropFieldGetter.class);
}
