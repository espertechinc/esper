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
import com.espertech.esper.client.annotation.Hint;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.epl.SupportStaticMethodLib;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.supportregression.rowrecog.SupportRecogBean;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ExecRowRecogIterateOnly implements RegressionExecution {

    public void configure(Configuration configuration) throws Exception {
        configuration.addEventType("MyEvent", SupportRecogBean.class);
        configuration.addImport(SupportStaticMethodLib.class.getName());
        configuration.addImport(Hint.class.getName());
        configuration.addVariable("mySleepDuration", long.class, 100);    // msec
    }

    public void run(EPServiceProvider epService) throws Exception {
        runAssertionNoListenerMode(epService);
        runAssertionPrev(epService);
        runAssertionPrevPartitioned(epService);
    }

    private void runAssertionNoListenerMode(EPServiceProvider epService) {
        String[] fields = "a".split(",");
        String text = "@Hint('iterate_only') select * from MyEvent#length(1) " +
                "match_recognize (" +
                "  measures A.theString as a" +
                "  all matches " +
                "  pattern (A) " +
                "  define A as SupportStaticMethodLib.sleepReturnTrue(mySleepDuration)" +
                ")";

        EPStatement stmt = epService.getEPAdministrator().createEPL(text);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        // this should not block
        long start = System.currentTimeMillis();
        for (int i = 0; i < 50; i++) {
            epService.getEPRuntime().sendEvent(new SupportRecogBean("E1", 1));
        }
        long end = System.currentTimeMillis();
        assertTrue((end - start) <= 100);
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportRecogBean("E2", 2));
        epService.getEPRuntime().setVariableValue("mySleepDuration", 0);
        assertFalse(listener.isInvoked());
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields,
                new Object[][]{{"E2"}});

        stmt.destroy();
    }

    private void runAssertionPrev(EPServiceProvider epService) {
        String[] fields = "a".split(",");
        String text = "@Hint('iterate_only') select * from MyEvent#lastevent " +
                "match_recognize (" +
                "  measures A.theString as a" +
                "  all matches " +
                "  pattern (A) " +
                "  define A as prev(A.value, 2) = value" +
                ")";

        EPStatement stmt = epService.getEPAdministrator().createEPL(text);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportRecogBean("E1", 1));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("E2", 2));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("E3", 3));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("E4", 4));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("E5", 2));
        assertFalse(stmt.iterator().hasNext());

        epService.getEPRuntime().sendEvent(new SupportRecogBean("E6", 4));
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields,
                new Object[][]{{"E6"}});

        epService.getEPRuntime().sendEvent(new SupportRecogBean("E7", 2));
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields,
                new Object[][]{{"E7"}});
        assertFalse(listener.isInvoked());

        stmt.destroy();
    }

    private void runAssertionPrevPartitioned(EPServiceProvider epService) {
        String[] fields = "a,cat".split(",");
        String text = "@Hint('iterate_only') select * from MyEvent#lastevent " +
                "match_recognize (" +
                "  partition by cat" +
                "  measures A.theString as a, A.cat as cat" +
                "  all matches " +
                "  pattern (A) " +
                "  define A as prev(A.value, 2) = value" +
                ")";

        EPStatement stmt = epService.getEPAdministrator().createEPL(text);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportRecogBean("E1", "A", 1));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("E2", "B", 1));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("E3", "B", 3));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("E4", "A", 4));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("E5", "B", 2));
        assertFalse(stmt.iterator().hasNext());

        epService.getEPRuntime().sendEvent(new SupportRecogBean("E6", "A", 1));
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields,
                new Object[][]{{"E6", "A"}});

        epService.getEPRuntime().sendEvent(new SupportRecogBean("E7", "B", 3));
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields,
                new Object[][]{{"E7", "B"}});
        assertFalse(listener.isInvoked());

        stmt.destroy();
    }
}
