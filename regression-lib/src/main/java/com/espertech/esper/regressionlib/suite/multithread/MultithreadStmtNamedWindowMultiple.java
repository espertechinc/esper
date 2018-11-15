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
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.regressionlib.support.client.SupportCompileDeployUtil;
import com.espertech.esper.runtime.client.EPRuntime;

import java.util.Random;

import static org.junit.Assert.assertTrue;

/**
 * Test for multithread-safety for a simple aggregation case using count(*).
 */
public class MultithreadStmtNamedWindowMultiple implements RegressionExecution {

    @Override
    public boolean excludeWhenInstrumented() {
        return true;
    }

    public void run(RegressionEnvironment env) {
        tryCount(env, 10, 500, 3);
    }

    public void tryCount(RegressionEnvironment env, int numUsers, int numOrders, int numThreads) {
        RegressionPath path = new RegressionPath();
        for (int i = 0; i < numUsers; i++) {
            env.compileDeploy("@Name('create_" + i + "') create window MyWindow_" + i + "#unique(orderId) as select * from OrderEvent", path);
            env.compileDeploy("@Name('insert_" + i + "') insert into MyWindow_" + i + " select * from OrderEvent(userId = 'user" + i + "')", path);
            env.compileDeploy("on OrderCancelEvent as d delete from MyWindow_" + i + " w where w.orderId = d.orderId", path);
            env.compileDeploy("@Name('select_" + i + "') on OrderEvent as s select sum(w.price) from MyWindow_" + i + " w where w.side = s.side group by w.side", path);
        }

        RunnableOrderSim[] runnables = new RunnableOrderSim[numThreads];
        Thread[] threads = new Thread[numThreads];
        for (int i = 0; i < numThreads; i++) {
            runnables[i] = new RunnableOrderSim(env.runtime(), i, numUsers, numOrders);
            threads[i] = new Thread(runnables[i], MultithreadStmtNamedWindowMultiple.class.getSimpleName());
        }

        for (int i = 0; i < threads.length; i++) {
            threads[i].start();
        }

        for (int i = 0; i < threads.length; i++) {
            SupportCompileDeployUtil.threadJoin(threads[i]);
            assertTrue(runnables[i].getStatus());
        }

        env.undeployAll();
    }

    public class RunnableOrderSim implements Runnable {
        private final EPRuntime runtime;
        private final int threadId;
        private final int numUsers;
        private final int numOrders;
        private final Random random = new Random();
        private boolean status;

        public RunnableOrderSim(EPRuntime runtime, int threadId, int numUsers, int numOrders) {
            this.runtime = runtime;
            this.threadId = threadId;
            this.numUsers = numUsers;
            this.numOrders = numOrders;
        }

        public void run() {
            String[] orderIds = new String[10];
            for (int i = 0; i < orderIds.length; i++) {
                orderIds[i] = "order_" + i + "_" + threadId;
            }

            for (int i = 0; i < numOrders; i++) {
                if (random.nextInt() % 3 == 0) {
                    String orderId = orderIds[random.nextInt(orderIds.length)];
                    for (int j = 0; j < numUsers; j++) {
                        OrderCancelEvent theEvent = new OrderCancelEvent("user" + j, orderId);
                        runtime.getEventService().sendEventBean(theEvent, theEvent.getClass().getSimpleName());
                    }
                } else {
                    String orderId = orderIds[random.nextInt(orderIds.length)];
                    for (int j = 0; j < numUsers; j++) {
                        OrderEvent theEvent = new OrderEvent("user" + j, orderId, 1000, "B");
                        runtime.getEventService().sendEventBean(theEvent, theEvent.getClass().getSimpleName());
                    }
                }
            }

            status = true;
        }

        public boolean getStatus() {
            return status;
        }
    }

    public class OrderEvent {
        private final String userId;
        private final String orderId;
        private final double price;
        private final String side;

        public OrderEvent(String userId, String orderId, double price, String side) {
            this.userId = userId;
            this.orderId = orderId;
            this.price = price;
            this.side = side;
        }

        public String getUserId() {
            return userId;
        }

        public String getOrderId() {
            return orderId;
        }

        public double getPrice() {
            return price;
        }

        public String getSide() {
            return side;
        }
    }

    public class OrderCancelEvent {
        private final String userId;
        private final String orderId;

        public OrderCancelEvent(String userId, String orderId) {
            this.userId = userId;
            this.orderId = orderId;
        }

        public String getUserId() {
            return userId;
        }

        public String getOrderId() {
            return orderId;
        }
    }
}