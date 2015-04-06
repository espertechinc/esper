/*
 * *************************************************************************************
 *  Copyright (C) 2006-2015 EsperTech, Inc. All rights reserved.                       *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 * *************************************************************************************
 */

package com.espertech.esper.multithread;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.support.client.SupportConfigFactory;
import junit.framework.TestCase;

import java.util.Random;

/**
 * Test for multithread-safety for a simple aggregation case using count(*).
 */
public class TestMTStmtNamedWindowMultiple extends TestCase
{
    private EPServiceProvider engine;

    public void setUp()
    {
        Configuration config = SupportConfigFactory.getConfiguration();
        config.addEventType("OrderEvent", OrderEvent.class.getName());
        config.addEventType("OrderCancelEvent", OrderCancelEvent.class.getName());
        engine = EPServiceProviderManager.getProvider("TestMTStmtNamedWindowMultiple", config);
    }

    public void tearDown()
    {
        engine.destroy();
    }

    public void testInsertDeleteSelect() throws Exception
    {
        tryCount(10, 500, 3);
    }

    public void tryCount(int numUsers, int numOrders, int numThreads) throws Exception
    {
        for (int i = 0; i < numUsers; i++)
        {
            engine.getEPAdministrator().createEPL("@Name('create_" + i + "') create window MyWindow_" + i + ".std:unique(orderId) as select * from OrderEvent");
            engine.getEPAdministrator().createEPL("@Name('insert_" + i + "') insert into MyWindow_" + i + " select * from OrderEvent(userId = 'user" + i + "')");
            engine.getEPAdministrator().createEPL("on OrderCancelEvent as d delete from MyWindow_" + i + " w where w.orderId = d.orderId");
            engine.getEPAdministrator().createEPL("@Name('select_" + i + "') on OrderEvent as s select sum(w.price) from MyWindow_" + i + " w where w.side = s.side group by w.side");
        }

        RunnableOrderSim[] runnables = new RunnableOrderSim[numThreads];
        Thread[] threads = new Thread[numThreads];
        for (int i = 0; i < numThreads; i++)
        {
            runnables[i] = new RunnableOrderSim(engine, i, numUsers, numOrders);
            threads[i] = new Thread(runnables[i]);
        }

        for (int i = 0; i < threads.length; i++)
        {
            threads[i].start();
        }

        for (int i = 0; i < threads.length; i++)
        {
            threads[i].join();
            assertTrue(runnables[i].getStatus());
        }
    }

    public class RunnableOrderSim implements Runnable
    {
        private final EPServiceProvider engine;
        private final int threadId;
        private final int numUsers;
        private final int numOrders;
        private final Random random = new Random();
        private boolean status;

        public RunnableOrderSim(EPServiceProvider engine, int threadId, int numUsers, int numOrders)
        {
            this.engine = engine;
            this.threadId = threadId;
            this.numUsers = numUsers;
            this.numOrders = numOrders;
        }

        public void run()
        {
            String[] orderIds = new String[10];
            for (int i = 0; i < orderIds.length; i++)
            {
                orderIds[i] = "order_" + i + "_" + threadId;
            }

            for (int i = 0; i < numOrders; i++)
            {
                if (random.nextInt() % 3 == 0)
                {
                    String orderId = orderIds[random.nextInt(orderIds.length)];
                    for (int j = 0; j < numUsers; j++)
                    {
                        OrderCancelEvent theEvent = new OrderCancelEvent("user" + j, orderId);
                        engine.getEPRuntime().sendEvent(theEvent);
                    }
                }
                else
                {
                    String orderId = orderIds[random.nextInt(orderIds.length)];
                    for (int j = 0; j < numUsers; j++)
                    {
                        OrderEvent theEvent = new OrderEvent("user" + j, orderId, 1000, "B");
                        engine.getEPRuntime().sendEvent(theEvent);
                    }
                }
            }

            status = true;
        }

        public boolean getStatus()
        {
            return status;
        }
    }

    public class OrderEvent
    {
        private final String userId;
        private final String orderId;
        private final double price;
        private final String side;

        public OrderEvent(String userId, String orderId, double price, String side)
        {
            this.userId = userId;
            this.orderId = orderId;
            this.price = price;
            this.side = side;
        }

        public String getUserId()
        {
            return userId;
        }

        public String getOrderId()
        {
            return orderId;
        }

        public double getPrice()
        {
            return price;
        }

        public String getSide()
        {
            return side;
        }
    }

    public class OrderCancelEvent
    {
        private final String userId;
        private final String orderId;

        public OrderCancelEvent(String userId, String orderId)
        {
            this.userId = userId;
            this.orderId = orderId;
        }

        public String getUserId()
        {
            return userId;
        }

        public String getOrderId()
        {
            return orderId;
        }
    }
}