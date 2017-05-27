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
package com.espertech.esper.regression.dataflow;

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.dataflow.*;
import com.espertech.esper.dataflow.util.DefaultSupportCaptureOp;
import com.espertech.esper.dataflow.util.DefaultSupportGraphOpProvider;
import com.espertech.esper.dataflow.util.DefaultSupportGraphOpProviderByOpName;
import com.espertech.esper.dataflow.util.DefaultSupportSourceOp;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.*;

public class ExecDataflowAPIRunStartCancelJoin implements RegressionExecution {

    public void run(EPServiceProvider epService) throws Exception {
        runAssertionNonBlockingJoinCancel(epService);
        runAssertionNonBlockingJoinException(epService);
        runAssertionNonBlockingException(epService);
        runAssertionBlockingException(epService);
        runAssertionBlockingCancel(epService);
        runAssertionNonBlockingCancel(epService);
        runAssertionInvalidJoinRun(epService);
        runAssertionNonBlockingJoinMultipleRunnable(epService);
        runAssertionBlockingMultipleRunnable(epService);
        runAssertionNonBlockingJoinSingleRunnable(epService);
        runAssertionBlockingRunJoin(epService);
        runAssertionFastCompleteBlocking(epService);
        runAssertionRunBlocking(epService);
        runAssertionFastCompleteNonBlocking(epService);
    }

    private void runAssertionNonBlockingJoinCancel(EPServiceProvider epService) throws Exception {
        epService.getEPAdministrator().createEPL("create schema SomeType ()");
        epService.getEPAdministrator().createEPL("create dataflow MyDataFlowOne " +
                "DefaultSupportSourceOp -> outstream<SomeType> {}" +
                "DefaultSupportCaptureOp(outstream) {}");

        final CountDownLatch latchOne = new CountDownLatch(1);
        DefaultSupportSourceOp src = new DefaultSupportSourceOp(new Object[]{latchOne});
        DefaultSupportCaptureOp<Object> output = new DefaultSupportCaptureOp<Object>();
        EPDataFlowInstantiationOptions options = new EPDataFlowInstantiationOptions().operatorProvider(new DefaultSupportGraphOpProvider(src, output));
        final EPDataFlowInstance dfOne = epService.getEPRuntime().getDataFlowRuntime().instantiate("MyDataFlowOne", options);

        dfOne.start();

        Thread cancellingThread = new Thread(new Runnable() {
            public void run() {
                try {
                    Thread.sleep(300);
                    dfOne.cancel();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        cancellingThread.start();
        dfOne.join();

        assertEquals(EPDataFlowState.CANCELLED, dfOne.getState());
        assertEquals(0, output.getAndReset().size());

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionNonBlockingJoinException(EPServiceProvider epService) throws Exception {
        epService.getEPAdministrator().createEPL("create schema SomeType ()");
        epService.getEPAdministrator().createEPL("create dataflow MyDataFlowOne " +
                "DefaultSupportSourceOp -> outstream<SomeType> {}" +
                "DefaultSupportCaptureOp(outstream) {}");

        final CountDownLatch latchOne = new CountDownLatch(1);
        DefaultSupportSourceOp src = new DefaultSupportSourceOp(new Object[]{latchOne, new MyRuntimeException("TestException")});
        DefaultSupportCaptureOp<Object> output = new DefaultSupportCaptureOp<Object>();
        EPDataFlowInstantiationOptions options = new EPDataFlowInstantiationOptions().operatorProvider(new DefaultSupportGraphOpProvider(src, output));
        final EPDataFlowInstance dfOne = epService.getEPRuntime().getDataFlowRuntime().instantiate("MyDataFlowOne", options);

        dfOne.start();

        Thread unlatchingThread = new Thread(new Runnable() {
            public void run() {
                try {
                    Thread.sleep(300);
                    latchOne.countDown();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        unlatchingThread.start();
        dfOne.join();

        assertEquals(EPDataFlowState.COMPLETE, dfOne.getState());
        assertEquals(0, output.getAndReset().size());
        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionNonBlockingException(EPServiceProvider epService) throws Exception {
        epService.getEPAdministrator().createEPL("create schema SomeType ()");
        epService.getEPAdministrator().createEPL("create dataflow MyDataFlowOne " +
                "DefaultSupportSourceOp -> outstream<SomeType> {}" +
                "DefaultSupportCaptureOp(outstream) {}");

        DefaultSupportSourceOp src = new DefaultSupportSourceOp(new Object[]{new MyRuntimeException("TestException")});
        DefaultSupportCaptureOp<Object> output = new DefaultSupportCaptureOp<Object>();
        EPDataFlowInstantiationOptions options = new EPDataFlowInstantiationOptions().operatorProvider(new DefaultSupportGraphOpProvider(src, output));
        final EPDataFlowInstance dfOne = epService.getEPRuntime().getDataFlowRuntime().instantiate("MyDataFlowOne", options);

        dfOne.start();
        Thread.sleep(200);
        assertEquals(EPDataFlowState.COMPLETE, dfOne.getState());
        assertEquals(0, output.getAndReset().size());
        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionBlockingException(EPServiceProvider epService) throws Exception {
        epService.getEPAdministrator().createEPL("create schema SomeType ()");
        epService.getEPAdministrator().createEPL("create dataflow MyDataFlowOne " +
                "DefaultSupportSourceOp -> outstream<SomeType> {}" +
                "DefaultSupportCaptureOp(outstream) {}");

        DefaultSupportSourceOp src = new DefaultSupportSourceOp(new Object[]{new MyRuntimeException("TestException")});
        DefaultSupportCaptureOp<Object> output = new DefaultSupportCaptureOp<Object>();
        EPDataFlowInstantiationOptions options = new EPDataFlowInstantiationOptions().operatorProvider(new DefaultSupportGraphOpProvider(src, output));
        final EPDataFlowInstance dfOne = epService.getEPRuntime().getDataFlowRuntime().instantiate("MyDataFlowOne", options);

        try {
            dfOne.run();
            fail();
        } catch (EPDataFlowExecutionException ex) {
            assertTrue(ex.getCause().getCause() instanceof MyRuntimeException);
            assertEquals("Support-graph-source generated exception: TestException", ex.getCause().getMessage());
        }

        assertEquals(EPDataFlowState.COMPLETE, dfOne.getState());
        assertEquals(0, output.getAndReset().size());
        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionBlockingCancel(EPServiceProvider epService) throws Exception {
        // declare
        epService.getEPAdministrator().createEPL("create schema SomeType ()");
        epService.getEPAdministrator().createEPL("create dataflow MyDataFlowOne " +
                "SourceOne -> outstream<SomeType> {}" +
                "OutputOp(outstream) {}");

        // instantiate
        CountDownLatch latchOne = new CountDownLatch(1);
        Map<String, Object> ops = new HashMap<String, Object>();
        ops.put("SourceOne", new DefaultSupportSourceOp(new Object[]{latchOne, new Object[]{1}}));
        DefaultSupportCaptureOp<Object> output = new DefaultSupportCaptureOp<Object>();
        ops.put("OutputOp", output);

        EPDataFlowInstantiationOptions options = new EPDataFlowInstantiationOptions().operatorProvider(new DefaultSupportGraphOpProviderByOpName(ops));
        final EPDataFlowInstance dfOne = epService.getEPRuntime().getDataFlowRuntime().instantiate("MyDataFlowOne", options);

        Thread cancellingThread = new Thread(new Runnable() {
            public void run() {
                try {
                    Thread.sleep(300);
                    dfOne.cancel();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        cancellingThread.start();

        try {
            dfOne.run();
            fail();
        } catch (EPDataFlowCancellationException ex) {
            assertEquals("Data flow 'MyDataFlowOne' execution was cancelled", ex.getMessage());
        }
        assertEquals(EPDataFlowState.CANCELLED, dfOne.getState());
        assertEquals(0, output.getAndReset().size());
        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionNonBlockingCancel(EPServiceProvider epService) throws Exception {
        // declare
        epService.getEPAdministrator().createEPL("create schema SomeType ()");
        epService.getEPAdministrator().createEPL("create dataflow MyDataFlowOne " +
                "SourceOne -> outstream<SomeType> {}" +
                "OutputOp(outstream) {}");

        // instantiate
        CountDownLatch latchOne = new CountDownLatch(1);
        Map<String, Object> ops = new HashMap<String, Object>();
        ops.put("SourceOne", new DefaultSupportSourceOp(new Object[]{latchOne, new Object[]{1}}));
        DefaultSupportCaptureOp<Object> output = new DefaultSupportCaptureOp<Object>();
        ops.put("OutputOp", output);

        EPDataFlowInstantiationOptions options = new EPDataFlowInstantiationOptions().operatorProvider(new DefaultSupportGraphOpProviderByOpName(ops));
        EPDataFlowInstance dfOne = epService.getEPRuntime().getDataFlowRuntime().instantiate("MyDataFlowOne", options);

        dfOne.start();
        assertEquals(EPDataFlowState.RUNNING, dfOne.getState());

        dfOne.cancel();

        latchOne.countDown();
        Thread.sleep(100);
        assertEquals(EPDataFlowState.CANCELLED, dfOne.getState());
        assertEquals(0, output.getAndReset().size());
        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionInvalidJoinRun(EPServiceProvider epService) throws Exception {
        epService.getEPAdministrator().createEPL("create dataflow MyDataFlowOne " +
                "BeaconSource -> BeaconStream {iterations : 1}");

        DefaultSupportSourceOp source = new DefaultSupportSourceOp(new Object[]{5000});
        EPDataFlowInstantiationOptions options = new EPDataFlowInstantiationOptions().operatorProvider(new DefaultSupportGraphOpProvider(source));
        EPDataFlowInstance dfOne = epService.getEPRuntime().getDataFlowRuntime().instantiate("MyDataFlowOne", options);

        // invalid join
        try {
            dfOne.join();
            fail();
        } catch (IllegalStateException ex) {
            assertEquals("Data flow 'MyDataFlowOne' instance has not been executed, please use join after start or run", ex.getMessage());
        }

        // cancel
        dfOne.cancel();

        // invalid run and start
        try {
            dfOne.run();
            fail();
        } catch (IllegalStateException ex) {
            assertEquals("Data flow 'MyDataFlowOne' instance has been cancelled and cannot be run or started", ex.getMessage());
        }

        try {
            dfOne.start();
            fail();
        } catch (IllegalStateException ex) {
            assertEquals("Data flow 'MyDataFlowOne' instance has been cancelled and cannot be run or started", ex.getMessage());
        }

        // cancel again
        dfOne.cancel();
        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionNonBlockingJoinMultipleRunnable(EPServiceProvider epService) throws Exception {

        // declare
        epService.getEPAdministrator().createEPL("create schema SomeType ()");
        epService.getEPAdministrator().createEPL("create dataflow MyDataFlowOne " +
                "SourceOne -> outstream<SomeType> {}" +
                "SourceTwo -> outstream<SomeType> {}" +
                "Future(outstream) {}");

        // instantiate
        CountDownLatch latchOne = new CountDownLatch(1);
        CountDownLatch latchTwo = new CountDownLatch(1);
        Map<String, Object> ops = new HashMap<String, Object>();
        ops.put("SourceOne", new DefaultSupportSourceOp(new Object[]{latchOne, new Object[]{1}}));
        ops.put("SourceTwo", new DefaultSupportSourceOp(new Object[]{latchTwo, new Object[]{1}}));
        DefaultSupportCaptureOp<Object> future = new DefaultSupportCaptureOp<Object>(2);
        ops.put("Future", future);

        EPDataFlowInstantiationOptions options = new EPDataFlowInstantiationOptions().operatorProvider(new DefaultSupportGraphOpProviderByOpName(ops));
        EPDataFlowInstance dfOne = epService.getEPRuntime().getDataFlowRuntime().instantiate("MyDataFlowOne", options);

        dfOne.start();
        Thread.sleep(50);
        assertEquals(EPDataFlowState.RUNNING, dfOne.getState());

        latchOne.countDown();
        Thread.sleep(200);
        assertEquals(EPDataFlowState.RUNNING, dfOne.getState());

        latchTwo.countDown();
        dfOne.join();
        assertEquals(EPDataFlowState.COMPLETE, dfOne.getState());
        assertEquals(2, future.getAndReset().size());
        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionBlockingMultipleRunnable(EPServiceProvider epService) throws Exception {

        // declare
        epService.getEPAdministrator().createEPL("create schema SomeType ()");
        epService.getEPAdministrator().createEPL("create dataflow MyDataFlowOne " +
                "SourceOne -> outstream<SomeType> {}" +
                "SourceTwo -> outstream<SomeType> {}" +
                "Future(outstream) {}");

        // instantiate
        CountDownLatch latchOne = new CountDownLatch(1);
        CountDownLatch latchTwo = new CountDownLatch(1);
        Map<String, Object> ops = new HashMap<String, Object>();
        ops.put("SourceOne", new DefaultSupportSourceOp(new Object[]{latchOne, new Object[]{1}}));
        ops.put("SourceTwo", new DefaultSupportSourceOp(new Object[]{latchTwo, new Object[]{1}}));
        DefaultSupportCaptureOp<Object> future = new DefaultSupportCaptureOp<Object>(2);
        ops.put("Future", future);

        EPDataFlowInstantiationOptions options = new EPDataFlowInstantiationOptions().operatorProvider(new DefaultSupportGraphOpProviderByOpName(ops));
        EPDataFlowInstance dfOne = epService.getEPRuntime().getDataFlowRuntime().instantiate("MyDataFlowOne", options);

        try {
            dfOne.run();
            fail();
        } catch (UnsupportedOperationException ex) {
            assertEquals("The data flow 'MyDataFlowOne' has zero or multiple sources and requires the use of the start method instead", ex.getMessage());
        }

        latchTwo.countDown();
        dfOne.start();
        latchOne.countDown();
        dfOne.join();

        assertEquals(EPDataFlowState.COMPLETE, dfOne.getState());
        assertEquals(2, future.getAndReset().size());
        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionNonBlockingJoinSingleRunnable(EPServiceProvider epService) throws Exception {

        // declare
        epService.getEPAdministrator().createEPL("create schema SomeType ()");
        epService.getEPAdministrator().createEPL("create dataflow MyDataFlowOne " +
                "DefaultSupportSourceOp -> outstream<SomeType> {}" +
                "DefaultSupportCaptureOp(outstream) {}");

        // instantiate
        CountDownLatch latch = new CountDownLatch(1);
        DefaultSupportSourceOp source = new DefaultSupportSourceOp(new Object[]{latch, new Object[]{1}});
        DefaultSupportCaptureOp<Object> future = new DefaultSupportCaptureOp<Object>(1);
        EPDataFlowInstantiationOptions options = new EPDataFlowInstantiationOptions().operatorProvider(new DefaultSupportGraphOpProvider(source, future));
        EPDataFlowInstance dfOne = epService.getEPRuntime().getDataFlowRuntime().instantiate("MyDataFlowOne", options);
        assertEquals("MyDataFlowOne", dfOne.getDataFlowName());
        assertEquals(EPDataFlowState.INSTANTIATED, dfOne.getState());

        dfOne.start();
        Thread.sleep(100);
        assertEquals(EPDataFlowState.RUNNING, dfOne.getState());

        latch.countDown();
        dfOne.join();
        assertEquals(EPDataFlowState.COMPLETE, dfOne.getState());
        assertEquals(1, future.getAndReset().get(0).size());
        assertEquals(2, source.getCurrentCount());

        dfOne.cancel();
        assertEquals(EPDataFlowState.COMPLETE, dfOne.getState());
        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionBlockingRunJoin(EPServiceProvider epService) throws Exception {

        // declare
        epService.getEPAdministrator().createEPL("create schema SomeType ()");
        epService.getEPAdministrator().createEPL("create dataflow MyDataFlowOne " +
                "DefaultSupportSourceOp -> s<SomeType> {}" +
                "DefaultSupportCaptureOp(s) {}");

        // instantiate
        final CountDownLatch latch = new CountDownLatch(1);
        DefaultSupportSourceOp source = new DefaultSupportSourceOp(new Object[]{latch, new Object[]{1}});
        DefaultSupportCaptureOp<Object> future = new DefaultSupportCaptureOp<Object>(1);
        EPDataFlowInstantiationOptions options = new EPDataFlowInstantiationOptions().operatorProvider(new DefaultSupportGraphOpProvider(source, future));
        final EPDataFlowInstance dfOne = epService.getEPRuntime().getDataFlowRuntime().instantiate("MyDataFlowOne", options);
        assertEquals("MyDataFlowOne", dfOne.getDataFlowName());
        assertEquals(EPDataFlowState.INSTANTIATED, dfOne.getState());

        MyJoiningRunnable joiningRunnable = new MyJoiningRunnable(dfOne);
        Thread joiningThread = new Thread(joiningRunnable);

        Thread unlatchingThread = new Thread(new Runnable() {
            public void run() {
                try {
                    while (dfOne.getState() != EPDataFlowState.RUNNING) {
                        Thread.sleep(10);
                    }
                    Thread.sleep(1000);
                    latch.countDown();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        joiningThread.start();
        unlatchingThread.start();
        dfOne.run();

        assertEquals(EPDataFlowState.COMPLETE, dfOne.getState());
        assertEquals(1, future.getAndReset().get(0).size());
        assertEquals(2, source.getCurrentCount());

        joiningThread.join();
        unlatchingThread.join();
        long deltaJoin = joiningRunnable.getEnd() - joiningRunnable.getStart();
        assertTrue("deltaJoin=" + deltaJoin, deltaJoin >= 500);
        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionFastCompleteBlocking(EPServiceProvider epService) throws Exception {

        // declare
        epService.getEPAdministrator().createEPL("create dataflow MyDataFlowOne " +
                "BeaconSource -> BeaconStream {iterations : 1}" +
                "DefaultSupportCaptureOp(BeaconStream) {}");

        // instantiate
        DefaultSupportCaptureOp<Object> future = new DefaultSupportCaptureOp<Object>(1);
        EPDataFlowInstantiationOptions options = new EPDataFlowInstantiationOptions().operatorProvider(new DefaultSupportGraphOpProvider(future));
        EPDataFlowInstance dfOne = epService.getEPRuntime().getDataFlowRuntime().instantiate("MyDataFlowOne", options);
        assertEquals("MyDataFlowOne", dfOne.getDataFlowName());
        assertEquals(EPDataFlowState.INSTANTIATED, dfOne.getState());

        // has not run
        Thread.sleep(1000);
        assertFalse(future.isDone());

        // blocking run
        dfOne.run();
        assertEquals(EPDataFlowState.COMPLETE, dfOne.getState());
        assertEquals(1, future.get().length);

        // assert past-exec
        tryAssertionAfterExec(dfOne);

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionRunBlocking(EPServiceProvider epService) throws Exception {

        // declare
        epService.getEPAdministrator().createEPL("create schema SomeType ()");
        epService.getEPAdministrator().createEPL("create dataflow MyDataFlowOne " +
                "DefaultSupportSourceOp -> s<SomeType> {}" +
                "DefaultSupportCaptureOp(s) {}");

        // instantiate
        final CountDownLatch latch = new CountDownLatch(1);
        DefaultSupportSourceOp source = new DefaultSupportSourceOp(new Object[]{latch, new Object[]{1}});
        DefaultSupportCaptureOp<Object> future = new DefaultSupportCaptureOp<Object>(1);
        EPDataFlowInstantiationOptions options = new EPDataFlowInstantiationOptions().operatorProvider(new DefaultSupportGraphOpProvider(future, source));
        final EPDataFlowInstance dfOne = epService.getEPRuntime().getDataFlowRuntime().instantiate("MyDataFlowOne", options);
        assertEquals("MyDataFlowOne", dfOne.getDataFlowName());
        assertEquals(EPDataFlowState.INSTANTIATED, dfOne.getState());

        Thread unlatchingThread = new Thread(new Runnable() {
            public void run() {
                try {
                    while (dfOne.getState() != EPDataFlowState.RUNNING)  {
                        Thread.sleep(0);
                    }
                    Thread.sleep(100);
                    latch.countDown();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        // blocking run
        unlatchingThread.start();
        dfOne.run();
        assertEquals(EPDataFlowState.COMPLETE, dfOne.getState());
        assertEquals(1, future.getAndReset().get(0).size());
        assertEquals(2, source.getCurrentCount());
        unlatchingThread.join();
        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionFastCompleteNonBlocking(EPServiceProvider epService) throws Exception {

        // declare
        epService.getEPAdministrator().createEPL("create dataflow MyDataFlowOne " +
                "BeaconSource -> BeaconStream {iterations : 1}" +
                "DefaultSupportCaptureOp(BeaconStream) {}");

        // instantiate
        DefaultSupportCaptureOp<Object> future = new DefaultSupportCaptureOp<Object>(1);
        EPDataFlowInstantiationOptions options = new EPDataFlowInstantiationOptions().operatorProvider(new DefaultSupportGraphOpProvider(future));
        EPDataFlowInstance dfOne = epService.getEPRuntime().getDataFlowRuntime().instantiate("MyDataFlowOne", options);
        assertEquals("MyDataFlowOne", dfOne.getDataFlowName());
        assertEquals(EPDataFlowState.INSTANTIATED, dfOne.getState());
        assertFalse(future.isDone());

        // non-blocking run, spinning wait
        dfOne.start();
        long start = System.currentTimeMillis();
        while (dfOne.getState() != EPDataFlowState.COMPLETE) {
            if (System.currentTimeMillis() - start > 1000) {
                fail();
            }
        }
        assertEquals(1, future.get().length);

        // assert past-exec
        tryAssertionAfterExec(dfOne);
        epService.getEPAdministrator().destroyAllStatements();
    }

    private void tryAssertionAfterExec(EPDataFlowInstance df) throws Exception {
        // cancel and join ignored
        df.join();

        // can't start or run again
        try {
            df.run();
            fail();
        } catch (IllegalStateException ex) {
            assertEquals("Data flow 'MyDataFlowOne' instance has already completed, please use instantiate to run the data flow again", ex.getMessage());
        }

        try {
            df.start();
            fail();
        } catch (IllegalStateException ex) {
            assertEquals("Data flow 'MyDataFlowOne' instance has already completed, please use instantiate to run the data flow again", ex.getMessage());
        }

        df.cancel();
        df.join();
    }

    public static class MyJoiningRunnable implements Runnable {
        private final EPDataFlowInstance instance;
        private long start;
        private long end;

        public MyJoiningRunnable(EPDataFlowInstance instance) {
            this.instance = instance;
        }

        public void run() {
            try {
                while (instance.getState() != EPDataFlowState.RUNNING) {
                    Thread.sleep(0);
                }
                start = System.currentTimeMillis();
                instance.join();
                end = System.currentTimeMillis();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        public long getStart() {
            return start;
        }

        public long getEnd() {
            return end;
        }
    }

    public static class MyRuntimeException extends RuntimeException {
        public MyRuntimeException(String message) {
            super(message);
        }
    }
}
