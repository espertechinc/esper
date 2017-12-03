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
import com.espertech.esper.client.PropertyAccessException;
import com.espertech.esper.filterspec.FilterSpecParamFilterForEval;
import com.espertech.esper.filterspec.MatchedEventMap;
import com.espertech.esper.filterspec.MatchedEventMapImpl;
import com.espertech.esper.filterspec.MatchedEventMapMeta;
import com.espertech.esper.supportunit.bean.SupportBean;
import com.espertech.esper.supportunit.event.SupportEventBeanFactory;
import junit.framework.TestCase;

public class TestRangeValueEventProp extends TestCase {
    private FilterSpecParamFilterForEval params[] = new FilterSpecParamFilterForEval[5];

    public void setUp() {
        params[0] = new FilterForEvalEventPropDouble("a", "b");
        params[1] = new FilterForEvalEventPropDouble("asName", "b");
        params[2] = new FilterForEvalEventPropDouble("asName", "boolPrimitive");
        params[3] = new FilterForEvalEventPropDouble("asName", "intPrimitive");
        params[4] = new FilterForEvalEventPropDouble("asName", "intPrimitive");
    }

    public void testGetFilterValue() {
        SupportBean eventBean = new SupportBean();
        eventBean.setIntPrimitive(1000);
        EventBean theEvent = SupportEventBeanFactory.createObject(eventBean);
        MatchedEventMap matchedEvents = new MatchedEventMapImpl(new MatchedEventMapMeta(new String[]{"asName"}, false));
        matchedEvents.add(0, theEvent);

        tryInvalidGetFilterValue(matchedEvents, params[0]);
        tryInvalidGetFilterValue(matchedEvents, params[1]);
        assertEquals(1000.0, params[3].getFilterValue(matchedEvents, null));
    }

    public void testEquals() {
        assertFalse(params[0].equals(params[1]));
        assertFalse(params[2].equals(params[3]));
        assertTrue(params[3].equals(params[4]));
    }

    private void tryInvalidGetFilterValue(MatchedEventMap matchedEvents, FilterSpecParamFilterForEval value) {
        try {
            value.getFilterValue(matchedEvents, null);
            fail();
        } catch (IllegalStateException ex) {
            // expected
        } catch (PropertyAccessException ex) {
            // expected
        }
    }
}
