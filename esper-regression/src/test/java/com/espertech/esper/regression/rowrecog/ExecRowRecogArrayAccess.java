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

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

public class ExecRowRecogArrayAccess implements RegressionExecution {

    public void run(EPServiceProvider epService) throws Exception {
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);

        runAssertionSingleMultiMix(epService);
        runAssertionMultiDepends(epService);
        runAssertionMeasuresClausePresence(epService);
        runAssertionLambda(epService);
    }

    private void runAssertionSingleMultiMix(EPServiceProvider epService) {
        String[] fields = "a,b0,c,d0,e".split(",");
        String text = "select * from SupportBean " +
                "match_recognize (" +
                " measures A.theString as a, B[0].theString as b0, C.theString as c, D[0].theString as d0, E.theString as e" +
                " pattern (A B+ C D+ E)" +
                " define" +
                " A as A.theString like 'A%', " +
                " B as B.theString like 'B%'," +
                " C as C.theString like 'C%' and C.intPrimitive = B[1].intPrimitive," +
                " D as D.theString like 'D%'," +
                " E as E.theString like 'E%' and E.intPrimitive = D[1].intPrimitive and E.intPrimitive = D[0].intPrimitive" +
                ")";

        EPStatement stmt = epService.getEPAdministrator().createEPL(text);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        listener.reset();

        sendEvents(epService, new Object[][]{{"A1", 100}, {"B1", 50}, {"B2", 49}, {"C1", 49}, {"D1", 2}, {"D2", 2}, {"E1", 2}});
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"A1", "B1", "C1", "D1", "E1"});

        sendEvents(epService, new Object[][]{{"A1", 100}, {"B1", 50}, {"C1", 49}, {"D1", 2}, {"D2", 2}, {"E1", 2}});
        assertFalse(listener.isInvoked());

        sendEvents(epService, new Object[][]{{"A1", 100}, {"B1", 50}, {"B2", 49}, {"C1", 49}, {"D1", 2}, {"D2", 3}, {"E1", 2}});
        assertFalse(listener.isInvoked());

        sendEvents(epService, new Object[][]{{"A1", 100}, {"B1", 50}, {"B2", 49}, {"C1", 49}, {"D1", 2}, {"D2", 2}, {"D3", 99}, {"E1", 2}});
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"A1", "B1", "C1", "D1", "E1"});

        stmt.destroy();
    }

    private void runAssertionMultiDepends(EPServiceProvider epService) {
        tryMultiDepends(epService, "A B A B C");
        tryMultiDepends(epService, "(A B)* C");
    }

    private void runAssertionMeasuresClausePresence(EPServiceProvider epService) throws Exception {
        tryMeasuresClausePresence(epService, "A as a_array, B as b");
        tryMeasuresClausePresence(epService, "B as b");
        tryMeasuresClausePresence(epService, "A as a_array");
        tryMeasuresClausePresence(epService, "1 as one");
    }

    private void runAssertionLambda(EPServiceProvider epService) {
        String[] fieldsOne = "a0,a1,a2,b".split(",");
        String eplOne = "select * from SupportBean " +
                "match_recognize (" +
                " measures A[0].theString as a0, A[1].theString as a1, A[2].theString as a2, B.theString as b" +
                " pattern (A* B)" +
                " define" +
                " B as (coalesce(A.sumOf(v => v.intPrimitive), 0) + B.intPrimitive) > 100" +
                ")";

        EPStatement stmt = epService.getEPAdministrator().createEPL(eplOne);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        listener.reset();

        sendEvents(epService, new Object[][]{{"E1", 50}, {"E2", 49}});
        assertFalse(listener.isInvoked());

        sendEvents(epService, new Object[][]{{"E3", 2}});
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fieldsOne, new Object[]{"E1", "E2", null, "E3"});

        sendEvents(epService, new Object[][]{{"E4", 101}});
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fieldsOne, new Object[]{null, null, null, "E4"});

        sendEvents(epService, new Object[][]{{"E5", 50}, {"E6", 51}});
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fieldsOne, new Object[]{"E5", null, null, "E6"});

        sendEvents(epService, new Object[][]{{"E7", 10}, {"E8", 10}, {"E9", 79}, {"E10", 1}, {"E11", 1}});
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fieldsOne, new Object[]{"E7", "E8", "E9", "E11"});
        stmt.destroy();

        String[] fieldsTwo = "a[0].theString,a[1].theString,b.theString".split(",");
        String eplTwo = "select * from SupportBean " +
                "match_recognize (" +
                " measures A as a, B as b " +
                " pattern (A+ B)" +
                " define" +
                " A as theString like 'A%', " +
                " B as theString like 'B%' and B.intPrimitive > A.sumOf(v => v.intPrimitive)" +
                ")";

        EPStatement stmtTwo = epService.getEPAdministrator().createEPL(eplTwo);
        stmtTwo.addListener(listener);
        listener.reset();

        sendEvents(epService, new Object[][]{{"A1", 1}, {"A2", 2}, {"B1", 3}});
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fieldsTwo, new Object[]{"A2", null, "B1"});

        sendEvents(epService, new Object[][]{{"A3", 1}, {"A4", 2}, {"B2", 4}});
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fieldsTwo, new Object[]{"A3", "A4", "B2"});

        sendEvents(epService, new Object[][]{{"A5", -1}, {"B3", 0}});
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fieldsTwo, new Object[]{"A5", null, "B3"});

        sendEvents(epService, new Object[][]{{"A6", 10}, {"B3", 9}, {"B4", 11}});
        sendEvents(epService, new Object[][]{{"A7", 10}, {"A8", 9}, {"A9", 8}});
        assertFalse(listener.isInvoked());

        sendEvents(epService, new Object[][]{{"B5", 18}});
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fieldsTwo, new Object[]{"A8", "A9", "B5"});

        sendEvents(epService, new Object[][]{{"A0", 10}, {"A11", 9}, {"A12", 8}, {"B6", 8}});
        assertFalse(listener.isInvoked());

        sendEvents(epService, new Object[][]{{"A13", 1}, {"A14", 1}, {"A15", 1}, {"A16", 1}, {"B7", 5}});
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fieldsTwo, new Object[]{"A13", "A14", "B7"});

        sendEvents(epService, new Object[][]{{"A17", 1}, {"A18", 1}, {"B8", 1}});
        assertFalse(listener.isInvoked());

        stmtTwo.destroy();
    }

    private void tryMeasuresClausePresence(EPServiceProvider epService, String measures) throws Exception {
        String text = "select * from SupportBean " +
                "match_recognize (" +
                " partition by theString " +
                " measures " + measures +
                " pattern (A+ B)" +
                " define" +
                " B as B.intPrimitive = A[0].intPrimitive" +
                ")";

        EPStatement stmt = epService.getEPAdministrator().createEPL(text);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        sendEvents(epService, new Object[][]{{"A", 1}, {"A", 0}});
        assertFalse(listener.isInvoked());

        sendEvents(epService, new Object[][]{{"B", 1}, {"B", 1}});
        assertNotNull(listener.assertOneGetNewAndReset());

        sendEvents(epService, new Object[][]{{"A", 2}, {"A", 3}});
        assertFalse(listener.isInvoked());

        sendEvents(epService, new Object[][]{{"B", 2}, {"B", 2}});
        assertNotNull(listener.assertOneGetNewAndReset());

        stmt.destroy();
    }

    private void tryMultiDepends(EPServiceProvider epService, String pattern) {
        String[] fields = "a0,a1,b0,b1,c".split(",");
        String text = "select * from SupportBean " +
                "match_recognize (" +
                " measures A[0].theString as a0, A[1].theString as a1, B[0].theString as b0, B[1].theString as b1, C.theString as c" +
                " pattern (" + pattern + ")" +
                " define" +
                " A as theString like 'A%', " +
                " B as theString like 'B%'," +
                " C as theString like 'C%' and " +
                "   C.intPrimitive = A[0].intPrimitive and " +
                "   C.intPrimitive = B[0].intPrimitive and " +
                "   C.intPrimitive = A[1].intPrimitive and " +
                "   C.intPrimitive = B[1].intPrimitive" +
                ")";

        EPStatement stmt = epService.getEPAdministrator().createEPL(text);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        listener.reset();

        sendEvents(epService, new Object[][]{{"A1", 1}, {"B1", 1}, {"A2", 1}, {"B2", 1}});
        epService.getEPRuntime().sendEvent(new SupportBean("C1", 1));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"A1", "A2", "B1", "B2", "C1"});

        sendEvents(epService, new Object[][]{{"A10", 1}, {"B10", 1}, {"A11", 1}, {"B11", 2}, {"C2", 2}});
        assertFalse(listener.isInvoked());

        sendEvents(epService, new Object[][]{{"A20", 2}, {"B20", 2}, {"A21", 1}, {"B21", 2}, {"C3", 2}});
        assertFalse(listener.isInvoked());

        stmt.destroy();
    }

    private void sendEvents(EPServiceProvider epService, Object[][] objects) {
        for (Object[] object : objects) {
            epService.getEPRuntime().sendEvent(new SupportBean((String) object[0], (Integer) object[1]));
        }
    }
}