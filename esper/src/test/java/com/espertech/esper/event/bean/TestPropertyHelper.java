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
import com.espertech.esper.client.EventPropertyGetter;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.core.support.SupportEventAdapterService;
import com.espertech.esper.supportunit.bean.SupportBeanPropertyNames;
import com.espertech.esper.supportunit.event.SupportEventBeanFactory;
import junit.framework.TestCase;
import net.sf.cglib.reflect.FastClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class TestPropertyHelper extends TestCase {
    public void testAddMappedProperties() {
        List<InternalEventPropDescriptor> result = new LinkedList<InternalEventPropDescriptor>();
        PropertyHelper.addMappedProperties(SupportBeanPropertyNames.class, result);
        assertEquals(6, result.size());

        List<String> propertyNames = new ArrayList<String>();
        for (InternalEventPropDescriptor desc : result) {
            log.debug("desc=" + desc.getPropertyName());
            propertyNames.add(desc.getPropertyName());
        }
        EPAssertionUtil.assertEqualsAnyOrder(new Object[]{"a", "AB", "ABC", "ab", "abc", "fooBah"}, propertyNames.toArray());
    }

    public void testAddIntrospectProperties() throws Exception {
        List<InternalEventPropDescriptor> result = new LinkedList<InternalEventPropDescriptor>();
        PropertyHelper.addIntrospectProperties(SupportBeanPropertyNames.class, result);

        for (InternalEventPropDescriptor desc : result) {
            log.debug("desc=" + desc.getPropertyName());
        }

        assertEquals(9, result.size()); // for "class" is also in there
        assertEquals("indexed", result.get(8).getPropertyName());
        assertNotNull(result.get(8).getReadMethod());
    }

    public void testRemoveDuplicateProperties() {
        List<InternalEventPropDescriptor> result = new LinkedList<InternalEventPropDescriptor>();
        result.add(new InternalEventPropDescriptor("x", (Method) null, null));
        result.add(new InternalEventPropDescriptor("x", (Method) null, null));
        result.add(new InternalEventPropDescriptor("y", (Method) null, null));

        PropertyHelper.removeDuplicateProperties(result);

        assertEquals(2, result.size());
        assertEquals("x", result.get(0).getPropertyName());
        assertEquals("y", result.get(1).getPropertyName());
    }

    public void testRemoveJavaProperties() {
        List<InternalEventPropDescriptor> result = new LinkedList<InternalEventPropDescriptor>();
        result.add(new InternalEventPropDescriptor("x", (Method) null, null));
        result.add(new InternalEventPropDescriptor("class", (Method) null, null));
        result.add(new InternalEventPropDescriptor("hashCode", (Method) null, null));
        result.add(new InternalEventPropDescriptor("toString", (Method) null, null));
        result.add(new InternalEventPropDescriptor("getClass", (Method) null, null));

        PropertyHelper.removeJavaProperties(result);

        assertEquals(1, result.size());
        assertEquals("x", result.get(0).getPropertyName());
    }

    public void testIntrospect() {
        PropertyDescriptor desc[] = PropertyHelper.introspect(SupportBeanPropertyNames.class);
        assertTrue(desc.length > 5);
    }

    public void testGetGetter() throws Exception {
        FastClass fastClass = FastClass.create(Thread.currentThread().getContextClassLoader(), SupportBeanPropertyNames.class);
        EventBean bean = SupportEventBeanFactory.createObject(new SupportBeanPropertyNames());
        Method method = SupportBeanPropertyNames.class.getMethod("getA", new Class[0]);
        EventPropertyGetter getter = PropertyHelper.getGetter(method, fastClass, SupportEventAdapterService.getService());
        assertEquals("", getter.get(bean));
    }

    private static final Logger log = LoggerFactory.getLogger(TestPropertyHelper.class);
}
