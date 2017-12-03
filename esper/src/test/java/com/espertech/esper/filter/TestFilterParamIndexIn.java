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
import com.espertech.esper.collection.MultiKeyUntyped;
import com.espertech.esper.filterspec.FilterOperator;
import com.espertech.esper.epl.expression.core.ExprFilterSpecLookupable;
import com.espertech.esper.supportunit.bean.SupportBean;
import com.espertech.esper.supportunit.event.SupportEventBeanFactory;
import com.espertech.esper.supportunit.filter.SupportEventEvaluator;
import junit.framework.TestCase;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class TestFilterParamIndexIn extends TestCase {
    private SupportEventEvaluator testEvaluator;
    private SupportBean testBean;
    private EventBean testEventBean;
    private EventType testEventType;
    private List<FilterHandle> matchesList;

    public void setUp() {
        testEvaluator = new SupportEventEvaluator();
        testBean = new SupportBean();
        testEventBean = SupportEventBeanFactory.createObject(testBean);
        testEventType = testEventBean.getEventType();
        matchesList = new LinkedList<FilterHandle>();
    }

    public void testIndex() {
        FilterParamIndexIn index = new FilterParamIndexIn(makeLookupable("longBoxed"), new ReentrantReadWriteLock());
        assertEquals(FilterOperator.IN_LIST_OF_VALUES, index.getFilterOperator());

        MultiKeyUntyped inList = new MultiKeyUntyped(new Object[]{2L, 5L});
        index.put(inList, testEvaluator);
        inList = new MultiKeyUntyped(new Object[]{10L, 5L});
        index.put(inList, testEvaluator);

        verify(index, 1L, 0);
        verify(index, 2L, 1);
        verify(index, 5L, 2);
        verify(index, 10L, 1);
        verify(index, 999L, 0);
        verify(index, null, 0);

        assertEquals(testEvaluator, index.get(inList));
        assertTrue(index.getReadWriteLock() != null);
        index.remove(inList);
        index.remove(inList);
        assertEquals(null, index.get(inList));

        try {
            index.put("a", testEvaluator);
            assertTrue(false);
        } catch (Exception ex) {
            // Expected
        }
    }

    private void verify(FilterParamIndexBase index, Long testValue, int numExpected) {
        testBean.setLongBoxed(testValue);
        index.matchEvent(testEventBean, matchesList);
        assertEquals(numExpected, testEvaluator.getAndResetCountInvoked());
    }

    private ExprFilterSpecLookupable makeLookupable(String fieldName) {
        return new ExprFilterSpecLookupable(fieldName, testEventType.getGetter(fieldName), testEventType.getPropertyType(fieldName), false);
    }
}
