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
package com.espertech.esper.common.internal.event.bean.getter;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventPropertyGetter;
import com.espertech.esper.common.internal.event.bean.service.BeanEventTypeFactory;
import com.espertech.esper.common.internal.supportunit.bean.SupportBeanCombinedProps;
import com.espertech.esper.common.internal.supportunit.event.SupportEventBeanFactory;
import junit.framework.TestCase;

import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class TestNestedPropertyGetter extends TestCase {
    private NestedPropertyGetter getter;
    private NestedPropertyGetter getterNull;
    private EventBean theEvent;
    private SupportBeanCombinedProps bean;
    private BeanEventTypeFactory beanEventTypeFactory;

    public void setUp() throws Exception {
        bean = SupportBeanCombinedProps.makeDefaultBean();
        theEvent = SupportEventBeanFactory.createObject(bean);

        List<EventPropertyGetter> getters = new LinkedList<EventPropertyGetter>();
        getters.add(makeGetterOne(0));
        getters.add(makeGetterTwo("0ma"));
        getter = new NestedPropertyGetter(getters, null, Map.class, null, null);

        getters = new LinkedList<EventPropertyGetter>();
        getters.add(makeGetterOne(2));
        getters.add(makeGetterTwo("0ma"));
        getterNull = new NestedPropertyGetter(getters, null, Map.class, null, null);
    }

    public void testGet() {
        assertEquals(bean.getIndexed(0).getMapped("0ma"), getter.get(theEvent));

        // test null value returned
        assertNull(getterNull.get(theEvent));
    }

    private KeyedMethodPropertyGetter makeGetterOne(int index) {
        Method methodOne = null;
        try {
            methodOne = SupportBeanCombinedProps.class.getMethod("getIndexed", new Class[]{int.class});
        } catch (NoSuchMethodException e) {
            fail();
        }
        return new KeyedMethodPropertyGetter(methodOne, index, null, null);
    }

    private KeyedMethodPropertyGetter makeGetterTwo(String key) {
        Method methodTwo = null;
        try {
            methodTwo = SupportBeanCombinedProps.NestedLevOne.class.getMethod("getMapped", new Class[]{String.class});
        } catch (NoSuchMethodException e) {
            fail();
        }
        return new KeyedMethodPropertyGetter(methodTwo, key, null, null);
    }
}
