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
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.supportregression.rowrecog.SupportRecogBean;

import static org.junit.Assert.assertFalse;

public class ExecRowRecogDelete implements RegressionExecution {

    // This test is for
    //  (a) on-delete of events from a named window
    //  (b) a sorted window which also posts a remove stream that is out-of-order
    // ... also termed Out-Of-Sequence Delete (OOSD).
    //
    // The test is for out-of-sequence (and in-sequence) deletes:
    //  (1) Make sure that partial pattern matches get removed
    //  (2) Make sure that PREV is handled by order-of-arrival, and is not affected (by default) by delete (versus normal ordered remove stream).
    //      Since it is impossible to make guarantees as the named window could be entirely deleted, and "prev" depth is therefore unknown.
    //
    // Prev
    //    has OOSD
    //      update          PREV operates on original order-of-arrival; OOSD impacts matching: resequence only when partial matches deleted
    //      iterate         PREV operates on original order-of-arrival; OOSD impacts matching: iterator may present unseen-before matches after delete
    //    no OOSD
    //      update          PREV operates on original order-of-arrival; no resequencing when in-order deleted
    //      iterate         PREV operates on original order-of-arrival
    // No-Prev
    //    has OOSD
    //      update
    //      iterate
    //    no OOSD
    //      update
    //      iterate

    public void configure(Configuration configuration) throws Exception {
        configuration.addEventType("MyEvent", SupportRecogBean.class);
        configuration.addEventType("MyDeleteEvent", SupportBean.class);
        configuration.addEventType("SupportRecogBean", SupportRecogBean.class);
        configuration.addEventType("SupportBean", SupportBean.class);
    }

    public void run(EPServiceProvider epService) throws Exception {
        runAssertionNamedWindowOnDeleteOutOfSeq(epService);
        runAssertionNamedWindowOutOfSequenceDelete(epService);
        runAssertionNamedWindowInSequenceDelete(epService);
    }

    private void runAssertionNamedWindowOnDeleteOutOfSeq(EPServiceProvider epService) {
        epService.getEPAdministrator().createEPL("create window MyNamedWindow#keepall as MyEvent");
        epService.getEPAdministrator().createEPL("insert into MyNamedWindow select * from MyEvent");
        epService.getEPAdministrator().createEPL("on MyDeleteEvent as d delete from MyNamedWindow w where d.intPrimitive = w.value");

        String[] fields = "a_string,b_string".split(",");
        String text = "select * from MyNamedWindow " +
                "match_recognize (" +
                "  measures A.theString as a_string, B.theString as b_string" +
                "  all matches pattern (A B) " +
                "  define " +
                "    A as PREV(A.theString, 3) = 'P3' and PREV(A.theString, 2) = 'P2' and PREV(A.theString, 4) = 'P4'," +
                "    B as B.value in (PREV(B.value, 4), PREV(B.value, 2))" +
                ")";

        EPStatement stmt = epService.getEPAdministrator().createEPL(text);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportRecogBean("P2", 1));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("P1", 2));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("P3", 3));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("P4", 4));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("P2", 1));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("E1", 3));
        assertFalse(listener.isInvoked());
        assertFalse(stmt.iterator().hasNext());

        epService.getEPRuntime().sendEvent(new SupportRecogBean("P4", 11));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("P3", 12));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("P2", 13));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("xx", 4));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("E2", -4));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("E3", 12));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields,
                new Object[][]{{"E2", "E3"}});
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields,
                new Object[][]{{"E2", "E3"}});

        epService.getEPRuntime().sendEvent(new SupportRecogBean("P4", 21));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("P3", 22));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("P2", 23));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("xx", -2));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("E5", -1));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("E6", -2));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields,
                new Object[][]{{"E5", "E6"}});
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields,
                new Object[][]{{"E2", "E3"}, {"E5", "E6"}});

        // delete an PREV-referenced event: no effect as PREV is an order-of-arrival operator
        epService.getEPRuntime().sendEvent(new SupportBean("D1", 21));      // delete P4 of second batch
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields,
                new Object[][]{{"E2", "E3"}, {"E5", "E6"}});

        // delete an partial-match event
        epService.getEPRuntime().sendEvent(new SupportBean("D2", -1));      // delete E5 of second batch
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields,
                new Object[][]{{"E2", "E3"}});

        epService.getEPRuntime().sendEvent(new SupportBean("D3", 12));      // delete P3 and E3 of first batch
        assertFalse(stmt.iterator().hasNext());

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionNamedWindowOutOfSequenceDelete(EPServiceProvider epService) {
        epService.getEPAdministrator().createEPL("create window MyWindow#keepall as SupportRecogBean");
        epService.getEPAdministrator().createEPL("insert into MyWindow select * from SupportRecogBean");
        epService.getEPAdministrator().createEPL("on SupportBean as s delete from MyWindow as w where s.theString = w.theString");

        String[] fields = "a0,a1,b0,b1,c".split(",");
        String text = "select * from MyWindow " +
                "match_recognize (" +
                "  measures A[0].theString as a0, A[1].theString as a1, B[0].theString as b0, B[1].theString as b1, C.theString as c" +
                "  pattern ( A+ B* C ) " +
                "  define " +
                "    A as (A.value = 1)," +
                "    B as (B.value = 2)," +
                "    C as (C.value = 3)" +
                ")";

        EPStatement stmt = epService.getEPAdministrator().createEPL(text);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportRecogBean("E1", 1));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("E2", 1));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 0));       // deletes E2
        epService.getEPRuntime().sendEvent(new SupportRecogBean("E3", 3));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields,
                new Object[][]{{"E1", null, null, null, "E3"}});
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields,
                new Object[][]{{"E1", null, null, null, "E3"}});

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 0));       // deletes E1
        epService.getEPRuntime().sendEvent(new SupportBean("E4", 0));       // deletes E4

        epService.getEPRuntime().sendEvent(new SupportRecogBean("E4", 1));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("E5", 1));
        epService.getEPRuntime().sendEvent(new SupportBean("E4", 0));       // deletes E4
        epService.getEPRuntime().sendEvent(new SupportRecogBean("E6", 3));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields,
                new Object[][]{{"E5", null, null, null, "E6"}});
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields,
                new Object[][]{{"E5", null, null, null, "E6"}});

        epService.getEPRuntime().sendEvent(new SupportRecogBean("E7", 1));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("E8", 1));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("E9", 2));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("E10", 2));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("E11", 2));
        epService.getEPRuntime().sendEvent(new SupportBean("E9", 0));       // deletes E9
        epService.getEPRuntime().sendEvent(new SupportRecogBean("E12", 3));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields,
                new Object[][]{{"E7", "E8", "E10", "E11", "E12"}});
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields,
                new Object[][]{{"E5", null, null, null, "E6"}, {"E7", "E8", "E10", "E11", "E12"}});    // note interranking among per-event result

        epService.getEPRuntime().sendEvent(new SupportRecogBean("E13", 1));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("E14", 1));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("E15", 2));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("E16", 2));
        epService.getEPRuntime().sendEvent(new SupportBean("E14", 0));       // deletes E14
        epService.getEPRuntime().sendEvent(new SupportBean("E15", 0));       // deletes E15
        epService.getEPRuntime().sendEvent(new SupportBean("E16", 0));       // deletes E16
        epService.getEPRuntime().sendEvent(new SupportBean("E13", 0));       // deletes E17
        epService.getEPRuntime().sendEvent(new SupportRecogBean("E18", 3));
        assertFalse(listener.isInvoked());
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields,
                new Object[][]{{"E5", null, null, null, "E6"}, {"E7", "E8", "E10", "E11", "E12"}});    // note interranking among per-event result

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionNamedWindowInSequenceDelete(EPServiceProvider epService) {
        epService.getEPAdministrator().createEPL("create window MyWindow#keepall as SupportRecogBean");
        epService.getEPAdministrator().createEPL("insert into MyWindow select * from SupportRecogBean");
        epService.getEPAdministrator().createEPL("on SupportBean as s delete from MyWindow as w where s.theString = w.theString");

        String[] fields = "a0,a1,b".split(",");
        String text = "select * from MyWindow " +
                "match_recognize (" +
                "  measures A[0].theString as a0, A[1].theString as a1, B.theString as b" +
                "  pattern ( A* B ) " +
                "  define " +
                "    A as (A.value = 1)," +
                "    B as (B.value = 2)" +
                ")";

        EPStatement stmt = epService.getEPAdministrator().createEPL(text);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportRecogBean("E1", 1));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("E2", 1));
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 0));       // deletes E1
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 0));       // deletes E2
        epService.getEPRuntime().sendEvent(new SupportRecogBean("E3", 3));
        assertFalse(listener.isInvoked());
        assertFalse(stmt.iterator().hasNext());

        epService.getEPRuntime().sendEvent(new SupportRecogBean("E4", 1));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("E5", 1));
        epService.getEPRuntime().sendEvent(new SupportBean("E4", 0));       // deletes E4
        epService.getEPRuntime().sendEvent(new SupportRecogBean("E6", 1));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("E7", 2));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields,
                new Object[][]{{"E5", "E6", "E7"}});
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields,
                new Object[][]{{"E5", "E6", "E7"}});

        epService.getEPAdministrator().destroyAllStatements();
    }
}