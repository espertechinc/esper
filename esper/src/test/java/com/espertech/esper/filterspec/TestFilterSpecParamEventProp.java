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
package com.espertech.esper.filterspec;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.epl.expression.core.ExprFilterSpecLookupable;
import com.espertech.esper.supportunit.bean.SupportBean;
import com.espertech.esper.supportunit.event.SupportEventBeanFactory;
import com.espertech.esper.util.SimpleNumberCoercer;
import com.espertech.esper.util.SimpleNumberCoercerFactory;
import junit.framework.TestCase;

public class TestFilterSpecParamEventProp extends TestCase {
    public void testEquals() {
        FilterSpecParamEventProp params[] = new FilterSpecParamEventProp[5];
        params[0] = makeParam("a", "intBoxed");
        params[1] = makeParam("b", "intBoxed");
        params[2] = makeParam("a", "intPrimitive");
        params[3] = makeParam("c", "intPrimitive");
        params[4] = makeParam("a", "intBoxed");

        assertEquals(params[0], params[4]);
        assertEquals(params[4], params[0]);
        assertFalse(params[0].equals(params[1]));
        assertFalse(params[0].equals(params[2]));
        assertFalse(params[0].equals(params[3]));
    }

    public void testGetFilterValue() {
        FilterSpecParamEventProp parameters = makeParam("asName", "intBoxed");

        SupportBean eventBean = new SupportBean();
        eventBean.setIntBoxed(1000);
        EventBean theEvent = SupportEventBeanFactory.createObject(eventBean);

        MatchedEventMap matchedEvents = new MatchedEventMapImpl(new MatchedEventMapMeta(new String[]{"asName"}, false));
        matchedEvents.add(0, theEvent);

        assertEquals(1000, parameters.getFilterValue(matchedEvents, null, null, null));
    }

    private FilterSpecParamEventProp makeParam(String eventAsName, String property) {
        SimpleNumberCoercer numberCoercer = SimpleNumberCoercerFactory.getCoercer(int.class, int.class);
        return new FilterSpecParamEventProp(makeLookupable("intPrimitive"), FilterOperator.EQUAL, eventAsName, property, false, numberCoercer, int.class, "Test");
    }

    private ExprFilterSpecLookupable makeLookupable(String fieldName) {
        return new ExprFilterSpecLookupable(fieldName, null, null, false);
    }
}
