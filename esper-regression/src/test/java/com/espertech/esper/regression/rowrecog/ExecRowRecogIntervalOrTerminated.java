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
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderIsolated;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.time.CurrentTimeEvent;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.supportregression.rowrecog.SupportRecogBean;
import com.espertech.esper.supportregression.util.SupportModelHelper;

import static org.junit.Assert.assertFalse;

public class ExecRowRecogIntervalOrTerminated implements RegressionExecution {

    public void configure(Configuration configuration) throws Exception {
        configuration.getEngineDefaults().getViewResources().setShareViews(false);
        configuration.addEventType("MyEvent", SupportRecogBean.class);
        configuration.getEngineDefaults().getExecution().setAllowIsolatedService(true);
    }

    public void run(EPServiceProvider epService) throws Exception {
        epService.getEPAdministrator().getConfiguration().addEventType("TemperatureSensorEvent",
                "id,device,temp".split(","), new Object[]{String.class, int.class, double.class});

        runAssertionDocSample(epService);

        runAssertion_A_Bstar(epService, false);

        runAssertion_A_Bstar(epService, true);

        runAssertion_Astar(epService);

        runAssertion_A_Bplus(epService);

        runAssertion_A_Bstar_or_Cstar(epService);

        runAssertion_A_B_Cstar(epService);

        runAssertion_A_B(epService);

        runAssertion_A_Bstar_or_C(epService);

        runAssertion_A_parenthesisBstar(epService);
    }

    private void runAssertion_A_Bstar_or_C(EPServiceProvider epService) {

        EPServiceProviderIsolated isolated = epService.getEPServiceIsolated("I1");
        sendTimer(isolated, 0);

        String[] fields = "a,b0,b1,b2,c".split(",");
        String text = "select * from MyEvent#keepall " +
                "match_recognize (" +
                " measures A.theString as a, B[0].theString as b0, B[1].theString as b1, B[2].theString as b2, C.theString as c " +
                " pattern (A (B* | C))" +
                " interval 10 seconds or terminated" +
                " define" +
                " A as A.theString like 'A%'," +
                " B as B.theString like 'B%'," +
                " C as C.theString like 'C%'" +
                ")";

        EPStatement stmt = isolated.getEPAdministrator().createEPL(text, "stmt1", null);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        isolated.getEPRuntime().sendEvent(new SupportRecogBean("A1"));
        isolated.getEPRuntime().sendEvent(new SupportRecogBean("C1"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"A1", null, null, null, "C1"});

        isolated.getEPRuntime().sendEvent(new SupportRecogBean("A2"));
        assertFalse(listener.isInvoked());
        isolated.getEPRuntime().sendEvent(new SupportRecogBean("B1"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"A2", null, null, null, null});

        isolated.getEPRuntime().sendEvent(new SupportRecogBean("B2"));
        assertFalse(listener.isInvoked());
        isolated.getEPRuntime().sendEvent(new SupportRecogBean("X1"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"A2", "B1", "B2", null, null});

        isolated.getEPRuntime().sendEvent(new SupportRecogBean("A3"));
        sendTimer(isolated, 10000);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"A3", null, null, null, null});

        sendTimer(isolated, Integer.MAX_VALUE);
        assertFalse(listener.isInvoked());

        // destroy
        stmt.destroy();
        isolated.destroy();
    }

    private void runAssertion_A_B(EPServiceProvider epService) {

        EPServiceProviderIsolated isolated = epService.getEPServiceIsolated("I1");
        sendTimer(isolated, 0);

        // the interval is not effective
        String[] fields = "a,b".split(",");
        String text = "select * from MyEvent#keepall " +
                "match_recognize (" +
                " measures A.theString as a, B.theString as b" +
                " pattern (A B)" +
                " interval 10 seconds or terminated" +
                " define" +
                " A as A.theString like 'A%'," +
                " B as B.theString like 'B%'" +
                ")";

        EPStatement stmt = isolated.getEPAdministrator().createEPL(text, "stmt1", null);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        isolated.getEPRuntime().sendEvent(new SupportRecogBean("A1"));
        isolated.getEPRuntime().sendEvent(new SupportRecogBean("B1"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"A1", "B1"});

        isolated.getEPRuntime().sendEvent(new SupportRecogBean("A2"));
        isolated.getEPRuntime().sendEvent(new SupportRecogBean("A3"));
        isolated.getEPRuntime().sendEvent(new SupportRecogBean("B2"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"A3", "B2"});

        // destroy
        stmt.destroy();
        isolated.destroy();
    }

    private void runAssertionDocSample(EPServiceProvider epService) {
        EPServiceProviderIsolated isolated = epService.getEPServiceIsolated("I1");
        sendTimer(isolated, 0);

        String[] fields = "a_id,count_b,first_b,last_b".split(",");
        String text = "select * from TemperatureSensorEvent\n" +
                "match_recognize (\n" +
                "  partition by device\n" +
                "  measures A.id as a_id, count(B.id) as count_b, first(B.id) as first_b, last(B.id) as last_b\n" +
                "  pattern (A B*)\n" +
                "  interval 5 seconds or terminated\n" +
                "  define\n" +
                "    A as A.temp > 100,\n" +
                "    B as B.temp > 100)";

        EPStatement stmt = isolated.getEPAdministrator().createEPL(text, "stmt1", null);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        sendTemperatureEvent(isolated, "E1", 1, 98);
        sendTemperatureEvent(isolated, "E2", 1, 101);
        sendTemperatureEvent(isolated, "E3", 1, 102);
        sendTemperatureEvent(isolated, "E4", 1, 101);   // falls below
        assertFalse(listener.isInvoked());

        sendTemperatureEvent(isolated, "E5", 1, 100);   // falls below
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E2", 2L, "E3", "E4"});

        sendTimer(isolated, Integer.MAX_VALUE);
        assertFalse(listener.isInvoked());

        // destroy
        stmt.destroy();
        isolated.destroy();
    }

    private void runAssertion_A_B_Cstar(EPServiceProvider epService) {

        EPServiceProviderIsolated isolated = epService.getEPServiceIsolated("I1");
        sendTimer(isolated, 0);

        String[] fields = "a,b,c0,c1,c2".split(",");
        String text = "select * from MyEvent#keepall " +
                "match_recognize (" +
                " measures A.theString as a, B.theString as b, " +
                "C[0].theString as c0, C[1].theString as c1, C[2].theString as c2 " +
                " pattern (A B C*)" +
                " interval 10 seconds or terminated" +
                " define" +
                " A as A.theString like 'A%'," +
                " B as B.theString like 'B%'," +
                " C as C.theString like 'C%'" +
                ")";

        EPStatement stmt = isolated.getEPAdministrator().createEPL(text, "stmt1", null);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        isolated.getEPRuntime().sendEvent(new SupportRecogBean("A1"));
        isolated.getEPRuntime().sendEvent(new SupportRecogBean("B1"));
        isolated.getEPRuntime().sendEvent(new SupportRecogBean("C1"));
        isolated.getEPRuntime().sendEvent(new SupportRecogBean("C2"));
        assertFalse(listener.isInvoked());

        isolated.getEPRuntime().sendEvent(new SupportRecogBean("B2"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"A1", "B1", "C1", "C2", null});

        isolated.getEPRuntime().sendEvent(new SupportRecogBean("A2"));
        isolated.getEPRuntime().sendEvent(new SupportRecogBean("X1"));
        isolated.getEPRuntime().sendEvent(new SupportRecogBean("B3"));
        isolated.getEPRuntime().sendEvent(new SupportRecogBean("X2"));
        assertFalse(listener.isInvoked());

        isolated.getEPRuntime().sendEvent(new SupportRecogBean("A3"));
        isolated.getEPRuntime().sendEvent(new SupportRecogBean("B4"));
        isolated.getEPRuntime().sendEvent(new SupportRecogBean("X3"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"A3", "B4", null, null, null});

        sendTimer(isolated, 20000);
        isolated.getEPRuntime().sendEvent(new SupportRecogBean("A4"));
        isolated.getEPRuntime().sendEvent(new SupportRecogBean("B5"));
        isolated.getEPRuntime().sendEvent(new SupportRecogBean("C3"));
        assertFalse(listener.isInvoked());

        sendTimer(isolated, 30000);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"A4", "B5", "C3", null, null});

        sendTimer(isolated, Integer.MAX_VALUE);
        assertFalse(listener.isInvoked());

        // destroy
        stmt.destroy();
        isolated.destroy();
    }

    private void runAssertion_A_Bstar_or_Cstar(EPServiceProvider epService) {

        EPServiceProviderIsolated isolated = epService.getEPServiceIsolated("I1");
        sendTimer(isolated, 0);

        String[] fields = "a,b0,b1,c0,c1".split(",");
        String text = "select * from MyEvent#keepall " +
                "match_recognize (" +
                " measures A.theString as a, " +
                "B[0].theString as b0, B[1].theString as b1, " +
                "C[0].theString as c0, C[1].theString as c1 " +
                " pattern (A (B* | C*))" +
                " interval 10 seconds or terminated" +
                " define" +
                " A as A.theString like 'A%'," +
                " B as B.theString like 'B%'," +
                " C as C.theString like 'C%'" +
                ")";

        EPStatement stmt = isolated.getEPAdministrator().createEPL(text, "stmt1", null);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        isolated.getEPRuntime().sendEvent(new SupportRecogBean("A1"));
        isolated.getEPRuntime().sendEvent(new SupportRecogBean("X1"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"A1", null, null, null, null});

        isolated.getEPRuntime().sendEvent(new SupportRecogBean("A2"));
        isolated.getEPRuntime().sendEvent(new SupportRecogBean("C1"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"A2", null, null, null, null});

        isolated.getEPRuntime().sendEvent(new SupportRecogBean("B1"));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields,
                new Object[][]{{"A2", null, null, "C1", null}});

        isolated.getEPRuntime().sendEvent(new SupportRecogBean("C2"));
        assertFalse(listener.isInvoked());

        // destroy
        stmt.destroy();
        isolated.destroy();
    }

    private void runAssertion_A_Bplus(EPServiceProvider epService) {

        EPServiceProviderIsolated isolated = epService.getEPServiceIsolated("I1");
        sendTimer(isolated, 0);

        String[] fields = "a,b0,b1,b2".split(",");
        String text = "select * from MyEvent#keepall " +
                "match_recognize (" +
                " measures A.theString as a, B[0].theString as b0, B[1].theString as b1, B[2].theString as b2" +
                " pattern (A B+)" +
                " interval 10 seconds or terminated" +
                " define" +
                " A as A.theString like 'A%'," +
                " B as B.theString like 'B%'" +
                ")";

        EPStatement stmt = isolated.getEPAdministrator().createEPL(text, "stmt1", null);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        isolated.getEPRuntime().sendEvent(new SupportRecogBean("A1"));
        isolated.getEPRuntime().sendEvent(new SupportRecogBean("X1"));

        isolated.getEPRuntime().sendEvent(new SupportRecogBean("A2"));
        isolated.getEPRuntime().sendEvent(new SupportRecogBean("B2"));
        assertFalse(listener.isInvoked());
        isolated.getEPRuntime().sendEvent(new SupportRecogBean("X2"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"A2", "B2", null, null});

        isolated.getEPRuntime().sendEvent(new SupportRecogBean("A3"));
        isolated.getEPRuntime().sendEvent(new SupportRecogBean("A4"));
        isolated.getEPRuntime().sendEvent(new SupportRecogBean("B3"));
        isolated.getEPRuntime().sendEvent(new SupportRecogBean("B4"));
        assertFalse(listener.isInvoked());
        isolated.getEPRuntime().sendEvent(new SupportRecogBean("X3", -1));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"A4", "B3", "B4", null});

        // destroy
        stmt.destroy();
        isolated.destroy();
    }

    private void runAssertion_Astar(EPServiceProvider epService) {

        EPServiceProviderIsolated isolated = epService.getEPServiceIsolated("I1");
        sendTimer(isolated, 0);

        String[] fields = "a0,a1,a2,a3,a4".split(",");
        String text = "select * from MyEvent#keepall " +
                "match_recognize (" +
                " measures A[0].theString as a0, A[1].theString as a1, A[2].theString as a2, A[3].theString as a3, A[4].theString as a4" +
                " pattern (A*)" +
                " interval 10 seconds or terminated" +
                " define" +
                " A as theString like 'A%'" +
                ")";

        EPStatement stmt = isolated.getEPAdministrator().createEPL(text, "stmt1", null);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        isolated.getEPRuntime().sendEvent(new SupportRecogBean("A1"));
        isolated.getEPRuntime().sendEvent(new SupportRecogBean("A2"));
        assertFalse(listener.isInvoked());
        isolated.getEPRuntime().sendEvent(new SupportRecogBean("B1"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"A1", "A2", null, null, null});

        sendTimer(isolated, 2000);
        isolated.getEPRuntime().sendEvent(new SupportRecogBean("A3"));
        isolated.getEPRuntime().sendEvent(new SupportRecogBean("A4"));
        isolated.getEPRuntime().sendEvent(new SupportRecogBean("A5"));
        assertFalse(listener.isInvoked());
        sendTimer(isolated, 12000);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"A3", "A4", "A5", null, null});

        isolated.getEPRuntime().sendEvent(new SupportRecogBean("A6"));
        isolated.getEPRuntime().sendEvent(new SupportRecogBean("B2"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"A3", "A4", "A5", "A6", null});
        isolated.getEPRuntime().sendEvent(new SupportRecogBean("B3"));
        assertFalse(listener.isInvoked());

        // destroy
        stmt.destroy();
        isolated.destroy();
    }

    private void runAssertion_A_Bstar(EPServiceProvider epService, boolean allMatches) {

        EPServiceProviderIsolated isolated = epService.getEPServiceIsolated("I1");
        sendTimer(isolated, 0);

        String[] fields = "a,b0,b1,b2".split(",");
        String text = "select * from MyEvent#keepall " +
                "match_recognize (" +
                " measures A.theString as a, B[0].theString as b0, B[1].theString as b1, B[2].theString as b2" +
                (allMatches ? " all matches" : "") +
                " pattern (A B*)" +
                " interval 10 seconds or terminated" +
                " define" +
                " A as A.theString like \"A%\"," +
                " B as B.theString like \"B%\"" +
                ")";

        EPStatement stmt = isolated.getEPAdministrator().createEPL(text, "stmt1", null);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        // test output by terminated because of misfit event
        isolated.getEPRuntime().sendEvent(new SupportRecogBean("A1"));
        isolated.getEPRuntime().sendEvent(new SupportRecogBean("B1"));
        assertFalse(listener.isInvoked());
        isolated.getEPRuntime().sendEvent(new SupportRecogBean("X1"));
        if (!allMatches) {
            EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"A1", "B1", null, null});
        } else {
            EPAssertionUtil.assertPropsPerRowAnyOrder(listener.getAndResetLastNewData(), fields,
                    new Object[][]{{"A1", "B1", null, null}, {"A1", null, null, null}});
        }

        sendTimer(isolated, 20000);
        assertFalse(listener.isInvoked());

        // test output by timer expiry
        isolated.getEPRuntime().sendEvent(new SupportRecogBean("A2"));
        isolated.getEPRuntime().sendEvent(new SupportRecogBean("B2"));
        assertFalse(listener.isInvoked());
        sendTimer(isolated, 29999);

        sendTimer(isolated, 30000);
        if (!allMatches) {
            EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"A2", "B2", null, null});
        } else {
            EPAssertionUtil.assertPropsPerRowAnyOrder(listener.getAndResetLastNewData(), fields,
                    new Object[][]{{"A2", "B2", null, null}, {"A2", null, null, null}});
        }

        // destroy
        stmt.destroy();
        isolated.destroy();

        EPStatement stmtFromModel = SupportModelHelper.compileCreate(epService, text);
        stmtFromModel.destroy();
    }

    private void runAssertion_A_parenthesisBstar(EPServiceProvider epService) {

        EPServiceProviderIsolated isolated = epService.getEPServiceIsolated("I1");
        sendTimer(isolated, 0);

        String[] fields = "a,b0,b1,b2".split(",");
        String text = "select * from MyEvent#keepall " +
                "match_recognize (" +
                " measures A.theString as a, B[0].theString as b0, B[1].theString as b1, B[2].theString as b2" +
                " pattern (A (B)*)" +
                " interval 10 seconds or terminated" +
                " define" +
                " A as A.theString like \"A%\"," +
                " B as B.theString like \"B%\"" +
                ")";

        EPStatement stmt = isolated.getEPAdministrator().createEPL(text, "stmt1", null);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        // test output by terminated because of misfit event
        isolated.getEPRuntime().sendEvent(new SupportRecogBean("A1"));
        isolated.getEPRuntime().sendEvent(new SupportRecogBean("B1"));
        assertFalse(listener.isInvoked());
        isolated.getEPRuntime().sendEvent(new SupportRecogBean("X1"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"A1", "B1", null, null});

        sendTimer(isolated, 20000);
        assertFalse(listener.isInvoked());

        // test output by timer expiry
        isolated.getEPRuntime().sendEvent(new SupportRecogBean("A2"));
        isolated.getEPRuntime().sendEvent(new SupportRecogBean("B2"));
        assertFalse(listener.isInvoked());
        sendTimer(isolated, 29999);

        sendTimer(isolated, 30000);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"A2", "B2", null, null});

        // destroy
        stmt.destroy();
        isolated.destroy();

        EPStatement stmtFromModel = SupportModelHelper.compileCreate(epService, text);
        stmtFromModel.destroy();
    }

    private void sendTemperatureEvent(EPServiceProviderIsolated isolated, String id, int device, double temp) {
        isolated.getEPRuntime().sendEvent(new Object[]{id, device, temp}, "TemperatureSensorEvent");
    }

    private void sendTimer(EPServiceProviderIsolated isolated, long time) {
        CurrentTimeEvent theEvent = new CurrentTimeEvent(time);
        isolated.getEPRuntime().sendEvent(theEvent);
    }
}