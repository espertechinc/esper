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
package com.espertech.esper.core.service;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.UpdateListener;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportunit.event.SupportEventBeanFactory;
import junit.framework.TestCase;

import java.util.HashSet;
import java.util.Set;

public class TestPatternListenerDispatch extends TestCase {
    private PatternListenerDispatch dispatch;

    private EventBean eventOne = SupportEventBeanFactory.createObject("a");
    private EventBean eventTwo = SupportEventBeanFactory.createObject("b");

    private SupportUpdateListener listener = new SupportUpdateListener();

    public void setUp() {
        Set<UpdateListener> listeners = new HashSet<UpdateListener>();
        listeners.add(listener);
        dispatch = new PatternListenerDispatch(listeners);
    }

    public void testSingle() {
        listener.reset();

        assertFalse(dispatch.hasData());
        dispatch.add(eventOne);
        assertTrue(dispatch.hasData());

        dispatch.execute();

        assertFalse(dispatch.hasData());
        assertEquals(1, listener.getLastNewData().length);
        assertEquals(eventOne, listener.getLastNewData()[0]);
    }

    public void testTwo() {
        listener.reset();
        assertFalse(dispatch.hasData());

        dispatch.add(eventOne);
        dispatch.add(eventTwo);
        assertTrue(dispatch.hasData());

        dispatch.execute();

        assertFalse(dispatch.hasData());
        assertEquals(2, listener.getLastNewData().length);
        assertEquals(eventOne, listener.getLastNewData()[0]);
        assertEquals(eventTwo, listener.getLastNewData()[1]);
    }
}
