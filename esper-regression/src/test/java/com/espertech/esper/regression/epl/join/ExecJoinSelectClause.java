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
package com.espertech.esper.regression.epl.join;

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import java.util.Iterator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ExecJoinSelectClause implements RegressionExecution {
    public void run(EPServiceProvider epService) throws Exception {
        String eventA = SupportBean.class.getName();
        String eventB = SupportBean.class.getName();

        String joinStatement = "select s0.doubleBoxed, s1.intPrimitive*s1.intBoxed/2.0 as div from " +
                eventA + "(theString='s0')#length(3) as s0," +
                eventB + "(theString='s1')#length(3) as s1" +
                " where s0.doubleBoxed = s1.doubleBoxed";

        EPStatement joinView = epService.getEPAdministrator().createEPL(joinStatement);
        SupportUpdateListener updateListener = new SupportUpdateListener();
        joinView.addListener(updateListener);

        EventType result = joinView.getEventType();
        assertEquals(Double.class, result.getPropertyType("s0.doubleBoxed"));
        assertEquals(Double.class, result.getPropertyType("div"));
        assertEquals(2, joinView.getEventType().getPropertyNames().length);

        assertNull(updateListener.getLastNewData());

        sendEvent(epService, "s0", 1, 4, 5);
        sendEvent(epService, "s1", 1, 3, 2);

        EventBean[] newEvents = updateListener.getLastNewData();
        assertEquals(1d, newEvents[0].get("s0.doubleBoxed"));
        assertEquals(3d, newEvents[0].get("div"));

        Iterator<EventBean> iterator = joinView.iterator();
        EventBean theEvent = iterator.next();
        assertEquals(1d, theEvent.get("s0.doubleBoxed"));
        assertEquals(3d, theEvent.get("div"));
    }

    private void sendEvent(EPServiceProvider epService, String s, double doubleBoxed, int intPrimitive, int intBoxed) {
        SupportBean bean = new SupportBean();
        bean.setTheString(s);
        bean.setDoubleBoxed(doubleBoxed);
        bean.setIntPrimitive(intPrimitive);
        bean.setIntBoxed(intBoxed);
        epService.getEPRuntime().sendEvent(bean);
    }
}
