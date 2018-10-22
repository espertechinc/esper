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
import com.espertech.esper.common.internal.support.SupportBeanComplexProps;
import com.espertech.esper.common.internal.supportunit.event.SupportEventBeanFactory;
import junit.framework.TestCase;

import java.lang.reflect.Method;

public class TestArrayMethodPropertyGetter extends TestCase {
    private ArrayMethodPropertyGetter getter;
    private ArrayMethodPropertyGetter getterOutOfBounds;
    private EventBean theEvent;
    private SupportBeanComplexProps bean;

    public void setUp() throws Exception {
        bean = SupportBeanComplexProps.makeDefaultBean();
        theEvent = SupportEventBeanFactory.createObject(bean);
        getter = makeGetter(0);
        getterOutOfBounds = makeGetter(Integer.MAX_VALUE);
    }

    public void testCtor() {
        try {
            makeGetter(-1);
            fail();
        } catch (IllegalArgumentException ex) {
            // expected
        }
    }

    public void testGet() {
        assertEquals(bean.getArrayProperty()[0], getter.get(theEvent));
        assertEquals(bean.getArrayProperty()[0], getter.get(theEvent, 0));

        assertNull(getterOutOfBounds.get(theEvent));
    }

    private ArrayMethodPropertyGetter makeGetter(int index) {
        Method method = null;
        try {
            method = SupportBeanComplexProps.class.getMethod("getArrayProperty", new Class[0]);
        } catch (NoSuchMethodException e) {
            fail();
        }
        return new ArrayMethodPropertyGetter(method, index, null, null);
    }
}
