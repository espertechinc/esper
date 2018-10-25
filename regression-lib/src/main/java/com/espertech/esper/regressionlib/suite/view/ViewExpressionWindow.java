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
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.support.bean.SupportBean_A;

import java.util.ArrayList;
import java.util.Collection;

import static org.junit.Assert.*;

public class ViewExpressionWindow {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ViewExpressionWindowSceneOne());
        execs.add(new ViewExpressionWindowNewestEventOldestEvent());
        execs.add(new ViewExpressionWindowLengthWindow());
        execs.add(new ViewExpressionWindowTimeWindow());
        execs.add(new ViewExpressionWindowUDFBuiltin());
        execs.add(new ViewExpressionWindowInvalid());
        execs.add(new ViewExpressionWindowPrev());
        execs.add(new ViewExpressionWindowAggregationUngrouped());
        execs.add(new ViewExpressionWindowAggregationWGroupwin());
        execs.add(new ViewExpressionWindowNamedWindowDelete());
        execs.add(new ViewExpressionWindowAggregationWOnDelete());
        execs.add(new ViewExpressionWindowVariable());
        execs.add(new ViewExpressionWindowDynamicTimeWindow());
        return execs;
    }

    public static class ViewExpressionWindowSceneOne implements RegressionExecution {

        public void run(RegressionEnvironment env) {

            String[] fields = "c0".split(",");
            env.advanceTime(0);

            String epl = "@Name('s0') select irstream theString as c0 from SupportBean#expr(newest_timestamp - oldest_timestamp < 1000)";
            env.compileDeploy(epl).addListener("s0").milestone(0);

            env.advanceTime(1000);
            EPAssertionUtil.assertPropsPerRow(env.iterator("s0"), fields, new Object[0][]);
            sendSupportBean(env, "E1");
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E1"});

            env.milestone(1);

            env.advanceTime(1500);
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), fields, new Object[][]{{"E1"}});
            sendSupportBean(env, "E2");
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E2"});

            env.milestone(2);

            env.advanceTime(2000);
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), fields, new Object[][]{{"E1"}, {"E2"}});
            sendSupportBean(env, "E3");
            EPAssertionUtil.assertProps(env.listener("s0").assertGetAndResetIRPair(), fields, new Object[]{"E3"}, new Object[]{"E1"});

            env.milestone(3);

            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), fields, new Object[][]{{"E2"}, {"E3"}});
            env.advanceTime(2499);

            env.milestone(4);

            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), fields, new Object[][]{{"E2"}, {"E3"}});
            sendSupportBean(env, "E4");
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E4"});

            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), fields, new Object[][]{{"E2"}, {"E3"}, {"E4"}});
            env.advanceTime(2500);

            env.milestone(5);

            env.milestone(6);

            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), fields, new Object[][]{{"E2"}, {"E3"}, {"E4"}});
            sendSupportBean(env, "E5");
            EPAssertionUtil.assertProps(env.listener("s0").assertGetAndResetIRPair(), fields, new Object[]{"E5"}, new Object[]{"E2"});
            env.advanceTime(10000);
            sendSupportBean(env, "E6");
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetIRPair(), fields, new Object[][]{{"E6"}},
                new Object[][]{{"E3"}, {"E4"}, {"E5"}});

            env.undeployAll();
        }
    }

    private static class ViewExpressionWindowNewestEventOldestEvent implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            String[] fields = new String[]{"theString"};
            String epl = "@name('s0') select irstream * from SupportBean#expr(newest_event.intPrimitive = oldest_event.intPrimitive)";
            env.compileDeploy(epl).addListener("s0").milestone(0);

            env.sendEventBean(new SupportBean("E1", 1));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E1"});
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"E1"}});

            env.milestone(1);

            env.sendEventBean(new SupportBean("E2", 1));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E2"});
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"E1"}, {"E2"}});

            env.milestone(2);

            env.sendEventBean(new SupportBean("E3", 2));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetDataListsFlattened(), fields,
                new Object[][]{{"E3"}}, new Object[][]{{"E1"}, {"E2"}});
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"E3"}});

            env.milestone(3);

            env.sendEventBean(new SupportBean("E4", 3));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetDataListsFlattened(), fields,
                new Object[][]{{"E4"}}, new Object[][]{{"E3"}});
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"E4"}});

            env.milestone(4);

            env.sendEventBean(new SupportBean("E5", 3));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E5"});
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"E4"}, {"E5"}});

            env.milestone(5);

            env.sendEventBean(new SupportBean("E6", 3));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E6"});
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"E4"}, {"E5"}, {"E6"}});

            env.milestone(6);

            env.sendEventBean(new SupportBean("E7", 2));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetDataListsFlattened(), fields,
                new Object[][]{{"E7"}}, new Object[][]{{"E4"}, {"E5"}, {"E6"}});
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"E7"}});

            env.undeployAll();
        }
    }

    private static class ViewExpressionWindowLengthWindow implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = new String[]{"theString"};
            String epl = "@name('s0') select * from SupportBean#expr(current_count <= 2)";
            env.compileDeploy(epl).addListener("s0");

            env.sendEventBean(new SupportBean("E1", 1));
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"E1"}});

            env.sendEventBean(new SupportBean("E2", 2));
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"E1"}, {"E2"}});

            env.milestone(0);

            env.sendEventBean(new SupportBean("E3", 3));
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"E2"}, {"E3"}});

            env.undeployAll();
        }
    }

    private static class ViewExpressionWindowTimeWindow implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.advanceTime(0);

            String[] fields = new String[]{"theString"};
            String epl = "@name('s0') select irstream * from SupportBean#expr(oldest_timestamp > newest_timestamp - 2000)";
            env.compileDeploy(epl).addListener("s0");

            env.advanceTime(1000);
            env.sendEventBean(new SupportBean("E1", 1));
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"E1"}});
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E1"});

            env.advanceTime(1500);
            env.sendEventBean(new SupportBean("E2", 2));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E2"});

            env.milestone(0);

            env.sendEventBean(new SupportBean("E3", 3));
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"E1"}, {"E2"}, {"E3"}});
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E3"});

            env.advanceTime(2500);
            env.sendEventBean(new SupportBean("E4", 4));
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"E1"}, {"E2"}, {"E3"}, {"E4"}});

            env.advanceTime(3000);
            env.sendEventBean(new SupportBean("E5", 5));
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"E2"}, {"E3"}, {"E4"}, {"E5"}});
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getLastNewData(), fields, new Object[][]{{"E5"}});
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getLastOldData(), fields, new Object[][]{{"E1"}});
            env.listener("s0").reset();

            env.advanceTime(3499);
            env.sendEventBean(new SupportBean("E6", 6));
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"E2"}, {"E3"}, {"E4"}, {"E5"}, {"E6"}});

            env.milestone(1);

            env.advanceTime(3500);
            env.sendEventBean(new SupportBean("E7", 7));
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"E4"}, {"E5"}, {"E6"}, {"E7"}});
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getLastNewData(), fields, new Object[][]{{"E7"}});
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getLastOldData(), fields, new Object[][]{{"E2"}, {"E3"}});
            env.listener("s0").reset();

            env.advanceTime(10000);
            env.sendEventBean(new SupportBean("E8", 8));
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"E8"}});
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getLastNewData(), fields, new Object[][]{{"E8"}});
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getLastOldData(), fields, new Object[][]{{"E4"}, {"E5"}, {"E6"}, {"E7"}});
            env.listener("s0").reset();

            env.undeployAll();
        }
    }

    private static class ViewExpressionWindowVariable implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.advanceTime(0);
            String[] fields = new String[]{"theString"};

            String epl = "create variable boolean KEEP = true;\n" +
                "@name('s0') select irstream * from SupportBean#expr(KEEP);\n";
            env.compileDeploy(epl).addListener("s0");

            env.advanceTime(1000);
            env.sendEventBean(new SupportBean("E1", 1));
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"E1"}});

            env.runtime().getVariableService().setVariableValue(env.deploymentId("s0"), "KEEP", false);
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"E1"}});

            env.listener("s0").reset();
            env.advanceTime(1001);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetOldAndReset(), fields, new Object[]{"E1"});
            assertFalse(env.statement("s0").iterator().hasNext());

            env.milestone(0);

            env.sendEventBean(new SupportBean("E2", 2));
            EPAssertionUtil.assertProps(env.listener("s0").getLastNewData()[0], fields, new Object[]{"E2"});
            EPAssertionUtil.assertProps(env.listener("s0").getLastOldData()[0], fields, new Object[]{"E2"});
            env.listener("s0").reset();
            assertFalse(env.statement("s0").iterator().hasNext());

            env.runtime().getVariableService().setVariableValue(env.deploymentId("s0"), "KEEP", true);

            env.sendEventBean(new SupportBean("E3", 3));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E3"});
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"E3"}});

            env.undeployAll();
        }
    }

    private static class ViewExpressionWindowDynamicTimeWindow implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.advanceTime(0);
            String[] fields = new String[]{"theString"};

            String epl = "create variable long SIZE = 1000;\n" +
                "@name('s0') select irstream * from SupportBean#expr(newest_timestamp - oldest_timestamp < SIZE)";
            env.compileDeploy(epl).addListener("s0").milestone(0);

            env.advanceTime(1000);
            env.sendEventBean(new SupportBean("E1", 0));
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"E1"}});

            env.milestone(1);

            env.advanceTime(2000);
            env.sendEventBean(new SupportBean("E2", 0));
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"E2"}});

            env.runtime().getVariableService().setVariableValue(env.deploymentId("s0"), "SIZE", 10000);

            env.milestone(2);

            env.advanceTime(5000);
            env.sendEventBean(new SupportBean("E3", 0));
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"E2"}, {"E3"}});

            env.milestone(3);

            env.runtime().getVariableService().setVariableValue(env.deploymentId("s0"), "SIZE", 2000);

            env.milestone(4);

            env.advanceTime(6000);
            env.sendEventBean(new SupportBean("E4", 0));
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"E3"}, {"E4"}});

            env.undeployAll();
        }
    }

    private static class ViewExpressionWindowUDFBuiltin implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select * from SupportBean#expr(udf(theString, view_reference, expired_count))";
            env.compileDeploy(epl).addListener("s0");

            LocalUDF.setResult(true);
            env.sendEventBean(new SupportBean("E1", 0));
            assertEquals("E1", LocalUDF.getKey());
            assertEquals(0, (int) LocalUDF.getExpiryCount());
            assertNotNull(LocalUDF.getViewref());

            env.sendEventBean(new SupportBean("E2", 0));

            LocalUDF.setResult(false);
            env.sendEventBean(new SupportBean("E3", 0));
            assertEquals("E3", LocalUDF.getKey());
            assertEquals(2, (int) LocalUDF.getExpiryCount());
            assertNotNull(LocalUDF.getViewref());

            env.undeployAll();
        }
    }

    private static class ViewExpressionWindowInvalid implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            SupportMessageAssertUtil.tryInvalidCompile(env, "select * from SupportBean#expr(1)",
                "Failed to validate data window declaration: Invalid return value for expiry expression, expected a boolean return value but received int [select * from SupportBean#expr(1)]");

            SupportMessageAssertUtil.tryInvalidCompile(env, "select * from SupportBean#expr((select * from SupportBean#lastevent))",
                "Failed to validate data window declaration: Invalid expiry expression: Sub-select, previous or prior functions are not supported in this context [select * from SupportBean#expr((select * from SupportBean#lastevent))]");
        }
    }

    private static class ViewExpressionWindowNamedWindowDelete implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = new String[]{"theString"};
            String epl = "@name('s0') create window NW#expr(true) as SupportBean;\n" +
                "insert into NW select * from SupportBean;\n" +
                "on SupportBean_A delete from NW where theString = id;\n";
            env.compileDeploy(epl).addListener("s0");

            env.sendEventBean(new SupportBean("E1", 1));
            env.sendEventBean(new SupportBean("E2", 2));
            env.sendEventBean(new SupportBean("E3", 3));
            env.listener("s0").reset();
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"E1"}, {"E2"}, {"E3"}});

            env.milestone(0);

            env.sendEventBean(new SupportBean_A("E2"));
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"E1"}, {"E3"}});
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetOldAndReset(), fields, new Object[]{"E2"});

            env.undeployAll();
        }
    }

    private static class ViewExpressionWindowPrev implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = new String[]{"val0"};
            String epl = "@name('s0') select prev(1, theString) as val0 from SupportBean#expr(true)";
            env.compileDeploy(epl).addListener("s0");

            env.sendEventBean(new SupportBean("E1", 1));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{null});

            env.milestone(0);

            env.sendEventBean(new SupportBean("E2", 2));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E1"});

            env.undeployAll();
        }
    }

    private static class ViewExpressionWindowAggregationUngrouped implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = new String[]{"theString"};
            String epl = "@name('s0') select irstream theString from SupportBean#expr(sum(intPrimitive) < 10)";
            env.compileDeploy(epl).addListener("s0");

            env.sendEventBean(new SupportBean("E1", 1));
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"E1"}});
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E1"});

            env.milestone(0);

            env.sendEventBean(new SupportBean("E2", 9));
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"E2"}});
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetDataListsFlattened(), fields, new Object[][]{{"E2"}}, new Object[][]{{"E1"}});

            env.sendEventBean(new SupportBean("E3", 11));
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, null);
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetDataListsFlattened(), fields, new Object[][]{{"E3"}}, new Object[][]{{"E2"}, {"E3"}});

            env.milestone(1);

            env.sendEventBean(new SupportBean("E4", 12));
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, null);
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetDataListsFlattened(), fields, new Object[][]{{"E4"}}, new Object[][]{{"E4"}});

            env.sendEventBean(new SupportBean("E5", 1));
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"E5"}});
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetDataListsFlattened(), fields, new Object[][]{{"E5"}}, null);

            env.milestone(2);

            env.sendEventBean(new SupportBean("E6", 2));
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"E5"}, {"E6"}});
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetDataListsFlattened(), fields, new Object[][]{{"E6"}}, null);

            env.sendEventBean(new SupportBean("E7", 3));
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"E5"}, {"E6"}, {"E7"}});
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetDataListsFlattened(), fields, new Object[][]{{"E7"}}, null);

            env.milestone(3);

            env.sendEventBean(new SupportBean("E8", 6));
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"E7"}, {"E8"}});
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetDataListsFlattened(), fields, new Object[][]{{"E8"}}, new Object[][]{{"E5"}, {"E6"}});

            env.sendEventBean(new SupportBean("E9", 9));
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"E9"}});
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetDataListsFlattened(), fields, new Object[][]{{"E9"}}, new Object[][]{{"E7"}, {"E8"}});

            env.undeployAll();
        }
    }

    private static class ViewExpressionWindowAggregationWGroupwin implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = new String[]{"theString"};
            String epl = "@name('s0') select irstream theString from SupportBean#groupwin(intPrimitive)#expr(sum(longPrimitive) < 10)";
            env.compileDeploy(epl).addListener("s0");

            sendEvent(env, "E1", 1, 5);
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetDataListsFlattened(), fields, new Object[][]{{"E1"}}, null);

            sendEvent(env, "E2", 2, 2);
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetDataListsFlattened(), fields, new Object[][]{{"E2"}}, null);

            sendEvent(env, "E3", 1, 3);
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetDataListsFlattened(), fields, new Object[][]{{"E3"}}, null);

            env.milestone(0);

            sendEvent(env, "E4", 2, 4);
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetDataListsFlattened(), fields, new Object[][]{{"E4"}}, null);

            sendEvent(env, "E5", 2, 6);
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetDataListsFlattened(), fields, new Object[][]{{"E5"}}, new Object[][]{{"E2"}, {"E4"}});

            sendEvent(env, "E6", 1, 2);
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetDataListsFlattened(), fields, new Object[][]{{"E6"}}, new Object[][]{{"E1"}});

            env.undeployAll();
        }
    }

    private static class ViewExpressionWindowAggregationWOnDelete implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = new String[]{"theString"};
            String epl = "@name('s0') create window NW#expr(sum(intPrimitive) < 10) as SupportBean;\n" +
                "insert into NW select * from SupportBean;\n" +
                "on SupportBean_A delete from NW where theString = id;\n";
            env.compileDeploy(epl).addListener("s0");

            env.sendEventBean(new SupportBean("E1", 1));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetDataListsFlattened(), fields, new Object[][]{{"E1"}}, null);

            env.sendEventBean(new SupportBean("E2", 8));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetDataListsFlattened(), fields, new Object[][]{{"E2"}}, null);

            env.milestone(0);

            env.sendEventBean(new SupportBean_A("E2"));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetDataListsFlattened(), fields, null, new Object[][]{{"E2"}});

            env.sendEventBean(new SupportBean("E3", 7));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetDataListsFlattened(), fields, new Object[][]{{"E3"}}, null);

            env.sendEventBean(new SupportBean("E4", 2));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetDataListsFlattened(), fields, new Object[][]{{"E4"}}, new Object[][]{{"E1"}});

            env.undeployAll();
        }
    }

    private static void sendEvent(RegressionEnvironment env, String theString, int intPrimitive, long longPrimitive) {
        SupportBean bean = new SupportBean(theString, intPrimitive);
        bean.setLongPrimitive(longPrimitive);
        env.sendEventBean(bean);
    }

    private static void sendSupportBean(RegressionEnvironment env, String theString) {
        env.sendEventBean(new SupportBean(theString, 0));
    }

    public static class LocalUDF {

        private static String key;
        private static Integer expiryCount;
        private static Object viewref;
        private static boolean result;

        public static boolean evaluateExpiryUDF(String key, Object viewref, Integer expiryCount) {
            LocalUDF.key = key;
            LocalUDF.viewref = viewref;
            LocalUDF.expiryCount = expiryCount;
            return result;
        }

        public static String getKey() {
            return key;
        }

        public static Integer getExpiryCount() {
            return expiryCount;
        }

        public static Object getViewref() {
            return viewref;
        }

        public static void setResult(boolean result) {
            LocalUDF.result = result;
        }
    }
}
