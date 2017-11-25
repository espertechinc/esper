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

import com.espertech.esper.client.*;
import com.espertech.esper.supportregression.client.SupportConfigFactory;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.supportregression.util.SupportMTUpdateListener;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ExecMTStmtNamedWindowUniqueTwoWJoinConsumer implements RegressionExecution {

    private int count;

    public void run(EPServiceProvider epService) throws Exception {
        runAssertion(1, true, null, null);
        runAssertion(2, false, true, ConfigurationEngineDefaults.Threading.Locking.SPIN);
        runAssertion(3, false, true, ConfigurationEngineDefaults.Threading.Locking.SUSPEND);
        runAssertion(4, false, false, null);
    }

    private void runAssertion(int engineNum, boolean useDefault, Boolean preserve, ConfigurationEngineDefaults.Threading.Locking locking) throws Exception {
        Configuration config = SupportConfigFactory.getConfiguration();
        if (!useDefault) {
            config.getEngineDefaults().getThreading().setNamedWindowConsumerDispatchPreserveOrder(preserve);
            config.getEngineDefaults().getThreading().setNamedWindowConsumerDispatchLocking(locking);
            config.getEngineDefaults().getThreading().setNamedWindowConsumerDispatchTimeout(100000);
        }

        EPServiceProvider epService = EPServiceProviderManager.getProvider(this.getClass().getSimpleName() + "_" + engineNum + "_" + (count++), config);
        epService.initialize();
        epService.getEPAdministrator().getConfiguration().addEventType(EventOne.class);
        epService.getEPAdministrator().getConfiguration().addEventType(EventTwo.class);

        String epl =
                "create window EventOneWindow#unique(key) as EventOne;\n" +
                        "insert into EventOneWindow select * from EventOne;\n" +
                        "create window EventTwoWindow#unique(key) as EventTwo;\n" +
                        "insert into EventTwoWindow select * from EventTwo;\n" +
                        "@name('out') select * from EventOneWindow as e1, EventTwoWindow as e2 where e1.key = e2.key";
        epService.getEPAdministrator().getDeploymentAdmin().parseDeploy(epl);

        SupportMTUpdateListener listener = new SupportMTUpdateListener();
        epService.getEPAdministrator().getStatement("out").addListener(listener);

        Runnable runnableOne = new Runnable() {
            public void run() {
                for (int i = 0; i < 33; i++) {
                    EventOne eventOne = new EventOne("TEST");
                    epService.getEPRuntime().sendEvent(eventOne);
                    EventTwo eventTwo = new EventTwo("TEST");
                    epService.getEPRuntime().sendEvent(eventTwo);
                }
            }
        };
        Runnable runnableTwo = new Runnable() {
            public void run() {
                for (int i = 0; i < 33; i++) {
                    EventTwo eventTwo = new EventTwo("TEST");
                    epService.getEPRuntime().sendEvent(eventTwo);
                    EventOne eventOne = new EventOne("TEST");
                    epService.getEPRuntime().sendEvent(eventOne);
                }
            }
        };
        Runnable runnableThree = new Runnable() {
            public void run() {
                for (int i = 0; i < 34; i++) {
                    EventTwo eventTwo = new EventTwo("TEST");
                    epService.getEPRuntime().sendEvent(eventTwo);
                    EventOne eventOne = new EventOne("TEST");
                    epService.getEPRuntime().sendEvent(eventOne);
                }
            }
        };

        Thread t1 = new Thread(runnableOne);
        Thread t2 = new Thread(runnableTwo);
        Thread t3 = new Thread(runnableThree);
        t1.start();
        t2.start();
        t3.start();
        Thread.sleep(1000);

        t1.join();
        t2.join();
        t3.join();
        Thread.sleep(200);

        List<EventBean[]> delivered = listener.getNewDataList();

        // count deliveries of multiple rows
        int countMultiDeliveries = 0;
        for (EventBean[] events : delivered) {
            countMultiDeliveries += events.length > 1 ? 1 : 0;
        }

        // count deliveries where instance doesn't monotonically increase from previous row for one column
        int countNotMonotone = 0;
        Long previousIdE1 = null;
        Long previousIdE2 = null;
        for (EventBean[] events : delivered) {
            long idE1 = (Long) events[0].get("e1.instance");
            long idE2 = (Long) events[0].get("e2.instance");
            // comment-in when needed: System.out.println("Received " + idE1 + " " + idE2);

            if (previousIdE1 != null) {
                boolean incorrect = idE1 != previousIdE1 && idE2 != previousIdE2;
                if (!incorrect) {
                    incorrect = idE1 == previousIdE1 && idE2 != (previousIdE2 + 1) ||
                            (idE2 == previousIdE2 && idE1 != (previousIdE1 + 1));
                }
                if (incorrect) {
                    // comment-in when needed: System.out.println("Non-Monotone increase (this is still correct but noteworthy)");
                    countNotMonotone++;
                }
            }

            previousIdE1 = idE1;
            previousIdE2 = idE2;
        }

        if (useDefault || preserve) {
            assertEquals("multiple row deliveries: " + countMultiDeliveries, 0, countMultiDeliveries);
            // the number of non-monotone delivers should be small but not zero
            // this is because when the event get generated and when the event actually gets processed may not be in the same order
            assertTrue("count not monotone: " + countNotMonotone, countNotMonotone < 100);
            assertTrue(delivered.size() >= 197); // its possible to not have 199 since there may not be events on one side of the join
        } else {
            assertTrue("multiple row deliveries: " + countMultiDeliveries, countMultiDeliveries > 0);
            assertTrue("count not monotone: " + countNotMonotone, countNotMonotone > 5);
        }

        epService.destroy();
    }

    public static class EventOne {

        private static final AtomicLong ATOMIC_LONG = new AtomicLong(1);
        private final long instance;
        private final String key;

        private EventOne(String key) {
            instance = ATOMIC_LONG.getAndIncrement();
            this.key = key;
        }

        public String getKey() {
            return key;
        }

        public long getInstance() {
            return instance;
        }

        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof EventOne)) return false;

            EventOne eventOne = (EventOne) o;

            return key.equals(eventOne.key);
        }

        public int hashCode() {
            return key.hashCode();
        }
    }

    public static class EventTwo {

        private static final AtomicLong ATOMIC_LONG = new AtomicLong(1);

        private final long instance;
        private final String key;

        public EventTwo(String key) {
            instance = ATOMIC_LONG.getAndIncrement();
            this.key = key;
        }

        public long getInstance() {
            return instance;
        }

        public String getKey() {
            return key;
        }

        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof EventTwo)) return false;

            EventTwo eventTwo = (EventTwo) o;

            return key.equals(eventTwo.key);

        }

        public int hashCode() {
            return key.hashCode();
        }
    }
}