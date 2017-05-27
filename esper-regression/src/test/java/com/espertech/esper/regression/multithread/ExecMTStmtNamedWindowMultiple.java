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
package com.espertech.esper.regression.multithread;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import java.util.Random;

import static org.junit.Assert.assertTrue;

/**
 * Test for multithread-safety for a simple aggregation case using count(*).
 */
public class ExecMTStmtNamedWindowMultiple implements RegressionExecution {
    public void configure(Configuration configuration) throws Exception {
        configuration.addEventType("OrderEvent", OrderEvent.class.getName());
        configuration.addEventType("OrderCancelEvent", OrderCancelEvent.class.getName());
    }

    public void run(EPServiceProvider epService) throws Exception {
        tryCount(epService, 10, 500, 3);
    }

    public void tryCount(EPServiceProvider epService, int numUsers, int numOrders, int numThreads) throws Exception {
        for (int i = 0; i < numUsers; i++) {
            epService.getEPAdministrator().createEPL("@Name('create_" + i + "') create window MyWindow_" + i + "#unique(orderId) as select * from OrderEvent");
            epService.getEPAdministrator().createEPL("@Name('insert_" + i + "') insert into MyWindow_" + i + " select * from OrderEvent(userId = 'user" + i + "')");
            epService.getEPAdministrator().createEPL("on OrderCancelEvent as d delete from MyWindow_" + i + " w where w.orderId = d.orderId");
            epService.getEPAdministrator().createEPL("@Name('select_" + i + "') on OrderEvent as s select sum(w.price) from MyWindow_" + i + " w where w.side = s.side group by w.side");
        }

        RunnableOrderSim[] runnables = new RunnableOrderSim[numThreads];
        Thread[] threads = new Thread[numThreads];
        for (int i = 0; i < numThreads; i++) {
            runnables[i] = new RunnableOrderSim(epService, i, numUsers, numOrders);
            threads[i] = new Thread(runnables[i]);
        }

        for (int i = 0; i < threads.length; i++) {
            threads[i].start();
        }

        for (int i = 0; i < threads.length; i++) {
            threads[i].join();
            assertTrue(runnables[i].getStatus());
        }
    }

    public class RunnableOrderSim implements Runnable {
        private final EPServiceProvider engine;
        private final int threadId;
        private final int numUsers;
        private final int numOrders;
        private final Random random = new Random();
        private boolean status;

        public RunnableOrderSim(EPServiceProvider engine, int threadId, int numUsers, int numOrders) {
            this.engine = engine;
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
                        engine.getEPRuntime().sendEvent(theEvent);
                    }
                } else {
                    String orderId = orderIds[random.nextInt(orderIds.length)];
                    for (int j = 0; j < numUsers; j++) {
                        OrderEvent theEvent = new OrderEvent("user" + j, orderId, 1000, "B");
                        engine.getEPRuntime().sendEvent(theEvent);
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