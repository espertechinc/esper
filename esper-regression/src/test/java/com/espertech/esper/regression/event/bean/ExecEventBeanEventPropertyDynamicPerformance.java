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
package com.espertech.esper.regression.event.bean;

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.bean.SupportBeanComplexProps;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ExecEventBeanEventPropertyDynamicPerformance implements RegressionExecution {
    public boolean excludeWhenInstrumented() {
        return true;
    }

    public void run(EPServiceProvider epService) throws Exception {

        String stmtText = "select simpleProperty?, " +
                "indexed[1]? as indexed, " +
                "mapped('keyOne')? as mapped " +
                "from " + SupportBeanComplexProps.class.getName();
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        EventType type = stmt.getEventType();
        assertEquals(Object.class, type.getPropertyType("simpleProperty?"));
        assertEquals(Object.class, type.getPropertyType("indexed"));
        assertEquals(Object.class, type.getPropertyType("mapped"));

        SupportBeanComplexProps inner = SupportBeanComplexProps.makeDefaultBean();
        epService.getEPRuntime().sendEvent(inner);
        EventBean theEvent = listener.assertOneGetNewAndReset();
        assertEquals(inner.getSimpleProperty(), theEvent.get("simpleProperty?"));
        assertEquals(inner.getIndexed(1), theEvent.get("indexed"));
        assertEquals(inner.getMapped("keyOne"), theEvent.get("mapped"));

        long start = System.currentTimeMillis();
        for (int i = 0; i < 10000; i++) {
            epService.getEPRuntime().sendEvent(inner);
            if (i % 1000 == 0) {
                listener.reset();
            }
        }
        long end = System.currentTimeMillis();
        long delta = end - start;
        assertTrue("delta=" + delta, delta < 1000);
    }
}
