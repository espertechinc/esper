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
import com.espertech.esper.common.client.fireandforget.EPFireAndForgetPreparedQueryParameterized;
import com.espertech.esper.common.client.fireandforget.EPFireAndForgetQueryResult;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionFlag;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.regressionlib.support.util.SupportThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;
import java.util.concurrent.*;
import java.util.function.Consumer;

import static com.espertech.esper.regressionlib.support.client.SupportCompileDeployUtil.*;
import static org.junit.Assert.*;

/**
 * Test for multithread-safety of named windows and fire-and-forget queries:
 * This test has a single inserting thread that produces unique id-numbers from 0 to N.
 * The test has multiple delete-threads that each poll for a just-inserted number and issue a FAF-delete.
 */
public class MultithreadStmtNamedWindowFAFDelete implements RegressionExecution {
    private final Logger log = LoggerFactory.getLogger(MultithreadStmtNamedWindowFAFDelete.class);

    @Override
    public EnumSet<RegressionFlag> flags() {
        return EnumSet.of(RegressionFlag.EXCLUDEWHENINSTRUMENTED, RegressionFlag.MULTITHREADED);
    }

    public void run(RegressionEnvironment env) {
        final int NUM_DELETE_THREADS = 4;
        final long MILLISEC_DURATION = 2000;

        RegressionPath path = new RegressionPath();
        env.compileDeploy("@public create window MyWindow#unique(id) as SupportBean_S0", path);
        env.compileDeploy("insert into MyWindow select * from SupportBean_S0", path);

        ConcurrentLinkedQueue<Integer> ids = new ConcurrentLinkedQueue<>();

        // insert into the named window producing new int-ids
        InsertRunnable insertRunnable = new InsertRunnable(env, ids::add);
        Thread insertThread = new Thread(insertRunnable);
        insertThread.start();

        // delete those that were inserted with FAF query
        ExecutorService threadPool = Executors.newFixedThreadPool(NUM_DELETE_THREADS, new SupportThreadFactory(MultithreadStmtNamedWindowFAFDelete.class));
        Future<Boolean>[] future = new Future[NUM_DELETE_THREADS];
        DeleteCallable[] callables = new DeleteCallable[NUM_DELETE_THREADS];
        for (int i = 0; i < NUM_DELETE_THREADS; i++) {
            callables[i] = new DeleteCallable(env, path, ids);
            future[i] = threadPool.submit(callables[i]);
        }

        // wait a little
        try {
            Thread.sleep(MILLISEC_DURATION);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        // shutdown insert
        insertRunnable.setShutdown(true);
        try {
            insertThread.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        assertNull(insertRunnable.throwable);

        // shutdown delete
        for (DeleteCallable deleteCallable : callables) {
            deleteCallable.setShutdown(true);
        }
        threadPool.shutdown();
        threadpoolAwait(threadPool, 10, TimeUnit.SECONDS);
        threadSleep(100);
        assertFutures(future);

        int countDeleted = 0;
        for (DeleteCallable deleteCallable : callables) {
            countDeleted += deleteCallable.getNumDeletes();
        }
        assertTrue(insertRunnable.numInserts > 1000);
        assertTrue(countDeleted > 100);

        env.undeployAll();
    }

    public class DeleteCallable implements Callable<Boolean> {
        private final RegressionEnvironment env;
        private final RegressionPath path;
        private final ConcurrentLinkedQueue<Integer> queue;
        private int numDeletes;
        private boolean shutdown;

        public DeleteCallable(RegressionEnvironment env, RegressionPath path, ConcurrentLinkedQueue<Integer> queue) {
            this.env = env;
            this.path = path;
            this.queue = queue;
        }

        public Boolean call() throws Exception {
            try {
                String fafDelete = "delete from MyWindow where id = ?::int";
                EPCompiled compiled = env.compileFAF(fafDelete, path);
                EPFireAndForgetPreparedQueryParameterized queryDelete = env.runtime().getFireAndForgetService().prepareQueryWithParameters(compiled);

                while(!shutdown) {
                    Integer next = queue.poll();
                    if (next == null) {
                        continue;
                    }

                    queryDelete.setObject(1, next);
                    EPFireAndForgetQueryResult queryResult = env.runtime().getFireAndForgetService().executeQuery(queryDelete);
                    int numDeleted = queryResult.getArray().length;
                    assertEquals(1, numDeleted);
                    numDeletes++;
                }
            } catch (Exception ex) {
                log.error("Error in thread " + Thread.currentThread().getId(), ex);
                return false;
            }
            return true;
        }

        public void setShutdown(boolean shutdown) {
            this.shutdown = shutdown;
        }

        public int getNumDeletes() {
            return numDeletes;
        }
    }

    private static class InsertRunnable implements Runnable {
        private final RegressionEnvironment env;
        private final Consumer<Integer> idConsumer;
        private Throwable throwable;
        private boolean shutdown;
        private int numInserts;

        public InsertRunnable(RegressionEnvironment env, Consumer<Integer> idConsumer) {
            this.env = env;
            this.idConsumer = idConsumer;
        }

        public void run() {
            try {
                while(!shutdown) {
                    int id = numInserts++;
                    env.sendEventBean(new SupportBean_S0(id));
                    idConsumer.accept(id);
                }
            } catch (Throwable t) {
                this.throwable = t;
            }
        }

        public void setShutdown(boolean shutdown) {
            this.shutdown = shutdown;
        }
    }
}
