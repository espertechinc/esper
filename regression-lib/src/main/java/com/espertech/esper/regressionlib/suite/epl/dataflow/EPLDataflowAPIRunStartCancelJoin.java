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
package com.espertech.esper.regressionlib.suite.epl.dataflow;

import com.espertech.esper.common.client.dataflow.core.*;
import com.espertech.esper.common.internal.epl.dataflow.util.DefaultSupportCaptureOp;
import com.espertech.esper.common.internal.epl.dataflow.util.DefaultSupportGraphOpProvider;
import com.espertech.esper.common.internal.epl.dataflow.util.DefaultSupportGraphOpProviderByOpName;
import com.espertech.esper.common.internal.epl.dataflow.util.DefaultSupportSourceOp;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import static com.espertech.esper.regressionlib.support.epl.SupportStaticMethodLib.sleep;
import static org.junit.Assert.*;

public class EPLDataflowAPIRunStartCancelJoin {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EPLDataflowNonBlockingJoinCancel());
        execs.add(new EPLDataflowNonBlockingJoinException());
        execs.add(new EPLDataflowNonBlockingException());
        execs.add(new EPLDataflowBlockingException());
        execs.add(new EPLDataflowBlockingCancel());
        execs.add(new EPLDataflowNonBlockingCancel());
        execs.add(new EPLDataflowInvalidJoinRun());
        execs.add(new EPLDataflowNonBlockingJoinMultipleRunnable());
        execs.add(new EPLDataflowBlockingMultipleRunnable());
        execs.add(new EPLDataflowNonBlockingJoinSingleRunnable());
        execs.add(new EPLDataflowFastCompleteBlocking());
        execs.add(new EPLDataflowRunBlocking());
        execs.add(new EPLDataflowFastCompleteNonBlocking());
        execs.add(new EPLDataflowBlockingRunJoin());
        return execs;
    }

    private static class EPLDataflowNonBlockingJoinCancel implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("create schema SomeType ()", path);
            env.compileDeploy("@name('flow') create dataflow MyDataFlowOne " +
                "DefaultSupportSourceOp -> outstream<SomeType> {}" +
                "DefaultSupportCaptureOp(outstream) {}", path);

            final CountDownLatch latchOne = new CountDownLatch(1);
            DefaultSupportSourceOp src = new DefaultSupportSourceOp(new Object[]{latchOne});
            DefaultSupportCaptureOp<Object> output = new DefaultSupportCaptureOp<Object>();
            EPDataFlowInstantiationOptions options = new EPDataFlowInstantiationOptions().operatorProvider(new DefaultSupportGraphOpProvider(src, output));
            final EPDataFlowInstance dfOne = env.runtime().getDataFlowService().instantiate(env.deploymentId("flow"), "MyDataFlowOne", options);

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
            }, this.getClass().getSimpleName() + "-cancelling");
            cancellingThread.start();
            try {
                dfOne.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            assertEquals(EPDataFlowState.CANCELLED, dfOne.getState());
            assertEquals(0, output.getAndReset().size());

            env.undeployAll();
        }
    }

    private static class EPLDataflowNonBlockingJoinException implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("create schema SomeType ()", path);
            env.compileDeploy("@name('flow') create dataflow MyDataFlowOne " +
                "DefaultSupportSourceOp -> outstream<SomeType> {}" +
                "DefaultSupportCaptureOp(outstream) {}", path);

            final CountDownLatch latchOne = new CountDownLatch(1);
            DefaultSupportSourceOp src = new DefaultSupportSourceOp(new Object[]{latchOne, new MyRuntimeException("TestException")});
            DefaultSupportCaptureOp<Object> output = new DefaultSupportCaptureOp<Object>();
            EPDataFlowInstantiationOptions options = new EPDataFlowInstantiationOptions().operatorProvider(new DefaultSupportGraphOpProvider(src, output));
            final EPDataFlowInstance dfOne = env.runtime().getDataFlowService().instantiate(env.deploymentId("flow"), "MyDataFlowOne", options);

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
            }, this.getClass().getSimpleName() + "-unlatching");
            unlatchingThread.start();
            try {
                dfOne.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            assertEquals(EPDataFlowState.COMPLETE, dfOne.getState());
            assertEquals(0, output.getAndReset().size());
            env.undeployAll();
        }
    }

    private static class EPLDataflowNonBlockingException implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("create schema SomeType ()", path);
            env.compileDeploy("@name('flow') create dataflow MyDataFlowOne " +
                "DefaultSupportSourceOp -> outstream<SomeType> {}" +
                "DefaultSupportCaptureOp(outstream) {}", path);

            DefaultSupportSourceOp src = new DefaultSupportSourceOp(new Object[]{new MyRuntimeException("TestException")});
            DefaultSupportCaptureOp<Object> output = new DefaultSupportCaptureOp<Object>();
            EPDataFlowInstantiationOptions options = new EPDataFlowInstantiationOptions().operatorProvider(new DefaultSupportGraphOpProvider(src, output));
            final EPDataFlowInstance dfOne = env.runtime().getDataFlowService().instantiate(env.deploymentId("flow"), "MyDataFlowOne", options);

            dfOne.start();
            sleep(200);
            assertEquals(EPDataFlowState.COMPLETE, dfOne.getState());
            assertEquals(0, output.getAndReset().size());
            env.undeployAll();
        }
    }

    private static class EPLDataflowBlockingException implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("create schema SomeType ()", path);
            env.compileDeploy("@name('flow') create dataflow MyDataFlowOne " +
                "DefaultSupportSourceOp -> outstream<SomeType> {}" +
                "DefaultSupportCaptureOp(outstream) {}", path);

            DefaultSupportSourceOp src = new DefaultSupportSourceOp(new Object[]{new MyRuntimeException("TestException")});
            DefaultSupportCaptureOp<Object> output = new DefaultSupportCaptureOp<Object>();
            EPDataFlowInstantiationOptions options = new EPDataFlowInstantiationOptions().operatorProvider(new DefaultSupportGraphOpProvider(src, output));
            final EPDataFlowInstance dfOne = env.runtime().getDataFlowService().instantiate(env.deploymentId("flow"), "MyDataFlowOne", options);

            try {
                dfOne.run();
                fail();
            } catch (EPDataFlowExecutionException ex) {
                assertTrue(ex.getCause().getCause() instanceof MyRuntimeException);
                assertEquals("Support-graph-source generated exception: TestException", ex.getCause().getMessage());
            }

            assertEquals(EPDataFlowState.COMPLETE, dfOne.getState());
            assertEquals(0, output.getAndReset().size());
            env.undeployAll();
        }
    }

    private static class EPLDataflowBlockingCancel implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            // declare
            RegressionPath path = new RegressionPath();
            env.compileDeploy("create schema SomeType ()", path);
            env.compileDeploy("@name('flow') create dataflow MyDataFlowOne " +
                "DefaultSupportSourceOp -> outstream<SomeType> {}" +
                "DefaultSupportCaptureOp(outstream) {}", path);

            // instantiate
            CountDownLatch latchOne = new CountDownLatch(1);
            Map<String, Object> ops = new HashMap<String, Object>();
            ops.put("DefaultSupportSourceOp", new DefaultSupportSourceOp(new Object[]{latchOne, new Object[]{1}}));
            DefaultSupportCaptureOp<Object> output = new DefaultSupportCaptureOp<Object>();
            ops.put("DefaultSupportCaptureOp", output);

            EPDataFlowInstantiationOptions options = new EPDataFlowInstantiationOptions().operatorProvider(new DefaultSupportGraphOpProviderByOpName(ops));
            final EPDataFlowInstance dfOne = env.runtime().getDataFlowService().instantiate(env.deploymentId("flow"), "MyDataFlowOne", options);

            Thread cancellingThread = new Thread(new Runnable() {
                public void run() {
                    try {
                        Thread.sleep(300);
                        dfOne.cancel();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }, this.getClass().getSimpleName() + "-cancelling");
            cancellingThread.start();

            try {
                dfOne.run();
                fail();
            } catch (EPDataFlowCancellationException ex) {
                assertEquals("Data flow 'MyDataFlowOne' execution was cancelled", ex.getMessage());
            }
            assertEquals(EPDataFlowState.CANCELLED, dfOne.getState());
            assertEquals(0, output.getAndReset().size());
            env.undeployAll();
        }
    }

    private static class EPLDataflowNonBlockingCancel implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            // declare
            RegressionPath path = new RegressionPath();
            env.compileDeploy("create schema SomeType ()", path);
            env.compileDeploy("@name('flow') create dataflow MyDataFlowOne " +
                "DefaultSupportSourceOp -> outstream<SomeType> {}" +
                "DefaultSupportCaptureOp(outstream) {}", path);

            // instantiate
            CountDownLatch latchOne = new CountDownLatch(1);
            Map<String, Object> ops = new HashMap<String, Object>();
            ops.put("DefaultSupportSourceOp", new DefaultSupportSourceOp(new Object[]{latchOne, new Object[]{1}}));
            DefaultSupportCaptureOp<Object> output = new DefaultSupportCaptureOp<Object>();
            ops.put("DefaultSupportCaptureOp", output);

            EPDataFlowInstantiationOptions options = new EPDataFlowInstantiationOptions().operatorProvider(new DefaultSupportGraphOpProviderByOpName(ops));
            EPDataFlowInstance dfOne = env.runtime().getDataFlowService().instantiate(env.deploymentId("flow"), "MyDataFlowOne", options);

            dfOne.start();
            assertEquals(EPDataFlowState.RUNNING, dfOne.getState());

            dfOne.cancel();

            latchOne.countDown();
            sleep(100);
            assertEquals(EPDataFlowState.CANCELLED, dfOne.getState());
            assertEquals(0, output.getAndReset().size());
            env.undeployAll();
        }
    }

    private static class EPLDataflowInvalidJoinRun implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.compileDeploy("@name('flow') create dataflow MyDataFlowOne " +
                "BeaconSource -> BeaconStream {iterations : 1}");

            DefaultSupportSourceOp source = new DefaultSupportSourceOp(new Object[]{5000});
            EPDataFlowInstantiationOptions options = new EPDataFlowInstantiationOptions().operatorProvider(new DefaultSupportGraphOpProvider(source));
            EPDataFlowInstance dfOne = env.runtime().getDataFlowService().instantiate(env.deploymentId("flow"), "MyDataFlowOne", options);

            // invalid join
            try {
                dfOne.join();
                fail();
            } catch (IllegalStateException ex) {
                assertEquals("Data flow 'MyDataFlowOne' instance has not been executed, please use join after start or run", ex.getMessage());
            } catch (InterruptedException ex) {
                throw new RuntimeException(ex);
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
            env.undeployAll();
        }
    }

    private static class EPLDataflowNonBlockingJoinMultipleRunnable implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            // declare
            RegressionPath path = new RegressionPath();
            env.compileDeploy("create schema SomeType ()", path);
            env.compileDeploy("@name('flow') create dataflow MyDataFlowOne " +
                "DefaultSupportSourceOp -> outstream<SomeType> { name: 'SourceOne' }" +
                "DefaultSupportSourceOp -> outstream<SomeType> { name: 'SourceTwo' }" +
                "DefaultSupportCaptureOp(outstream) {}", path);

            // instantiate
            CountDownLatch latchOne = new CountDownLatch(1);
            CountDownLatch latchTwo = new CountDownLatch(1);
            Map<String, Object> ops = new HashMap<String, Object>();
            ops.put("SourceOne", new DefaultSupportSourceOp(new Object[]{latchOne, new Object[]{1}}));
            ops.put("SourceTwo", new DefaultSupportSourceOp(new Object[]{latchTwo, new Object[]{1}}));
            DefaultSupportCaptureOp<Object> future = new DefaultSupportCaptureOp<Object>(2);
            ops.put("DefaultSupportCaptureOp", future);

            EPDataFlowInstantiationOptions options = new EPDataFlowInstantiationOptions().operatorProvider(new DefaultSupportGraphOpProviderByOpName(ops));
            EPDataFlowInstance dfOne = env.runtime().getDataFlowService().instantiate(env.deploymentId("flow"), "MyDataFlowOne", options);

            dfOne.start();
            sleep(50);
            assertEquals(EPDataFlowState.RUNNING, dfOne.getState());

            latchOne.countDown();
            sleep(200);
            assertEquals(EPDataFlowState.RUNNING, dfOne.getState());

            latchTwo.countDown();
            try {
                dfOne.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            assertEquals(EPDataFlowState.COMPLETE, dfOne.getState());
            assertEquals(2, future.getAndReset().size());
            env.undeployAll();
        }
    }

    private static class EPLDataflowBlockingMultipleRunnable implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            // declare
            RegressionPath path = new RegressionPath();
            env.compileDeploy("create schema SomeType ()", path);
            env.compileDeploy("@name('flow') create dataflow MyDataFlowOne " +
                "DefaultSupportSourceOp -> outstream<SomeType> {name: 'SourceOne'}" +
                "DefaultSupportSourceOp -> outstream<SomeType> {name: 'SourceTwo'}" +
                "DefaultSupportCaptureOp(outstream) {}", path);

            // instantiate
            CountDownLatch latchOne = new CountDownLatch(1);
            CountDownLatch latchTwo = new CountDownLatch(1);
            Map<String, Object> ops = new HashMap<String, Object>();
            ops.put("SourceOne", new DefaultSupportSourceOp(new Object[]{latchOne, new Object[]{1}}));
            ops.put("SourceTwo", new DefaultSupportSourceOp(new Object[]{latchTwo, new Object[]{1}}));
            DefaultSupportCaptureOp<Object> future = new DefaultSupportCaptureOp<Object>(2);
            ops.put("DefaultSupportCaptureOp", future);

            EPDataFlowInstantiationOptions options = new EPDataFlowInstantiationOptions().operatorProvider(new DefaultSupportGraphOpProviderByOpName(ops));
            EPDataFlowInstance dfOne = env.runtime().getDataFlowService().instantiate(env.deploymentId("flow"), "MyDataFlowOne", options);

            try {
                dfOne.run();
                fail();
            } catch (UnsupportedOperationException ex) {
                assertEquals("The data flow 'MyDataFlowOne' has zero or multiple sources and requires the use of the start method instead", ex.getMessage());
            }

            latchTwo.countDown();
            dfOne.start();
            latchOne.countDown();
            try {
                dfOne.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            assertEquals(EPDataFlowState.COMPLETE, dfOne.getState());
            assertEquals(2, future.getAndReset().size());
            env.undeployAll();
        }
    }

    private static class EPLDataflowNonBlockingJoinSingleRunnable implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            // declare
            RegressionPath path = new RegressionPath();
            env.compileDeploy("create schema SomeType ()", path);
            env.compileDeploy("@name('flow') create dataflow MyDataFlowOne " +
                "DefaultSupportSourceOp -> outstream<SomeType> {}" +
                "DefaultSupportCaptureOp(outstream) {}", path);

            // instantiate
            CountDownLatch latch = new CountDownLatch(1);
            DefaultSupportSourceOp source = new DefaultSupportSourceOp(new Object[]{latch, new Object[]{1}});
            DefaultSupportCaptureOp<Object> future = new DefaultSupportCaptureOp<Object>(1);
            EPDataFlowInstantiationOptions options = new EPDataFlowInstantiationOptions().operatorProvider(new DefaultSupportGraphOpProvider(source, future));
            EPDataFlowInstance dfOne = env.runtime().getDataFlowService().instantiate(env.deploymentId("flow"), "MyDataFlowOne", options);
            assertEquals("MyDataFlowOne", dfOne.getDataFlowName());
            assertEquals(EPDataFlowState.INSTANTIATED, dfOne.getState());

            dfOne.start();
            sleep(100);
            assertEquals(EPDataFlowState.RUNNING, dfOne.getState());

            latch.countDown();
            try {
                dfOne.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            assertEquals(EPDataFlowState.COMPLETE, dfOne.getState());
            assertEquals(1, future.getAndReset().get(0).size());
            assertEquals(2, source.getCurrentCount());

            dfOne.cancel();
            assertEquals(EPDataFlowState.COMPLETE, dfOne.getState());
            env.undeployAll();
        }
    }

    private static class EPLDataflowBlockingRunJoin implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            // declare
            RegressionPath path = new RegressionPath();
            env.compileDeploy("create schema SomeType ()", path);
            env.compileDeploy("@name('flow') create dataflow MyDataFlowOne " +
                "DefaultSupportSourceOp -> s<SomeType> {}" +
                "DefaultSupportCaptureOp(s) {}", path);

            // instantiate
            final CountDownLatch latch = new CountDownLatch(1);
            DefaultSupportSourceOp source = new DefaultSupportSourceOp(new Object[]{latch, new Object[]{1}});
            DefaultSupportCaptureOp<Object> future = new DefaultSupportCaptureOp<Object>(1);
            EPDataFlowInstantiationOptions options = new EPDataFlowInstantiationOptions().operatorProvider(new DefaultSupportGraphOpProvider(source, future));
            final EPDataFlowInstance dfOne = env.runtime().getDataFlowService().instantiate(env.deploymentId("flow"), "MyDataFlowOne", options);
            assertEquals("MyDataFlowOne", dfOne.getDataFlowName());
            assertEquals(EPDataFlowState.INSTANTIATED, dfOne.getState());

            MyJoiningRunnable joiningRunnable = new MyJoiningRunnable(dfOne);
            Thread joiningThread = new Thread(joiningRunnable, this.getClass().getSimpleName() + "-joining");

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
            }, this.getClass().getSimpleName() + "-unlatching");

            joiningThread.start();
            unlatchingThread.start();
            dfOne.run();

            assertEquals(EPDataFlowState.COMPLETE, dfOne.getState());
            assertEquals(1, future.getAndReset().get(0).size());
            assertEquals(2, source.getCurrentCount());

            try {
                joiningThread.join();
                unlatchingThread.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            long deltaJoin = joiningRunnable.getEnd() - joiningRunnable.getStart();
            assertTrue("deltaJoin=" + deltaJoin, deltaJoin >= 500);
            env.undeployAll();
        }
    }

    private static class EPLDataflowFastCompleteBlocking implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            // declare
            env.compileDeploy("@name('flow') create dataflow MyDataFlowOne " +
                "BeaconSource -> BeaconStream {iterations : 1}" +
                "DefaultSupportCaptureOp(BeaconStream) {}");

            // instantiate
            DefaultSupportCaptureOp<Object> future = new DefaultSupportCaptureOp<Object>(1);
            EPDataFlowInstantiationOptions options = new EPDataFlowInstantiationOptions().operatorProvider(new DefaultSupportGraphOpProvider(future));
            EPDataFlowInstance dfOne = env.runtime().getDataFlowService().instantiate(env.deploymentId("flow"), "MyDataFlowOne", options);
            assertEquals("MyDataFlowOne", dfOne.getDataFlowName());
            assertEquals(EPDataFlowState.INSTANTIATED, dfOne.getState());

            // has not run
            sleep(1000);
            assertFalse(future.isDone());

            // blocking run
            dfOne.run();
            assertEquals(EPDataFlowState.COMPLETE, dfOne.getState());
            try {
                assertEquals(1, future.get().length);
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }

            // assert past-exec
            tryAssertionAfterExec(dfOne);

            env.undeployAll();
        }
    }

    private static class EPLDataflowRunBlocking implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            // declare
            RegressionPath path = new RegressionPath();
            env.compileDeploy("create schema SomeType ()", path);
            env.compileDeploy("@name('flow') create dataflow MyDataFlowOne " +
                "DefaultSupportSourceOp -> s<SomeType> {}" +
                "DefaultSupportCaptureOp(s) {}", path);

            // instantiate
            final CountDownLatch latch = new CountDownLatch(1);
            DefaultSupportSourceOp source = new DefaultSupportSourceOp(new Object[]{latch, new Object[]{1}});
            DefaultSupportCaptureOp<Object> future = new DefaultSupportCaptureOp<Object>(1);
            EPDataFlowInstantiationOptions options = new EPDataFlowInstantiationOptions().operatorProvider(new DefaultSupportGraphOpProvider(future, source));
            final EPDataFlowInstance dfOne = env.runtime().getDataFlowService().instantiate(env.deploymentId("flow"), "MyDataFlowOne", options);
            assertEquals("MyDataFlowOne", dfOne.getDataFlowName());
            assertEquals(EPDataFlowState.INSTANTIATED, dfOne.getState());

            Thread unlatchingThread = new Thread(new Runnable() {
                public void run() {
                    try {
                        while (dfOne.getState() != EPDataFlowState.RUNNING) {
                            Thread.sleep(0);
                        }
                        Thread.sleep(100);
                        latch.countDown();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }, this.getClass().getSimpleName() + "-unlatching");

            // blocking run
            unlatchingThread.start();
            dfOne.run();
            assertEquals(EPDataFlowState.COMPLETE, dfOne.getState());
            assertEquals(1, future.getAndReset().get(0).size());
            assertEquals(2, source.getCurrentCount());
            try {
                unlatchingThread.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            env.undeployAll();
        }
    }

    private static class EPLDataflowFastCompleteNonBlocking implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            // declare
            env.compileDeploy("@name('flow') create dataflow MyDataFlowOne " +
                "BeaconSource -> BeaconStream {iterations : 1}" +
                "DefaultSupportCaptureOp(BeaconStream) {}");

            // instantiate
            DefaultSupportCaptureOp<Object> future = new DefaultSupportCaptureOp<Object>(1);
            EPDataFlowInstantiationOptions options = new EPDataFlowInstantiationOptions().operatorProvider(new DefaultSupportGraphOpProvider(future));
            EPDataFlowInstance dfOne = env.runtime().getDataFlowService().instantiate(env.deploymentId("flow"), "MyDataFlowOne", options);
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
            try {
                assertEquals(1, future.get().length);
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }

            // assert past-exec
            tryAssertionAfterExec(dfOne);
            env.undeployAll();
        }
    }

    private static void tryAssertionAfterExec(EPDataFlowInstance df) {
        // cancel and join ignored
        try {
            df.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

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
        try {
            df.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
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
