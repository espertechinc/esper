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

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.support.bean.SupportBean_A;
import com.espertech.esper.regressionlib.support.bean.SupportMarketDataBean;

import java.util.ArrayList;
import java.util.Collection;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

public class ViewLengthBatch {
    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ViewLengthBatchSceneOne());
        execs.add(new ViewLengthBatchSize2());
        execs.add(new ViewLengthBatchSize1());
        execs.add(new ViewLengthBatchSize3());
        execs.add(new ViewLengthBatchInvalid());
        execs.add(new ViewLengthBatchNormal(ViewLengthBatchNormalRunType.VIEW, null));
        execs.add(new ViewLengthBatchPrev());
        execs.add(new ViewLengthBatchDelete());
        execs.add(new ViewLengthBatchNormal(ViewLengthBatchNormalRunType.NAMEDWINDOW, null));
        execs.add(new ViewLengthBatchNormal(ViewLengthBatchNormalRunType.GROUPWIN, null));
        return execs;
    }

    private static class ViewLengthBatchSceneOne implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String text = "@name('s0') select irstream * from SupportMarketDataBean#length_batch(3)";
            env.compileDeployAddListenerMileZero(text, "s0");

            env.sendEventBean(makeMarketDataEvent("E1"));

            env.milestone(1);

            env.sendEventBean(makeMarketDataEvent("E2"));

            env.milestone(2);

            env.sendEventBean(makeMarketDataEvent("E3"));
            EventBean[] newEvents = env.listener("s0").getNewDataListFlattened();
            EPAssertionUtil.assertPropsPerRow(newEvents, new String[]{"symbol"},
                new Object[][]{{"E1"}, {"E2"}, {"E3"}});
            assertNull(env.listener("s0").getLastOldData());
            env.listener("s0").reset();

            env.milestone(3);

            env.sendEventBean(makeMarketDataEvent("E4"));

            env.milestone(4);

            env.sendEventBean(makeMarketDataEvent("E5"));

            env.milestone(5);

            // test iterator
            EventBean[] events = EPAssertionUtil.iteratorToArray(env.iterator("s0"));
            EPAssertionUtil.assertPropsPerRow(events, new String[]{"symbol"}, new Object[][]{{"E4"}, {"E5"}});

            env.sendEventBean(makeMarketDataEvent("E6"));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getNewDataListFlattened(), "symbol".split(","), new Object[][]{{"E4"}, {"E5"}, {"E6"}});
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getOldDataListFlattened(), "symbol".split(","), new Object[][]{{"E1"}, {"E2"}, {"E3"}});
            env.listener("s0").reset();

            env.milestone(6);

            env.sendEventBean(makeMarketDataEvent("E7"));

            env.milestone(7);

            env.sendEventBean(makeMarketDataEvent("E8"));

            env.milestone(8);

            env.sendEventBean(makeMarketDataEvent("E9"));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getNewDataListFlattened(), "symbol".split(","), new Object[][]{{"E7"}, {"E8"}, {"E9"}});
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getOldDataListFlattened(), "symbol".split(","), new Object[][]{{"E4"}, {"E5"}, {"E6"}});
            env.listener("s0").reset();

            env.milestone(9);

            env.sendEventBean(makeMarketDataEvent("E10"));

            env.milestone(10);

            env.undeployAll();
        }
    }

    private static class ViewLengthBatchSize2 implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select irstream * from SupportBean#length_batch(2)";
            env.compileDeployAddListenerMileZero(epl, "s0");
            SupportBean[] events = get10Events();

            sendEvent(events[0], env);
            assertFalse(env.listener("s0").isInvoked());
            EPAssertionUtil.assertEqualsExactOrderUnderlying(new SupportBean[]{events[0]}, env.statement("s0").iterator());

            sendEvent(events[1], env);
            EPAssertionUtil.assertUnderlyingPerRow(env.listener("s0").assertInvokedAndReset(), new SupportBean[]{events[0], events[1]}, null);
            EPAssertionUtil.assertEqualsExactOrderUnderlying(null, env.statement("s0").iterator());

            sendEvent(events[2], env);
            assertFalse(env.listener("s0").isInvoked());
            EPAssertionUtil.assertEqualsExactOrderUnderlying(new SupportBean[]{events[2]}, env.statement("s0").iterator());

            sendEvent(events[3], env);
            EPAssertionUtil.assertUnderlyingPerRow(env.listener("s0").assertInvokedAndReset(), new SupportBean[]{events[2], events[3]}, new SupportBean[]{events[0], events[1]});
            EPAssertionUtil.assertEqualsExactOrderUnderlying(null, env.statement("s0").iterator());

            sendEvent(events[4], env);
            assertFalse(env.listener("s0").isInvoked());
            EPAssertionUtil.assertEqualsExactOrderUnderlying(new SupportBean[]{events[4]}, env.statement("s0").iterator());

            sendEvent(events[5], env);
            EPAssertionUtil.assertUnderlyingPerRow(env.listener("s0").assertInvokedAndReset(), new SupportBean[]{events[4], events[5]}, new SupportBean[]{events[2], events[3]});
            EPAssertionUtil.assertEqualsExactOrderUnderlying(null, env.statement("s0").iterator());

            env.undeployAll();
        }
    }

    private static class ViewLengthBatchSize1 implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select irstream * from SupportBean#length_batch(1)";
            env.compileDeployAddListenerMileZero(epl, "s0");
            SupportBean[] events = get10Events();

            sendEvent(events[0], env);
            EPAssertionUtil.assertUnderlyingPerRow(env.listener("s0").assertInvokedAndReset(), new SupportBean[]{events[0]}, null);
            EPAssertionUtil.assertEqualsExactOrderUnderlying(null, env.statement("s0").iterator());

            sendEvent(events[1], env);
            EPAssertionUtil.assertUnderlyingPerRow(env.listener("s0").assertInvokedAndReset(), new SupportBean[]{events[1]}, new SupportBean[]{events[0]});
            EPAssertionUtil.assertEqualsExactOrderUnderlying(null, env.statement("s0").iterator());

            sendEvent(events[2], env);
            EPAssertionUtil.assertUnderlyingPerRow(env.listener("s0").assertInvokedAndReset(), new SupportBean[]{events[2]}, new SupportBean[]{events[1]});
            EPAssertionUtil.assertEqualsExactOrderUnderlying(null, env.statement("s0").iterator());

            env.undeployAll();
        }
    }

    private static class ViewLengthBatchSize3 implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select irstream * from SupportBean#length_batch(3)";
            env.compileDeployAddListenerMileZero(epl, "s0");
            SupportBean[] events = get10Events();

            sendEvent(events[0], env);
            assertFalse(env.listener("s0").isInvoked());
            EPAssertionUtil.assertEqualsExactOrderUnderlying(new SupportBean[]{events[0]}, env.statement("s0").iterator());

            sendEvent(events[1], env);
            assertFalse(env.listener("s0").isInvoked());
            EPAssertionUtil.assertEqualsExactOrderUnderlying(new SupportBean[]{events[0], events[1]}, env.statement("s0").iterator());

            sendEvent(events[2], env);
            EPAssertionUtil.assertUnderlyingPerRow(env.listener("s0").assertInvokedAndReset(), new SupportBean[]{events[0], events[1], events[2]}, null);
            EPAssertionUtil.assertEqualsExactOrderUnderlying(null, env.statement("s0").iterator());

            sendEvent(events[3], env);
            assertFalse(env.listener("s0").isInvoked());
            EPAssertionUtil.assertEqualsExactOrderUnderlying(new SupportBean[]{events[3]}, env.statement("s0").iterator());

            sendEvent(events[4], env);
            assertFalse(env.listener("s0").isInvoked());
            EPAssertionUtil.assertEqualsExactOrderUnderlying(new SupportBean[]{events[3], events[4]}, env.statement("s0").iterator());

            sendEvent(events[5], env);
            EPAssertionUtil.assertUnderlyingPerRow(env.listener("s0").assertInvokedAndReset(), new SupportBean[]{events[3], events[4], events[5]}, new SupportBean[]{events[0], events[1], events[2]});
            EPAssertionUtil.assertEqualsExactOrderUnderlying(null, env.statement("s0").iterator());

            env.undeployAll();
        }
    }

    private static class ViewLengthBatchInvalid implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            SupportMessageAssertUtil.tryInvalidCompile(env, "select * from SupportMarketDataBean#length_batch(0)",
                "Failed to validate data window declaration: Error in view 'length_batch', Length-Batch view requires a positive integer for size but received 0");
        }
    }

    public static class ViewLengthBatchPrev implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String text = "@name('s0') select irstream *, " +
                "prev(1, symbol) as prev1, " +
                "prevtail(0, symbol) as prevTail0, " +
                "prevtail(1, symbol) as prevTail1, " +
                "prevcount(symbol) as prevCountSym, " +
                "prevwindow(symbol) as prevWindowSym " +
                "from SupportMarketDataBean#length_batch(3)";
            env.compileDeployAddListenerMileZero(text, "s0");

            String[] fields = new String[]{"symbol", "prev1", "prevTail0", "prevTail1", "prevCountSym", "prevWindowSym"};
            env.sendEventBean(makeMarketDataEvent("E1"));
            env.sendEventBean(makeMarketDataEvent("E2"));

            env.milestone(1);

            env.sendEventBean(makeMarketDataEvent("E3"));
            EventBean[] newEvents = env.listener("s0").getNewDataListFlattened();
            Object[] win = new Object[]{"E3", "E2", "E1"};
            EPAssertionUtil.assertPropsPerRow(newEvents, fields,
                new Object[][]{{"E1", null, "E1", "E2", 3L, win}, {"E2", "E1", "E1", "E2", 3L, win}, {"E3", "E2", "E1", "E2", 3L, win}});
            assertNull(env.listener("s0").getLastOldData());
            env.listener("s0").reset();

            env.undeployAll();
        }
    }

    public static class ViewLengthBatchDelete implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "theString".split(",");

            String epl = "create window ABCWin#length_batch(3) as SupportBean;\n" +
                "insert into ABCWin select * from SupportBean;\n" +
                "on SupportBean_A delete from ABCWin where theString = id;\n" +
                "@Name('s0') select irstream * from ABCWin;\n";
            env.compileDeployAddListenerMileZero(epl, "s0");

            EPAssertionUtil.assertPropsPerRow(env.iterator("s0"), fields, null);

            sendSupportBean(env, "E1");
            assertFalse(env.listener("s0").isInvoked());

            env.milestone(1);
            EPAssertionUtil.assertPropsPerRow(env.iterator("s0"), fields, new Object[][]{{"E1"}});

            sendSupportBean_A(env, "E1");    // delete
            assertFalse(env.listener("s0").isInvoked());  // batch is quiet-delete

            env.milestone(2);
            EPAssertionUtil.assertPropsPerRow(env.iterator("s0"), fields, new Object[0][]);

            sendSupportBean(env, "E2");
            sendSupportBean(env, "E3");
            assertFalse(env.listener("s0").isInvoked());

            env.milestone(3);

            EPAssertionUtil.assertPropsPerRow(env.iterator("s0"), fields, new Object[][]{{"E2"}, {"E3"}});
            sendSupportBean_A(env, "E3");    // delete
            assertFalse(env.listener("s0").isInvoked());  // batch is quiet-delete

            env.milestone(4);

            sendSupportBean(env, "E4");
            assertFalse(env.listener("s0").isInvoked());
            sendSupportBean(env, "E5");
            assertNull(env.listener("s0").getLastOldData());
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, new Object[][]{{"E2"}, {"E4"}, {"E5"}});

            env.milestone(5);
            EPAssertionUtil.assertPropsPerRow(env.iterator("s0"), fields, new Object[0][]);

            sendSupportBean(env, "E6");
            sendSupportBean(env, "E7");
            assertFalse(env.listener("s0").isInvoked());

            env.milestone(6);
            EPAssertionUtil.assertPropsPerRow(env.iterator("s0"), fields, new Object[][]{{"E6"}, {"E7"}});

            sendSupportBean(env, "E8");
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetIRPair(), fields, new Object[][]{{"E6"}, {"E7"}, {"E8"}}, new Object[][]{{"E2"}, {"E4"}, {"E5"}});

            env.undeployAll();
        }
    }

    public static class ViewLengthBatchNormal implements RegressionExecution {
        private final ViewLengthBatchNormalRunType runType;
        private final String optionalDatawindow;

        public ViewLengthBatchNormal(ViewLengthBatchNormalRunType runType, String optionalDatawindow) {
            this.runType = runType;
            this.optionalDatawindow = optionalDatawindow;
        }

        public void run(RegressionEnvironment env) {
            String[] fields = "theString".split(",");

            String epl;
            if (runType == ViewLengthBatchNormalRunType.VIEW) {
                epl = "@Name('s0') select irstream theString, prev(1, theString) as prevString " +
                    "from SupportBean" + (optionalDatawindow == null ? "#length_batch(3)" : optionalDatawindow);
            } else if (runType == ViewLengthBatchNormalRunType.GROUPWIN) {
                epl = "@Name('s0') select irstream * from SupportBean#groupwin(doubleBoxed)#length_batch(3)";
            } else if (runType == ViewLengthBatchNormalRunType.NAMEDWINDOW) {
                epl = "create window ABCWin#length_batch(3) as SupportBean;\n" +
                    "insert into ABCWin select * from SupportBean;\n" +
                    "@Name('s0') select irstream * from ABCWin;\n";
            } else {
                throw new RuntimeException("Unrecognized variant " + runType);
            }
            env.compileDeployAddListenerMileZero(epl, "s0");

            env.milestone(1);
            EPAssertionUtil.assertPropsPerRow(env.iterator("s0"), fields, null);

            sendSupportBean(env, "E1");
            assertFalse(env.listener("s0").isInvoked());

            env.milestone(2);
            EPAssertionUtil.assertPropsPerRow(env.iterator("s0"), fields, new Object[][]{{"E1"}});

            sendSupportBean(env, "E2");
            assertFalse(env.listener("s0").isInvoked());

            env.milestone(3);
            EPAssertionUtil.assertPropsPerRow(env.iterator("s0"), fields, new Object[][]{{"E1"}, {"E2"}});

            sendSupportBean(env, "E3");
            assertNull(env.listener("s0").getLastOldData());
            if (runType == ViewLengthBatchNormalRunType.VIEW) {
                EPAssertionUtil.assertPropsPerRow(env.listener("s0").getLastNewData(), "prevString".split(","), new Object[][]{{null}, {"E1"}, {"E2"}});
            }
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, new Object[][]{{"E1"}, {"E2"}, {"E3"}});

            env.milestone(4);
            EPAssertionUtil.assertPropsPerRow(env.iterator("s0"), fields, new Object[0][]);

            sendSupportBean(env, "E4");
            sendSupportBean(env, "E5");
            assertFalse(env.listener("s0").isInvoked());

            env.milestone(5);
            EPAssertionUtil.assertPropsPerRow(env.iterator("s0"), fields, new Object[][]{{"E4"}, {"E5"}});

            sendSupportBean(env, "E6");
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetIRPair(), fields, new Object[][]{{"E4"}, {"E5"}, {"E6"}}, new Object[][]{{"E1"}, {"E2"}, {"E3"}});

            env.milestone(6);
            EPAssertionUtil.assertPropsPerRow(env.iterator("s0"), fields, new Object[0][]);

            env.milestone(7);

            sendSupportBean(env, "E7");
            sendSupportBean(env, "E8");
            assertFalse(env.listener("s0").isInvoked());

            sendSupportBean(env, "E9");
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetIRPair(), fields, new Object[][]{{"E7"}, {"E8"}, {"E9"}}, new Object[][]{{"E4"}, {"E5"}, {"E6"}});

            env.undeployAll();
        }
    }

    public static enum ViewLengthBatchNormalRunType {
        VIEW,
        GROUPWIN,
        NAMEDWINDOW
    }

    private static void sendSupportBean_A(RegressionEnvironment env, String e3) {
        env.sendEventBean(new SupportBean_A(e3));
    }

    private static void sendSupportBean(RegressionEnvironment env, String e1) {
        env.sendEventBean(new SupportBean(e1, 0));
    }

    private static void sendEvent(SupportBean theEvent, RegressionEnvironment env) {
        env.sendEventBean(theEvent);
    }

    private static SupportBean[] get10Events() {
        SupportBean[] events = new SupportBean[10];
        for (int i = 0; i < events.length; i++) {
            events[i] = new SupportBean();
        }
        return events;
    }

    private static SupportMarketDataBean makeMarketDataEvent(String symbol) {
        return new SupportMarketDataBean(symbol, 0, 0L, null);
    }
}
