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
package com.espertech.esper.event.property;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventPropertyGetter;
import com.espertech.esper.core.support.SupportEventAdapterService;
import com.espertech.esper.event.bean.BeanEventType;
import com.espertech.esper.supportunit.bean.SupportBeanComplexProps;
import com.espertech.esper.supportunit.event.SupportEventBeanFactory;
import junit.framework.TestCase;

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
        EventPropertyGetter getter = prop.getGetter(eventType, SupportEventAdapterService.getService());
        assertEquals("simple", getter.get(theEvent));

        assertNull(invalidDummy.getGetter(eventType, SupportEventAdapterService.getService()));
        assertNull(invalidPropMap.getGetter(eventType, SupportEventAdapterService.getService()));
        assertNull(invalidPropIndexed.getGetter(eventType, SupportEventAdapterService.getService()));
    }

    public void testGetPropertyType() {
        assertEquals(String.class, prop.getPropertyType(eventType, SupportEventAdapterService.getService()));

        assertNull(invalidDummy.getGetter(eventType, SupportEventAdapterService.getService()));
        assertNull(invalidPropMap.getGetter(eventType, SupportEventAdapterService.getService()));
        assertNull(invalidPropIndexed.getGetter(eventType, SupportEventAdapterService.getService()));
    }
}
