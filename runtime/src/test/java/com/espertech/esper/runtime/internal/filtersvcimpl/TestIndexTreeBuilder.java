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
package com.espertech.esper.runtime.internal.filtersvcimpl;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.filterspec.FilterOperator;
import com.espertech.esper.common.internal.filterspec.FilterSpecActivatable;
import com.espertech.esper.common.internal.filterspec.FilterValueSetParam;
import com.espertech.esper.common.internal.filtersvc.FilterHandle;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.runtime.internal.support.SupportEventBeanFactory;
import junit.framework.TestCase;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class TestIndexTreeBuilder extends TestCase {
    private List<FilterHandle> matches;
    private EventBean eventBean;
    private EventType eventType;
    private FilterHandle testFilterCallback[];
    private FilterServiceGranularLockFactory lockFactory = new FilterServiceGranularLockFactoryReentrant();

    public void setUp() {
        SupportBean testBean = new SupportBean();
        testBean.setIntPrimitive(50);
        testBean.setDoublePrimitive(0.5);
        testBean.setTheString("jack");
        testBean.setLongPrimitive(10);
        testBean.setShortPrimitive((short) 20);

        eventBean = SupportEventBeanFactory.createObject(testBean);
        eventType = eventBean.getEventType();

        matches = new LinkedList<FilterHandle>();

        // Allocate a couple of callbacks
        testFilterCallback = new SupportFilterHandle[20];
        for (int i = 0; i < testFilterCallback.length; i++) {
            testFilterCallback[i] = new SupportFilterHandle();
        }
    }

    public void testBuildWithMatch() {
        FilterHandleSetNode topNode = new FilterHandleSetNode(new ReentrantReadWriteLock());

        // Add some parameter-less expression
        FilterValueSetParam[][] filterSpec = makeFilterValues();
        IndexTreeBuilderAdd.add(filterSpec, testFilterCallback[0], topNode, lockFactory);
        assertTrue(topNode.contains(testFilterCallback[0]));

        // Attempt a match
        topNode.matchEvent(eventBean, matches);
        assertTrue(matches.size() == 1);
        matches.clear();

        // Add a filter that won't match, with a single parameter matching against an int
        filterSpec = makeFilterValues("intPrimitive", FilterOperator.EQUAL, 100);
        IndexTreeBuilderAdd.add(filterSpec, testFilterCallback[1], topNode, lockFactory);
        assertTrue(topNode.getIndizes().size() == 1);
        assertTrue(topNode.getIndizes().get(0).sizeExpensive() == 1);

        // Match again
        topNode.matchEvent(eventBean, matches);
        assertTrue(matches.size() == 1);
        matches.clear();

        // Add a filter that will match
        filterSpec = makeFilterValues("intPrimitive", FilterOperator.EQUAL, 50);
        IndexTreeBuilderAdd.add(filterSpec, testFilterCallback[2], topNode, lockFactory);
        assertTrue(topNode.getIndizes().size() == 1);
        assertTrue(topNode.getIndizes().get(0).sizeExpensive() == 2);

        // match
        topNode.matchEvent(eventBean, matches);
        assertTrue(matches.size() == 2);
        matches.clear();

        // Add some filter against a double
        filterSpec = makeFilterValues("doublePrimitive", FilterOperator.LESS, 1.1);
        IndexTreeBuilderAdd.add(filterSpec, testFilterCallback[3], topNode, lockFactory);
        assertTrue(topNode.getIndizes().size() == 2);
        assertTrue(topNode.getIndizes().get(0).sizeExpensive() == 2);
        assertTrue(topNode.getIndizes().get(1).sizeExpensive() == 1);

        topNode.matchEvent(eventBean, matches);
        assertTrue(matches.size() == 3);
        matches.clear();

        filterSpec = makeFilterValues("doublePrimitive", FilterOperator.LESS_OR_EQUAL, 0.5);
        IndexTreeBuilderAdd.add(filterSpec, testFilterCallback[4], topNode, lockFactory);
        assertTrue(topNode.getIndizes().size() == 3);
        assertTrue(topNode.getIndizes().get(0).sizeExpensive() == 2);
        assertTrue(topNode.getIndizes().get(1).sizeExpensive() == 1);
        assertTrue(topNode.getIndizes().get(2).sizeExpensive() == 1);

        topNode.matchEvent(eventBean, matches);
        assertTrue(matches.size() == 4);
        matches.clear();

        // Add an filterSpec against double and string
        filterSpec = makeFilterValues("doublePrimitive", FilterOperator.LESS, 1.1,
                "theString", FilterOperator.EQUAL, "jack");
        IndexTreeBuilderAdd.add(filterSpec, testFilterCallback[5], topNode, lockFactory);
        assertTrue(topNode.getIndizes().size() == 3);
        assertTrue(topNode.getIndizes().get(0).sizeExpensive() == 2);
        assertTrue(topNode.getIndizes().get(1).sizeExpensive() == 1);
        assertTrue(topNode.getIndizes().get(2).sizeExpensive() == 1);
        FilterHandleSetNode nextLevelSetNode = (FilterHandleSetNode) topNode.getIndizes().get(1).get(Double.valueOf(1.1));
        assertTrue(nextLevelSetNode != null);
        assertTrue(nextLevelSetNode.getIndizes().size() == 1);

        topNode.matchEvent(eventBean, matches);
        assertTrue(matches.size() == 5);
        matches.clear();

        filterSpec = makeFilterValues("doublePrimitive", FilterOperator.LESS, 1.1,
                "theString", FilterOperator.EQUAL, "beta");
        IndexTreeBuilderAdd.add(filterSpec, testFilterCallback[6], topNode, lockFactory);

        topNode.matchEvent(eventBean, matches);
        assertTrue(matches.size() == 5);
        matches.clear();

        filterSpec = makeFilterValues("doublePrimitive", FilterOperator.LESS, 1.1,
                "theString", FilterOperator.EQUAL, "jack");
        IndexTreeBuilderAdd.add(filterSpec, testFilterCallback[7], topNode, lockFactory);
        assertTrue(nextLevelSetNode.getIndizes().size() == 1);
        FilterHandleSetNode nodeTwo = (FilterHandleSetNode) nextLevelSetNode.getIndizes().get(0).get("jack");
        assertTrue(nodeTwo.getFilterCallbackCount() == 2);

        topNode.matchEvent(eventBean, matches);
        assertTrue(matches.size() == 6);
        matches.clear();

        // Try depth first
        filterSpec = makeFilterValues("theString", FilterOperator.EQUAL, "jack",
                "longPrimitive", FilterOperator.EQUAL, 10L,
                "shortPrimitive", FilterOperator.EQUAL, (short) 20);
        IndexTreeBuilderAdd.add(filterSpec, testFilterCallback[8], topNode, lockFactory);

        topNode.matchEvent(eventBean, matches);
        assertTrue(matches.size() == 7);
        matches.clear();

        // Add an filterSpec in the middle
        filterSpec = makeFilterValues("longPrimitive", FilterOperator.EQUAL, 10L,
                "theString", FilterOperator.EQUAL, "jack");
        IndexTreeBuilderAdd.add(filterSpec, testFilterCallback[9], topNode, lockFactory);

        filterSpec = makeFilterValues("longPrimitive", FilterOperator.EQUAL, 10L,
                "theString", FilterOperator.EQUAL, "jim");
        IndexTreeBuilderAdd.add(filterSpec, testFilterCallback[10], topNode, lockFactory);

        filterSpec = makeFilterValues("longPrimitive", FilterOperator.EQUAL, 10L,
                "theString", FilterOperator.EQUAL, "joe");
        IndexTreeBuilderAdd.add(filterSpec, testFilterCallback[11], topNode, lockFactory);

        topNode.matchEvent(eventBean, matches);
        assertTrue(matches.size() == 8);
        matches.clear();
    }

    public void testBuildMatchRemove() {
        FilterHandleSetNode top = new FilterHandleSetNode(new ReentrantReadWriteLock());

        // Add a parameter-less filter
        FilterValueSetParam[][] filterSpecNoParams = makeFilterValues();
        IndexTreeBuilderAdd.add(filterSpecNoParams, testFilterCallback[0], top, lockFactory);

        // Try a match
        top.matchEvent(eventBean, matches);
        assertTrue(matches.size() == 1);
        matches.clear();

        // Remove filter
        IndexTreeBuilderRemove.remove(eventType, testFilterCallback[0], filterSpecNoParams[0], top);

        // Match should not be found
        top.matchEvent(eventBean, matches);
        assertTrue(matches.size() == 0);
        matches.clear();

        // Add a depth-first filterSpec
        FilterValueSetParam[][] filterSpecOne = makeFilterValues(
                "theString", FilterOperator.EQUAL, "jack",
                "longPrimitive", FilterOperator.EQUAL, 10L,
                "shortPrimitive", FilterOperator.EQUAL, (short) 20);
        IndexTreeBuilderAdd.add(filterSpecOne, testFilterCallback[1], top, lockFactory);

        FilterValueSetParam[][] filterSpecTwo = makeFilterValues(
                "theString", FilterOperator.EQUAL, "jack",
                "longPrimitive", FilterOperator.EQUAL, 10L,
                "shortPrimitive", FilterOperator.EQUAL, (short) 20);
        IndexTreeBuilderAdd.add(filterSpecTwo, testFilterCallback[2], top, lockFactory);

        FilterValueSetParam[][] filterSpecThree = makeFilterValues(
                "theString", FilterOperator.EQUAL, "jack",
                "longPrimitive", FilterOperator.EQUAL, 10L);
        IndexTreeBuilderAdd.add(filterSpecThree, testFilterCallback[3], top, lockFactory);

        FilterValueSetParam[][] filterSpecFour = makeFilterValues(
                "theString", FilterOperator.EQUAL, "jack");
        IndexTreeBuilderAdd.add(filterSpecFour, testFilterCallback[4], top, lockFactory);

        FilterValueSetParam[][] filterSpecFive = makeFilterValues(
                "longPrimitive", FilterOperator.EQUAL, 10L);
        IndexTreeBuilderAdd.add(filterSpecFive, testFilterCallback[5], top, lockFactory);

        top.matchEvent(eventBean, matches);
        assertTrue(matches.size() == 5);
        matches.clear();

        // Remove some of the nodes
        IndexTreeBuilderRemove.remove(eventType, testFilterCallback[2], filterSpecTwo[0], top);

        top.matchEvent(eventBean, matches);
        assertTrue(matches.size() == 4);
        matches.clear();

        // Remove some of the nodes
        IndexTreeBuilderRemove.remove(eventType, testFilterCallback[4], filterSpecFour[0], top);

        top.matchEvent(eventBean, matches);
        assertTrue(matches.size() == 3);
        matches.clear();

        // Remove some of the nodes
        IndexTreeBuilderRemove.remove(eventType, testFilterCallback[5], filterSpecFive[0], top);

        top.matchEvent(eventBean, matches);
        assertTrue(matches.size() == 2);
        matches.clear();

        // Remove some of the nodes
        IndexTreeBuilderRemove.remove(eventType, testFilterCallback[1], filterSpecOne[0], top);

        top.matchEvent(eventBean, matches);
        assertTrue(matches.size() == 1);
        matches.clear();

        // Remove some of the nodes
        IndexTreeBuilderRemove.remove(eventType, testFilterCallback[3], filterSpecThree[0], top);

        top.matchEvent(eventBean, matches);
        assertTrue(matches.size() == 0);
        matches.clear();
    }

    private FilterValueSetParam[][] makeFilterValues(Object... filterSpecArgs) {
        FilterSpecActivatable spec = SupportFilterSpecBuilder.build(eventType, filterSpecArgs);
        return spec.getValueSet(null, null, null, null);
    }
}
