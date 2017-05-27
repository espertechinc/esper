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
package com.espertech.esper.regression.rowrecog;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPRuntime;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.soda.EPStatementObjectModel;
import com.espertech.esper.client.time.CurrentTimeEvent;
import com.espertech.esper.client.util.DateTime;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.supportregression.rowrecog.SupportRecogBean;
import com.espertech.esper.util.SerializableObjectCopier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class ExecRowRecogInterval implements RegressionExecution {
    public void configure(Configuration configuration) throws Exception {
        configuration.addEventType("MyEvent", SupportRecogBean.class);
    }

    public void run(EPServiceProvider epService) throws Exception {
        runAssertionInterval(epService);
        runAssertionPartitioned(epService);
        runAssertionMultiCompleted(epService);
        runAssertionMonthScoped(epService);
    }

    private void runAssertionInterval(EPServiceProvider epService) throws Exception {
        sendTimer(0, epService);
        String text = "select * from MyEvent#keepall " +
                "match_recognize (" +
                " measures A.theString as a, B[0].theString as b0, B[1].theString as b1, last(B.theString) as lastb" +
                " pattern (A B*)" +
                " interval 10 seconds" +
                " define" +
                " A as A.theString like \"A%\"," +
                " B as B.theString like \"B%\"" +
                ") order by a, b0, b1, lastb";

        EPStatement stmt = epService.getEPAdministrator().createEPL(text);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        tryAssertionInterval(epService, listener, stmt);

        stmt.destroy();
        EPStatementObjectModel model = epService.getEPAdministrator().compileEPL(text);
        SerializableObjectCopier.copy(model);
        assertEquals(text, model.toEPL());
        stmt = epService.getEPAdministrator().create(model);
        stmt.addListener(listener);
        assertEquals(text, stmt.getText());

        tryAssertionInterval(epService, listener, stmt);

        stmt.destroy();
    }

    private void tryAssertionInterval(EPServiceProvider epService, SupportUpdateListener listener, EPStatement stmt) {

        String[] fields = "a,b0,b1,lastb".split(",");
        sendTimer(1000, epService);
        epService.getEPRuntime().sendEvent(new SupportRecogBean("A1", 1));
        assertFalse(listener.isInvoked());

        sendTimer(10999, epService);
        assertFalse(listener.isInvoked());
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"A1", null, null, null}});

        sendTimer(11000, epService);
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"A1", null, null, null}});
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][]{{"A1", null, null, null}});

        sendTimer(13000, epService);
        epService.getEPRuntime().sendEvent(new SupportRecogBean("A2", 2));
        assertFalse(listener.isInvoked());

        sendTimer(15000, epService);
        epService.getEPRuntime().sendEvent(new SupportRecogBean("B1", 3));
        assertFalse(listener.isInvoked());

        sendTimer(22999, epService);
        assertFalse(listener.isInvoked());

        sendTimer(23000, epService);
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"A1", null, null, null}, {"A2", "B1", null, "B1"}});
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][]{{"A2", "B1", null, "B1"}});

        sendTimer(25000, epService);
        epService.getEPRuntime().sendEvent(new SupportRecogBean("A3", 4));
        assertFalse(listener.isInvoked());

        sendTimer(26000, epService);
        epService.getEPRuntime().sendEvent(new SupportRecogBean("B2", 5));
        assertFalse(listener.isInvoked());

        sendTimer(29000, epService);
        epService.getEPRuntime().sendEvent(new SupportRecogBean("B3", 6));
        assertFalse(listener.isInvoked());

        sendTimer(34999, epService);
        epService.getEPRuntime().sendEvent(new SupportRecogBean("B4", 7));
        assertFalse(listener.isInvoked());

        sendTimer(35000, epService);
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"A1", null, null, null}, {"A2", "B1", null, "B1"}, {"A3", "B2", "B3", "B4"}});
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][]{{"A3", "B2", "B3", "B4"}});
    }

    private void runAssertionPartitioned(EPServiceProvider epService) {
        sendTimer(0, epService);
        String[] fields = "a,b0,b1,lastb".split(",");
        String text = "select * from MyEvent#keepall " +
                "match_recognize (" +
                "  partition by cat " +
                "  measures A.theString as a, B[0].theString as b0, B[1].theString as b1, last(B.theString) as lastb" +
                "  pattern (A B*) " +
                "  INTERVAL 10 seconds " +
                "  define " +
                "    A as A.theString like 'A%'," +
                "    B as B.theString like 'B%'" +
                ") order by a, b0, b1, lastb";

        EPStatement stmt = epService.getEPAdministrator().createEPL(text);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        sendTimer(1000, epService);
        epService.getEPRuntime().sendEvent(new SupportRecogBean("A1", "C1", 1));

        sendTimer(1000, epService);
        epService.getEPRuntime().sendEvent(new SupportRecogBean("A2", "C2", 2));

        sendTimer(2000, epService);
        epService.getEPRuntime().sendEvent(new SupportRecogBean("A3", "C3", 3));

        sendTimer(3000, epService);
        epService.getEPRuntime().sendEvent(new SupportRecogBean("A4", "C4", 4));

        epService.getEPRuntime().sendEvent(new SupportRecogBean("B1", "C3", 5));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("B2", "C1", 6));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("B3", "C1", 7));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("B4", "C4", 7));
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{
                {"A1", "B2", "B3", "B3"}, {"A2", null, null, null}, {"A3", "B1", null, "B1"}, {"A4", "B4", null, "B4"}});

        sendTimer(10999, epService);
        assertFalse(listener.isInvoked());

        sendTimer(11000, epService);
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][]{{"A1", "B2", "B3", "B3"}, {"A2", null, null, null}});

        sendTimer(11999, epService);
        assertFalse(listener.isInvoked());

        sendTimer(12000, epService);
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][]{{"A3", "B1", null, "B1"}});

        sendTimer(13000, epService);
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][]{{"A4", "B4", null, "B4"}});

        stmt.destroy();
    }

    private void runAssertionMultiCompleted(EPServiceProvider epService) {
        sendTimer(0, epService);
        String[] fields = "a,b0,b1,lastb".split(",");
        String text = "select * from MyEvent#keepall " +
                "match_recognize (" +
                "  measures A.theString as a, B[0].theString as b0, B[1].theString as b1, last(B.theString) as lastb" +
                "  pattern (A B*) " +
                "  interval 10 seconds " +
                "  define " +
                "    A as A.theString like 'A%'," +
                "    B as B.theString like 'B%'" +
                ") order by a, b0, b1, lastb";

        EPStatement stmt = epService.getEPAdministrator().createEPL(text);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        sendTimer(1000, epService);
        epService.getEPRuntime().sendEvent(new SupportRecogBean("A1", 1));
        assertFalse(listener.isInvoked());

        sendTimer(5000, epService);
        epService.getEPRuntime().sendEvent(new SupportRecogBean("A2", 2));
        assertFalse(listener.isInvoked());

        sendTimer(10999, epService);
        assertFalse(listener.isInvoked());
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"A1", null, null, null}, {"A2", null, null, null}});

        sendTimer(11000, epService);
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][]{{"A1", null, null, null}});

        sendTimer(15000, epService);
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][]{{"A2", null, null, null}});

        sendTimer(21000, epService);
        epService.getEPRuntime().sendEvent(new SupportRecogBean("A3", 3));
        assertFalse(listener.isInvoked());

        sendTimer(22000, epService);
        epService.getEPRuntime().sendEvent(new SupportRecogBean("A4", 4));
        assertFalse(listener.isInvoked());

        sendTimer(23000, epService);
        epService.getEPRuntime().sendEvent(new SupportRecogBean("B1", 5));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("B2", 6));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("B3", 7));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("B4", 8));
        assertFalse(listener.isInvoked());

        sendTimer(31000, epService);
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][]{{"A3", null, null, null}});
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"A1", null, null, null}, {"A2", null, null, null}, {"A3", null, null, null}, {"A4", "B1", "B2", "B4"}});

        sendTimer(32000, epService);
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][]{{"A4", "B1", "B2", "B4"}});

        stmt.destroy();
    }

    private void runAssertionMonthScoped(EPServiceProvider epService) {
        SupportUpdateListener listener = new SupportUpdateListener();

        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);
        sendCurrentTime(epService, "2002-02-01T09:00:00.000");
        String text = "select * from SupportBean " +
                "match_recognize (" +
                " measures A.theString as a, B[0].theString as b0, B[1].theString as b1 " +
                " pattern (A B*)" +
                " interval 1 month" +
                " define" +
                " A as A.theString like \"A%\"," +
                " B as B.theString like \"B%\"" +
                ")";
        EPStatement stmt = epService.getEPAdministrator().createEPL(text);
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("A1", 0));
        epService.getEPRuntime().sendEvent(new SupportBean("B1", 0));
        sendCurrentTimeWithMinus(epService, "2002-03-01T09:00:00.000", 1);
        assertFalse(listener.getAndClearIsInvoked());

        sendCurrentTime(epService, "2002-03-01T09:00:00.000");
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), "a,b0,b1".split(","),
                new Object[][]{{"A1", "B1", null}});

        stmt.destroy();
    }

    private void sendCurrentTime(EPServiceProvider epService, String time) {
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(DateTime.parseDefaultMSec(time)));
    }

    private void sendCurrentTimeWithMinus(EPServiceProvider epService, String time, long minus) {
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(DateTime.parseDefaultMSec(time) - minus));
    }

    private void sendTimer(long time, EPServiceProvider epService) {
        CurrentTimeEvent theEvent = new CurrentTimeEvent(time);
        EPRuntime runtime = epService.getEPRuntime();
        runtime.sendEvent(theEvent);
    }
}
