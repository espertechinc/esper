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

package com.espertech.esper.regression.resultset;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.soda.*;
import com.espertech.esper.client.time.CurrentTimeEvent;
import com.espertech.esper.client.util.DateTime;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.support.bean.SupportBean;
import com.espertech.esper.support.client.SupportConfigFactory;
import junit.framework.TestCase;

public class TestOutputLimitAfter extends TestCase
 {
     private EPServiceProvider epService;
     private SupportUpdateListener listener;
     private String[] fields;

     public void setUp()
     {
         fields = new String[] {"theString"};
         Configuration config = SupportConfigFactory.getConfiguration();
         config.addEventType("SupportBean", SupportBean.class);
         epService = EPServiceProviderManager.getDefaultProvider(config);
         epService.initialize();
         if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
         listener = new SupportUpdateListener();
     }

     protected void tearDown() throws Exception {
         if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
         listener = null;
         fields = null;
     }

     public void testAfterWithOutputLast() {
         runAssertionAfterWithOutputLast(false);
         runAssertionAfterWithOutputLast(true);
     }

     private void runAssertionAfterWithOutputLast(boolean hinted) {
         String hint = hinted ? "@Hint('enable_outputlimit_opt') " : "";
         String epl = hint + "select sum(intPrimitive) as thesum " +
                 "from SupportBean.win:keepall() " +
                 "output after 4 events last every 2 events";
         EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
         stmt.addListener(listener);

         epService.getEPRuntime().sendEvent(new SupportBean("E1", 10));
         epService.getEPRuntime().sendEvent(new SupportBean("E2", 20));
         epService.getEPRuntime().sendEvent(new SupportBean("E3", 30));
         epService.getEPRuntime().sendEvent(new SupportBean("E4", 40));
         epService.getEPRuntime().sendEvent(new SupportBean("E5", 50));
         assertFalse(listener.isInvoked());

         epService.getEPRuntime().sendEvent(new SupportBean("E6", 60));
         EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "thesum".split(","), new Object[] {210});

         stmt.destroy();
     }

     public void testEveryPolicy()
     {
         sendTimer(0);
         String stmtText = "select theString from SupportBean.win:keepall() output after 0 days 0 hours 0 minutes 20 seconds 0 milliseconds every 0 days 0 hours 0 minutes 5 seconds 0 milliseconds";
         EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
         stmt.addListener(listener);

         runAssertion();
         
         EPStatementObjectModel model = new EPStatementObjectModel();
         model.setSelectClause(SelectClause.create("theString"));
         model.setFromClause(FromClause.create(FilterStream.create("SupportBean").addView("win", "keepall")));
         model.setOutputLimitClause(OutputLimitClause.create(Expressions.timePeriod(0, 0, 0, 5, 0)).afterTimePeriodExpression(Expressions.timePeriod(0, 0, 0, 20, 0)));
         assertEquals(stmtText, model.toEPL());
     }

     public void testMonthScoped() {
         epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);
         sendCurrentTime("2002-02-01T09:00:00.000");
         epService.getEPAdministrator().createEPL("select * from SupportBean output after 1 month").addListener(listener);

         epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
         sendCurrentTimeWithMinus("2002-03-01T09:00:00.000", 1);
         epService.getEPRuntime().sendEvent(new SupportBean("E2", 2));
         assertFalse(listener.isInvoked());

         sendCurrentTime("2002-03-01T09:00:00.000");
         epService.getEPRuntime().sendEvent(new SupportBean("E3", 3));
         EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "theString".split(","), new Object[]{"E3"});
     }

     private void runAssertion()
     {
         sendTimer(1);
         sendEvent("E1");

         sendTimer(6000);
         sendEvent("E2");
         sendTimer(16000);
         sendEvent("E3");
         assertFalse(listener.isInvoked());

         sendTimer(20000);
         sendEvent("E4");
         assertFalse(listener.isInvoked());

         sendTimer(24999);
         sendEvent("E5");

         sendTimer(25000);
         EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), fields, new Object[][]{{"E4"}, {"E5"}});
         listener.reset();

         sendTimer(27000);
         sendEvent("E6");

         sendTimer(29999);
         assertFalse(listener.isInvoked());

         sendTimer(30000);
         EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E6"});
     }

     public void testDirectNumberOfEvents()
     {
         String stmtText = "select theString from SupportBean.win:keepall() output after 3 events";
         EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
         stmt.addListener(listener);

         sendEvent("E1");
         sendEvent("E2");
         sendEvent("E3");
         assertFalse(listener.isInvoked());

         sendEvent("E4");
         EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E4"});

         sendEvent("E5");
         EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E5"});

         stmt.destroy();
         
         EPStatementObjectModel model = new EPStatementObjectModel();
         model.setSelectClause(SelectClause.create("theString"));
         model.setFromClause(FromClause.create(FilterStream.create("SupportBean").addView("win", "keepall")));
         model.setOutputLimitClause(OutputLimitClause.createAfter(3));
         assertEquals("select theString from SupportBean.win:keepall() output after 3 events ", model.toEPL());

         stmt = epService.getEPAdministrator().create(model);
         stmt.addListener(listener);

         sendEvent("E1");
         sendEvent("E2");
         sendEvent("E3");
         assertFalse(listener.isInvoked());

         sendEvent("E4");
         EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E4"});

         sendEvent("E5");
         EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E5"});
         
         model = epService.getEPAdministrator().compileEPL("select theString from SupportBean.win:keepall() output after 3 events");
         assertEquals("select theString from SupportBean.win:keepall() output after 3 events ", model.toEPL());
     }

     public void testDirectTimePeriod()
     {
         sendTimer(0);
         String stmtText = "select theString from SupportBean.win:keepall() output after 20 seconds ";
         EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
         stmt.addListener(listener);

         sendTimer(1);
         sendEvent("E1");

         sendTimer(6000);
         sendEvent("E2");

         sendTimer(19999);
         sendEvent("E3");
         assertFalse(listener.isInvoked());

         sendTimer(20000);
         sendEvent("E4");
         EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E4"});

         sendTimer(21000);
         sendEvent("E5");
         EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E5"});
     }

     public void testSnapshotVariable()
     {
         epService.getEPAdministrator().createEPL("create variable int myvar = 1");

         sendTimer(0);
         String stmtText = "select theString from SupportBean.win:keepall() output after 20 seconds snapshot when myvar=1";
         EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
         stmt.addListener(listener);

         runAssertionSnapshotVar();
         
         stmt.destroy();
         EPStatementObjectModel model = epService.getEPAdministrator().compileEPL(stmtText);
         assertEquals(stmtText, model.toEPL());
         stmt = epService.getEPAdministrator().create(model);
         assertEquals(stmtText, stmt.getText());
     }

     public void testOutputWhenThen()
     {
         epService.getEPAdministrator().createEPL("create variable boolean myvar0 = false");
         epService.getEPAdministrator().createEPL("create variable boolean myvar1 = false");
         epService.getEPAdministrator().createEPL("create variable boolean myvar2 = false");

         String epl = "@Name(\"select-streamstar+outputvar\")\n" +
                 "select a.* from SupportBean.win:time(10) a output after 3 events when myvar0=true then set myvar1=true, myvar2=true";

         EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
         stmt.addListener(listener);

         sendEvent("E1");
         sendEvent("E2");
         sendEvent("E3");
         assertFalse(listener.isInvoked());

         epService.getEPRuntime().setVariableValue("myvar0", true);
         sendEvent("E4");
         assertTrue(listener.isInvoked());
         
         assertEquals(true, epService.getEPRuntime().getVariableValue("myvar1"));
         assertEquals(true, epService.getEPRuntime().getVariableValue("myvar2"));
     }

     private void runAssertionSnapshotVar()
     {
         sendTimer(6000);
         sendEvent("E1");
         sendEvent("E2");

         sendTimer(19999);
         sendEvent("E3");
         assertFalse(listener.isInvoked());

         sendTimer(20000);
         sendEvent("E4");
         EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), fields, new Object[][]{{"E1"}, {"E2"}, {"E3"}, {"E4"}});
         listener.reset();

         sendTimer(21000);
         sendEvent("E5");
         EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), fields, new Object[][]{{"E1"}, {"E2"}, {"E3"}, {"E4"}, {"E5"}});
         listener.reset();
     }

     private void sendTimer(long time)
     {
         epService.getEPRuntime().sendEvent(new CurrentTimeEvent(time));
     }

     private void sendEvent(String theString)
     {
         epService.getEPRuntime().sendEvent(new SupportBean(theString, 0));
     }

     private void sendCurrentTime(String time) {
         epService.getEPRuntime().sendEvent(new CurrentTimeEvent(DateTime.parseDefaultMSec(time)));
     }

     private void sendCurrentTimeWithMinus(String time, long minus) {
         epService.getEPRuntime().sendEvent(new CurrentTimeEvent(DateTime.parseDefaultMSec(time) - minus));
     }
 }
