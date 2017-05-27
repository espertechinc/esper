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
import com.espertech.esper.supportregression.bean.SupportBean_A;
import com.espertech.esper.supportregression.bean.SupportBean_B;
import com.espertech.esper.supportregression.bean.SupportBean_C;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertTrue;

public class ExecJoin3StreamAndPropertyPerformance implements RegressionExecution {
    public void run(EPServiceProvider epService) throws Exception {
        runAssertionPerfAllProps(epService);
        runAssertionPerfPartialProps(epService);
        runAssertionPerfPartialStreams(epService);
    }

    private void runAssertionPerfAllProps(EPServiceProvider epService) {
        // Statement where all streams are reachable from each other via properties
        String stmt = "select * from " +
                SupportBean_A.class.getName() + "()#length(1000000) s1," +
                SupportBean_B.class.getName() + "()#length(1000000) s2," +
                SupportBean_C.class.getName() + "()#length(1000000) s3" +
                " where s1.id=s2.id and s2.id=s3.id and s1.id=s3.id";
        tryJoinPerf3Streams(epService, stmt);
    }

    private void runAssertionPerfPartialProps(EPServiceProvider epService) {
        // Statement where the s1 stream is not reachable by joining s2 to s3 and s3 to s1
        String stmt = "select * from " +
                SupportBean_A.class.getName() + "#length(1000000) s1," +
                SupportBean_B.class.getName() + "#length(1000000) s2," +
                SupportBean_C.class.getName() + "#length(1000000) s3" +
                " where s1.id=s2.id and s2.id=s3.id";   // ==> therefore s1.id = s3.id
        tryJoinPerf3Streams(epService, stmt);
    }

    private void runAssertionPerfPartialStreams(EPServiceProvider epService) {
        String methodName = ".testPerfPartialStreams";

        // Statement where the s1 stream is not reachable by joining s2 to s3 and s3 to s1
        String epl = "select * from " +
                SupportBean_A.class.getName() + "()#length(1000000) s1," +
                SupportBean_B.class.getName() + "()#length(1000000) s2," +
                SupportBean_C.class.getName() + "()#length(1000000) s3" +
                " where s1.id=s2.id";   // ==> stream s3 no properties supplied, full s3 scan

        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener updateListener = new SupportUpdateListener();
        stmt.addListener(updateListener);

        // preload s3 with just 1 event
        sendEvent(epService, new SupportBean_C("GE_0"));

        // Send events for each stream
        log.info(methodName + " Preloading events");
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 1000; i++) {
            sendEvent(epService, new SupportBean_A("CSCO_" + i));
            sendEvent(epService, new SupportBean_B("IBM_" + i));
        }
        log.info(methodName + " Done preloading");

        long endTime = System.currentTimeMillis();
        log.info(methodName + " delta=" + (endTime - startTime));

        // Stay below 500, no index would be 4 sec plus
        assertTrue((endTime - startTime) < 500);
        stmt.destroy();
    }

    private void tryJoinPerf3Streams(EPServiceProvider epService, String epl) {
        String methodName = ".tryJoinPerf3Streams";

        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener updateListener = new SupportUpdateListener();
        stmt.addListener(updateListener);

        // Send events for each stream
        log.info(methodName + " Preloading events");
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 100; i++) {
            sendEvent(epService, new SupportBean_A("CSCO_" + i));
            sendEvent(epService, new SupportBean_B("IBM_" + i));
            sendEvent(epService, new SupportBean_C("GE_" + i));
        }
        log.info(methodName + " Done preloading");

        long endTime = System.currentTimeMillis();
        log.info(methodName + " delta=" + (endTime - startTime));

        // Stay below 500, no index would be 4 sec plus
        assertTrue((endTime - startTime) < 500);

        stmt.destroy();
    }

    private void sendEvent(EPServiceProvider epService, Object theEvent) {
        epService.getEPRuntime().sendEvent(theEvent);
    }

    private static final Logger log = LoggerFactory.getLogger(ExecJoin3StreamAndPropertyPerformance.class);
}
