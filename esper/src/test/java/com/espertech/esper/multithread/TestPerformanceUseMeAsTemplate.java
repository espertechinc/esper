package com.espertech.esper.multithread;

import com.espertech.esper.client.*;
import junit.framework.TestCase;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class TestPerformanceUseMeAsTemplate extends TestCase {

    public void testPerformance() throws InterruptedException {
        int numEvents = 1;
        int numThreads = 2;

        Configuration config = new Configuration();
        config.getEngineDefaults().getThreading().setListenerDispatchPreserveOrder(false);
        EPServiceProvider engine = EPServiceProviderManager.getDefaultProvider(config);

        engine.getEPAdministrator().getConfiguration().addEventType(TransactionEvent.class);
        engine.getEPAdministrator().getConfiguration().addPlugInSingleRowFunction("MyDynModel", this.getClass().getName(), "MyDynModel");

        String epl = "select MyDynModel({col_001, col_002, col_003}) as model_score from TransactionEvent";
        EPStatement stmt = engine.getEPAdministrator().createEPL(epl);
        stmt.setSubscriber(new MySubscriber());

        LinkedBlockingDeque<Runnable> queue = new LinkedBlockingDeque<Runnable>();
        CountDownLatch latch = new CountDownLatch(numEvents);
        for (int i = 0; i < numEvents; i++) {
            queue.add(new MyRunnable(engine.getEPRuntime(), latch, new TransactionEvent(1,2,3)));
        }

        long startTime = System.currentTimeMillis();
        ThreadPoolExecutor threads = new ThreadPoolExecutor(numThreads, numThreads, 10, TimeUnit.SECONDS, queue);
        threads.prestartAllCoreThreads();
        latch.await(1, TimeUnit.MINUTES);
        if (latch.getCount() > 0) {
            throw new RuntimeException("Failed to complete in 1 minute");
        }
        long delta = System.currentTimeMillis() - startTime;
        System.out.println("Took " + delta + " millis");
        threads.shutdownNow();
    }

    public static int MyDynModel(Object[] args) {
        try {
            Thread.sleep(1);
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static class MySubscriber {
        public void update(Object[] args) {}
    }

    public static class MyRunnable implements Runnable {

        private final EPRuntime runtime;
        private final CountDownLatch latch;
        private final Object event;

        public MyRunnable(EPRuntime runtime, CountDownLatch latch, Object event) {
            this.runtime = runtime;
            this.latch = latch;
            this.event = event;
        }

        public void run() {
            try {
                runtime.sendEvent(event);
            }
            catch (RuntimeException ex) {
                ex.printStackTrace();
            }
            finally {
                latch.countDown();
            }
        }
    }

    public static class TransactionEvent {
        private final int col_001;
        private final int col_002;
        private final int col_003;

        public TransactionEvent(int col_001, int col_002, int col_003) {
            this.col_001 = col_001;
            this.col_002 = col_002;
            this.col_003 = col_003;
        }

        public int getCol_001() {
            return col_001;
        }

        public int getCol_002() {
            return col_002;
        }

        public int getCol_003() {
            return col_003;
        }
    }
}
