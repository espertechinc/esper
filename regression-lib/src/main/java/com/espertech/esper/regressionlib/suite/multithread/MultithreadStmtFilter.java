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
import com.espertech.esper.regressionlib.support.bean.SupportCollection;
import com.espertech.esper.regressionlib.support.client.SupportCompileDeployUtil;
import com.espertech.esper.regressionlib.support.multithread.GeneratorIterator;
import com.espertech.esper.regressionlib.support.multithread.GeneratorIteratorCallback;
import com.espertech.esper.regressionlib.support.multithread.MTListener;
import com.espertech.esper.regressionlib.support.multithread.SendEventCallable;
import com.espertech.esper.regressionlib.support.util.SupportThreadFactory;
import org.junit.Assert;

import java.util.Arrays;
import java.util.Collection;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

/**
 * Test for multithread-safety for a simple aggregation case using count(*).
 */
public class MultithreadStmtFilter implements RegressionExecution {
    @Override
    public boolean excludeWhenInstrumented() {
        return true;
    }

    public void run(RegressionEnvironment env) {
        String plainFilter = "@name('s0') select count(*) as mycount from SupportBean";
        tryCount(env, 2, 1000, plainFilter, GeneratorIterator.DEFAULT_SUPPORTEBEAN_CB);
        tryCount(env, 4, 1000, plainFilter, GeneratorIterator.DEFAULT_SUPPORTEBEAN_CB);

        GeneratorIteratorCallback enumCallback = new GeneratorIteratorCallback() {
            private final Collection<String> vals = Arrays.asList("a", "b", "c", "d", "e", "f", "g", "h", "i", "j");

            public Object getObject(int numEvent) {
                SupportCollection bean = new SupportCollection();
                bean.setStrvals(vals);
                return bean;
            }
        };
        String enumFilter = "@name('s0') select count(*) as mycount from SupportCollection(strvals.anyOf(v => v = 'j'))";
        tryCount(env, 4, 1000, enumFilter, enumCallback);
    }

    private void tryCount(RegressionEnvironment env, int numThreads, int numMessages, String epl, GeneratorIteratorCallback generatorIteratorCallback) {
        ExecutorService threadPool = Executors.newFixedThreadPool(numThreads, new SupportThreadFactory(MultithreadStmtFilter.class));
        env.compileDeploy(epl);
        MTListener listener = new MTListener("mycount");
        env.statement("s0").addListener(listener);

        Future[] future = new Future[numThreads];
        for (int i = 0; i < numThreads; i++) {
            future[i] = threadPool.submit(new SendEventCallable(i, env.runtime(), new GeneratorIterator(numMessages, generatorIteratorCallback)));
        }

        threadPool.shutdown();
        SupportCompileDeployUtil.threadpoolAwait(threadPool, 10, TimeUnit.SECONDS);
        SupportCompileDeployUtil.assertFutures(future);

        // verify results
        Assert.assertEquals(numMessages * numThreads, listener.getValues().size());
        TreeSet<Integer> result = new TreeSet<Integer>();
        for (Object row : listener.getValues()) {
            result.add(((Number) row).intValue());
        }
        assertEquals(numMessages * numThreads, result.size());
        assertEquals(1, (Object) result.first());
        assertEquals(numMessages * numThreads, (Object) result.last());

        env.undeployAll();
    }
}
