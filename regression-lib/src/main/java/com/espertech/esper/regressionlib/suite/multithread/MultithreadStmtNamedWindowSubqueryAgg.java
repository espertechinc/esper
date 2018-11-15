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
import com.espertech.esper.regressionlib.support.multithread.StmtNamedWindowSubqueryAggCallable;
import com.espertech.esper.regressionlib.support.util.SupportThreadFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

/**
 * Test for multithread-safety and named window subqueries and aggregation.
 */
public class MultithreadStmtNamedWindowSubqueryAgg implements RegressionExecution {

    @Override
    public boolean excludeWhenInstrumented() {
        return true;
    }

    public void run(RegressionEnvironment env) {
        trySend(env, 3, 1000, false);
        trySend(env, 3, 1000, true);
    }

    private static void trySend(RegressionEnvironment env, int numThreads, int numEventsPerThread, boolean indexShare) {
        // setup statements
        RegressionPath path = new RegressionPath();
        String schemas = "create schema UpdateEvent as (uekey string, ueint int);\n" +
            "create schema WindowSchema as (wskey string, wsint int);\n";
        env.compileDeployWBusPublicType(schemas, path);

        String createEpl = "@name('namedWindow') create window MyWindow#keepall as WindowSchema";
        if (indexShare) {
            createEpl = "@Hint('enable_window_subquery_indexshare') " + createEpl;
        }
        env.compileDeploy(createEpl, path);

        env.compileDeploy("create index ABC on MyWindow(wskey)", path);
        env.compileDeploy("on UpdateEvent mue merge MyWindow mw " +
            "where uekey = wskey and ueint = wsint " +
            "when not matched then insert select uekey as wskey, ueint as wsint " +
            "when matched then delete", path);
        // note: here all threads use the same string key to insert/delete and different values for the int
        env.compileDeploy("@name('target') select (select intListAgg(wsint) from MyWindow mw where wskey = sb.theString) as val from SupportBean sb", path);

        // execute
        ExecutorService threadPool = Executors.newFixedThreadPool(numThreads, new SupportThreadFactory(MultithreadStmtNamedWindowSubqueryAgg.class));
        Future<Boolean>[] future = new Future[numThreads];
        for (int i = 0; i < numThreads; i++) {
            future[i] = threadPool.submit(new StmtNamedWindowSubqueryAggCallable(i, env.runtime(), numEventsPerThread, env.statement("target")));
        }

        threadPool.shutdown();
        SupportCompileDeployUtil.threadpoolAwait(threadPool, 10, TimeUnit.SECONDS);
        SupportCompileDeployUtil.assertFutures(future);

        EventBean[] events = EPAssertionUtil.iteratorToArray(env.statement("namedWindow").iterator());
        assertEquals(0, events.length);

        env.undeployAll();
    }
}
