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
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.regressionlib.support.client.SupportCompileDeployUtil;
import com.espertech.esper.regressionlib.support.multithread.StmtNamedWindowSubqueryLookupCallable;
import com.espertech.esper.regressionlib.support.util.SupportThreadFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

/**
 * Test for multithread-safety and named window subqueries and direct index-based lookup.
 */
public class MultithreadStmtNamedWindowSubqueryLookup implements RegressionExecution {

    @Override
    public boolean excludeWhenInstrumented() {
        return true;
    }

    public void run(RegressionEnvironment env) {
        trySend(env, 3, 10000);
    }

    private static void trySend(RegressionEnvironment env, int numThreads, int numEventsPerThread) {
        RegressionPath path = new RegressionPath();
        String schemas = "create schema MyUpdateEvent as (key string, intupd int);\n" +
            "create schema MySchema as (theString string, intval int);\n";
        env.compileDeployWBusPublicType(schemas, path);

        env.compileDeploy("@name('window') create window MyWindow#keepall as MySchema", path);
        env.compileDeploy("on MyUpdateEvent mue merge MyWindow mw " +
            "where mw.theString = mue.key " +
            "when not matched then insert select key as theString, intupd as intval " +
            "when matched then delete", path);
        env.compileDeploy("@name('target') select (select intval from MyWindow mw where mw.theString = sb.theString) as val from SupportBean sb", path);

        // execute
        ExecutorService threadPool = Executors.newFixedThreadPool(numThreads, new SupportThreadFactory(MultithreadStmtNamedWindowSubqueryLookup.class));
        Future<Boolean>[] future = new Future[numThreads];
        for (int i = 0; i < numThreads; i++) {
            future[i] = threadPool.submit(new StmtNamedWindowSubqueryLookupCallable(i, env.runtime(), numEventsPerThread, env.statement("target")));
        }

        threadPool.shutdown();
        SupportCompileDeployUtil.threadpoolAwait(threadPool, 10, TimeUnit.SECONDS);
        SupportCompileDeployUtil.assertFutures(future);

        EventBean[] events = EPAssertionUtil.iteratorToArray(env.iterator("window"));
        assertEquals(0, events.length);

        env.undeployAll();
    }
}
