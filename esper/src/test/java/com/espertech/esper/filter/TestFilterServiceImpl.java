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
import com.espertech.esper.filterspec.FilterOperator;
import com.espertech.esper.filterspec.FilterValueSet;
import com.espertech.esper.supportunit.bean.SupportBean;
import com.espertech.esper.supportunit.bean.SupportBeanSimple;
import com.espertech.esper.supportunit.event.SupportEventBeanFactory;
import com.espertech.esper.supportunit.event.SupportEventTypeFactory;
import com.espertech.esper.supportunit.filter.SupportFilterHandle;
import com.espertech.esper.supportunit.filter.SupportFilterSpecBuilder;
import junit.framework.TestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicReference;

public class TestFilterServiceImpl extends TestCase {
    private EventType eventTypeOne;
    private EventType eventTypeTwo;
    private FilterServiceLockCoarse filterService;
    private Vector<FilterValueSet> filterSpecs;
    private Vector<SupportFilterHandle> filterCallbacks;
    private Vector<EventBean> events;
    private Vector<int[]> matchesExpected;

    public void setUp() {
        filterService = new FilterServiceLockCoarse(false);

        eventTypeOne = SupportEventTypeFactory.createBeanType(SupportBean.class);
        eventTypeTwo = SupportEventTypeFactory.createBeanType(SupportBeanSimple.class);

        filterSpecs = new Vector<FilterValueSet>();
        filterSpecs.add(SupportFilterSpecBuilder.build(eventTypeOne, new Object[0]).getValueSet(null, null, null, null, null));
        filterSpecs.add(SupportFilterSpecBuilder.build(eventTypeOne, new Object[]{
                "intPrimitive", FilterOperator.RANGE_CLOSED, 10, 20,
                "theString", FilterOperator.EQUAL, "HELLO",
                "boolPrimitive", FilterOperator.EQUAL, false,
                "doubleBoxed", FilterOperator.GREATER, 100d}).getValueSet(null, null, null, null, null));
        filterSpecs.add(SupportFilterSpecBuilder.build(eventTypeTwo, new Object[0]).getValueSet(null, null, null, null, null));
        filterSpecs.add(SupportFilterSpecBuilder.build(eventTypeTwo, new Object[]{
                "myInt", FilterOperator.RANGE_HALF_CLOSED, 1, 10,
                "myString", FilterOperator.EQUAL, "Hello"}).getValueSet(null, null, null, null, null));

        // Create callbacks and add
        filterCallbacks = new Vector<SupportFilterHandle>();
        for (int i = 0; i < filterSpecs.size(); i++) {
            filterCallbacks.add(new SupportFilterHandle());
            filterService.add(filterSpecs.get(i), filterCallbacks.get(i));
        }

        // Create events
        matchesExpected = new Vector<int[]>();
        events = new Vector<EventBean>();

        events.add(makeTypeOneEvent(15, "HELLO", false, 101));
        matchesExpected.add(new int[]{1, 1, 0, 0});

        events.add(makeTypeTwoEvent("Hello", 100));
        matchesExpected.add(new int[]{0, 0, 1, 0});

        events.add(makeTypeTwoEvent("Hello", 1));       // eventNumber = 2
        matchesExpected.add(new int[]{0, 0, 1, 0});

        events.add(makeTypeTwoEvent("Hello", 2));
        matchesExpected.add(new int[]{0, 0, 1, 1});

        events.add(makeTypeOneEvent(15, "HELLO", true, 100));
        matchesExpected.add(new int[]{1, 0, 0, 0});

        events.add(makeTypeOneEvent(15, "HELLO", false, 99));
        matchesExpected.add(new int[]{1, 0, 0, 0});

        events.add(makeTypeOneEvent(9, "HELLO", false, 100));
        matchesExpected.add(new int[]{1, 0, 0, 0});

        events.add(makeTypeOneEvent(10, "no", false, 100));
        matchesExpected.add(new int[]{1, 0, 0, 0});

        events.add(makeTypeOneEvent(15, "HELLO", false, 999999));      // number 8
        matchesExpected.add(new int[]{1, 1, 0, 0});

        events.add(makeTypeTwoEvent("Hello", 10));
        matchesExpected.add(new int[]{0, 0, 1, 1});

        events.add(makeTypeTwoEvent("Hello", 11));
        matchesExpected.add(new int[]{0, 0, 1, 0});
    }

    public void testEvalEvents() {
        for (int i = 0; i < events.size(); i++) {
            List<FilterHandle> matchList = new LinkedList<FilterHandle>();
            filterService.evaluate(events.get(i), matchList);
            for (FilterHandle match : matchList) {
                SupportFilterHandle handle = (SupportFilterHandle) match;
                handle.matchFound(events.get(i), null);
            }

            int[] matches = matchesExpected.get(i);

            for (int j = 0; j < matches.length; j++) {
                SupportFilterHandle callback = filterCallbacks.get(j);

                if (matches[j] != callback.getAndResetCountInvoked()) {
                    log.debug(".testEvalEvents Match failed, event=" + events.get(i).getUnderlying());
                    log.debug(".testEvalEvents Match failed, eventNumber=" + i + " index=" + j);
                    assertTrue(false);
                }
            }
        }
    }

    /**
     * Test for removing a callback that is waiting to occur,
     * ie. a callback is removed which was a result of an evaluation and it
     * thus needs to be removed from the tree AND the current dispatch list.
     */
    public void testActiveCallbackRemove() {
        FilterValueSet spec = SupportFilterSpecBuilder.build(eventTypeOne, new Object[0]).getValueSet(null, null, null, null, null);
        final SupportFilterHandle callbackTwo = new SupportFilterHandle();

        // callback that removes another matching filter spec callback
        final AtomicReference<FilterServiceEntry> filterServiceEntryOne = new AtomicReference<FilterServiceEntry>();
        FilterHandleCallback callbackOne = new FilterHandleCallback() {
            public int getStatementId() {
                return 1;
            }

            public void matchFound(EventBean theEvent, Collection<FilterHandleCallback> allStmtMatches) {
                log.debug(".matchFound Removing callbackTwo");
                filterService.remove(callbackTwo, filterServiceEntryOne.get());
            }

            public boolean isSubSelect() {
                return false;
            }
        };

        FilterServiceEntry filterServiceEntry = filterService.add(spec, callbackOne);
        filterServiceEntryOne.set(filterServiceEntry);
        filterService.add(spec, callbackTwo);

        // send event
        EventBean theEvent = makeTypeOneEvent(1, "HELLO", false, 1);
        List<FilterHandle> matches = new LinkedList<FilterHandle>();
        filterService.evaluate(theEvent, matches);
        for (FilterHandle match : matches) {
            FilterHandleCallback handle = (FilterHandleCallback) match;
            handle.matchFound(theEvent, null);
        }

        // Callback two MUST be invoked, was removed by callback one, but since the
        // callback invocation order should not matter, the second one MUST also execute
        assertEquals(1, callbackTwo.getAndResetCountInvoked());
    }

    private EventBean makeTypeOneEvent(int intPrimitive, String theString, boolean boolPrimitive, double doubleBoxed) {
        SupportBean bean = new SupportBean();
        bean.setIntPrimitive(intPrimitive);
        bean.setTheString(theString);
        bean.setBoolPrimitive(boolPrimitive);
        bean.setDoubleBoxed(doubleBoxed);
        return SupportEventBeanFactory.createObject(bean);
    }

    private EventBean makeTypeTwoEvent(String myString, int myInt) {
        SupportBeanSimple bean = new SupportBeanSimple(myString, myInt);
        return SupportEventBeanFactory.createObject(bean);
    }

    private static final Logger log = LoggerFactory.getLogger(TestFilterServiceImpl.class);
}
