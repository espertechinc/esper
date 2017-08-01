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
package com.espertech.esper.epl.join.base;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.collection.MultiKey;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.supportunit.bean.SupportBean;
import com.espertech.esper.supportunit.epl.SupportExprNodeFactory;
import com.espertech.esper.supportunit.event.SupportEventBeanFactory;
import junit.framework.TestCase;

import java.util.HashSet;
import java.util.Set;

public class TestJoinSetFilter extends TestCase {
    public void testFilter() throws Exception {
        ExprNode topNode = SupportExprNodeFactory.make2SubNodeAnd();

        EventBean[] pairOne = new EventBean[2];
        pairOne[0] = makeEvent(1, 2, "a");
        pairOne[1] = makeEvent(2, 1, "a");

        EventBean[] pairTwo = new EventBean[2];
        pairTwo[0] = makeEvent(1, 2, "a");
        pairTwo[1] = makeEvent(2, 999, "a");

        Set<MultiKey<EventBean>> eventSet = new HashSet<MultiKey<EventBean>>();
        eventSet.add(new MultiKey<EventBean>(pairOne));
        eventSet.add(new MultiKey<EventBean>(pairTwo));

        JoinSetFilter.filter(topNode.getForge().getExprEvaluator(), eventSet, true, null);

        assertEquals(1, eventSet.size());
        assertSame(pairOne, eventSet.iterator().next().getArray());
    }

    private EventBean makeEvent(int intPrimitive, int intBoxed, String theStringValue) {
        SupportBean theEvent = new SupportBean();
        theEvent.setIntPrimitive(intPrimitive);
        theEvent.setIntBoxed(intBoxed);
        theEvent.setTheString(theStringValue);
        return SupportEventBeanFactory.createObject(theEvent);
    }
}
