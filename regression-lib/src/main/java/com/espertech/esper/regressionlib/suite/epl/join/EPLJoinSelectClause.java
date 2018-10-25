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
package com.espertech.esper.regressionlib.suite.epl.join;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.common.internal.support.SupportBean;

import java.util.Iterator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class EPLJoinSelectClause implements RegressionExecution {

    public void run(RegressionEnvironment env) {

        String epl = "@name('s0') select s0.doubleBoxed, s1.intPrimitive*s1.intBoxed/2.0 as div from " +
            "SupportBean(theString='s0')#length(3) as s0," +
            "SupportBean(theString='s1')#length(3) as s1" +
            " where s0.doubleBoxed = s1.doubleBoxed";
        env.compileDeployAddListenerMileZero(epl, "s0");

        EventType result = env.statement("s0").getEventType();
        assertEquals(Double.class, result.getPropertyType("s0.doubleBoxed"));
        assertEquals(Double.class, result.getPropertyType("div"));
        assertEquals(2, env.statement("s0").getEventType().getPropertyNames().length);

        assertNull(env.listener("s0").getLastNewData());

        sendEvent(env, "s0", 1, 4, 5);

        env.milestone(1);

        sendEvent(env, "s1", 1, 3, 2);
        EventBean[] newEvents = env.listener("s0").getLastNewData();
        assertEquals(1d, newEvents[0].get("s0.doubleBoxed"));
        assertEquals(3d, newEvents[0].get("div"));

        env.milestone(2);

        Iterator<EventBean> iterator = env.statement("s0").iterator();
        EventBean theEvent = iterator.next();
        assertEquals(1d, theEvent.get("s0.doubleBoxed"));
        assertEquals(3d, theEvent.get("div"));

        env.undeployAll();
    }

    private static void sendEvent(RegressionEnvironment env, String s, double doubleBoxed, int intPrimitive, int intBoxed) {
        SupportBean bean = new SupportBean();
        bean.setTheString(s);
        bean.setDoubleBoxed(doubleBoxed);
        bean.setIntPrimitive(intPrimitive);
        bean.setIntBoxed(intBoxed);
        env.sendEventBean(bean);
    }
}
