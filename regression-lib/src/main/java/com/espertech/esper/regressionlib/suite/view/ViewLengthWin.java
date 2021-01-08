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
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.support.bean.SupportBeanComplexProps;
import com.espertech.esper.regressionlib.support.bean.SupportMarketDataBean;

import java.util.ArrayList;
import java.util.Collection;

import static org.junit.Assert.assertEquals;

public class ViewLengthWin {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ViewLengthWindowSceneOne());
        execs.add(new ViewLengthWindowWPrevPrior());
        execs.add(new ViewLengthWinWPropertyDetail());
        execs.add(new ViewLengthWindowIterator());
        return execs;
    }

    public static class ViewLengthWindowSceneOne implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "theString".split(",");

            env.milestone(0);

            String epl = "@Name('s0') select irstream * from SupportBean#length(2)";
            env.compileDeployAddListenerMile(epl, "s0", 1);

            env.assertPropsPerRowIterator("s0", fields, null);

            sendSupportBean(env, "E1");
            env.assertPropsNew("s0", fields, new Object[]{"E1"});

            env.milestone(2);
            env.assertPropsPerRowIterator("s0", fields, new Object[][]{{"E1"}});

            sendSupportBean(env, "E2");
            env.assertPropsNew("s0", fields, new Object[]{"E2"});

            env.milestone(3);
            env.assertPropsPerRowIterator("s0", fields, new Object[][]{{"E1"}, {"E2"}});

            sendSupportBean(env, "E3");
            env.assertPropsIRPair("s0", fields, new Object[]{"E3"}, new Object[]{"E1"});

            env.milestone(4);
            env.assertPropsPerRowIterator("s0", fields, new Object[][]{{"E2"}, {"E3"}});

            env.milestone(5);
            env.assertPropsPerRowIterator("s0", fields, new Object[][]{{"E2"}, {"E3"}});

            sendSupportBean(env, "E4");
            env.assertPropsIRPair("s0", fields, new Object[]{"E4"}, new Object[]{"E2"});
            env.assertPropsPerRowIterator("s0", fields, new Object[][]{{"E3"}, {"E4"}});

            env.undeployAll();
        }
    }

    public static class ViewLengthWindowWPrevPrior implements RegressionExecution {

        public void run(RegressionEnvironment env) {
            String text = "@name('s0') select irstream symbol, " +
                "prev(1, symbol) as prev1, " +
                "prior(1, symbol) as prio1, " +
                "prevtail(symbol) as prevtail0, " +
                "prevcount(symbol) as prevCountSym, " +
                "prevwindow(symbol) as prevWindowSym " +
                "from SupportMarketDataBean.win:length(2)";
            env.compileDeployAddListenerMileZero(text, "s0");
            String[] fields = new String[]{"symbol", "prev1", "prio1", "prevtail0", "prevCountSym", "prevWindowSym"};

            env.sendEventBean(makeMarketDataEvent("E1"));
            env.assertPropsNew("s0", fields, new Object[]{"E1", null, null, "E1", 1L, new Object[]{"E1"}});

            env.milestone(1);

            env.sendEventBean(makeMarketDataEvent("E2"));
            env.assertPropsNew("s0", fields, new Object[]{"E2", "E1", "E1", "E1", 2L, new Object[]{"E2", "E1"}});

            env.milestone(2);

            for (int i = 3; i < 10; i++) {
                env.sendEventBean(makeMarketDataEvent("E" + i));

                env.assertPropsNV("s0", new Object[][]{{"symbol", "E" + i}, {"prev1", "E" + (i - 1)}, {"prio1", "E" + (i - 1)}, {"prevtail0", "E" + (i - 1)}}, // new data
                    new Object[][]{{"symbol", "E" + (i - 2)}, {"prev1", null}, {"prevtail0", null}} //  old data
                );

                env.milestone(i);
            }

            // Lets try the iterator
            env.assertIterator("s0", events -> {
                for (int i = 8; i < 10; i++) {
                    EventBean event = events.next();
                    assertEquals("E" + i, event.get("symbol"));
                }
            });
            env.undeployAll();
        }
    }

    private static class ViewLengthWinWPropertyDetail implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select mapped('keyOne') as a," +
                "indexed[1] as b, nested.nestedNested.nestedNestedValue as c, mapProperty, " +
                "arrayProperty[0] " +
                "  from SupportBeanComplexProps#length(3) " +
                " where mapped('keyOne') = 'valueOne' and " +
                " indexed[1] = 2 and " +
                " nested.nestedNested.nestedNestedValue = 'nestedNestedValue'";
            env.compileDeployAddListenerMileZero(epl, "s0");

            SupportBeanComplexProps eventObject = SupportBeanComplexProps.makeDefaultBean();
            env.sendEventBean(eventObject);
            env.assertEventNew("s0", theEvent -> {
                assertEquals(eventObject.getMapped("keyOne"), theEvent.get("a"));
                assertEquals(eventObject.getIndexed(1), theEvent.get("b"));
                assertEquals(eventObject.getNested().getNestedNested().getNestedNestedValue(), theEvent.get("c"));
                assertEquals(eventObject.getMapProperty(), theEvent.get("mapProperty"));
                assertEquals(eventObject.getArrayProperty()[0], theEvent.get("arrayProperty[0]"));
            });

            env.milestone(1);

            eventObject.setIndexed(1, Integer.MIN_VALUE);
            env.assertListenerNotInvoked("s0");
            env.sendEventBean(eventObject);
            env.assertListenerNotInvoked("s0");

            env.milestone(2);

            eventObject.setIndexed(1, 2);
            env.sendEventBean(eventObject);
            env.assertListenerInvoked("s0");

            env.undeployAll();
        }
    }

    private static class ViewLengthWindowIterator implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "symbol,price".split(",");
            String epl = "@name('s0') select symbol, price from SupportMarketDataBean#length(2)";
            env.compileDeployAddListenerMileZero(epl, "s0");

            sendEvent(env, "ABC", 20);
            sendEvent(env, "DEF", 100);

            env.assertPropsPerRowIterator("s0", fields, new Object[][] {{"ABC", 20d}, {"DEF", 100d}});

            sendEvent(env, "EFG", 50);

            env.milestone(1);

            env.assertPropsPerRowIterator("s0", fields, new Object[][] {{"DEF", 100d}, {"EFG", 50d}, });

            env.undeployAll();
        }
    }

    private static SupportMarketDataBean makeMarketDataEvent(String symbol) {
        return new SupportMarketDataBean(symbol, 0, 0L, null);
    }

    private static void sendSupportBean(RegressionEnvironment env, String theString) {
        env.sendEventBean(new SupportBean(theString, 0));
    }

    private static void sendEvent(RegressionEnvironment env, String symbol, double price) {
        SupportMarketDataBean theEvent = new SupportMarketDataBean(symbol, price, 0L, "feed1");
        env.sendEventBean(theEvent);
    }
}
