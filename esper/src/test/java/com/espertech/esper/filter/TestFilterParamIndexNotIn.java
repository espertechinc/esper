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

public class TestFilterParamIndexNotIn extends TestCase {
    private SupportEventEvaluator testEvaluators[];
    private SupportBean testBean;
    private EventBean testEventBean;
    private EventType testEventType;
    private List<FilterHandle> matchesList;

    public void setUp() {
        testEvaluators = new SupportEventEvaluator[4];
        for (int i = 0; i < testEvaluators.length; i++) {
            testEvaluators[i] = new SupportEventEvaluator();
        }

        testBean = new SupportBean();
        testEventBean = SupportEventBeanFactory.createObject(testBean);
        testEventType = testEventBean.getEventType();
        matchesList = new LinkedList<FilterHandle>();
    }

    public void testIndex() {
        FilterParamIndexNotIn index = new FilterParamIndexNotIn(makeLookupable("longBoxed"), new ReentrantReadWriteLock());
        assertEquals(FilterOperator.NOT_IN_LIST_OF_VALUES, index.getFilterOperator());

        index.put(new MultiKeyUntyped(new Object[]{2L, 5L}), testEvaluators[0]);
        index.put(new MultiKeyUntyped(new Object[]{3L, 4L, 5L}), testEvaluators[1]);
        index.put(new MultiKeyUntyped(new Object[]{1L, 4L, 5L}), testEvaluators[2]);
        index.put(new MultiKeyUntyped(new Object[]{2L, 5L}), testEvaluators[3]);

        verify(index, 0L, new boolean[]{true, true, true, true});
        verify(index, 1L, new boolean[]{true, true, false, true});
        verify(index, 2L, new boolean[]{false, true, true, false});
        verify(index, 3L, new boolean[]{true, false, true, true});
        verify(index, 4L, new boolean[]{true, false, false, true});
        verify(index, 5L, new boolean[]{false, false, false, false});
        verify(index, 6L, new boolean[]{true, true, true, true});

        MultiKeyUntyped inList = new MultiKeyUntyped(new Object[]{3L, 4L, 5L});
        assertEquals(testEvaluators[1], index.get(inList));
        assertTrue(index.getReadWriteLock() != null);
        index.remove(inList);
        index.remove(inList);
        assertEquals(null, index.get(inList));

        // now that {3,4,5} is removed, verify results again
        verify(index, 0L, new boolean[]{true, false, true, true});
        verify(index, 1L, new boolean[]{true, false, false, true});
        verify(index, 2L, new boolean[]{false, false, true, false});
        verify(index, 3L, new boolean[]{true, false, true, true});
        verify(index, 4L, new boolean[]{true, false, false, true});
        verify(index, 5L, new boolean[]{false, false, false, false});
        verify(index, 6L, new boolean[]{true, false, true, true});

        try {
            index.put("a", testEvaluators[0]);
            assertTrue(false);
        } catch (Exception ex) {
            // Expected
        }
    }

    private void verify(FilterParamIndexBase index, Long testValue, boolean[] expected) {
        testBean.setLongBoxed(testValue);
        index.matchEvent(testEventBean, matchesList);
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], testEvaluators[i].getAndResetCountInvoked() == 1);
        }
    }

    private ExprFilterSpecLookupable makeLookupable(String fieldName) {
        return new ExprFilterSpecLookupable(fieldName, testEventType.getGetter(fieldName), testEventType.getPropertyType(fieldName), false);
    }
}
