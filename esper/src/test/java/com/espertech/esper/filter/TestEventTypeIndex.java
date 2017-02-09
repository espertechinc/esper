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
package com.espertech.esper.filter;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.supportunit.bean.*;
import com.espertech.esper.supportunit.event.SupportEventBeanFactory;
import com.espertech.esper.supportunit.event.SupportEventTypeFactory;
import com.espertech.esper.supportunit.filter.SupportFilterHandle;
import junit.framework.TestCase;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class TestEventTypeIndex extends TestCase {
    private EventTypeIndex testIndex;

    private EventBean testEventBean;
    private EventType testEventType;

    private FilterHandleSetNode handleSetNode;
    private FilterHandle filterCallback;

    public void setUp() {
        SupportBean testBean = new SupportBean();
        testEventBean = SupportEventBeanFactory.createObject(testBean);
        testEventType = testEventBean.getEventType();

        handleSetNode = new FilterHandleSetNode(new ReentrantReadWriteLock());
        filterCallback = new SupportFilterHandle();
        handleSetNode.add(filterCallback);

        testIndex = new EventTypeIndex(new FilterServiceGranularLockFactoryReentrant());
        testIndex.add(testEventType, handleSetNode);
    }

    public void testMatch() {
        List<FilterHandle> matchesList = new LinkedList<FilterHandle>();

        // Invoke match
        testIndex.matchEvent(testEventBean, matchesList);

        assertEquals(1, matchesList.size());
        assertEquals(filterCallback, matchesList.get(0));
    }

    public void testInvalidSecondAdd() {
        try {
            testIndex.add(testEventType, handleSetNode);
            assertTrue(false);
        } catch (IllegalStateException ex) {
            // Expected
        }
    }

    public void testGet() {
        assertEquals(handleSetNode, testIndex.get(testEventType));
    }

    public void testSuperclassMatch() {
        testEventBean = SupportEventBeanFactory.createObject(new ISupportAImplSuperGImplPlus());
        testEventType = SupportEventTypeFactory.createBeanType(ISupportA.class);

        testIndex = new EventTypeIndex(new FilterServiceGranularLockFactoryReentrant());
        testIndex.add(testEventType, handleSetNode);

        List<FilterHandle> matchesList = new LinkedList<FilterHandle>();
        testIndex.matchEvent(testEventBean, matchesList);

        assertEquals(1, matchesList.size());
        assertEquals(filterCallback, matchesList.get(0));
    }

    public void testInterfaceMatch() {
        testEventBean = SupportEventBeanFactory.createObject(new ISupportABCImpl("a", "b", "ab", "c"));
        testEventType = SupportEventTypeFactory.createBeanType(ISupportBaseAB.class);

        testIndex = new EventTypeIndex(new FilterServiceGranularLockFactoryReentrant());
        testIndex.add(testEventType, handleSetNode);

        List<FilterHandle> matchesList = new LinkedList<FilterHandle>();
        testIndex.matchEvent(testEventBean, matchesList);

        assertEquals(1, matchesList.size());
        assertEquals(filterCallback, matchesList.get(0));
    }
}