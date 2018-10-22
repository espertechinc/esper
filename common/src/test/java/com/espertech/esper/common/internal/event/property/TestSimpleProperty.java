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
package com.espertech.esper.common.internal.event.property;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventPropertyGetter;
import com.espertech.esper.common.internal.event.bean.core.BeanEventType;
import com.espertech.esper.common.internal.support.SupportBeanComplexProps;
import com.espertech.esper.common.internal.supportunit.event.SupportEventBeanFactory;
import junit.framework.TestCase;

import static com.espertech.esper.common.internal.event.core.EventBeanTypedEventFactoryCompileTime.INSTANCE;
import static com.espertech.esper.common.internal.supportunit.event.SupportEventTypeFactory.BEAN_EVENT_TYPE_FACTORY;

public class TestSimpleProperty extends TestCase {
    private SimpleProperty prop;
    private SimpleProperty invalidPropMap;
    private SimpleProperty invalidPropIndexed;
    private SimpleProperty invalidDummy;
    private EventBean theEvent;
    private BeanEventType eventType;

    public void setUp() {
        prop = new SimpleProperty("simpleProperty");
        invalidPropMap = new SimpleProperty("mapped");
        invalidPropIndexed = new SimpleProperty("indexed");
        invalidDummy = new SimpleProperty("dummy");
        theEvent = SupportEventBeanFactory.createObject(SupportBeanComplexProps.makeDefaultBean());
        eventType = (BeanEventType) theEvent.getEventType();
    }

    public void testGetGetter() {
        EventPropertyGetter getter = prop.getGetter(eventType, INSTANCE, BEAN_EVENT_TYPE_FACTORY);
        assertEquals("simple", getter.get(theEvent));

        assertNull(invalidDummy.getGetter(eventType, INSTANCE, BEAN_EVENT_TYPE_FACTORY));
        assertNull(invalidPropMap.getGetter(eventType, INSTANCE, BEAN_EVENT_TYPE_FACTORY));
        assertNull(invalidPropIndexed.getGetter(eventType, INSTANCE, BEAN_EVENT_TYPE_FACTORY));
    }

    public void testGetPropertyType() {
        assertEquals(String.class, prop.getPropertyType(eventType, BEAN_EVENT_TYPE_FACTORY));

        assertNull(invalidDummy.getGetter(eventType, INSTANCE, BEAN_EVENT_TYPE_FACTORY));
        assertNull(invalidPropMap.getGetter(eventType, INSTANCE, BEAN_EVENT_TYPE_FACTORY));
        assertNull(invalidPropIndexed.getGetter(eventType, INSTANCE, BEAN_EVENT_TYPE_FACTORY));
    }
}
