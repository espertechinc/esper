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
package com.espertech.esper.regression.nwtable.tbl;

import com.espertech.esper.client.EPOnDemandPreparedQueryParameterized;
import com.espertech.esper.client.EPOnDemandQueryResult;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ExecTableMTGroupedFAFReadFAFWriteChain implements RegressionExecution {
    private static final Logger log = LoggerFactory.getLogger(ExecTableMTGroupedFAFReadFAFWriteChain.class);

    /**
     * Tests fire-and-forget lock cleanup:
     * create table MyTable(key int primary key, p0 int)   (5 props)
     * <p>
     * The following threads are in a chain communicating by queue holding key values:
     * - Insert: populates MyTable={key=N, p0=N}, last row indicated by -1
     * - Select-Table-Access: select MyTable[N].p0 from SupportBean
     */
    public void run(EPServiceProvider epService) throws Exception {
        tryMT(epService, 1000);
    }

    private void tryMT(EPServiceProvider epService, int numInserted) throws Exception {

        String epl = "create table MyTable (key int primary key, p0 int);";
        epService.getEPAdministrator().getDeploymentAdmin().parseDeploy(epl);

        List<BaseRunnable> runnables = new ArrayList<BaseRunnable>();

        LinkedBlockingDeque<Integer> insertOutQ = new LinkedBlockingDeque<Integer>();
        InsertRunnable insert = new InsertRunnable(epService, numInserted, insertOutQ);
        runnables.add(insert);

        LinkedBlockingDeque<Integer> selectOutQ = new LinkedBlockingDeque<Integer>();
        SelectRunnable select = new SelectRunnable(epService, insertOutQ, selectOutQ);
        runnables.add(select);

        LinkedBlockingDeque<Integer> updateOutQ = new LinkedBlockingDeque<Integer>();
        UpdateRunnable update = new UpdateRunnable(epService, selectOutQ, updateOutQ);
        runnables.add(update);

        LinkedBlockingDeque<Integer> deleteOutQ = new LinkedBlockingDeque<Integer>();
        DeleteRunnable delete = new DeleteRunnable(epService, updateOutQ, deleteOutQ);
        runnables.add(delete);

        // start
        Thread[] threads = new Thread[runnables.size()];
        for (int i = 0; i < runnables.size(); i++) {
            threads[i] = new Thread(runnables.get(i));
            threads[i].start();
        }

        // join
        for (Thread t : threads) {
            t.join();
        }

        // assert
        for (BaseRunnable runnable : runnables) {
            assertNull(runnable.getException());
            assertEquals("failed for " + runnable, numInserted + 1, runnable.getNumberOfOperations());    // account for -1 indicator
        }
    }

    public abstract static class BaseRunnable implements Runnable {
        protected final EPServiceProvider epService;
        protected final String workName;
        protected int numberOfOperations;
        private Exception exception;

        protected BaseRunnable(EPServiceProvider epService, String workName) {
            this.epService = epService;
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

        public InsertRunnable(EPServiceProvider epService, int numInserted, Queue<Integer> stageOutput) {
            super(epService, "Insert");
            this.numInserted = numInserted;
            this.stageOutput = stageOutput;
        }

        public void runWork() {
            EPOnDemandPreparedQueryParameterized q = epService.getEPRuntime().prepareQueryWithParameters("insert into MyTable (key, p0) values (?, ?)");
            for (int i = 0; i < numInserted; i++) {
                process(q, i);
            }
            process(q, -1);
        }

        private void process(EPOnDemandPreparedQueryParameterized q, int id) {
            q.setObject(1, id);
            q.setObject(2, id);
            epService.getEPRuntime().executeQuery(q);
            stageOutput.add(id);
            numberOfOperations++;
        }
    }

    public static class SelectRunnable extends BaseRunnable {
        private final BlockingQueue<Integer> stageInput;
        private final Queue<Integer> stageOutput;

        public SelectRunnable(EPServiceProvider epService, BlockingQueue<Integer> stageInput, Queue<Integer> stageOutput) {
            super(epService, "Select");
            this.stageInput = stageInput;
            this.stageOutput = stageOutput;
        }

        public void runWork() throws InterruptedException {
            String epl = "select p0 from MyTable where key = ?";
            EPOnDemandPreparedQueryParameterized q = epService.getEPRuntime().prepareQueryWithParameters(epl);
            while (true) {
                int id = stageInput.take();
                process(q, id);
                if (id == -1) {
                    break;
                }
            }
        }

        private void process(EPOnDemandPreparedQueryParameterized q, int id) {
            q.setObject(1, id);
            EPOnDemandQueryResult result = epService.getEPRuntime().executeQuery(q);
            assertEquals("failed for id " + id, 1, result.getArray().length);
            assertEquals(id, result.getArray()[0].get("p0"));
            stageOutput.add(id);
            numberOfOperations++;
        }
    }

    public static class UpdateRunnable extends BaseRunnable {
        private final BlockingQueue<Integer> stageInput;
        private final Queue<Integer> stageOutput;

        public UpdateRunnable(EPServiceProvider epService, BlockingQueue<Integer> stageInput, Queue<Integer> stageOutput) {
            super(epService, "Update");
            this.stageInput = stageInput;
            this.stageOutput = stageOutput;
        }

        public void runWork() throws InterruptedException {
            String epl = "update MyTable set p0 = 99999999 where key = ?";
            EPOnDemandPreparedQueryParameterized q = epService.getEPRuntime().prepareQueryWithParameters(epl);
            while (true) {
                int id = stageInput.take();
                process(q, id);
                if (id == -1) {
                    break;
                }
            }
        }

        private void process(EPOnDemandPreparedQueryParameterized q, int id) {
            q.setObject(1, id);
            epService.getEPRuntime().executeQuery(q);
            stageOutput.add(id);
            numberOfOperations++;
        }
    }

    public static class DeleteRunnable extends BaseRunnable {
        private final BlockingQueue<Integer> stageInput;
        private final Queue<Integer> stageOutput;

        public DeleteRunnable(EPServiceProvider epService, BlockingQueue<Integer> stageInput, Queue<Integer> stageOutput) {
            super(epService, "Delete");
            this.stageInput = stageInput;
            this.stageOutput = stageOutput;
        }

        public void runWork() throws InterruptedException {
            String epl = "delete from MyTable where key = ?";
            EPOnDemandPreparedQueryParameterized q = epService.getEPRuntime().prepareQueryWithParameters(epl);
            while (true) {
                int id = stageInput.take();
                process(q, id);
                if (id == -1) {
                    break;
                }
            }
        }

        private void process(EPOnDemandPreparedQueryParameterized q, int id) {
            q.setObject(1, id);
            epService.getEPRuntime().executeQuery(q);
            stageOutput.add(id);
            numberOfOperations++;
        }
    }
}
