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
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.supportregression.rowrecog.SupportRecogBean;

import static org.junit.Assert.assertFalse;

public class ExecRowRecogGreedyness implements RegressionExecution {

    public void configure(Configuration configuration) throws Exception {
        configuration.addEventType("MyEvent", SupportRecogBean.class);
    }

    public void run(EPServiceProvider epService) throws Exception {
        runAssertionReluctantZeroToOne(epService);
        runAssertionReluctantZeroToMany(epService);
        runAssertionReluctantOneToMany(epService);
    }

    private void runAssertionReluctantZeroToOne(EPServiceProvider epService) {
        String[] fields = "a_string,b_string".split(",");
        String text = "select * from MyEvent#keepall " +
                "match_recognize (" +
                "  measures A.theString as a_string, B.theString as b_string " +
                "  pattern (A?? B?) " +
                "  define " +
                "   A as A.value = 1," +
                "   B as B.value = 1" +
                ")";

        EPStatement stmt = epService.getEPAdministrator().createEPL(text);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportRecogBean("E1", 1));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields,
                new Object[][]{{null, "E1"}});
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields,
                new Object[][]{{null, "E1"}});

        stmt.destroy();
    }

    private void runAssertionReluctantZeroToMany(EPServiceProvider epService) {
        String[] fields = "a0,a1,a2,b,c".split(",");
        String text = "select * from MyEvent#keepall " +
                "match_recognize (" +
                "  measures A[0].theString as a0, A[1].theString as a1, A[2].theString as a2, B.theString as b, C.theString as c" +
                "  pattern (A*? B? C) " +
                "  define " +
                "   A as A.value = 1," +
                "   B as B.value in (1, 2)," +
                "   C as C.value = 3" +
                ")";

        EPStatement stmt = epService.getEPAdministrator().createEPL(text);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportRecogBean("E1", 1));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("E2", 1));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("E3", 1));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("E4", 3));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields,
                new Object[][]{{"E1", "E2", null, "E3", "E4"}});

        epService.getEPRuntime().sendEvent(new SupportRecogBean("E11", 1));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("E12", 1));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("E13", 1));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("E14", 1));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("E15", 3));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields,
                new Object[][]{{"E11", "E12", "E13", "E14", "E15"}});

        epService.getEPRuntime().sendEvent(new SupportRecogBean("E16", 1));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("E17", 3));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields,
                new Object[][]{{null, null, null, "E16", "E17"}});

        epService.getEPRuntime().sendEvent(new SupportRecogBean("E18", 3));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields,
                new Object[][]{{null, null, null, null, "E18"}});

        stmt.destroy();
    }

    private void runAssertionReluctantOneToMany(EPServiceProvider epService) {
        String[] fields = "a0,a1,a2,b,c".split(",");
        String text = "select * from MyEvent#keepall " +
                "match_recognize (" +
                "  measures A[0].theString as a0, A[1].theString as a1, A[2].theString as a2, B.theString as b, C.theString as c" +
                "  pattern (A+? B? C) " +
                "  define " +
                "   A as A.value = 1," +
                "   B as B.value in (1, 2)," +
                "   C as C.value = 3" +
                ")";

        EPStatement stmt = epService.getEPAdministrator().createEPL(text);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportRecogBean("E1", 1));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("E2", 1));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("E3", 1));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("E4", 3));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields,
                new Object[][]{{"E1", "E2", null, "E3", "E4"}});

        epService.getEPRuntime().sendEvent(new SupportRecogBean("E11", 1));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("E12", 1));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("E13", 1));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("E14", 1));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("E15", 3));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields,
                new Object[][]{{"E11", "E12", "E13", "E14", "E15"}});

        epService.getEPRuntime().sendEvent(new SupportRecogBean("E16", 1));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("E17", 3));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields,
                new Object[][]{{"E16", null, null, null, "E17"}});

        epService.getEPRuntime().sendEvent(new SupportRecogBean("E18", 3));
        assertFalse(listener.isInvoked());

        stmt.destroy();
    }
}
