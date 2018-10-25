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
package com.espertech.esper.regressionlib.suite.resultset.outputlimit;

import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.client.soda.*;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.support.bean.SupportBeanNumeric;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.common.internal.support.SupportBean_S1;
import org.junit.Assert;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import static com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil.tryInvalidCompile;
import static org.junit.Assert.assertFalse;

public class ResultSetOutputLimitRowLimit {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ResultSetLimitOneWithOrderOptimization());
        execs.add(new ResultSetBatchNoOffsetNoOrder());
        execs.add(new ResultSetOrderBy());
        execs.add(new ResultSetBatchOffsetNoOrderOM());
        execs.add(new ResultSetFullyGroupedOrdered());
        execs.add(new ResultSetEventPerRowUnGrouped());
        execs.add(new ResultSetGroupedSnapshot());
        execs.add(new ResultSetGroupedSnapshotNegativeRowcount());
        execs.add(new ResultSetInvalid());
        execs.add(new ResultSetLengthOffsetVariable());
        return execs;
    }

    private static class ResultSetLimitOneWithOrderOptimization implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            // batch-window assertions
            RegressionPath path = new RegressionPath();
            String eplWithBatchSingleKey = "@name('s0') select theString from SupportBean#length_batch(10) order by theString limit 1";
            tryAssertionLimitOneSingleKeySortBatch(env, path, eplWithBatchSingleKey);

            String eplWithBatchMultiKey = "@name('s0') select theString, intPrimitive from SupportBean#length_batch(5) order by theString asc, intPrimitive desc limit 1";
            tryAssertionLimitOneMultiKeySortBatch(env, path, eplWithBatchMultiKey);

            // context output-when-terminated assertions
            env.compileDeploy("create context StartS0EndS1 as start SupportBean_S0 end SupportBean_S1", path);

            String eplContextSingleKey = "@name('s0') context StartS0EndS1 " +
                "select theString from SupportBean#keepall " +
                "output snapshot when terminated " +
                "order by theString limit 1";
            tryAssertionLimitOneSingleKeySortBatch(env, path, eplContextSingleKey);

            String eplContextMultiKey = "@name('s0') context StartS0EndS1 " +
                "select theString, intPrimitive from SupportBean#keepall " +
                "output snapshot when terminated " +
                "order by theString asc, intPrimitive desc limit 1";
            tryAssertionLimitOneMultiKeySortBatch(env, path, eplContextMultiKey);

            env.undeployAll();
        }

        private static void tryAssertionLimitOneMultiKeySortBatch(RegressionEnvironment env, RegressionPath path, String epl) {
            env.compileDeploy(epl, path).addListener("s0");

            sendSBSequenceAndAssert(env, "F", 10, new Object[][]{{"F", 10}, {"X", 8}, {"F", 8}, {"G", 10}, {"X", 1}});
            sendSBSequenceAndAssert(env, "G", 12, new Object[][]{{"X", 10}, {"G", 12}, {"H", 100}, {"G", 10}, {"X", 1}});
            sendSBSequenceAndAssert(env, "G", 11, new Object[][]{{"G", 10}, {"G", 8}, {"G", 8}, {"G", 10}, {"G", 11}});

            env.undeployModuleContaining("s0");
        }

        private static void tryAssertionLimitOneSingleKeySortBatch(RegressionEnvironment env, RegressionPath path, String epl) {
            env.compileDeploy(epl, path).addListener("s0");

            sendSBSequenceAndAssert(env, "A", new String[]{"F", "Q", "R", "T", "M", "T", "A", "I", "P", "B"});
            sendSBSequenceAndAssert(env, "B", new String[]{"P", "Q", "P", "T", "P", "T", "P", "P", "P", "B"});
            sendSBSequenceAndAssert(env, "C", new String[]{"C", "P", "Q", "P", "T", "P", "T", "P", "P", "P", "X"});

            env.undeployModuleContaining("s0");
        }
    }

    private static class ResultSetBatchNoOffsetNoOrder implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select irstream * from SupportBean#length_batch(3) limit 1";
            env.compileDeploy(epl).addListener("s0");

            tryAssertion(env);
            env.undeployAll();
        }
    }

    private static class ResultSetLengthOffsetVariable implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("create variable int myrows = 2", path);
            env.compileDeploy("create variable int myoffset = 1", path);
            env.compileDeploy("on SupportBeanNumeric set myrows = intOne, myoffset = intTwo", path);

            String epl;

            epl = "@name('s0') select * from SupportBean#length(5) output every 5 events limit myoffset, myrows";
            env.compileDeploy(epl, path).addListener("s0");
            tryAssertionVariable(env);
            env.undeployModuleContaining("s0");

            env.sendEventBean(new SupportBeanNumeric(2, 1));

            epl = "@name('s0') select * from SupportBean#length(5) output every 5 events limit myrows offset myoffset";
            env.compileDeploy(epl, path).addListener("s0");
            tryAssertionVariable(env);
            env.undeployModuleContaining("s0");

            env.sendEventBean(new SupportBeanNumeric(2, 1));

            env.eplToModelCompileDeploy(epl, path).addListener("s0");
            tryAssertionVariable(env);

            env.undeployAll();
        }
    }

    private static class ResultSetOrderBy implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select * from SupportBean#length(5) output every 5 events order by intPrimitive limit 2 offset 2";
            env.compileDeploy(epl).addListener("s0");

            String[] fields = "theString".split(",");

            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, null);

            sendEvent(env, "E1", 90);
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, null);

            sendEvent(env, "E2", 5);
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, null);

            sendEvent(env, "E3", 60);
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"E1"}});

            sendEvent(env, "E4", 99);
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"E1"}, {"E4"}});
            assertFalse(env.listener("s0").isInvoked());

            sendEvent(env, "E5", 6);
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"E3"}, {"E1"}});
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, new Object[][]{{"E3"}, {"E1"}});

            env.undeployAll();
        }
    }

    private static class ResultSetBatchOffsetNoOrderOM implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            EPStatementObjectModel model = new EPStatementObjectModel();
            model.setSelectClause(SelectClause.createWildcard());
            model.getSelectClause().setStreamSelector(StreamSelector.RSTREAM_ISTREAM_BOTH);
            model.setFromClause(FromClause.create(FilterStream.create("SupportBean").addView("length_batch", Expressions.constant(3))));
            model.setRowLimitClause(RowLimitClause.create(1));

            String epl = "select irstream * from SupportBean#length_batch(3) limit 1";
            Assert.assertEquals(epl, model.toEPL());

            model.setAnnotations(Collections.singletonList(AnnotationPart.nameAnnotation("s0")));
            env.compileDeploy(model).addListener("s0");

            tryAssertion(env);
            env.undeployAll();

            env.eplToModelCompileDeploy("@name('s0') " + epl).addListener("s0");
            tryAssertion(env);
            env.undeployAll();
        }
    }

    private static class ResultSetFullyGroupedOrdered implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select theString, sum(intPrimitive) as mysum from SupportBean#length(5) group by theString order by sum(intPrimitive) limit 2";
            env.compileDeploy(epl).addListener("s0");

            String[] fields = "theString,mysum".split(",");

            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, null);

            sendEvent(env, "E1", 90);
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"E1", 90}});

            sendEvent(env, "E2", 5);
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"E2", 5}, {"E1", 90}});

            sendEvent(env, "E3", 60);
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"E2", 5}, {"E3", 60}});

            sendEvent(env, "E3", 40);
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"E2", 5}, {"E1", 90}});

            sendEvent(env, "E2", 1000);
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"E1", 90}, {"E3", 100}});

            env.undeployAll();
        }
    }

    private static class ResultSetEventPerRowUnGrouped implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            sendTimer(env, 1000);
            String epl = "@name('s0') select theString, sum(intPrimitive) as mysum from SupportBean#length(5) output every 10 seconds order by theString desc limit 2";
            env.compileDeploy(epl).addListener("s0");

            String[] fields = "theString,mysum".split(",");

            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, null);

            sendEvent(env, "E1", 10);
            sendEvent(env, "E2", 5);
            sendEvent(env, "E3", 20);
            sendEvent(env, "E4", 30);

            sendTimer(env, 11000);
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getLastNewData(), fields, new Object[][]{{"E4", 65}, {"E3", 35}});

            env.undeployAll();
        }
    }

    private static class ResultSetGroupedSnapshot implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            sendTimer(env, 1000);
            String epl = "@name('s0') select theString, sum(intPrimitive) as mysum from SupportBean#length(5) group by theString output snapshot every 10 seconds order by sum(intPrimitive) desc limit 2";
            env.compileDeploy(epl).addListener("s0");

            String[] fields = "theString,mysum".split(",");

            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, null);

            sendEvent(env, "E1", 10);
            sendEvent(env, "E2", 5);
            sendEvent(env, "E3", 20);
            sendEvent(env, "E1", 30);

            sendTimer(env, 11000);
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getLastNewData(), fields, new Object[][]{{"E1", 40}, {"E3", 20}});

            env.undeployAll();
        }
    }

    private static class ResultSetGroupedSnapshotNegativeRowcount implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            sendTimer(env, 1000);
            String epl = "@name('s0') select theString, sum(intPrimitive) as mysum from SupportBean#length(5) group by theString output snapshot every 10 seconds order by sum(intPrimitive) desc limit -1 offset 1";
            env.compileDeploy(epl).addListener("s0");

            String[] fields = "theString,mysum".split(",");

            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, null);

            sendEvent(env, "E1", 10);
            sendEvent(env, "E2", 5);
            sendEvent(env, "E3", 20);
            sendEvent(env, "E1", 30);

            sendTimer(env, 11000);
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getLastNewData(), fields, new Object[][]{{"E3", 20}, {"E2", 5}});

            env.undeployAll();
        }
    }

    private static class ResultSetInvalid implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("create variable string myrows = 'abc'", path);
            tryInvalidCompile(env, path, "select * from SupportBean limit myrows",
                "Limit clause requires a variable of numeric type [select * from SupportBean limit myrows]");
            tryInvalidCompile(env, path, "select * from SupportBean limit 1, myrows",
                "Limit clause requires a variable of numeric type [select * from SupportBean limit 1, myrows]");
            tryInvalidCompile(env, path, "select * from SupportBean limit dummy",
                "Limit clause variable by name 'dummy' has not been declared [select * from SupportBean limit dummy]");
            tryInvalidCompile(env, path, "select * from SupportBean limit 1,dummy",
                "Limit clause variable by name 'dummy' has not been declared [select * from SupportBean limit 1,dummy]");
            env.undeployAll();
        }
    }

    private static void sendTimer(RegressionEnvironment env, long timeInMSec) {
        env.advanceTime(timeInMSec);
    }

    private static void tryAssertionVariable(RegressionEnvironment env) {
        String[] fields = "theString".split(",");

        EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, null);

        sendEvent(env, "E1", 1);
        sendEvent(env, "E2", 2);
        EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"E2"}});

        sendEvent(env, "E3", 3);
        EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"E2"}, {"E3"}});

        sendEvent(env, "E4", 4);
        EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"E2"}, {"E3"}});
        assertFalse(env.listener("s0").isInvoked());

        sendEvent(env, "E5", 5);
        EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"E2"}, {"E3"}});
        EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, new Object[][]{{"E2"}, {"E3"}});

        sendEvent(env, "E6", 6);
        EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"E3"}, {"E4"}});
        assertFalse(env.listener("s0").isInvoked());

        // change variable values
        env.sendEventBean(new SupportBeanNumeric(2, 3));
        sendEvent(env, "E7", 7);
        EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"E6"}, {"E7"}});
        assertFalse(env.listener("s0").isInvoked());

        env.sendEventBean(new SupportBeanNumeric(-1, 0));
        sendEvent(env, "E8", 8);
        EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"E4"}, {"E5"}, {"E6"}, {"E7"}, {"E8"}});
        assertFalse(env.listener("s0").isInvoked());

        env.sendEventBean(new SupportBeanNumeric(10, 0));
        sendEvent(env, "E9", 9);
        EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"E5"}, {"E6"}, {"E7"}, {"E8"}, {"E9"}});
        assertFalse(env.listener("s0").isInvoked());

        env.sendEventBean(new SupportBeanNumeric(6, 3));
        sendEvent(env, "E10", 10);
        EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"E9"}, {"E10"}});
        EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, new Object[][]{{"E9"}, {"E10"}});

        env.sendEventBean(new SupportBeanNumeric(1, 1));
        EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"E7"}});

        env.sendEventBean(new SupportBeanNumeric(2, 1));
        EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"E7"}, {"E8"}});

        env.sendEventBean(new SupportBeanNumeric(1, 2));
        EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"E8"}});

        env.sendEventBean(new SupportBeanNumeric(6, 6));
        EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, null);

        env.sendEventBean(new SupportBeanNumeric(1, 4));
        EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"E10"}});

        env.sendEventBean(new SupportBeanNumeric((Integer) null, null));
        EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"E6"}, {"E7"}, {"E8"}, {"E9"}, {"E10"}});

        env.sendEventBean(new SupportBeanNumeric(null, 2));
        EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"E8"}, {"E9"}, {"E10"}});

        env.sendEventBean(new SupportBeanNumeric(2, null));
        EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"E6"}, {"E7"}});

        env.sendEventBean(new SupportBeanNumeric(-1, 4));
        EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"E10"}});

        env.sendEventBean(new SupportBeanNumeric(-1, 0));
        EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"E6"}, {"E7"}, {"E8"}, {"E9"}, {"E10"}});

        env.sendEventBean(new SupportBeanNumeric(0, 0));
        EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, null);
    }

    private static void tryAssertion(RegressionEnvironment env) {
        String[] fields = "theString".split(",");

        sendEvent(env, "E1", 1);
        EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"E1"}});

        sendEvent(env, "E2", 2);
        assertFalse(env.listener("s0").isInvoked());
        EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"E1"}});

        sendEvent(env, "E3", 3);
        EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, new Object[][]{{"E1"}});
        EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, null);

        sendEvent(env, "E4", 4);
        EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"E4"}});

        sendEvent(env, "E5", 5);
        EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"E4"}});

        sendEvent(env, "E6", 6);
        EPAssertionUtil.assertPropsPerRow(env.listener("s0").getLastNewData(), fields, new Object[][]{{"E4"}});
        EPAssertionUtil.assertPropsPerRow(env.listener("s0").getLastOldData(), fields, new Object[][]{{"E1"}});
        EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, null);
    }

    private static void sendEvent(RegressionEnvironment env, String theString, int intPrimitive) {
        env.sendEventBean(new SupportBean(theString, intPrimitive));
    }

    private static void sendSBSequenceAndAssert(RegressionEnvironment env, String expected, String[] theStrings) {
        env.sendEventBean(new SupportBean_S0(0));
        for (String theString : theStrings) {
            sendEvent(env, theString, 0);
        }
        env.sendEventBean(new SupportBean_S1(0));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "theString".split(","), new Object[]{expected});
    }

    private static void sendSBSequenceAndAssert(RegressionEnvironment env, String expectedString, int expectedInt, Object[][] rows) {
        env.sendEventBean(new SupportBean_S0(0));
        for (Object[] row : rows) {
            sendEvent(env, row[0].toString(), (Integer) row[1]);
        }
        env.sendEventBean(new SupportBean_S1(0));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "theString,intPrimitive".split(","), new Object[]{expectedString, expectedInt});
    }
}
