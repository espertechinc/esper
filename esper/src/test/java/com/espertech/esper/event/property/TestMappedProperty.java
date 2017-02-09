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
            EventPropertyGetter getter = mapped[i].getGetter(eventType, SupportEventAdapterService.getService());
            assertEquals(expected[i], getter.get(theEvent));
        }

        // try invalid case
        MappedProperty mpd = new MappedProperty("dummy", "dummy");
        assertNull(mpd.getGetter(eventType, SupportEventAdapterService.getService()));
    }

    public void testGetPropertyType() {
        Class[] expected = new Class[]{String.class, String.class};
        for (int i = 0; i < mapped.length; i++) {
            assertEquals(expected[i], mapped[i].getPropertyType(eventType, SupportEventAdapterService.getService()));
        }

        // try invalid case
        MappedProperty mpd = new MappedProperty("dummy", "dummy");
        assertNull(mpd.getPropertyType(eventType, SupportEventAdapterService.getService()));
        mpd = new MappedProperty("mapProperty", "dummy");
        assertEquals(String.class, mpd.getPropertyType(eventType, SupportEventAdapterService.getService()));
    }
}
