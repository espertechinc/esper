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

import com.espertech.esper.common.client.EPCompiled;
import com.espertech.esper.common.client.fireandforget.EPFireAndForgetPreparedQuery;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.common.internal.support.SupportBean_S1;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionFlag;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.regressionlib.support.client.SupportCompileDeployUtil;
import com.espertech.esper.regressionlib.support.util.SupportThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertNull;

public class MultithreadFireAndForgetIndex implements RegressionExecution {
    private static final Logger log = LoggerFactory.getLogger(MultithreadFireAndForgetIndex.class);

    private final static int NUMREPEATS_QUERY = 10000;

    public void run(RegressionEnvironment env) {
        RegressionPath path = new RegressionPath();
        String epl = "@public create window MyWindow#keepall as (key string, value string);\n" +
                "create index MyIndex on MyWindow(key);\n" +
                "on SupportBean_S0 merge MyWindow insert select p00 as key, p01 as value;\n" +
                "on SupportBean_S1 as s1 delete from MyWindow as mw where mw.key = s1.p10;\n";
        env.compileDeploy(epl, path);
        sendS0(env, 1, "A");

        String faf = "select * from MyWindow where key = 'A' and value like '%hello%'";
        EPFireAndForgetPreparedQuery query = prepare(env, path, faf);

        ExecutorService threadPool = Executors.newFixedThreadPool(3, new SupportThreadFactory(MultithreadFireAndForgetIndex.class));
        QueryRunnable runnable = new QueryRunnable(NUMREPEATS_QUERY, query);
        threadPool.submit(runnable);

        for (int i = 0; i < NUMREPEATS_QUERY; i++) {
            sendS0(env, 0, "A");
            sendS1(env, 0, "A");
        }

        threadPool.shutdown();
        SupportCompileDeployUtil.threadpoolAwait(threadPool, 10, TimeUnit.SECONDS);
        assertNull(runnable.exception);

        env.undeployAll();
    }

    private void sendS0(RegressionEnvironment env, int id, String p00) {
        env.sendEventBean(new SupportBean_S0(id, p00));
    }

    private void sendS1(RegressionEnvironment env, int id, String p10) {
        env.sendEventBean(new SupportBean_S1(id, p10));
    }

    private EPFireAndForgetPreparedQuery prepare(RegressionEnvironment env, RegressionPath path, String faf) {
        EPCompiled compiled = env.compileFAF(faf, path);
        return env.runtime().getFireAndForgetService().prepareQuery(compiled);
    }

    public EnumSet<RegressionFlag> flags() {
        return EnumSet.of(RegressionFlag.EXCLUDEWHENINSTRUMENTED, RegressionFlag.MULTITHREADED);
    }

    public class QueryRunnable implements Runnable {
        private final int numRepeats;
        private final EPFireAndForgetPreparedQuery query;
        private RuntimeException exception;

        public QueryRunnable(int numRepeats, EPFireAndForgetPreparedQuery query) {
            this.numRepeats = numRepeats;
            this.query = query;
        }

        public void run() {
            try {
                for (int i = 0; i < numRepeats; i++) {
                    query.execute();
                }
            } catch (Exception ex) {
                log.error("Error in thread " + Thread.currentThread().getId(), ex);
                this.exception = exception;
            }
        }

        public RuntimeException getException() {
            return exception;
        }
    }
}
