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
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.bean.*;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.*;

public class ExecJoin5StreamPerformance implements RegressionExecution {
    public void run(EPServiceProvider epService) throws Exception {
        String statement = "select * from " +
                SupportBean_S0.class.getName() + "#length(100000) as s0," +
                SupportBean_S1.class.getName() + "#length(100000) as s1," +
                SupportBean_S2.class.getName() + "#length(100000) as s2," +
                SupportBean_S3.class.getName() + "#length(100000) as s3," +
                SupportBean_S4.class.getName() + "#length(100000) as s4" +
                " where s0.p00 = s1.p10 " +
                "and s1.p10 = s2.p20 " +
                "and s2.p20 = s3.p30 " +
                "and s3.p30 = s4.p40 ";

        EPStatement stmt = epService.getEPAdministrator().createEPL(statement);
        SupportUpdateListener updateListener = new SupportUpdateListener();
        stmt.addListener(updateListener);

        log.info(".testPerfAllProps Preloading events");
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 1000; i++) {
            sendEvents(epService, new int[]{0, 0, 0, 0, 0}, new String[]{"s0" + i, "s1" + i, "s2" + i, "s3" + i, "s4" + i});
        }

        long endTime = System.currentTimeMillis();
        log.info(".testPerfAllProps delta=" + (endTime - startTime));
        assertTrue((endTime - startTime) < 1500);

        // test if join returns data
        assertNull(updateListener.getLastNewData());
        String[] propertyValues = new String[]{"x", "x", "x", "x", "x"};
        int[] ids = new int[]{1, 2, 3, 4, 5};
        sendEvents(epService, ids, propertyValues);
        assertEventsReceived(updateListener, ids);
    }

    private void assertEventsReceived(SupportUpdateListener updateListener, int[] expectedIds) {
        assertEquals(1, updateListener.getLastNewData().length);
        assertNull(updateListener.getLastOldData());
        EventBean theEvent = updateListener.getLastNewData()[0];
        assertEquals(expectedIds[0], ((SupportBean_S0) theEvent.get("s0")).getId());
        assertEquals(expectedIds[1], ((SupportBean_S1) theEvent.get("s1")).getId());
        assertEquals(expectedIds[2], ((SupportBean_S2) theEvent.get("s2")).getId());
        assertEquals(expectedIds[3], ((SupportBean_S3) theEvent.get("s3")).getId());
        assertEquals(expectedIds[4], ((SupportBean_S4) theEvent.get("s4")).getId());
    }

    private void sendEvent(EPServiceProvider epService, Object theEvent) {
        epService.getEPRuntime().sendEvent(theEvent);
    }

    private void sendEvents(EPServiceProvider epService, int[] ids, String[] propertyValues) {
        sendEvent(epService, new SupportBean_S0(ids[0], propertyValues[0]));
        sendEvent(epService, new SupportBean_S1(ids[1], propertyValues[1]));
        sendEvent(epService, new SupportBean_S2(ids[2], propertyValues[2]));
        sendEvent(epService, new SupportBean_S3(ids[3], propertyValues[3]));
        sendEvent(epService, new SupportBean_S4(ids[4], propertyValues[4]));
    }

    private static final Logger log = LoggerFactory.getLogger(ExecJoin5StreamPerformance.class);
}
