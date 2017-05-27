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
import com.espertech.esper.client.time.CurrentTimeEvent;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.supportregression.rowrecog.SupportRecogBean;

import static org.junit.Assert.assertFalse;

public class ExecRowRecogPrev implements RegressionExecution {

    public void configure(Configuration configuration) throws Exception {
        configuration.addEventType("MyEvent", SupportRecogBean.class);
        configuration.addEventType("MyDeleteEvent", SupportBean.class);
    }

    public void run(EPServiceProvider epService) throws Exception {
        runAssertionTimeWindowUnpartitioned(epService);
        runAssertionTimeWindowPartitioned(epService);
        runAssertionTimeWindowPartitionedSimple(epService);
        runAssertionPartitionBy2FieldsKeepall(epService);
        runAssertionUnpartitionedKeepAll(epService);
    }

    private void runAssertionTimeWindowUnpartitioned(EPServiceProvider epService) {
        sendTimer(0, epService);
        String[] fields = "a_string,b_string".split(",");
        String text = "select * from MyEvent#time(5) " +
                "match_recognize (" +
                "  measures A.theString as a_string, B.theString as b_string" +
                "  all matches pattern (A B) " +
                "  define " +
                "    A as PREV(A.theString, 3) = 'P3' and PREV(A.theString, 2) = 'P2' and PREV(A.theString, 4) = 'P4' and Math.abs(prev(A.value, 0)) >= 0," +
                "    B as B.value in (PREV(B.value, 4), PREV(B.value, 2))" +
                ")";

        EPStatement stmt = epService.getEPAdministrator().createEPL(text);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        sendTimer(1000, epService);
        epService.getEPRuntime().sendEvent(new SupportRecogBean("P2", 1));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("P1", 2));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("P3", 3));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("P4", 4));
        sendTimer(2000, epService);
        epService.getEPRuntime().sendEvent(new SupportRecogBean("P2", 1));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("E1", 3));
        assertFalse(listener.isInvoked());
        assertFalse(stmt.iterator().hasNext());

        sendTimer(3000, epService);
        epService.getEPRuntime().sendEvent(new SupportRecogBean("P4", 11));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("P3", 12));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("P2", 13));
        sendTimer(4000, epService);
        epService.getEPRuntime().sendEvent(new SupportRecogBean("xx", 4));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("E2", -1));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("E3", 12));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields,
                new Object[][]{{"E2", "E3"}});
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields,
                new Object[][]{{"E2", "E3"}});

        sendTimer(5000, epService);
        epService.getEPRuntime().sendEvent(new SupportRecogBean("P4", 21));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("P3", 22));
        sendTimer(6000, epService);
        epService.getEPRuntime().sendEvent(new SupportRecogBean("P2", 23));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("xx", -2));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("E5", -1));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("E6", -2));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields,
                new Object[][]{{"E5", "E6"}});
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields,
                new Object[][]{{"E2", "E3"}, {"E5", "E6"}});

        sendTimer(8500, epService);
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields,
                new Object[][]{{"E2", "E3"}, {"E5", "E6"}});

        sendTimer(9500, epService);
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields,
                new Object[][]{{"E5", "E6"}});

        sendTimer(10500, epService);
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields,
                new Object[][]{{"E5", "E6"}});

        sendTimer(11500, epService);
        assertFalse(stmt.iterator().hasNext());

        stmt.destroy();
    }

    private void runAssertionTimeWindowPartitioned(EPServiceProvider epService) {
        sendTimer(0, epService);
        String[] fields = "cat,a_string,b_string".split(",");
        String text = "select * from MyEvent#time(5) " +
                "match_recognize (" +
                "  partition by cat" +
                "  measures A.cat as cat, A.theString as a_string, B.theString as b_string" +
                "  all matches pattern (A B) " +
                "  define " +
                "    A as PREV(A.theString, 3) = 'P3' and PREV(A.theString, 2) = 'P2' and PREV(A.theString, 4) = 'P4'," +
                "    B as B.value in (PREV(B.value, 4), PREV(B.value, 2))" +
                ") order by cat";

        EPStatement stmt = epService.getEPAdministrator().createEPL(text);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        sendTimer(1000, epService);
        epService.getEPRuntime().sendEvent(new SupportRecogBean("P4", "c2", 1));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("P3", "c1", 2));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("P2", "c2", 3));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("xx", "c1", 4));
        sendTimer(2000, epService);
        epService.getEPRuntime().sendEvent(new SupportRecogBean("P2", "c1", 1));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("E1", "c1", 3));
        assertFalse(listener.isInvoked());
        assertFalse(stmt.iterator().hasNext());

        sendTimer(3000, epService);
        epService.getEPRuntime().sendEvent(new SupportRecogBean("P4", "c1", 11));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("P3", "c1", 12));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("P2", "c1", 13));
        sendTimer(4000, epService);
        epService.getEPRuntime().sendEvent(new SupportRecogBean("xx", "c1", 4));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("E2", "c1", -1));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("E3", "c1", 12));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields,
                new Object[][]{{"c1", "E2", "E3"}});
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields,
                new Object[][]{{"c1", "E2", "E3"}});

        sendTimer(5000, epService);
        epService.getEPRuntime().sendEvent(new SupportRecogBean("P4", "c2", 21));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("P3", "c2", 22));
        sendTimer(6000, epService);
        epService.getEPRuntime().sendEvent(new SupportRecogBean("P2", "c2", 23));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("xx", "c2", -2));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("E5", "c2", -1));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("E6", "c2", -2));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields,
                new Object[][]{{"c2", "E5", "E6"}});
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields,
                new Object[][]{{"c1", "E2", "E3"}, {"c2", "E5", "E6"}});

        sendTimer(8500, epService);
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields,
                new Object[][]{{"c1", "E2", "E3"}, {"c2", "E5", "E6"}});

        sendTimer(9500, epService);
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields,
                new Object[][]{{"c2", "E5", "E6"}});

        sendTimer(10500, epService);
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields,
                new Object[][]{{"c2", "E5", "E6"}});

        sendTimer(11500, epService);
        assertFalse(stmt.iterator().hasNext());

        stmt.destroy();
    }

    private void runAssertionTimeWindowPartitionedSimple(EPServiceProvider epService) {
        sendTimer(0, epService);
        String[] fields = "a_string".split(",");
        String text = "select * from MyEvent#time(5 sec) " +
                "match_recognize (" +
                "  partition by cat " +
                "  measures A.cat as cat, A.theString as a_string" +
                "  all matches pattern (A) " +
                "  define " +
                "    A as PREV(A.value) = (A.value - 1)" +
                ") order by a_string";

        EPStatement stmt = epService.getEPAdministrator().createEPL(text);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        sendTimer(1000, epService);
        epService.getEPRuntime().sendEvent(new SupportRecogBean("E1", "S1", 100));

        sendTimer(2000, epService);
        epService.getEPRuntime().sendEvent(new SupportRecogBean("E2", "S3", 100));

        sendTimer(2500, epService);
        epService.getEPRuntime().sendEvent(new SupportRecogBean("E3", "S2", 102));

        sendTimer(6200, epService);
        epService.getEPRuntime().sendEvent(new SupportRecogBean("E4", "S1", 101));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields,
                new Object[][]{{"E4"}});
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields,
                new Object[][]{{"E4"}});

        sendTimer(6500, epService);
        epService.getEPRuntime().sendEvent(new SupportRecogBean("E5", "S3", 101));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields,
                new Object[][]{{"E5"}});
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields,
                new Object[][]{{"E4"}, {"E5"}});

        sendTimer(7000, epService);
        epService.getEPRuntime().sendEvent(new SupportRecogBean("E6", "S1", 102));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields,
                new Object[][]{{"E6"}});
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields,
                new Object[][]{{"E4"}, {"E5"}, {"E6"}});

        sendTimer(10000, epService);
        epService.getEPRuntime().sendEvent(new SupportRecogBean("E7", "S2", 103));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields,
                new Object[][]{{"E7"}});
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields,
                new Object[][]{{"E4"}, {"E5"}, {"E6"}, {"E7"}});

        epService.getEPRuntime().sendEvent(new SupportRecogBean("E8", "S2", 102));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("E8", "S1", 101));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("E8", "S2", 104));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("E8", "S1", 105));
        assertFalse(listener.isInvoked());

        sendTimer(11199, epService);
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields,
                new Object[][]{{"E4"}, {"E5"}, {"E6"}, {"E7"}});

        sendTimer(11200, epService);
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields,
                new Object[][]{{"E5"}, {"E6"}, {"E7"}});

        sendTimer(11600, epService);
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields,
                new Object[][]{{"E6"}, {"E7"}});

        sendTimer(16000, epService);
        assertFalse(stmt.iterator().hasNext());

        stmt.destroy();
    }

    private void runAssertionPartitionBy2FieldsKeepall(EPServiceProvider epService) {
        String[] fields = "a_string,a_cat,a_value,b_value".split(",");
        String text = "select * from MyEvent#keepall " +
                "match_recognize (" +
                "  partition by theString, cat" +
                "  measures A.theString as a_string, A.cat as a_cat, A.value as a_value, B.value as b_value " +
                "  all matches pattern (A B) " +
                "  define " +
                "    A as (A.value > PREV(A.value))," +
                "    B as (B.value > PREV(B.value))" +
                ") order by a_string, a_cat";

        EPStatement stmt = epService.getEPAdministrator().createEPL(text);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportRecogBean("S1", "T1", 5));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("S2", "T1", 110));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("S1", "T2", 21));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("S1", "T1", 7));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("S2", "T1", 111));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("S1", "T2", 20));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("S2", "T1", 110));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("S2", "T2", 1000));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("S2", "T2", 1001));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("S1", null, 9));
        assertFalse(listener.isInvoked());
        assertFalse(stmt.iterator().hasNext());

        epService.getEPRuntime().sendEvent(new SupportRecogBean("S1", "T1", 9));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields,
                new Object[][]{{"S1", "T1", 7, 9}});
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields,
                new Object[][]{{"S1", "T1", 7, 9}});

        epService.getEPRuntime().sendEvent(new SupportRecogBean("S2", "T2", 1001));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("S2", "T1", 109));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("S1", "T2", 25));
        assertFalse(listener.isInvoked());
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields,
                new Object[][]{{"S1", "T1", 7, 9}});

        epService.getEPRuntime().sendEvent(new SupportRecogBean("S2", "T2", 1002));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("S2", "T2", 1003));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields,
                new Object[][]{{"S2", "T2", 1002, 1003}});
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields,
                new Object[][]{{"S1", "T1", 7, 9}, {"S2", "T2", 1002, 1003}});

        epService.getEPRuntime().sendEvent(new SupportRecogBean("S1", "T2", 28));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields,
                new Object[][]{{"S1", "T2", 25, 28}});
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields,
                new Object[][]{{"S1", "T1", 7, 9}, {"S1", "T2", 25, 28}, {"S2", "T2", 1002, 1003}});

        stmt.destroy();
    }

    private void runAssertionUnpartitionedKeepAll(EPServiceProvider epService) {
        String[] fields = "a_string".split(",");
        String text = "select * from MyEvent#keepall " +
                "match_recognize (" +
                "  measures A.theString as a_string" +
                "  all matches pattern (A) " +
                "  define A as (A.value > PREV(A.value))" +
                ") " +
                "order by a_string";

        EPStatement stmt = epService.getEPAdministrator().createEPL(text);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportRecogBean("E1", 5));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("E2", 3));
        assertFalse(listener.isInvoked());
        assertFalse(stmt.iterator().hasNext());

        epService.getEPRuntime().sendEvent(new SupportRecogBean("E3", 6));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields,
                new Object[][]{{"E3"}});
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields,
                new Object[][]{{"E3"}});

        epService.getEPRuntime().sendEvent(new SupportRecogBean("E4", 4));
        assertFalse(listener.isInvoked());
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields,
                new Object[][]{{"E3"}});

        epService.getEPRuntime().sendEvent(new SupportRecogBean("E5", 6));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields,
                new Object[][]{{"E5"}});
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields,
                new Object[][]{{"E3"}, {"E5"}});

        epService.getEPRuntime().sendEvent(new SupportRecogBean("E6", 10));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields,
                new Object[][]{{"E6"}});
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields,
                new Object[][]{{"E3"}, {"E5"}, {"E6"}});

        epService.getEPRuntime().sendEvent(new SupportRecogBean("E7", 9));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("E8", 4));
        assertFalse(listener.isInvoked());
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields,
                new Object[][]{{"E3"}, {"E5"}, {"E6"}});

        stmt.stop();

        text = "select * from MyEvent#keepall " +
                "match_recognize (" +
                "  measures A.theString as a_string" +
                "  all matches pattern (A) " +
                "  define A as (PREV(A.value, 2) = 5)" +
                ") " +
                "order by a_string";

        stmt = epService.getEPAdministrator().createEPL(text);
        listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportRecogBean("E1", 5));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("E2", 4));
        assertFalse(listener.isInvoked());
        assertFalse(stmt.iterator().hasNext());

        epService.getEPRuntime().sendEvent(new SupportRecogBean("E3", 6));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields,
                new Object[][]{{"E3"}});
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields,
                new Object[][]{{"E3"}});

        epService.getEPRuntime().sendEvent(new SupportRecogBean("E4", 3));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("E5", 3));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("E5", 5));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("E6", 5));
        assertFalse(listener.isInvoked());
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields,
                new Object[][]{{"E3"}});

        epService.getEPRuntime().sendEvent(new SupportRecogBean("E7", 6));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields,
                new Object[][]{{"E7"}});
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields,
                new Object[][]{{"E3"}, {"E7"}});

        epService.getEPRuntime().sendEvent(new SupportRecogBean("E8", 6));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields,
                new Object[][]{{"E8"}});
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields,
                new Object[][]{{"E3"}, {"E7"}, {"E8"}});

        stmt.destroy();
    }

    private void sendTimer(long time, EPServiceProvider epService) {
        CurrentTimeEvent theEvent = new CurrentTimeEvent(time);
        EPRuntime runtime = epService.getEPRuntime();
        runtime.sendEvent(theEvent);
    }
}
