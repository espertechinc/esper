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
package com.espertech.esper.regression.resultset.outputlimit;

import com.espertech.esper.client.EPRuntime;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportSubscriber;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.soda.*;
import com.espertech.esper.client.time.CurrentTimeEvent;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportMarketDataBean;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import java.util.Calendar;
import java.util.GregorianCalendar;

import static com.espertech.esper.supportregression.util.SupportMessageAssertUtil.tryInvalid;
import static org.junit.Assert.*;

public class ExecOutputLimitCrontabWhen implements RegressionExecution {
    public void run(EPServiceProvider epService) throws Exception {
        epService.getEPAdministrator().getConfiguration().addEventType("MarketData", SupportMarketDataBean.class);
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);

        runAssertionOutputCrontabAtVariable(epService);
        runAssertionOutputCrontabAt(epService);
        runAssertionOutputCrontabAtOMCreate(epService);
        runAssertionOutputCrontabAtOMCompile(epService);
        runAssertionOutputWhenThenExpression(epService);
        runAssertionOutputWhenExpression(epService);
        runAssertionOutputWhenBuiltInCountInsert(epService);
        runAssertionOutputWhenBuiltInCountRemove(epService);
        runAssertionOutputWhenBuiltInLastTimestamp(epService);
        runAssertionInvalid(epService);
    }

    private void runAssertionOutputCrontabAtVariable(EPServiceProvider epService) {

        // every 15 minutes 8am to 5pm
        sendTimeEvent(epService, 1, 17, 10, 0, 0);
        epService.getEPAdministrator().createEPL("create variable int VFREQ = 15");
        epService.getEPAdministrator().createEPL("create variable int VMIN = 8");
        epService.getEPAdministrator().createEPL("create variable int VMAX = 17");
        String expression = "select * from MarketData#lastevent output at (*/VFREQ, VMIN:VMAX, *, *, *)";
        EPStatement stmt = epService.getEPAdministrator().createEPL(expression);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        tryAssertionCrontab(epService, 1, stmt, listener);
    }

    private void runAssertionOutputCrontabAt(EPServiceProvider epService) {

        // every 15 minutes 8am to 5pm
        sendTimeEvent(epService, 1, 17, 10, 0, 0);
        String expression = "select * from MarketData#lastevent output at (*/15, 8:17, *, *, *)";
        EPStatement stmt = epService.getEPAdministrator().createEPL(expression);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        tryAssertionCrontab(epService, 1, stmt, listener);
    }

    private void runAssertionOutputCrontabAtOMCreate(EPServiceProvider epService) {

        // every 15 minutes 8am to 5pm
        sendTimeEvent(epService, 1, 17, 10, 0, 0);
        String expression = "select * from MarketData#lastevent output at (*/15, 8:17, *, *, *)";

        EPStatementObjectModel model = new EPStatementObjectModel();
        model.setSelectClause(SelectClause.createWildcard());
        model.setFromClause(FromClause.create(FilterStream.create("MarketData").addView("lastevent")));
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
        EPStatement stmt = epService.getEPAdministrator().create(model);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        tryAssertionCrontab(epService, 1, stmt, listener);
    }

    private void runAssertionOutputCrontabAtOMCompile(EPServiceProvider epService) {
        // every 15 minutes 8am to 5pm
        sendTimeEvent(epService, 1, 17, 10, 0, 0);
        String expression = "select * from MarketData#lastevent output at (*/15, 8:17, *, *, *)";

        EPStatementObjectModel model = epService.getEPAdministrator().compileEPL(expression);
        assertEquals(expression, model.toEPL());
        EPStatement stmt = epService.getEPAdministrator().create(model);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        tryAssertionCrontab(epService, 1, stmt, listener);
    }

    private void tryAssertionCrontab(EPServiceProvider epService, int days, EPStatement statement, SupportUpdateListener listener) {
        String[] fields = "symbol".split(",");
        sendEvent(epService, "S1", 0);
        assertFalse(listener.isInvoked());

        sendTimeEvent(epService, days, 17, 14, 59, 0);
        sendEvent(epService, "S2", 0);
        assertFalse(listener.isInvoked());

        sendTimeEvent(epService, days, 17, 15, 0, 0);
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][]{{"S1"}, {"S2"}});

        sendTimeEvent(epService, days, 17, 18, 0, 0);
        sendEvent(epService, "S3", 0);
        assertFalse(listener.isInvoked());

        sendTimeEvent(epService, days, 17, 30, 0, 0);
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][]{{"S3"}});

        sendTimeEvent(epService, days, 17, 35, 0, 0);
        sendTimeEvent(epService, days, 17, 45, 0, 0);
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, null);

        sendEvent(epService, "S4", 0);
        sendEvent(epService, "S5", 0);
        sendTimeEvent(epService, days, 18, 0, 0, 0);
        assertFalse(listener.isInvoked());

        sendTimeEvent(epService, days, 18, 1, 0, 0);
        sendEvent(epService, "S6", 0);

        sendTimeEvent(epService, days, 18, 15, 0, 0);
        assertFalse(listener.isInvoked());

        sendTimeEvent(epService, days + 1, 7, 59, 59, 0);
        assertFalse(listener.isInvoked());

        sendTimeEvent(epService, days + 1, 8, 0, 0, 0);
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][]{{"S4"}, {"S5"}, {"S6"}});

        statement.destroy();
        listener.reset();
    }

    private void runAssertionOutputWhenThenExpression(EPServiceProvider epService) throws Exception {
        sendTimeEvent(epService, 1, 8, 0, 0, 0);
        epService.getEPAdministrator().getConfiguration().addVariable("myvar", int.class, 0);
        epService.getEPAdministrator().getConfiguration().addVariable("count_insert_var", int.class, 0);
        epService.getEPAdministrator().createEPL("on SupportBean set myvar = intPrimitive");

        String expression = "select symbol from MarketData#length(2) output when myvar=1 then set myvar=0, count_insert_var=count_insert";
        EPStatement stmt = epService.getEPAdministrator().createEPL(expression);
        tryAssertion(epService, 1, stmt);

        EPStatementObjectModel model = new EPStatementObjectModel();
        model.setSelectClause(SelectClause.create("symbol"));
        model.setFromClause(FromClause.create(FilterStream.create("MarketData").addView("length", Expressions.constant(2))));
        model.setOutputLimitClause(OutputLimitClause.create(Expressions.eq("myvar", 1))
                .addThenAssignment(Expressions.eq(Expressions.property("myvar"), Expressions.constant(0)))
                .addThenAssignment(Expressions.eq(Expressions.property("count_insert_var"), Expressions.property("count_insert"))));

        String epl = model.toEPL();
        assertEquals(expression, epl);
        stmt = epService.getEPAdministrator().create(model);
        tryAssertion(epService, 2, stmt);

        model = epService.getEPAdministrator().compileEPL(expression);
        assertEquals(expression, model.toEPL());
        stmt = epService.getEPAdministrator().create(model);
        tryAssertion(epService, 3, stmt);

        String outputLast = "select symbol from MarketData#length(2) output last when myvar=1 ";
        model = epService.getEPAdministrator().compileEPL(outputLast);
        assertEquals(outputLast.trim(), model.toEPL().trim());

        // test same variable referenced multiple times JIRA-386
        sendTimer(epService, 0);
        SupportUpdateListener listenerOne = new SupportUpdateListener();
        SupportUpdateListener listenerTwo = new SupportUpdateListener();
        EPStatement stmtOne = epService.getEPAdministrator().createEPL("select * from MarketData output last when myvar=100");
        stmtOne.addListener(listenerOne);
        EPStatement stmtTwo = epService.getEPAdministrator().createEPL("select * from MarketData output last when myvar=100");
        stmtTwo.addListener(listenerTwo);
        epService.getEPRuntime().sendEvent(new SupportMarketDataBean("ABC", "E1", 100));
        epService.getEPRuntime().sendEvent(new SupportMarketDataBean("ABC", "E2", 100));

        sendTimer(epService, 1000);
        assertFalse(listenerOne.isInvoked());
        assertFalse(listenerTwo.isInvoked());

        epService.getEPRuntime().setVariableValue("myvar", 100);
        sendTimer(epService, 2000);
        assertTrue(listenerTwo.isInvoked());
        assertTrue(listenerOne.isInvoked());

        stmtOne.destroy();
        stmtTwo.destroy();

        // test when-then with condition triggered by output events
        sendTimeEvent(epService, 2, 8, 0, 0, 0);
        String eplToDeploy = "create variable boolean varOutputTriggered = false\n;" +
                "@Audit @Name('out') select * from SupportBean#lastevent output snapshot when (count_insert > 1 and varOutputTriggered = false) then set varOutputTriggered = true;";
        SupportUpdateListener listener = new SupportUpdateListener();
        epService.getEPAdministrator().getDeploymentAdmin().parseDeploy(eplToDeploy);
        epService.getEPAdministrator().getStatement("out").addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 2));
        assertEquals("E2", listener.assertOneGetNewAndReset().get("theString"));

        epService.getEPRuntime().sendEvent(new SupportBean("E3", 3));
        epService.getEPRuntime().sendEvent(new SupportBean("E4", 4));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().setVariableValue("varOutputTriggered", false); // turns true right away as triggering output

        epService.getEPRuntime().sendEvent(new SupportBean("E5", 5));
        sendTimeEvent(epService, 2, 8, 0, 1, 0);
        assertEquals("E5", listener.assertOneGetNewAndReset().get("theString"));

        epService.getEPRuntime().sendEvent(new SupportBean("E6", 6));
        assertFalse(listener.isInvoked());

        epService.getEPAdministrator().destroyAllStatements();

        // test count_total for insert and remove
        epService.getEPAdministrator().createEPL("create variable int var_cnt_total = 3");
        String expressionTotal = "select theString from SupportBean#length(2) output when count_insert_total = var_cnt_total or count_remove_total > 2";
        EPStatement stmtTotal = epService.getEPAdministrator().createEPL(expressionTotal);
        stmtTotal.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 1));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean("E3", 1));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), "theString".split(","), new Object[][]{{"E1"}, {"E2"}, {"E3"}});

        epService.getEPRuntime().setVariableValue("var_cnt_total", -1);

        epService.getEPRuntime().sendEvent(new SupportBean("E4", 1));
        assertFalse(listener.getAndClearIsInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean("E5", 1));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), "theString".split(","), new Object[][]{{"E4"}, {"E5"}});
        epService.getEPAdministrator().destroyAllStatements();
    }

    private void tryAssertion(EPServiceProvider epService, int days, EPStatement stmt) {
        SupportSubscriber subscriber = new SupportSubscriber();
        stmt.setSubscriber(subscriber);

        sendEvent(epService, "S1", 0);

        // now scheduled for output
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        assertEquals(0, epService.getEPRuntime().getVariableValue("myvar"));
        assertFalse(subscriber.isInvoked());

        sendTimeEvent(epService, days, 8, 0, 1, 0);
        EPAssertionUtil.assertEqualsExactOrder(new Object[]{"S1"}, subscriber.getAndResetLastNewData());
        assertEquals(0, epService.getEPRuntime().getVariableValue("myvar"));
        assertEquals(1, epService.getEPRuntime().getVariableValue("count_insert_var"));

        sendEvent(epService, "S2", 0);
        sendEvent(epService, "S3", 0);
        sendTimeEvent(epService, days, 8, 0, 2, 0);
        sendTimeEvent(epService, days, 8, 0, 3, 0);
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 1));
        assertEquals(0, epService.getEPRuntime().getVariableValue("myvar"));
        assertEquals(2, epService.getEPRuntime().getVariableValue("count_insert_var"));

        assertFalse(subscriber.isInvoked());
        sendTimeEvent(epService, days, 8, 0, 4, 0);
        EPAssertionUtil.assertEqualsExactOrder(new Object[]{"S2", "S3"}, subscriber.getAndResetLastNewData());
        assertEquals(0, epService.getEPRuntime().getVariableValue("myvar"));

        sendTimeEvent(epService, days, 8, 0, 5, 0);
        assertFalse(subscriber.isInvoked());
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        assertEquals(0, epService.getEPRuntime().getVariableValue("myvar"));
        assertFalse(subscriber.isInvoked());

        stmt.destroy();
    }

    private void runAssertionOutputWhenExpression(EPServiceProvider epService) {
        sendTimeEvent(epService, 1, 8, 0, 0, 0);
        epService.getEPAdministrator().getConfiguration().addVariable("myint", int.class, 0);
        epService.getEPAdministrator().getConfiguration().addVariable("mystring", String.class, "");
        epService.getEPAdministrator().createEPL("on SupportBean set myint = intPrimitive, mystring = theString");

        String expression = "select symbol from MarketData#length(2) output when myint = 1 and mystring like 'F%'";
        EPStatement stmt = epService.getEPAdministrator().createEPL(expression);
        SupportSubscriber subscriber = new SupportSubscriber();
        stmt.setSubscriber(subscriber);

        sendEvent(epService, "S1", 0);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        assertEquals(1, epService.getEPRuntime().getVariableValue("myint"));
        assertEquals("E1", epService.getEPRuntime().getVariableValue("mystring"));

        sendEvent(epService, "S2", 0);
        sendTimeEvent(epService, 1, 8, 0, 1, 0);
        assertFalse(subscriber.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean("F1", 0));
        assertEquals(0, epService.getEPRuntime().getVariableValue("myint"));
        assertEquals("F1", epService.getEPRuntime().getVariableValue("mystring"));

        sendTimeEvent(epService, 1, 8, 0, 2, 0);
        sendEvent(epService, "S3", 0);
        assertFalse(subscriber.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean("F2", 1));
        assertEquals(1, epService.getEPRuntime().getVariableValue("myint"));
        assertEquals("F2", epService.getEPRuntime().getVariableValue("mystring"));

        sendEvent(epService, "S4", 0);
        EPAssertionUtil.assertEqualsExactOrder(new Object[]{"S1", "S2", "S3", "S4"}, subscriber.getAndResetLastNewData());

        stmt.destroy();
    }

    private void runAssertionOutputWhenBuiltInCountInsert(EPServiceProvider epService) {
        String expression = "select symbol from MarketData#length(2) output when count_insert >= 3";
        EPStatement stmt = epService.getEPAdministrator().createEPL(expression);
        SupportSubscriber subscriber = new SupportSubscriber();
        stmt.setSubscriber(subscriber);

        sendEvent(epService, "S1", 0);
        sendEvent(epService, "S2", 0);
        assertFalse(subscriber.isInvoked());

        sendEvent(epService, "S3", 0);
        EPAssertionUtil.assertEqualsExactOrder(new Object[]{"S1", "S2", "S3"}, subscriber.getAndResetLastNewData());

        sendEvent(epService, "S4", 0);
        sendEvent(epService, "S5", 0);
        assertFalse(subscriber.isInvoked());

        sendEvent(epService, "S6", 0);
        EPAssertionUtil.assertEqualsExactOrder(new Object[]{"S4", "S5", "S6"}, subscriber.getAndResetLastNewData());

        sendEvent(epService, "S7", 0);
        assertFalse(subscriber.isInvoked());

        stmt.destroy();
    }

    private void runAssertionOutputWhenBuiltInCountRemove(EPServiceProvider epService) {
        String expression = "select symbol from MarketData#length(2) output when count_remove >= 2";
        EPStatement stmt = epService.getEPAdministrator().createEPL(expression);
        SupportSubscriber subscriber = new SupportSubscriber();
        stmt.setSubscriber(subscriber);

        sendEvent(epService, "S1", 0);
        sendEvent(epService, "S2", 0);
        sendEvent(epService, "S3", 0);
        assertFalse(subscriber.isInvoked());

        sendEvent(epService, "S4", 0);
        EPAssertionUtil.assertEqualsExactOrder(new Object[]{"S1", "S2", "S3", "S4"}, subscriber.getAndResetLastNewData());

        sendEvent(epService, "S5", 0);
        assertFalse(subscriber.isInvoked());

        sendEvent(epService, "S6", 0);
        EPAssertionUtil.assertEqualsExactOrder(new Object[]{"S5", "S6"}, subscriber.getAndResetLastNewData());

        sendEvent(epService, "S7", 0);
        assertFalse(subscriber.isInvoked());

        stmt.destroy();
    }

    private void runAssertionOutputWhenBuiltInLastTimestamp(EPServiceProvider epService) {
        sendTimeEvent(epService, 1, 8, 0, 0, 0);
        String expression = "select symbol from MarketData#length(2) output when current_timestamp - last_output_timestamp >= 2000";
        EPStatement stmt = epService.getEPAdministrator().createEPL(expression);
        SupportSubscriber subscriber = new SupportSubscriber();
        stmt.setSubscriber(subscriber);

        sendEvent(epService, "S1", 0);

        sendTimeEvent(epService, 1, 8, 0, 1, 900);
        sendEvent(epService, "S2", 0);

        sendTimeEvent(epService, 1, 8, 0, 2, 0);
        assertFalse(subscriber.isInvoked());

        sendEvent(epService, "S3", 0);
        EPAssertionUtil.assertEqualsExactOrder(new Object[]{"S1", "S2", "S3"}, subscriber.getAndResetLastNewData());

        sendTimeEvent(epService, 1, 8, 0, 3, 0);
        sendEvent(epService, "S4", 0);

        sendTimeEvent(epService, 1, 8, 0, 3, 500);
        sendEvent(epService, "S5", 0);
        assertFalse(subscriber.isInvoked());

        sendTimeEvent(epService, 1, 8, 0, 4, 0);
        sendEvent(epService, "S6", 0);
        EPAssertionUtil.assertEqualsExactOrder(new Object[]{"S4", "S5", "S6"}, subscriber.getAndResetLastNewData());

        stmt.destroy();
    }

    private void sendEvent(EPServiceProvider epService, String symbol, double price) {
        SupportMarketDataBean bean = new SupportMarketDataBean(symbol, price, 0L, null);
        epService.getEPRuntime().sendEvent(bean);
    }

    private void sendTimeEvent(EPServiceProvider epService, int day, int hour, int minute, int second, int millis) {
        Calendar calendar = GregorianCalendar.getInstance();
        calendar.set(2008, 1, day, hour, minute, second);
        calendar.set(Calendar.MILLISECOND, millis);
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(calendar.getTimeInMillis()));
    }

    private void runAssertionInvalid(EPServiceProvider epService) {
        epService.getEPAdministrator().getConfiguration().addVariable("myvardummy", int.class, 0);
        epService.getEPAdministrator().getConfiguration().addVariable("myvarlong", long.class, 0);

        tryInvalid(epService, "select * from MarketData output when sum(price) > 0",
                "Error validating expression: Failed to validate output limit expression '(sum(price))>0': Property named 'price' is not valid in any stream [select * from MarketData output when sum(price) > 0]");

        tryInvalid(epService, "select * from MarketData output when sum(count_insert) > 0",
                "Error validating expression: An aggregate function may not appear in a OUTPUT LIMIT clause [select * from MarketData output when sum(count_insert) > 0]");

        tryInvalid(epService, "select * from MarketData output when prev(1, count_insert) = 0",
                "Error validating expression: Failed to validate output limit expression 'prev(1,count_insert)=0': Previous function cannot be used in this context [select * from MarketData output when prev(1, count_insert) = 0]");

        tryInvalid(epService, "select * from MarketData output when myvardummy",
                "Error validating expression: The when-trigger expression in the OUTPUT WHEN clause must return a boolean-type value [select * from MarketData output when myvardummy]");

        tryInvalid(epService, "select * from MarketData output when true then set myvardummy = 'b'",
                "Error starting statement: Error in the output rate limiting clause: Variable 'myvardummy' of declared type java.lang.Integer cannot be assigned a value of type java.lang.String [select * from MarketData output when true then set myvardummy = 'b']");

        tryInvalid(epService, "select * from MarketData output when true then set myvardummy = sum(myvardummy)",
                "Error validating expression: An aggregate function may not appear in a OUTPUT LIMIT clause [select * from MarketData output when true then set myvardummy = sum(myvardummy)]");

        tryInvalid(epService, "select * from MarketData output when true then set 1",
                "Error starting statement: Error in the output rate limiting clause: Missing variable assignment expression in assignment number 0 [select * from MarketData output when true then set 1]");

        tryInvalid(epService, "select theString, count(*) from SupportBean#length(2) group by theString output all every 0 seconds",
                "Error starting statement: Invalid time period expression returns a zero or negative time interval [select theString, count(*) from SupportBean#length(2) group by theString output all every 0 seconds]");
    }

    private void sendTimer(EPServiceProvider epService, long timeInMSec) {
        CurrentTimeEvent theEvent = new CurrentTimeEvent(timeInMSec);
        EPRuntime runtime = epService.getEPRuntime();
        runtime.sendEvent(theEvent);
    }
}
