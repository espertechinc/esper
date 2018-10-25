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
import com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.support.bean.SupportMarketDataBean;
import com.espertech.esper.runtime.client.EPStatement;
import com.espertech.esper.runtime.client.scopetest.SupportSubscriber;

import java.util.*;

import static org.junit.Assert.*;

public class ResultSetOutputLimitCrontabWhen {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ResultSetOutputCrontabAt());
        execs.add(new ResultSetOutputCrontabAtOMCreate());
        execs.add(new ResultSetOutputCrontabAtOMCompile());
        execs.add(new ResultSetOutputWhenBuiltInCountInsert());
        execs.add(new ResultSetOutputWhenBuiltInCountRemove());
        execs.add(new ResultSetOutputWhenBuiltInLastTimestamp());
        execs.add(new ResultSetOutputCrontabAtVariable());
        execs.add(new ResultSetOutputWhenExpression());
        execs.add(new ResultSetOutputWhenThenExpression());
        execs.add(new ResultSetOutputWhenThenExpressionSODA());
        execs.add(new ResultSetOutputWhenThenSameVarTwice());
        execs.add(new ResultSetOutputWhenThenWVariable());
        execs.add(new ResultSetOutputWhenThenWCount());
        execs.add(new ResultSetInvalid());
        return execs;
    }

    private static class ResultSetOutputCrontabAtVariable implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            // every 15 minutes 8am to 5pm
            sendTimeEvent(env, 1, 17, 10, 0, 0);
            String epl = "create variable int VFREQ = 15;\n" +
                "create variable int VMIN = 8;\n" +
                "create variable int VMAX = 17;\n" +
                "@name('s0') select * from SupportMarketDataBean#lastevent output at (*/VFREQ, VMIN:VMAX, *, *, *);\n";
            env.compileDeploy(epl).addListener("s0");

            tryAssertionCrontab(env, 1);
        }
    }

    private static class ResultSetOutputCrontabAt implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            // every 15 minutes 8am to 5pm
            sendTimeEvent(env, 1, 17, 10, 0, 0);
            String expression = "@name('s0') select * from SupportMarketDataBean#lastevent output at (*/15, 8:17, *, *, *)";
            env.compileDeploy(expression).addListener("s0");

            tryAssertionCrontab(env, 1);
        }
    }

    private static class ResultSetOutputCrontabAtOMCreate implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            // every 15 minutes 8am to 5pm
            sendTimeEvent(env, 1, 17, 10, 0, 0);
            String expression = "select * from SupportMarketDataBean#lastevent output at (*/15, 8:17, *, *, *)";

            EPStatementObjectModel model = new EPStatementObjectModel();
            model.setSelectClause(SelectClause.createWildcard());
            model.setFromClause(FromClause.create(FilterStream.create("SupportMarketDataBean").addView("lastevent")));
            Expression[] crontabParams = new Expression[]{
                Expressions.crontabScheduleFrequency(15),
                Expressions.crontabScheduleRange(8, 17),
                Expressions.crontabScheduleWildcard(),
                Expressions.crontabScheduleWildcard(),
                Expressions.crontabScheduleWildcard()
            };
            model.setOutputLimitClause(OutputLimitClause.createSchedule(crontabParams));

            String epl = model.toEPL();
            assertEquals(expression, epl);

            model.setAnnotations(Collections.singletonList(AnnotationPart.nameAnnotation("s0")));
            env.compileDeploy(model).addListener("s0");

            tryAssertionCrontab(env, 1);
        }
    }

    private static class ResultSetOutputCrontabAtOMCompile implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            // every 15 minutes 8am to 5pm
            sendTimeEvent(env, 1, 17, 10, 0, 0);
            String expression = "@name('s0') select * from SupportMarketDataBean#lastevent output at (*/15, 8:17, *, *, *)";

            env.eplToModelCompileDeploy(expression).addListener("s0");

            tryAssertionCrontab(env, 1);
        }
    }

    private static class ResultSetOutputWhenThenExpression implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.runtime().getVariableService().setVariableValue(null, "myvar", 0);
            sendTimeEvent(env, 1, 8, 0, 0, 0);
            env.compileDeploy("on SupportBean set myvar = intPrimitive");

            String expression = "@name('s0') select symbol from SupportMarketDataBean#length(2) output when myvar=1 then set myvar=0, count_insert_var=count_insert";
            env.compileDeploy(expression).addListener("s0");
            tryAssertion(env, 1);
            env.undeployAll();
        }
    }

    private static class ResultSetOutputWhenThenExpressionSODA implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.runtime().getVariableService().setVariableValue(null, "myvar", 0);
            sendTimeEvent(env, 1, 8, 0, 0, 0);
            env.compileDeploy("on SupportBean set myvar = intPrimitive");

            String expression = "@name('s0') select symbol from SupportMarketDataBean#length(2) output when myvar=1 then set myvar=0, count_insert_var=count_insert";
            EPStatementObjectModel model = new EPStatementObjectModel();
            model.setSelectClause(SelectClause.create("symbol"));
            model.setFromClause(FromClause.create(FilterStream.create("SupportMarketDataBean").addView("length", Expressions.constant(2))));
            model.setOutputLimitClause(OutputLimitClause.create(Expressions.eq("myvar", 1))
                .addThenAssignment(Expressions.eq(Expressions.property("myvar"), Expressions.constant(0)))
                .addThenAssignment(Expressions.eq(Expressions.property("count_insert_var"), Expressions.property("count_insert"))));
            model.setAnnotations(Collections.singletonList(AnnotationPart.nameAnnotation("s0")));
            String epl = model.toEPL();
            assertEquals(expression, epl);
            env.runtime().getVariableService().setVariableValue(null, "myvar", 0);
            env.compileDeploy(model).addListener("s0");

            env.undeployAll();
        }
    }

    private static class ResultSetOutputWhenThenSameVarTwice implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            // test same variable referenced multiple times JIRA-386
            sendTimer(env, 0);
            env.compileDeploy("@name('s1') select * from SupportMarketDataBean output last when myvar=100").addListener("s1");
            env.compileDeploy("@name('s2') select * from SupportMarketDataBean output last when myvar=100").addListener("s2");

            env.sendEventBean(new SupportMarketDataBean("ABC", "E1", 100));
            env.sendEventBean(new SupportMarketDataBean("ABC", "E2", 100));

            sendTimer(env, 1000);
            assertFalse(env.listener("s1").isInvoked());
            assertFalse(env.listener("s2").isInvoked());

            env.runtime().getVariableService().setVariableValue(null, "myvar", 100);
            sendTimer(env, 2000);
            assertTrue(env.listener("s2").isInvoked());
            assertTrue(env.listener("s1").isInvoked());

            env.undeployModuleContaining("s1");
            env.undeployModuleContaining("s2");
        }
    }

    private static class ResultSetOutputWhenThenWVariable implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            // test when-then with condition triggered by output events
            sendTimeEvent(env, 2, 8, 0, 0, 0);
            String eplToDeploy = "create variable boolean varOutputTriggered = false\n;" +
                "@Audit @Name('s0') select * from SupportBean#lastevent output snapshot when (count_insert > 1 and varOutputTriggered = false) then set varOutputTriggered = true;";
            env.compileDeploy(eplToDeploy).addListener("s0");

            env.sendEventBean(new SupportBean("E1", 1));
            assertFalse(env.listener("s0").isInvoked());

            env.sendEventBean(new SupportBean("E2", 2));
            assertEquals("E2", env.listener("s0").assertOneGetNewAndReset().get("theString"));

            env.sendEventBean(new SupportBean("E3", 3));
            env.sendEventBean(new SupportBean("E4", 4));
            assertFalse(env.listener("s0").isInvoked());

            env.runtime().getVariableService().setVariableValue(env.deploymentId("s0"), "varOutputTriggered", false); // turns true right away as triggering output

            env.sendEventBean(new SupportBean("E5", 5));
            sendTimeEvent(env, 2, 8, 0, 1, 0);
            assertEquals("E5", env.listener("s0").assertOneGetNewAndReset().get("theString"));

            env.sendEventBean(new SupportBean("E6", 6));
            assertFalse(env.listener("s0").isInvoked());

            env.undeployAll();
        }
    }

    private static class ResultSetOutputWhenThenWCount implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            // test count_total for insert and remove
            RegressionPath path = new RegressionPath();
            env.compileDeploy("@name('var') create variable int var_cnt_total = 3", path);
            String expressionTotal = "@name('s0') select theString from SupportBean#length(2) output when count_insert_total = var_cnt_total or count_remove_total > 2";
            env.compileDeploy(expressionTotal, path).addListener("s0");

            env.sendEventBean(new SupportBean("E1", 1));
            env.sendEventBean(new SupportBean("E2", 1));
            assertFalse(env.listener("s0").isInvoked());

            env.sendEventBean(new SupportBean("E3", 1));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), "theString".split(","), new Object[][]{{"E1"}, {"E2"}, {"E3"}});

            env.runtime().getVariableService().setVariableValue(env.deploymentId("var"), "var_cnt_total", -1);

            env.sendEventBean(new SupportBean("E4", 1));
            assertFalse(env.listener("s0").getAndClearIsInvoked());

            env.sendEventBean(new SupportBean("E5", 1));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), "theString".split(","), new Object[][]{{"E4"}, {"E5"}});

            env.undeployAll();
        }
    }

    private static class ResultSetOutputWhenExpression implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            sendTimeEvent(env, 1, 8, 0, 0, 0);
            env.compileDeploy("on SupportBean set myint = intPrimitive, mystring = theString");

            String expression = "@name('s0') select symbol from SupportMarketDataBean#length(2) output when myint = 1 and mystring like 'F%'";
            env.compileDeploy(expression);
            EPStatement stmt = env.statement("s0");
            SupportSubscriber subscriber = new SupportSubscriber();
            stmt.setSubscriber(subscriber);

            sendEvent(env, "S1", 0);

            env.sendEventBean(new SupportBean("E1", 1));
            assertEquals(1, env.runtime().getVariableService().getVariableValue(null, "myint"));
            assertEquals("E1", env.runtime().getVariableService().getVariableValue(null, "mystring"));

            sendEvent(env, "S2", 0);
            sendTimeEvent(env, 1, 8, 0, 1, 0);
            assertFalse(subscriber.isInvoked());

            env.sendEventBean(new SupportBean("F1", 0));
            assertEquals(0, env.runtime().getVariableService().getVariableValue(null, "myint"));
            assertEquals("F1", env.runtime().getVariableService().getVariableValue(null, "mystring"));

            sendTimeEvent(env, 1, 8, 0, 2, 0);
            sendEvent(env, "S3", 0);
            assertFalse(subscriber.isInvoked());

            env.sendEventBean(new SupportBean("F2", 1));
            assertEquals(1, env.runtime().getVariableService().getVariableValue(null, "myint"));
            assertEquals("F2", env.runtime().getVariableService().getVariableValue(null, "mystring"));

            sendEvent(env, "S4", 0);
            EPAssertionUtil.assertEqualsExactOrder(new Object[]{"S1", "S2", "S3", "S4"}, subscriber.getAndResetLastNewData());

            env.undeployAll();
        }
    }

    private static class ResultSetOutputWhenBuiltInCountInsert implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String expression = "@name('s0') select symbol from SupportMarketDataBean#length(2) output when count_insert >= 3";
            EPStatement stmt = env.compileDeploy(expression).statement("s0");
            SupportSubscriber subscriber = new SupportSubscriber();
            stmt.setSubscriber(subscriber);

            sendEvent(env, "S1", 0);
            sendEvent(env, "S2", 0);
            assertFalse(subscriber.isInvoked());

            sendEvent(env, "S3", 0);
            EPAssertionUtil.assertEqualsExactOrder(new Object[]{"S1", "S2", "S3"}, subscriber.getAndResetLastNewData());

            sendEvent(env, "S4", 0);
            sendEvent(env, "S5", 0);
            assertFalse(subscriber.isInvoked());

            sendEvent(env, "S6", 0);
            EPAssertionUtil.assertEqualsExactOrder(new Object[]{"S4", "S5", "S6"}, subscriber.getAndResetLastNewData());

            sendEvent(env, "S7", 0);
            assertFalse(subscriber.isInvoked());

            env.undeployAll();
        }
    }

    private static class ResultSetOutputWhenBuiltInCountRemove implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String expression = "@name('s0') select symbol from SupportMarketDataBean#length(2) output when count_remove >= 2";
            EPStatement stmt = env.compileDeploy(expression).statement("s0");
            SupportSubscriber subscriber = new SupportSubscriber();
            stmt.setSubscriber(subscriber);

            sendEvent(env, "S1", 0);
            sendEvent(env, "S2", 0);
            sendEvent(env, "S3", 0);
            assertFalse(subscriber.isInvoked());

            sendEvent(env, "S4", 0);
            EPAssertionUtil.assertEqualsExactOrder(new Object[]{"S1", "S2", "S3", "S4"}, subscriber.getAndResetLastNewData());

            sendEvent(env, "S5", 0);
            assertFalse(subscriber.isInvoked());

            sendEvent(env, "S6", 0);
            EPAssertionUtil.assertEqualsExactOrder(new Object[]{"S5", "S6"}, subscriber.getAndResetLastNewData());

            sendEvent(env, "S7", 0);
            assertFalse(subscriber.isInvoked());

            env.undeployAll();
        }
    }

    private static class ResultSetOutputWhenBuiltInLastTimestamp implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            sendTimeEvent(env, 1, 8, 0, 0, 0);
            String expression = "@name('s0') select symbol from SupportMarketDataBean#length(2) output when current_timestamp - last_output_timestamp >= 2000";
            EPStatement stmt = env.compileDeploy(expression).statement("s0");
            SupportSubscriber subscriber = new SupportSubscriber();
            stmt.setSubscriber(subscriber);

            sendEvent(env, "S1", 0);

            sendTimeEvent(env, 1, 8, 0, 1, 900);
            sendEvent(env, "S2", 0);

            sendTimeEvent(env, 1, 8, 0, 2, 0);
            assertFalse(subscriber.isInvoked());

            sendEvent(env, "S3", 0);
            EPAssertionUtil.assertEqualsExactOrder(new Object[]{"S1", "S2", "S3"}, subscriber.getAndResetLastNewData());

            sendTimeEvent(env, 1, 8, 0, 3, 0);
            sendEvent(env, "S4", 0);

            sendTimeEvent(env, 1, 8, 0, 3, 500);
            sendEvent(env, "S5", 0);
            assertFalse(subscriber.isInvoked());

            sendTimeEvent(env, 1, 8, 0, 4, 0);
            sendEvent(env, "S6", 0);
            EPAssertionUtil.assertEqualsExactOrder(new Object[]{"S4", "S5", "S6"}, subscriber.getAndResetLastNewData());

            env.undeployAll();
        }
    }

    private static class ResultSetInvalid implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            SupportMessageAssertUtil.tryInvalidCompile(env, "select * from SupportMarketDataBean output when myvardummy",
                "The when-trigger expression in the OUTPUT WHEN clause must return a boolean-type value [select * from SupportMarketDataBean output when myvardummy]");

            SupportMessageAssertUtil.tryInvalidCompile(env, "select * from SupportMarketDataBean output when true then set myvardummy = 'b'",
                "Error in the output rate limiting clause: Variable 'myvardummy' of declared type java.lang.Integer cannot be assigned a value of type java.lang.String [select * from SupportMarketDataBean output when true then set myvardummy = 'b']");

            SupportMessageAssertUtil.tryInvalidCompile(env, "select * from SupportMarketDataBean output when true then set myvardummy = sum(myvardummy)",
                "An aggregate function may not appear in a OUTPUT LIMIT clause [select * from SupportMarketDataBean output when true then set myvardummy = sum(myvardummy)]");

            SupportMessageAssertUtil.tryInvalidCompile(env, "select * from SupportMarketDataBean output when true then set 1",
                "Error in the output rate limiting clause: Missing variable assignment expression in assignment number 0 [select * from SupportMarketDataBean output when true then set 1]");

            SupportMessageAssertUtil.tryInvalidCompile(env, "select * from SupportMarketDataBean output when sum(price) > 0",
                "Failed to validate output limit expression '(sum(price))>0': Property named 'price' is not valid in any stream [select * from SupportMarketDataBean output when sum(price) > 0]");

            SupportMessageAssertUtil.tryInvalidCompile(env, "select * from SupportMarketDataBean output when sum(count_insert) > 0",
                "An aggregate function may not appear in a OUTPUT LIMIT clause [select * from SupportMarketDataBean output when sum(count_insert) > 0]");

            SupportMessageAssertUtil.tryInvalidCompile(env, "select * from SupportMarketDataBean output when prev(1, count_insert) = 0",
                "Failed to validate output limit expression 'prev(1,count_insert)=0': Previous function cannot be used in this context [select * from SupportMarketDataBean output when prev(1, count_insert) = 0]");

            SupportMessageAssertUtil.tryInvalidCompile(env, "select theString, count(*) from SupportBean#length(2) group by theString output all every 0 seconds",
                "Invalid time period expression returns a zero or negative time interval [select theString, count(*) from SupportBean#length(2) group by theString output all every 0 seconds]");
        }
    }

    private static void tryAssertionCrontab(RegressionEnvironment env, int days) {
        String[] fields = "symbol".split(",");
        sendEvent(env, "S1", 0);
        assertFalse(env.listener("s0").isInvoked());

        sendTimeEvent(env, days, 17, 14, 59, 0);
        sendEvent(env, "S2", 0);
        assertFalse(env.listener("s0").isInvoked());

        sendTimeEvent(env, days, 17, 15, 0, 0);
        EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, new Object[][]{{"S1"}, {"S2"}});

        sendTimeEvent(env, days, 17, 18, 0, 0);
        sendEvent(env, "S3", 0);
        assertFalse(env.listener("s0").isInvoked());

        sendTimeEvent(env, days, 17, 30, 0, 0);
        EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, new Object[][]{{"S3"}});

        sendTimeEvent(env, days, 17, 35, 0, 0);
        sendTimeEvent(env, days, 17, 45, 0, 0);
        EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, null);

        sendEvent(env, "S4", 0);
        sendEvent(env, "S5", 0);
        sendTimeEvent(env, days, 18, 0, 0, 0);
        assertFalse(env.listener("s0").isInvoked());

        sendTimeEvent(env, days, 18, 1, 0, 0);
        sendEvent(env, "S6", 0);

        sendTimeEvent(env, days, 18, 15, 0, 0);
        assertFalse(env.listener("s0").isInvoked());

        sendTimeEvent(env, days + 1, 7, 59, 59, 0);
        assertFalse(env.listener("s0").isInvoked());

        sendTimeEvent(env, days + 1, 8, 0, 0, 0);
        EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, new Object[][]{{"S4"}, {"S5"}, {"S6"}});

        env.undeployAll();
    }

    private static void tryAssertion(RegressionEnvironment env, int days) {
        SupportSubscriber subscriber = new SupportSubscriber();
        env.statement("s0").setSubscriber(subscriber);

        sendEvent(env, "S1", 0);

        // now scheduled for output
        env.sendEventBean(new SupportBean("E1", 1));
        assertEquals(0, env.runtime().getVariableService().getVariableValue(null, "myvar"));
        assertFalse(subscriber.isInvoked());

        sendTimeEvent(env, days, 8, 0, 1, 0);
        EPAssertionUtil.assertEqualsExactOrder(new Object[]{"S1"}, subscriber.getAndResetLastNewData());
        assertEquals(0, env.runtime().getVariableService().getVariableValue(null, "myvar"));
        assertEquals(1, env.runtime().getVariableService().getVariableValue(null, "count_insert_var"));

        sendEvent(env, "S2", 0);
        sendEvent(env, "S3", 0);
        sendTimeEvent(env, days, 8, 0, 2, 0);
        sendTimeEvent(env, days, 8, 0, 3, 0);
        env.sendEventBean(new SupportBean("E2", 1));
        assertEquals(0, env.runtime().getVariableService().getVariableValue(null, "myvar"));
        assertEquals(2, env.runtime().getVariableService().getVariableValue(null, "count_insert_var"));

        assertFalse(subscriber.isInvoked());
        sendTimeEvent(env, days, 8, 0, 4, 0);
        EPAssertionUtil.assertEqualsExactOrder(new Object[]{"S2", "S3"}, subscriber.getAndResetLastNewData());
        assertEquals(0, env.runtime().getVariableService().getVariableValue(null, "myvar"));

        sendTimeEvent(env, days, 8, 0, 5, 0);
        assertFalse(subscriber.isInvoked());
        env.sendEventBean(new SupportBean("E1", 1));
        assertEquals(0, env.runtime().getVariableService().getVariableValue(null, "myvar"));
        assertFalse(subscriber.isInvoked());

        env.undeployAll();
    }

    private static void sendTimer(RegressionEnvironment env, long timeInMSec) {
        env.advanceTime(timeInMSec);
    }

    private static void sendEvent(RegressionEnvironment env, String symbol, double price) {
        SupportMarketDataBean bean = new SupportMarketDataBean(symbol, price, 0L, null);
        env.sendEventBean(bean);
    }

    private static void sendTimeEvent(RegressionEnvironment env, int day, int hour, int minute, int second, int millis) {
        Calendar calendar = GregorianCalendar.getInstance();
        calendar.set(2008, 1, day, hour, minute, second);
        calendar.set(Calendar.MILLISECOND, millis);
        env.advanceTime(calendar.getTimeInMillis());
    }
}
