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

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.soda.*;
import com.espertech.esper.client.time.CurrentTimeEvent;
import com.espertech.esper.client.util.DateTime;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.epl.SupportOutputLimitOpt;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import static org.junit.Assert.*;

public class ExecOutputLimitAfter implements RegressionExecution {
    public void configure(Configuration configuration) throws Exception {
        configuration.addEventType("SupportBean", SupportBean.class);
    }

    public void run(EPServiceProvider epService) throws Exception {
        runAssertionAfterWithOutputLast(epService);
        runAssertionEveryPolicy(epService);
        runAssertionMonthScoped(epService);
        runAssertionDirectNumberOfEvents(epService);
        runAssertionDirectTimePeriod(epService);
        runAssertionSnapshotVariable(epService);
        runAssertionOutputWhenThen(epService);
    }

    private void runAssertionAfterWithOutputLast(EPServiceProvider epService) {
        for (SupportOutputLimitOpt outputLimitOpt : SupportOutputLimitOpt.values()) {
            runAssertionAfterWithOutputLast(epService, outputLimitOpt);
        }
    }

    private void runAssertionAfterWithOutputLast(EPServiceProvider epService, SupportOutputLimitOpt opt) {
        String epl = opt.getHint() + "select sum(intPrimitive) as thesum " +
                "from SupportBean#keepall " +
                "output after 4 events last every 2 events";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 10));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 20));
        epService.getEPRuntime().sendEvent(new SupportBean("E3", 30));
        epService.getEPRuntime().sendEvent(new SupportBean("E4", 40));
        epService.getEPRuntime().sendEvent(new SupportBean("E5", 50));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean("E6", 60));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "thesum".split(","), new Object[]{210});

        stmt.destroy();
    }

    private void runAssertionEveryPolicy(EPServiceProvider epService) {
        sendTimer(epService, 0);
        String stmtText = "select theString from SupportBean#keepall output after 0 days 0 hours 0 minutes 20 seconds 0 milliseconds every 0 days 0 hours 0 minutes 5 seconds 0 milliseconds";
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        tryAssertionEveryPolicy(epService, listener);

        stmt.destroy();

        EPStatementObjectModel model = new EPStatementObjectModel();
        model.setSelectClause(SelectClause.create("theString"));
        model.setFromClause(FromClause.create(FilterStream.create("SupportBean").addView("keepall")));
        model.setOutputLimitClause(OutputLimitClause.create(Expressions.timePeriod(0, 0, 0, 5, 0)).afterTimePeriodExpression(Expressions.timePeriod(0, 0, 0, 20, 0)));
        assertEquals(stmtText, model.toEPL());
    }

    private void runAssertionMonthScoped(EPServiceProvider epService) {
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);
        sendCurrentTime(epService, "2002-02-01T09:00:00.000");
        SupportUpdateListener listener = new SupportUpdateListener();
        epService.getEPAdministrator().createEPL("select * from SupportBean output after 1 month").addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        sendCurrentTimeWithMinus(epService, "2002-03-01T09:00:00.000", 1);
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 2));
        assertFalse(listener.isInvoked());

        sendCurrentTime(epService, "2002-03-01T09:00:00.000");
        epService.getEPRuntime().sendEvent(new SupportBean("E3", 3));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "theString".split(","), new Object[]{"E3"});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void tryAssertionEveryPolicy(EPServiceProvider epService, SupportUpdateListener listener) {
        String[] fields = "theString".split(",");
        sendTimer(epService, 1);
        sendEvent(epService, "E1");

        sendTimer(epService, 6000);
        sendEvent(epService, "E2");
        sendTimer(epService, 16000);
        sendEvent(epService, "E3");
        assertFalse(listener.isInvoked());

        sendTimer(epService, 20000);
        sendEvent(epService, "E4");
        assertFalse(listener.isInvoked());

        sendTimer(epService, 24999);
        sendEvent(epService, "E5");

        sendTimer(epService, 25000);
        EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), fields, new Object[][]{{"E4"}, {"E5"}});
        listener.reset();

        sendTimer(epService, 27000);
        sendEvent(epService, "E6");

        sendTimer(epService, 29999);
        assertFalse(listener.isInvoked());

        sendTimer(epService, 30000);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E6"});
    }

    private void runAssertionDirectNumberOfEvents(EPServiceProvider epService) {
        String[] fields = "theString".split(",");
        String stmtText = "select theString from SupportBean#keepall output after 3 events";
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        sendEvent(epService, "E1");
        sendEvent(epService, "E2");
        sendEvent(epService, "E3");
        assertFalse(listener.isInvoked());

        sendEvent(epService, "E4");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E4"});

        sendEvent(epService, "E5");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E5"});

        stmt.destroy();

        EPStatementObjectModel model = new EPStatementObjectModel();
        model.setSelectClause(SelectClause.create("theString"));
        model.setFromClause(FromClause.create(FilterStream.create("SupportBean").addView("keepall")));
        model.setOutputLimitClause(OutputLimitClause.createAfter(3));
        assertEquals("select theString from SupportBean#keepall output after 3 events ", model.toEPL());

        stmt = epService.getEPAdministrator().create(model);
        stmt.addListener(listener);

        sendEvent(epService, "E1");
        sendEvent(epService, "E2");
        sendEvent(epService, "E3");
        assertFalse(listener.isInvoked());

        sendEvent(epService, "E4");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E4"});

        sendEvent(epService, "E5");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E5"});

        model = epService.getEPAdministrator().compileEPL("select theString from SupportBean#keepall output after 3 events");
        assertEquals("select theString from SupportBean#keepall output after 3 events ", model.toEPL());

        stmt.destroy();
    }

    private void runAssertionDirectTimePeriod(EPServiceProvider epService) {
        sendTimer(epService, 0);
        String[] fields = "theString".split(",");
        String stmtText = "select theString from SupportBean#keepall output after 20 seconds ";
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        sendTimer(epService, 1);
        sendEvent(epService, "E1");

        sendTimer(epService, 6000);
        sendEvent(epService, "E2");

        sendTimer(epService, 19999);
        sendEvent(epService, "E3");
        assertFalse(listener.isInvoked());

        sendTimer(epService, 20000);
        sendEvent(epService, "E4");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E4"});

        sendTimer(epService, 21000);
        sendEvent(epService, "E5");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E5"});

        stmt.destroy();
    }

    private void runAssertionSnapshotVariable(EPServiceProvider epService) {
        epService.getEPAdministrator().createEPL("create variable int myvar = 1");

        sendTimer(epService, 0);
        String stmtText = "select theString from SupportBean#keepall output after 20 seconds snapshot when myvar=1";
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        tryAssertionSnapshotVar(epService, listener);

        stmt.destroy();
        EPStatementObjectModel model = epService.getEPAdministrator().compileEPL(stmtText);
        assertEquals(stmtText, model.toEPL());
        stmt = epService.getEPAdministrator().create(model);
        assertEquals(stmtText, stmt.getText());
        stmt.destroy();
    }

    private void runAssertionOutputWhenThen(EPServiceProvider epService) {
        epService.getEPAdministrator().createEPL("create variable boolean myvar0 = false");
        epService.getEPAdministrator().createEPL("create variable boolean myvar1 = false");
        epService.getEPAdministrator().createEPL("create variable boolean myvar2 = false");

        String epl = "@Name(\"select-streamstar+outputvar\")\n" +
                "select a.* from SupportBean#time(10) a output after 3 events when myvar0=true then set myvar1=true, myvar2=true";

        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        sendEvent(epService, "E1");
        sendEvent(epService, "E2");
        sendEvent(epService, "E3");
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().setVariableValue("myvar0", true);
        sendEvent(epService, "E4");
        assertTrue(listener.isInvoked());

        assertEquals(true, epService.getEPRuntime().getVariableValue("myvar1"));
        assertEquals(true, epService.getEPRuntime().getVariableValue("myvar2"));

        stmt.destroy();
    }

    private void tryAssertionSnapshotVar(EPServiceProvider epService, SupportUpdateListener listener) {
        sendTimer(epService, 6000);
        sendEvent(epService, "E1");
        sendEvent(epService, "E2");

        sendTimer(epService, 19999);
        sendEvent(epService, "E3");
        assertFalse(listener.isInvoked());

        sendTimer(epService, 20000);
        sendEvent(epService, "E4");
        String[] fields = "theString".split(",");
        EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), fields, new Object[][]{{"E1"}, {"E2"}, {"E3"}, {"E4"}});
        listener.reset();

        sendTimer(epService, 21000);
        sendEvent(epService, "E5");
        EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), fields, new Object[][]{{"E1"}, {"E2"}, {"E3"}, {"E4"}, {"E5"}});
        listener.reset();
    }

    private void sendTimer(EPServiceProvider epService, long time) {
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(time));
    }

    private void sendEvent(EPServiceProvider epService, String theString) {
        epService.getEPRuntime().sendEvent(new SupportBean(theString, 0));
    }

    private void sendCurrentTime(EPServiceProvider epService, String time) {
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(DateTime.parseDefaultMSec(time)));
    }

    private void sendCurrentTimeWithMinus(EPServiceProvider epService, String time, long minus) {
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(DateTime.parseDefaultMSec(time) - minus));
    }
}
