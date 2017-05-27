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
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ExecJoin3StreamCoercionPerformance implements RegressionExecution {
    public void run(EPServiceProvider epService) throws Exception {
        runAssertionPerfCoercion3waySceneOne(epService);
        runAssertionPerfCoercion3waySceneTwo(epService);
        runAssertionPerfCoercion3waySceneThree(epService);
    }

    private void runAssertionPerfCoercion3waySceneOne(EPServiceProvider epService) {
        String stmtText = "select s1.intBoxed as value from " +
                SupportBean.class.getName() + "(theString='A')#length(1000000) s1," +
                SupportBean.class.getName() + "(theString='B')#length(1000000) s2," +
                SupportBean.class.getName() + "(theString='C')#length(1000000) s3" +
                " where s1.intBoxed=s2.longBoxed and s1.intBoxed=s3.doubleBoxed";

        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        // preload
        for (int i = 0; i < 10000; i++) {
            sendEvent(epService, "B", 0, i, 0);
            sendEvent(epService, "C", 0, 0, i);
        }

        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 5000; i++) {
            int index = 5000 + i % 1000;
            sendEvent(epService, "A", index, 0, 0);
            assertEquals(index, listener.assertOneGetNewAndReset().get("value"));
        }
        long endTime = System.currentTimeMillis();
        long delta = endTime - startTime;

        assertTrue("Failed perf test, delta=" + delta, delta < 1500);
        stmt.destroy();
    }

    private void runAssertionPerfCoercion3waySceneTwo(EPServiceProvider epService) {
        String stmtText = "select s1.intBoxed as value from " +
                SupportBean.class.getName() + "(theString='A')#length(1000000) s1," +
                SupportBean.class.getName() + "(theString='B')#length(1000000) s2," +
                SupportBean.class.getName() + "(theString='C')#length(1000000) s3" +
                " where s1.intBoxed=s2.longBoxed and s1.intBoxed=s3.doubleBoxed";

        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        // preload
        for (int i = 0; i < 10000; i++) {
            sendEvent(epService, "A", i, 0, 0);
            sendEvent(epService, "B", 0, i, 0);
        }

        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 5000; i++) {
            int index = 5000 + i % 1000;
            sendEvent(epService, "C", 0, 0, index);
            assertEquals(index, listener.assertOneGetNewAndReset().get("value"));
        }
        long endTime = System.currentTimeMillis();
        long delta = endTime - startTime;

        stmt.destroy();
        assertTrue("Failed perf test, delta=" + delta, delta < 1500);
    }

    private void runAssertionPerfCoercion3waySceneThree(EPServiceProvider epService) {
        String stmtText = "select s1.intBoxed as value from " +
                SupportBean.class.getName() + "(theString='A')#length(1000000) s1," +
                SupportBean.class.getName() + "(theString='B')#length(1000000) s2," +
                SupportBean.class.getName() + "(theString='C')#length(1000000) s3" +
                " where s1.intBoxed=s2.longBoxed and s1.intBoxed=s3.doubleBoxed";

        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        // preload
        for (int i = 0; i < 10000; i++) {
            sendEvent(epService, "A", i, 0, 0);
            sendEvent(epService, "C", 0, 0, i);
        }

        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 5000; i++) {
            int index = 5000 + i % 1000;
            sendEvent(epService, "B", 0, index, 0);
            assertEquals(index, listener.assertOneGetNewAndReset().get("value"));
        }
        long endTime = System.currentTimeMillis();
        long delta = endTime - startTime;

        stmt.destroy();
        assertTrue("Failed perf test, delta=" + delta, delta < 1500);
    }

    private void sendEvent(EPServiceProvider epService, String theString, int intBoxed, long longBoxed, double doubleBoxed) {
        SupportBean bean = new SupportBean();
        bean.setTheString(theString);
        bean.setIntBoxed(intBoxed);
        bean.setLongBoxed(longBoxed);
        bean.setDoubleBoxed(doubleBoxed);
        epService.getEPRuntime().sendEvent(bean);
    }

    private static final Logger log = LoggerFactory.getLogger(ExecJoin3StreamCoercionPerformance.class);
}
