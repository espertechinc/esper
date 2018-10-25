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
package com.espertech.esper.regressionlib.suite.multithread;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.configuration.Configuration;
import com.espertech.esper.common.client.util.FilterServiceProfile;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.regressionlib.support.client.SupportCompileDeployUtil;
import com.espertech.esper.regressionlib.support.util.SupportMTUpdateListener;
import com.espertech.esper.runtime.client.*;

import static com.espertech.esper.regressionlib.support.client.SupportCompileDeployUtil.threadJoin;
import static org.junit.Assert.assertEquals;

public class MultithreadStmtPatternFollowedBy {

    public void run(Configuration configuration) {
        runAssertionPatternFollowedBy(FilterServiceProfile.READMOSTLY, configuration);
        runAssertionPatternFollowedBy(FilterServiceProfile.READWRITE, configuration);
    }

    private static void runAssertionPatternFollowedBy(FilterServiceProfile profile, Configuration config) {

        config.getCommon().addEventType("S0", SupportBean_S0.class);
        String runtimeURI = MultithreadStmtPatternFollowedBy.class.getSimpleName() + "_" + profile;
        EPRuntime runtime = EPRuntimeProvider.getRuntime(runtimeURI, config);
        runtime.initialize();

        String[] epls = {
            "select sa.id,sb.id,sc.id,sd.id from pattern [(sa=S0(id=0)->sb=S0(id=1)) or (sc=S0(id=1)->sd=S0(id=0))]",
            "select sa.id,sb.id,sc.id,sd.id from pattern [(sa=S0(id=1)->sb=S0(id=2)) or (sc=S0(id=2)->sd=S0(id=1))]",
            "select sa.id,sb.id,sc.id,sd.id from pattern [(sa=S0(id=2)->sb=S0(id=3)) or (sc=S0(id=3)->sd=S0(id=2))]",
            "select sa.id,sb.id,sc.id,sd.id from pattern [(sa=S0(id=3)->sb=S0(id=4)) or (sc=S0(id=4)->sd=S0(id=3))]",
            "select sa.id,sb.id,sc.id,sd.id from pattern [(sa=S0(id=4)->sb=S0(id=5)) or (sc=S0(id=5)->sd=S0(id=4))]",
            "select sa.id,sb.id,sc.id,sd.id from pattern [(sa=S0(id=5)->sb=S0(id=6)) or (sc=S0(id=6)->sd=S0(id=5))]",
            "select sa.id,sb.id,sc.id,sd.id from pattern [(sa=S0(id=6)->sb=S0(id=7)) or (sc=S0(id=7)->sd=S0(id=6))]",
            "select sa.id,sb.id,sc.id,sd.id from pattern [(sa=S0(id=7)->sb=S0(id=8)) or (sc=S0(id=8)->sd=S0(id=7))]",
            "select sa.id,sb.id,sc.id,sd.id from pattern [(sa=S0(id=8)->sb=S0(id=9)) or (sc=S0(id=9)->sd=S0(id=8))]"
        };

        for (int i = 0; i < 20; i++) {
            System.out.println("i=" + i);
            SupportMTUpdateListener listener = new SupportMTUpdateListener();
            EPStatement[] stmts = new EPStatement[epls.length];
            for (int j = 0; j < epls.length; j++) {
                EPDeployment deployed = SupportCompileDeployUtil.compileDeploy(epls[j], runtime, config);
                stmts[j] = deployed.getStatements()[0];
                stmts[j].addListener(listener);
            }

            int[] threadOneValues = new int[]{0, 2, 4, 6, 8};
            int[] threadTwoValues = new int[]{1, 3, 5, 7, 9};

            Thread threadOne = new Thread(new SenderRunnable(runtime.getEventService(), threadOneValues), MultithreadStmtPatternFollowedBy.class.getSimpleName() + "-one");
            Thread threadTwo = new Thread(new SenderRunnable(runtime.getEventService(), threadTwoValues), MultithreadStmtPatternFollowedBy.class.getSimpleName() + "-two");

            threadOne.start();
            threadTwo.start();
            threadJoin(threadOne);
            threadJoin(threadTwo);

            EventBean[] events = listener.getNewDataListFlattened();
            /* Comment in to print events delivered.
            for (int j = 0; j < events.length; j++) {
                EventBean out = events[j];
                /*
                System.out.println(" sa=" + getNull(out.get("sa.id")) +
                                   " sb=" + getNull(out.get("sb.id")) +
                                   " sc=" + getNull(out.get("sc.id")) +
                                   " sd=" + getNull(out.get("sd.id")));
            }
             */
            assertEquals(9, events.length);

            for (int j = 0; j < epls.length; j++) {
                try {
                    runtime.getDeploymentService().undeploy(stmts[j].getDeploymentId());
                } catch (EPUndeployException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        runtime.destroy();
    }

    public static class SenderRunnable implements Runnable {

        private final EPEventService runtime;
        private final int[] values;

        public SenderRunnable(EPEventService runtime, int[] values) {
            this.runtime = runtime;
            this.values = values;
        }

        public void run() {
            for (int i = 0; i < values.length; i++) {
                runtime.sendEventBean(new SupportBean_S0(values[i]), "S0");
            }
        }
    }

    private String getNull(Object value) {
        if (value == null) {
            return "-";
        }
        return value.toString();
    }
}
