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
package com.espertech.esper.regressionlib.suite.infra.tbl;

import com.espertech.esper.common.client.EPCompiled;
import com.espertech.esper.common.client.fireandforget.EPFireAndForgetPreparedQueryParameterized;
import com.espertech.esper.common.client.fireandforget.EPFireAndForgetQueryResult;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * NOTE: More table-related tests in "nwtable"
 */
public class InfraTableMTGroupedFAFReadFAFWriteChain implements RegressionExecution {
    private static final Logger log = LoggerFactory.getLogger(InfraTableMTGroupedFAFReadFAFWriteChain.class);

    @Override
    public boolean excludeWhenInstrumented() {
        return true;
    }

    /**
     * Tests fire-and-forget lock cleanup:
     * create table MyTable(key int primary key, p0 int)   (5 props)
     * <p>
     * The following threads are in a chain communicating by queue holding key values:
     * - Insert: populates MyTable={key=N, p0=N}, last row indicated by -1
     * - Select-Table-Access: select MyTable[N].p0 from SupportBean
     */
    public void run(RegressionEnvironment env) {
        try {
            tryMT(env, 1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private static void tryMT(RegressionEnvironment env, int numInserted) throws InterruptedException {

        RegressionPath path = new RegressionPath();
        String epl = "create table MyTable (key int primary key, p0 int);";
        env.compileDeploy(epl, path);

        List<BaseRunnable> runnables = new ArrayList<BaseRunnable>();

        LinkedBlockingDeque<Integer> insertOutQ = new LinkedBlockingDeque<Integer>();
        InsertRunnable insert = new InsertRunnable(env, path, numInserted, insertOutQ);
        runnables.add(insert);

        LinkedBlockingDeque<Integer> selectOutQ = new LinkedBlockingDeque<Integer>();
        SelectRunnable select = new SelectRunnable(env, path, insertOutQ, selectOutQ);
        runnables.add(select);

        LinkedBlockingDeque<Integer> updateOutQ = new LinkedBlockingDeque<Integer>();
        UpdateRunnable update = new UpdateRunnable(env, path, selectOutQ, updateOutQ);
        runnables.add(update);

        LinkedBlockingDeque<Integer> deleteOutQ = new LinkedBlockingDeque<Integer>();
        DeleteRunnable delete = new DeleteRunnable(env, path, updateOutQ, deleteOutQ);
        runnables.add(delete);

        // start
        Thread[] threads = new Thread[runnables.size()];
        for (int i = 0; i < runnables.size(); i++) {
            threads[i] = new Thread(runnables.get(i), InfraTableMTGroupedFAFReadFAFWriteChain.class.getSimpleName() + "-" + i);
            threads[i].start();
        }

        // join
        for (Thread t : threads) {
            t.join();
        }

        env.undeployAll();

        // assert
        for (BaseRunnable runnable : runnables) {
            assertNull(runnable.getException());
            assertEquals("failed for " + runnable, numInserted + 1, runnable.getNumberOfOperations());    // account for -1 indicator
        }
    }

    public abstract static class BaseRunnable implements Runnable {
        protected final RegressionEnvironment env;
        protected final RegressionPath path;
        protected final String workName;
        protected int numberOfOperations;
        private Exception exception;

        protected BaseRunnable(RegressionEnvironment env, RegressionPath path, String workName) {
            this.env = env;
            this.path = path;
            this.workName = workName;
        }

        public abstract void runWork() throws InterruptedException;

        public final void run() {
            log.info("Starting " + workName);
            try {
                runWork();
            } catch (Exception ex) {
                log.error("Exception encountered: " + ex.getMessage(), ex);
                exception = ex;
            }
            log.info("Completed " + workName);
        }

        public Exception getException() {
            return exception;
        }

        public int getNumberOfOperations() {
            return numberOfOperations;
        }
    }

    public static class InsertRunnable extends BaseRunnable {
        private final int numInserted;
        private final Queue<Integer> stageOutput;

        public InsertRunnable(RegressionEnvironment env, RegressionPath path, int numInserted, Queue<Integer> stageOutput) {
            super(env, path, "Insert");
            this.numInserted = numInserted;
            this.stageOutput = stageOutput;
        }

        public void runWork() {
            EPCompiled compiled = env.compileFAF("insert into MyTable (key, p0) values (cast(?, int), cast(?, int))", path);
            EPFireAndForgetPreparedQueryParameterized q = env.runtime().getFireAndForgetService().prepareQueryWithParameters(compiled);
            for (int i = 0; i < numInserted; i++) {
                process(q, i);
            }
            process(q, -1);
        }

        private void process(EPFireAndForgetPreparedQueryParameterized q, int id) {
            q.setObject(1, id);
            q.setObject(2, id);
            env.runtime().getFireAndForgetService().executeQuery(q);
            stageOutput.add(id);
            numberOfOperations++;
        }
    }

    public static class SelectRunnable extends BaseRunnable {
        private final BlockingQueue<Integer> stageInput;
        private final Queue<Integer> stageOutput;

        public SelectRunnable(RegressionEnvironment env, RegressionPath path, BlockingQueue<Integer> stageInput, Queue<Integer> stageOutput) {
            super(env, path, "Select");
            this.stageInput = stageInput;
            this.stageOutput = stageOutput;
        }

        public void runWork() throws InterruptedException {
            String epl = "select p0 from MyTable where key = cast(?, int)";
            EPCompiled compiled = env.compileFAF(epl, path);
            EPFireAndForgetPreparedQueryParameterized q = env.runtime().getFireAndForgetService().prepareQueryWithParameters(compiled);
            while (true) {
                int id = stageInput.take();
                process(q, id);
                if (id == -1) {
                    break;
                }
            }
        }

        private void process(EPFireAndForgetPreparedQueryParameterized q, int id) {
            q.setObject(1, id);
            EPFireAndForgetQueryResult result = env.runtime().getFireAndForgetService().executeQuery(q);
            assertEquals("failed for id " + id, 1, result.getArray().length);
            assertEquals(id, result.getArray()[0].get("p0"));
            stageOutput.add(id);
            numberOfOperations++;
        }
    }

    public static class UpdateRunnable extends BaseRunnable {
        private final BlockingQueue<Integer> stageInput;
        private final Queue<Integer> stageOutput;

        public UpdateRunnable(RegressionEnvironment env, RegressionPath path, BlockingQueue<Integer> stageInput, Queue<Integer> stageOutput) {
            super(env, path, "Update");
            this.stageInput = stageInput;
            this.stageOutput = stageOutput;
        }

        public void runWork() throws InterruptedException {
            String epl = "update MyTable set p0 = 99999999 where key = cast(?, int)";
            EPCompiled compiled = env.compileFAF(epl, path);
            EPFireAndForgetPreparedQueryParameterized q = env.runtime().getFireAndForgetService().prepareQueryWithParameters(compiled);
            while (true) {
                int id = stageInput.take();
                process(q, id);
                if (id == -1) {
                    break;
                }
            }
        }

        private void process(EPFireAndForgetPreparedQueryParameterized q, int id) {
            q.setObject(1, id);
            env.runtime().getFireAndForgetService().executeQuery(q);
            stageOutput.add(id);
            numberOfOperations++;
        }
    }

    public static class DeleteRunnable extends BaseRunnable {
        private final BlockingQueue<Integer> stageInput;
        private final Queue<Integer> stageOutput;

        public DeleteRunnable(RegressionEnvironment env, RegressionPath path, BlockingQueue<Integer> stageInput, Queue<Integer> stageOutput) {
            super(env, path, "Delete");
            this.stageInput = stageInput;
            this.stageOutput = stageOutput;
        }

        public void runWork() throws InterruptedException {
            String epl = "delete from MyTable where key = cast(?, int)";
            EPCompiled compiled = env.compileFAF(epl, path);
            EPFireAndForgetPreparedQueryParameterized q = env.runtime().getFireAndForgetService().prepareQueryWithParameters(compiled);
            while (true) {
                int id = stageInput.take();
                process(q, id);
                if (id == -1) {
                    break;
                }
            }
        }

        private void process(EPFireAndForgetPreparedQueryParameterized q, int id) {
            q.setObject(1, id);
            env.runtime().getFireAndForgetService().executeQuery(q);
            stageOutput.add(id);
            numberOfOperations++;
        }
    }
}
