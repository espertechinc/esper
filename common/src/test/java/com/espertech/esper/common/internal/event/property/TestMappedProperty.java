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
import com.espertech.esper.common.internal.event.core.EventBeanTypedEventFactoryCompileTime;
import com.espertech.esper.common.internal.support.SupportBeanComplexProps;
import com.espertech.esper.common.internal.supportunit.event.SupportEventBeanFactory;
import com.espertech.esper.common.internal.supportunit.event.SupportEventTypeFactory;
import junit.framework.TestCase;

public class TestMappedProperty extends TestCase {
    private MappedProperty[] mapped;
    private EventBean theEvent;
    private BeanEventType eventType;

    public void setUp() {
        mapped = new MappedProperty[2];
        mapped[0] = new MappedProperty("mapped", "keyOne");
        mapped[1] = new MappedProperty("mapped", "keyTwo");

        theEvent = SupportEventBeanFactory.createObject(SupportBeanComplexProps.makeDefaultBean());
        eventType = (BeanEventType) theEvent.getEventType();
    }

    public void testGetGetter() {
        Object[] expected = new String[]{"valueOne", "valueTwo"};
        for (int i = 0; i < mapped.length; i++) {
            EventPropertyGetter getter = mapped[i].getGetter(eventType, EventBeanTypedEventFactoryCompileTime.INSTANCE, SupportEventTypeFactory.BEAN_EVENT_TYPE_FACTORY);
            assertEquals(expected[i], getter.get(theEvent));
        }

        // try invalid case
        MappedProperty mpd = new MappedProperty("dummy", "dummy");
        assertNull(mpd.getGetter(eventType, EventBeanTypedEventFactoryCompileTime.INSTANCE, SupportEventTypeFactory.BEAN_EVENT_TYPE_FACTORY));
    }

    public void testGetPropertyType() {
        Class[] expected = new Class[]{String.class, String.class};
        for (int i = 0; i < mapped.length; i++) {
            assertEquals(expected[i], mapped[i].getPropertyType(eventType, SupportEventTypeFactory.BEAN_EVENT_TYPE_FACTORY));
        }

        // try invalid case
        MappedProperty mpd = new MappedProperty("dummy", "dummy");
        assertNull(mpd.getPropertyType(eventType, SupportEventTypeFactory.BEAN_EVENT_TYPE_FACTORY));
        mpd = new MappedProperty("mapProperty", "dummy");
        assertEquals(String.class, mpd.getPropertyType(eventType, SupportEventTypeFactory.BEAN_EVENT_TYPE_FACTORY));
    }
}
