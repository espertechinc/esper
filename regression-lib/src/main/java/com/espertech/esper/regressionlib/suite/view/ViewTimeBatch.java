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
package com.espertech.esper.regressionlib.suite.view;

import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.client.util.DateTime;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.support.bean.SupportMarketDataBean;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Random;

import static org.junit.Assert.assertNull;

public class ViewTimeBatch {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ViewTimeBatchSceneOne());
        execs.add(new ViewTimeBatch10Sec());
        execs.add(new ViewTimeBatchStartEagerForceUpdateSceneTwo());
        execs.add(new ViewTimeBatchMonthScoped());
        execs.add(new ViewTimeBatchStartEagerForceUpdate());
        execs.add(new ViewTimeBatchLonger());
        execs.add(new ViewTimeBatchMultirow());
        execs.add(new ViewTimeBatchMultiBatch());
        execs.add(new ViewTimeBatchNoRefPoint());
        execs.add(new ViewTimeBatchRefPoint());
        return execs;
    }

    private static class ViewTimeBatchSceneOne implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            sendTimer(env, 0);

            String[] fields = "symbol".split(",");
            String text = "@name('s0') select irstream * from SupportMarketDataBean#time_batch(1 sec)";
            env.compileDeployAddListenerMileZero(text, "s0");

            sendTimer(env, 1500);
            env.assertListenerNotInvoked("s0");

            env.milestone(1);

            // Tell the runtimethe time after a join point as using external timer
            sendTimer(env, 1500);
            env.sendEventBean(makeMarketDataEvent("E1"));

            env.milestone(2);

            sendTimer(env, 1700);

            env.milestone(3);

            env.sendEventBean(makeMarketDataEvent("E2"));

            env.milestone(4);

            sendTimer(env, 2499);

            env.milestone(5);

            sendTimer(env, 2500);
            env.assertListener("s0", listener -> {
                EPAssertionUtil.assertPropsPerRow(listener.getNewDataListFlattened(), fields, new Object[][]{{"E1"}, {"E2"}});
                assertNull(listener.getLastOldData());
                listener.reset();
            });

            env.milestone(6);

            env.sendEventBean(makeMarketDataEvent("E3"));
            env.sendEventBean(makeMarketDataEvent("E4"));

            env.milestone(7);

            sendTimer(env, 2600);
            env.sendEventBean(makeMarketDataEvent("E5"));

            env.milestone(8);

            // test iterator
            env.assertPropsPerRowIterator("s0", fields, new Object[][]{{"E3"}, {"E4"}, {"E5"}});

            sendTimer(env, 3500);
            env.assertListener("s0", listener -> {
                EPAssertionUtil.assertPropsPerRow(listener.getNewDataListFlattened(), fields, new Object[][]{{"E3"}, {"E4"}, {"E5"}});
                EPAssertionUtil.assertPropsPerRow(listener.getOldDataListFlattened(), fields, new Object[][]{{"E1"}, {"E2"}});
                listener.reset();
            });

            env.milestone(9);

            sendTimer(env, 4500);
            env.assertListener("s0", listener -> {
                EPAssertionUtil.assertPropsPerRow(listener.getNewDataListFlattened(), fields, null);
                EPAssertionUtil.assertPropsPerRow(listener.getOldDataListFlattened(), fields, new Object[][]{{"E3"}, {"E4"}, {"E5"}});
                listener.reset();
            });

            env.milestone(10);

            sendTimer(env, 5500);
            env.assertListenerNotInvoked("s0");

            env.milestone(11);

            env.sendEventBean(makeMarketDataEvent("E6"));

            env.milestone(12);

            sendTimer(env, 6500);
            env.assertListener("s0", listener -> {
                EPAssertionUtil.assertPropsPerRow(listener.getNewDataListFlattened(), fields, new Object[][]{{"E6"}});
                EPAssertionUtil.assertPropsPerRow(listener.getOldDataListFlattened(), fields, null);
                listener.reset();
            });

            env.milestone(13);

            sendTimer(env, 7500);
            env.assertListener("s0", listener -> {
                EPAssertionUtil.assertPropsPerRow(listener.getNewDataListFlattened(), fields, null);
                EPAssertionUtil.assertPropsPerRow(listener.getOldDataListFlattened(), fields, new Object[][]{{"E6"}});
                listener.reset();
            });

            env.milestone(14);

            env.sendEventBean(makeMarketDataEvent("E7"));

            env.milestone(15);

            sendTimer(env, 8500);
            env.assertListener("s0", listener -> {
                EPAssertionUtil.assertPropsPerRow(listener.getNewDataListFlattened(), fields, new Object[][]{{"E7"}});
                EPAssertionUtil.assertPropsPerRow(listener.getOldDataListFlattened(), fields, null);
                listener.reset();
            });

            env.milestone(16);

            env.undeployAll();
        }
    }

    public static class ViewTimeBatch10Sec implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "theString".split(",");

            sendTimer(env, 0);
            String epl = "@Name('s0') select irstream * from SupportBean#time_batch(10 sec)";
            env.compileDeployAddListenerMileZero(epl, "s0");

            env.assertPropsPerRowIterator("s0", fields, null);

            sendTimer(env, 1000);
            sendSupportBean(env, "E1");
            env.assertListenerNotInvoked("s0");

            env.milestone(1);

            env.assertPropsPerRowIterator("s0", fields, new Object[][]{{"E1"}});
            sendTimer(env, 2000);
            sendSupportBean(env, "E2");
            env.assertListenerNotInvoked("s0");

            env.milestone(2);

            env.assertPropsPerRowIterator("s0", fields, new Object[][]{{"E1"}, {"E2"}});
            sendTimer(env, 10999);
            env.assertListenerNotInvoked("s0");

            env.milestone(3);

            sendTimer(env, 11000);   // push a batch
            env.assertPropsPerRowLastNew("s0", fields, new Object[][]{{"E1"}, {"E2"}});

            env.milestone(4);

            env.assertPropsPerRowIterator("s0", fields, new Object[0][]);
            sendSupportBean(env, "E3");
            env.assertListenerNotInvoked("s0");

            env.milestone(5);

            env.assertPropsPerRowIterator("s0", fields, new Object[][]{{"E3"}});
            sendTimer(env, 21000);   // push a batch
            env.assertPropsPerRowIRPair("s0", fields, new Object[][]{{"E3"}}, new Object[][]{{"E1"}, {"E2"}});

            env.milestone(6);

            sendTimer(env, 31000);   // push a batch
            env.assertPropsPerRowIRPair("s0", fields, null, new Object[][]{{"E3"}});

            env.milestone(7);

            sendTimer(env, 41000);   // push a batch
            env.assertListenerNotInvoked("s0");

            env.undeployAll();
        }
    }

    private static class ViewTimeBatchMonthScoped implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            sendCurrentTime(env, "2002-02-01T09:00:00.000");

            env.compileDeployAddListenerMileZero("@name('s0') select * from SupportBean#time_batch(1 month)", "s0");

            env.sendEventBean(new SupportBean("E1", 1));
            sendCurrentTimeWithMinus(env, "2002-03-01T09:00:00.000", 1);
            env.assertListenerNotInvoked("s0");

            sendCurrentTime(env, "2002-03-01T09:00:00.000");
            env.assertPropsNew("s0", "theString".split(","), new Object[]{"E1"});

            env.sendEventBean(new SupportBean("E2", 1));
            sendCurrentTimeWithMinus(env, "2002-04-01T09:00:00.000", 1);
            env.assertListenerNotInvoked("s0");

            sendCurrentTime(env, "2002-04-01T09:00:00.000");
            env.assertPropsNew("s0", "theString".split(","), new Object[]{"E2"});

            env.sendEventBean(new SupportBean("E3", 1));
            sendCurrentTime(env, "2002-05-01T09:00:00.000");
            env.assertPropsNew("s0", "theString".split(","), new Object[]{"E3"});

            env.undeployAll();
        }
    }

    private static class ViewTimeBatchStartEagerForceUpdate implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            sendTimer(env, 1000);

            String epl = "@name('s0') select irstream * from SupportBean#time_batch(1, \"START_EAGER,FORCE_UPDATE\")";
            env.compileDeployAddListenerMileZero(epl, "s0");

            sendTimer(env, 1999);
            env.assertListenerNotInvoked("s0");
            env.assertListenerNotInvoked("s0");

            sendTimer(env, 2000);
            env.assertListenerInvoked("s0");

            sendTimer(env, 2999);
            env.assertListenerNotInvoked("s0");

            sendTimer(env, 3000);
            env.assertListenerInvoked("s0");

            env.sendEventBean(new SupportBean("E1", 1));
            env.assertListenerNotInvoked("s0");

            sendTimer(env, 4000);
            env.assertPropsNew("s0", "theString".split(","), new Object[]{"E1"});

            sendTimer(env, 5000);
            env.assertPropsOld("s0", "theString".split(","), new Object[]{"E1"});

            sendTimer(env, 5999);
            env.assertListenerNotInvoked("s0");

            sendTimer(env, 6000);
            env.assertListenerInvoked("s0");

            sendTimer(env, 7000);
            env.assertListenerInvoked("s0");

            env.undeployAll();
        }
    }

    private static class ViewTimeBatchStartEagerForceUpdateSceneTwo implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            sendTimer(env, 0);

            String text = "@name('s0') select irstream symbol from SupportMarketDataBean#time_batch(1 sec, \"START_EAGER, FORCE_UPDATE\")";
            env.compileDeployAddListenerMileZero(text, "s0");

            sendTimer(env, 1000);
            env.assertListenerInvoked("s0");

            sendTimer(env, 2000);
            env.assertListenerInvoked("s0");

            sendTimer(env, 2700);
            env.sendEventBean(makeMarketDataEvent("E1"));
            sendTimer(env, 2900);
            env.sendEventBean(makeMarketDataEvent("E2"));

            env.milestone(1);

            sendTimer(env, 3000);
            env.assertPropsPerRowLastNew("s0", new String[]{"symbol"}, new Object[][]{{"E1"}, {"E2"}});

            env.milestone(2);

            sendTimer(env, 4000);
            env.assertPropsPerRowLastOld("s0", new String[]{"symbol"}, new Object[][]{{"E1"}, {"E2"}});

            sendTimer(env, 5000);
            env.assertListenerInvoked("s0");

            env.undeployAll();
        }
    }

    private static class ViewTimeBatchLonger implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String text = "@name('s0') select irstream * from SupportMarketDataBean#time_batch(1 sec)";
            env.compileDeploy(text).addListener("s0");

            Random random = new Random();
            int count = 0;
            int sec = 0;
            sendTimer(env, 0);

            for (int i = 0; i < 20; i++) {
                int numEvents = random.nextInt() % 10;
                if (numEvents > 6) {
                    numEvents = 0;
                }

                sendTimer(env, sec);
                for (int j = 0; j < numEvents; j++) {
                    env.sendEventBean(makeMarketDataEvent("E_" + count));
                    count++;
                }

                env.milestone(i);
                sec += 1000;
            }

            env.undeployAll();
        }
    }

    private static class ViewTimeBatchMultirow implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "theString".split(",");

            sendTimer(env, 0);
            String epl = "@Name('s0') select irstream * from SupportBean#time_batch(10 sec)";
            env.compileDeployAddListenerMileZero(epl, "s0");

            env.assertPropsPerRowIterator("s0", fields, null);

            sendTimer(env, 1000);
            sendSupportBean(env, "E1");
            sendSupportBean(env, "E2");
            env.assertListenerNotInvoked("s0");

            env.milestone(1);
            env.assertPropsPerRowIterator("s0", fields, new Object[][]{{"E1"}, {"E2"}});

            sendTimer(env, 2000);
            sendSupportBean(env, "E3");
            env.assertListenerNotInvoked("s0");

            env.milestone(2);
            env.assertPropsPerRowIterator("s0", fields, new Object[][]{{"E1"}, {"E2"}, {"E3"}});

            sendTimer(env, 3000);
            sendSupportBean(env, "E4");
            env.milestone(3);

            env.milestone(4);

            env.assertPropsPerRowIterator("s0", fields, new Object[][]{{"E1"}, {"E2"}, {"E3"}, {"E4"}});
            env.assertListenerNotInvoked("s0");

            sendTimer(env, 11000);
            env.assertPropsPerRowIRPair("s0", fields, new Object[][]{{"E1"}, {"E2"}, {"E3"}, {"E4"}}, null);

            env.milestone(5);

            sendTimer(env, 21000);
            env.assertPropsPerRowIRPair("s0", fields, null, new Object[][]{{"E1"}, {"E2"}, {"E3"}, {"E4"}});

            env.milestone(6);

            sendTimer(env, 31000);
            env.assertListenerNotInvoked("s0");

            sendSupportBean(env, "E5");
            env.assertListenerNotInvoked("s0");

            env.milestone(7);

            env.assertPropsPerRowIterator("s0", fields, new Object[][]{{"E5"}});

            sendTimer(env, 41000);
            env.assertPropsPerRowIRPair("s0", fields, new Object[][]{{"E5"}}, null);

            env.undeployAll();
        }
    }

    private static class ViewTimeBatchMultiBatch implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "theString".split(",");

            sendTimer(env, 0);
            String epl = "@Name('s0') select irstream * from SupportBean#time_batch(10 sec)";
            env.compileDeploy(epl).addListener("s0");

            sendTimer(env, 1000);
            sendSupportBean(env, "E1");
            sendSupportBean(env, "E2");
            env.assertListenerNotInvoked("s0");

            env.milestone(0);

            sendTimer(env, 11000);
            env.assertPropsPerRowLastNew("s0", fields, new Object[][]{{"E1"}, {"E2"}});

            sendSupportBean(env, "E3");
            sendSupportBean(env, "E4");

            env.milestone(1);

            sendTimer(env, 21000);
            env.assertPropsPerRowIRPair("s0", fields, new Object[][]{{"E3"}, {"E4"}}, new Object[][]{{"E1"}, {"E2"}});

            sendSupportBean(env, "E5");
            sendSupportBean(env, "E6");

            env.milestone(2);

            sendTimer(env, 31000);
            env.assertPropsPerRowIRPair("s0", fields, new Object[][]{{"E5"}, {"E6"}}, new Object[][]{{"E3"}, {"E4"}});

            env.milestone(3);

            sendSupportBean(env, "E7");
            sendSupportBean(env, "E8");
            sendTimer(env, 41000);

            env.assertPropsPerRowIRPair("s0", fields, new Object[][]{{"E7"}, {"E8"}}, new Object[][]{{"E5"}, {"E6"}});

            env.milestone(4);

            sendTimer(env, 51000);
            env.assertPropsPerRowIRPair("s0", fields, null, new Object[][]{{"E7"}, {"E8"}});

            sendTimer(env, 61000);
            env.assertListenerNotInvoked("s0");

            env.undeployAll();
        }
    }

    private static class ViewTimeBatchNoRefPoint implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select * from SupportBean#time_batch(10 minutes)";
            env.compileDeploy(epl).addListener("s0");

            env.advanceTime(0);

            sendEvent(env);

            sendTimerAssertNotInvoked(env, 10 * 60 * 1000 - 1);
            sendTimerAssertInvoked(env, 10 * 60 * 1000);

            env.undeployAll();
        }
    }

    private static class ViewTimeBatchRefPoint implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.advanceTime(00);

            String epl = "@name('s0') select * from SupportBean#time_batch(10 minutes, 10L)";
            env.compileDeployAddListenerMileZero(epl, "s0");

            env.advanceTime(10);

            sendEvent(env);

            sendTimerAssertNotInvoked(env, 10 * 60 * 1000 - 1 + 10);
            sendTimerAssertInvoked(env, 10 * 60 * 1000 + 10);

            env.undeployAll();
        }
    }

    private static void sendTimer(RegressionEnvironment env, long timeInMSec) {
        env.advanceTime(timeInMSec);
    }

    private static void sendCurrentTime(RegressionEnvironment env, String time) {
        sendTimer(env, DateTime.parseDefaultMSec(time));
    }

    private static void sendCurrentTimeWithMinus(RegressionEnvironment env, String time, long minus) {
        sendTimer(env, DateTime.parseDefaultMSec(time) - minus);
    }

    private static void sendSupportBean(RegressionEnvironment env, String e1) {
        env.sendEventBean(new SupportBean(e1, 0));
    }

    private static SupportMarketDataBean makeMarketDataEvent(String symbol) {
        return new SupportMarketDataBean(symbol, 0, 0L, null);
    }

    private static void sendEvent(RegressionEnvironment env) {
        SupportBean theEvent = new SupportBean();
        env.sendEventBean(theEvent);
    }

    private static void sendTimerAssertNotInvoked(RegressionEnvironment env, long timeInMSec) {
        env.advanceTime(timeInMSec);
        env.assertListenerNotInvoked("s0");
    }

    private static void sendTimerAssertInvoked(RegressionEnvironment env, long timeInMSec) {
        env.advanceTime(timeInMSec);
        env.assertListenerInvoked("s0");
    }
}
