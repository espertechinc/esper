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
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.common.internal.support.SupportBean_S1;
import com.espertech.esper.common.internal.support.SupportBean_S2;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.support.bean.*;
import com.espertech.esper.runtime.client.scopetest.SupportListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.*;

public class EPLJoin5StreamPerformance implements RegressionExecution {
    @Override
    public boolean excludeWhenInstrumented() {
        return true;
    }

    public void run(RegressionEnvironment env) {
        String statement = "@name('s0') select * from " +
            "SupportBean_S0#length(100000) as s0," +
            "SupportBean_S1#length(100000) as s1," +
            "SupportBean_S2#length(100000) as s2," +
            "SupportBean_S3#length(100000) as s3," +
            "SupportBean_S4#length(100000) as s4" +
            " where s0.p00 = s1.p10 " +
            "and s1.p10 = s2.p20 " +
            "and s2.p20 = s3.p30 " +
            "and s3.p30 = s4.p40 ";
        env.compileDeployAddListenerMileZero(statement, "s0");

        log.info(".testPerfAllProps Preloading events");
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 1000; i++) {
            sendEvents(env, new int[]{0, 0, 0, 0, 0}, new String[]{"s0" + i, "s1" + i, "s2" + i, "s3" + i, "s4" + i});
        }

        long endTime = System.currentTimeMillis();
        log.info(".testPerfAllProps delta=" + (endTime - startTime));
        assertTrue((endTime - startTime) < 1500);

        // test if join returns data
        assertNull(env.listener("s0").getLastNewData());
        String[] propertyValues = new String[]{"x", "x", "x", "x", "x"};
        int[] ids = new int[]{1, 2, 3, 4, 5};
        sendEvents(env, ids, propertyValues);
        assertEventsReceived(env.listener("s0"), ids);

        env.undeployAll();
    }

    private static void assertEventsReceived(SupportListener updateListener, int[] expectedIds) {
        assertEquals(1, updateListener.getLastNewData().length);
        assertNull(updateListener.getLastOldData());
        EventBean theEvent = updateListener.getLastNewData()[0];
        assertEquals(expectedIds[0], ((SupportBean_S0) theEvent.get("s0")).getId());
        assertEquals(expectedIds[1], ((SupportBean_S1) theEvent.get("s1")).getId());
        assertEquals(expectedIds[2], ((SupportBean_S2) theEvent.get("s2")).getId());
        assertEquals(expectedIds[3], ((SupportBean_S3) theEvent.get("s3")).getId());
        assertEquals(expectedIds[4], ((SupportBean_S4) theEvent.get("s4")).getId());
    }

    private static void sendEvent(RegressionEnvironment env, Object theEvent) {
        env.sendEventBean(theEvent);
    }

    private static void sendEvents(RegressionEnvironment env, int[] ids, String[] propertyValues) {
        sendEvent(env, new SupportBean_S0(ids[0], propertyValues[0]));
        sendEvent(env, new SupportBean_S1(ids[1], propertyValues[1]));
        sendEvent(env, new SupportBean_S2(ids[2], propertyValues[2]));
        sendEvent(env, new SupportBean_S3(ids[3], propertyValues[3]));
        sendEvent(env, new SupportBean_S4(ids[4], propertyValues[4]));
    }

    private static final Logger log = LoggerFactory.getLogger(EPLJoin5StreamPerformance.class);
}
