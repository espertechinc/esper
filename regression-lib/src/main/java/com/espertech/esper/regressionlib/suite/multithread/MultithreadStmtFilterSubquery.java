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

import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.runtime.client.EPRuntime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.espertech.esper.regressionlib.support.client.SupportCompileDeployUtil.threadJoin;

/**
 * Test for multithread-safety for a simple aggregation case using count(*).
 */
public class MultithreadStmtFilterSubquery implements RegressionExecution {
    private static final Logger log = LoggerFactory.getLogger(MultithreadStmtFilterSubquery.class);

    @Override
    public boolean excludeWhenInstrumented() {
        return true;
    }

    public void run(RegressionEnvironment env) {
        tryNamedWindowFilterSubquery(env);
        tryStreamFilterSubquery(env);
    }

    private static void tryNamedWindowFilterSubquery(RegressionEnvironment env) {
        RegressionPath path = new RegressionPath();
        env.compileDeploy("create window MyWindow#keepall as SupportBean_S0", path);
        env.compileDeploy("insert into MyWindow select * from SupportBean_S0", path);

        String epl = "select * from pattern[SupportBean_S0 -> SupportBean(not exists (select * from MyWindow mw where mw.p00 = 'E'))]";
        env.compileDeploy(epl, path);
        env.sendEventBean(new SupportBean_S0(1));

        Thread insertThread = new Thread(new InsertRunnable(env.runtime(), 1000), MultithreadStmtFilterSubquery.class.getSimpleName() + "-insert");
        Thread filterThread = new Thread(new FilterRunnable(env.runtime(), 1000), MultithreadStmtFilterSubquery.class.getSimpleName() + "-filter");

        log.info("Starting threads");
        insertThread.start();
        filterThread.start();

        log.info("Waiting for join");
        threadJoin(insertThread);
        threadJoin(filterThread);

        env.undeployAll();
    }

    private static void tryStreamFilterSubquery(RegressionEnvironment env) {
        String epl = "select * from SupportBean(not exists (select * from SupportBean_S0#keepall mw where mw.p00 = 'E'))";
        env.compileDeploy(epl);

        Thread insertThread = new Thread(new InsertRunnable(env.runtime(), 1000), MultithreadStmtFilterSubquery.class.getSimpleName() + "-insert");
        Thread filterThread = new Thread(new FilterRunnable(env.runtime(), 1000), MultithreadStmtFilterSubquery.class.getSimpleName() + "-filter");

        log.info("Starting threads");
        insertThread.start();
        filterThread.start();

        log.info("Waiting for join");
        threadJoin(insertThread);
        threadJoin(filterThread);

        env.undeployAll();
    }

    public static class InsertRunnable implements Runnable {
        private final EPRuntime runtime;
        private final int numInserts;

        public InsertRunnable(EPRuntime runtime, int numInserts) {
            this.runtime = runtime;
            this.numInserts = numInserts;
        }

        public void run() {
            log.info("Starting insert thread");
            for (int i = 0; i < numInserts; i++) {
                runtime.getEventService().sendEventBean(new SupportBean_S0(i, "E"), "SupportBean_S0");
            }
            log.info("Completed insert thread, " + numInserts + " inserted");
        }
    }

    public static class FilterRunnable implements Runnable {
        private final EPRuntime runtime;
        private final int numEvents;

        public FilterRunnable(EPRuntime runtime, int numEvents) {
            this.runtime = runtime;
            this.numEvents = numEvents;
        }

        public void run() {
            log.info("Starting filter thread");
            for (int i = 0; i < numEvents; i++) {
                runtime.getEventService().sendEventBean(new SupportBean("G" + i, i), "SupportBean");
            }
            log.info("Completed filter thread, " + numEvents + " completed");
        }
    }
}
