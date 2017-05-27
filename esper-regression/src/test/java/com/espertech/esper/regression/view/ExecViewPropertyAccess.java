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
package com.espertech.esper.regression.view;

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.bean.SupportBeanComplexProps;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import static org.junit.Assert.*;

public class ExecViewPropertyAccess implements RegressionExecution {
    public void run(EPServiceProvider epService) throws Exception {
        String epl = "select mapped('keyOne') as a," +
                "indexed[1] as b, nested.nestedNested.nestedNestedValue as c, mapProperty, " +
                "arrayProperty[0] " +
                "  from " + SupportBeanComplexProps.class.getName() + "#length(3) " +
                " where mapped('keyOne') = 'valueOne' and " +
                " indexed[1] = 2 and " +
                " nested.nestedNested.nestedNestedValue = 'nestedNestedValue'";

        EPStatement testView = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        testView.addListener(listener);

        SupportBeanComplexProps eventObject = SupportBeanComplexProps.makeDefaultBean();
        epService.getEPRuntime().sendEvent(eventObject);
        EventBean theEvent = listener.getAndResetLastNewData()[0];
        assertEquals(eventObject.getMapped("keyOne"), theEvent.get("a"));
        assertEquals(eventObject.getIndexed(1), theEvent.get("b"));
        assertEquals(eventObject.getNested().getNestedNested().getNestedNestedValue(), theEvent.get("c"));
        assertEquals(eventObject.getMapProperty(), theEvent.get("mapProperty"));
        assertEquals(eventObject.getArrayProperty()[0], theEvent.get("arrayProperty[0]"));

        eventObject.setIndexed(1, Integer.MIN_VALUE);
        assertFalse(listener.isInvoked());
        epService.getEPRuntime().sendEvent(eventObject);
        assertFalse(listener.isInvoked());

        eventObject.setIndexed(1, 2);
        epService.getEPRuntime().sendEvent(eventObject);
        assertTrue(listener.isInvoked());
    }
}
