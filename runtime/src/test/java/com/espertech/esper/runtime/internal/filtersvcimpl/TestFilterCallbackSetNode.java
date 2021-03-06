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
import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.internal.epl.expression.core.ExprFilterSpecLookupable;
import com.espertech.esper.common.internal.filtersvc.FilterHandle;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportBeanSimple;
import com.espertech.esper.runtime.internal.support.SupportEventBeanFactory;
import com.espertech.esper.runtime.internal.support.SupportEventTypeFactory;
import com.espertech.esper.runtime.internal.support.SupportExprEventEvaluator;
import junit.framework.TestCase;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class TestFilterCallbackSetNode extends TestCase {
    private SupportEventEvaluator testEvaluator;
    private FilterHandleSetNode testNode;

    public void setUp() {
        testEvaluator = new SupportEventEvaluator();
        testNode = new FilterHandleSetNode(new ReentrantReadWriteLock());
    }

    public void testNodeGetSet() {
        FilterHandle exprOne = new SupportFilterHandle();

        // Check pre-conditions
        assertTrue(testNode.getNodeRWLock() != null);
        assertFalse(testNode.contains(exprOne));
        assertEquals(0, testNode.getFilterCallbackCount());
        assertEquals(0, testNode.getIndizes().size());
        assertTrue(testNode.isEmpty());

        testNode.add(exprOne);

        // Check after add
        assertTrue(testNode.contains(exprOne));
        assertEquals(1, testNode.getFilterCallbackCount());
        assertFalse(testNode.isEmpty());

        // Add an indexOne
        EventType eventType = SupportEventTypeFactory.createBeanType(SupportBean.class);
        SupportExprEventEvaluator eval = new SupportExprEventEvaluator(eventType.getGetter("intPrimitive"));
        ExprFilterSpecLookupable lookupable = new ExprFilterSpecLookupable("intPrimitive", eval, null, (EPTypeClass) eventType.getPropertyEPType("intPrimitive"), false, null);
        FilterParamIndexBase indexOne = new SupportFilterParamIndex(lookupable);
        testNode.add(indexOne);

        // Check after add
        assertEquals(1, testNode.getIndizes().size());
        assertEquals(indexOne, testNode.getIndizes().get(0));

        // Check removes
        assertTrue(testNode.remove(exprOne));
        assertFalse(testNode.isEmpty());
        assertFalse(testNode.remove(exprOne));
        assertTrue(testNode.remove(indexOne));
        assertFalse(testNode.remove(indexOne));
        assertTrue(testNode.isEmpty());
    }

    public void testNodeMatching() {
        SupportBeanSimple eventObject = new SupportBeanSimple("DepositEvent_1", 1);
        EventBean eventBean = SupportEventBeanFactory.createObject(eventObject);

        FilterHandle expr = new SupportFilterHandle();
        testNode.add(expr);

        // Check matching without an index node
        List<FilterHandle> matches = new LinkedList<FilterHandle>();
        testNode.matchEvent(eventBean, matches, null);
        assertEquals(1, matches.size());
        assertEquals(expr, matches.get(0));
        matches.clear();

        // Create, add and populate an index node
        FilterParamIndexBase index = new FilterParamIndexEquals(makeLookupable("myString", eventBean.getEventType()), new ReentrantReadWriteLock());
        testNode.add(index);
        index.put("DepositEvent_1", testEvaluator);

        // Verify matcher instance stored in index is called
        testNode.matchEvent(eventBean, matches, null);

        assertTrue(testEvaluator.getAndResetCountInvoked() == 1);
        assertTrue(testEvaluator.getLastEvent() == eventBean);
        assertEquals(1, matches.size());
        assertEquals(expr, matches.get(0));
    }

    private ExprFilterSpecLookupable makeLookupable(String fieldName, EventType eventType) {
        SupportExprEventEvaluator eval = new SupportExprEventEvaluator(eventType.getGetter(fieldName));
        return new ExprFilterSpecLookupable(fieldName, eval, null, (EPTypeClass) eventType.getPropertyEPType(fieldName), false, null);
    }
}