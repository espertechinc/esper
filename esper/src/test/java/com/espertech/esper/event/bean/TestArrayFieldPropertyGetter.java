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

import java.lang.reflect.Field;

public class TestArrayFieldPropertyGetter extends TestCase {
    private ArrayFieldPropertyGetter getter;
    private ArrayFieldPropertyGetter getterOutOfBounds;
    private EventBean theEvent;
    private SupportLegacyBean bean;

    public void setUp() throws Exception {
        bean = new SupportLegacyBean(new String[]{"a", "b"});
        theEvent = SupportEventBeanFactory.createObject(bean);

        getter = makeGetter(0);
        getterOutOfBounds = makeGetter(Integer.MAX_VALUE);
    }

    public void testCtor() throws Exception {
        try {
            makeGetter(-1);
            fail();
        } catch (IllegalArgumentException ex) {
            // expected
        }
    }

    public void testGet() {
        assertEquals(bean.fieldStringArray[0], getter.get(theEvent));
        assertEquals(bean.fieldStringArray[0], getter.get(theEvent, 0));

        assertNull(getterOutOfBounds.get(theEvent));

        try {
            getter.get(SupportEventBeanFactory.createObject(""));
            fail();
        } catch (PropertyAccessException ex) {
            // expected
        }
    }

    private ArrayFieldPropertyGetter makeGetter(int index) throws Exception {
        Field field = SupportLegacyBean.class.getField("fieldStringArray");
        return new ArrayFieldPropertyGetter(field, index, SupportEventAdapterService.getService());
    }
}
