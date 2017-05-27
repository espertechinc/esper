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
import com.espertech.esper.rowregex.RegexPartitionStateRepoGroup;
import com.espertech.esper.supportregression.bean.SupportBean_A;
import com.espertech.esper.supportregression.bean.SupportBean_B;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.supportregression.rowrecog.SupportRecogBean;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ExecRowRecogOps implements RegressionExecution {
    public void configure(Configuration configuration) throws Exception {
        configuration.addEventType("MyEvent", SupportRecogBean.class);
    }

    public void run(EPServiceProvider epService) throws Exception {
        runAssertionConcatenation(epService);
        runAssertionZeroToMany(epService);
        runAssertionOneToMany(epService);
        runAssertionZeroToOne(epService);
        runAssertionPartitionBy(epService);
        runAssertionUnlimitedPartition(epService);
        runAssertionConcatWithinAlter(epService);
        runAssertionAlterWithinConcat(epService);
        runAssertionVariableMoreThenOnce(epService);
        runAssertionRegex();
    }

    private void runAssertionConcatenation(EPServiceProvider epService) {
        String[] fields = "a_string,b_string".split(",");
        String text = "select * from MyEvent#keepall " +
                "match_recognize (" +
                "  measures A.theString as a_string, B.theString as b_string " +
                "  all matches " +
                "  pattern (A B) " +
                "  define B as B.value > A.value" +
                ") " +
                "order by a_string, b_string";

        EPStatement stmt = epService.getEPAdministrator().createEPL(text);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportRecogBean("E1", 5));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("E2", 3));
        assertFalse(listener.isInvoked());
        assertFalse(stmt.iterator().hasNext());

        epService.getEPRuntime().sendEvent(new SupportRecogBean("E3", 6));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields,
                new Object[][]{{"E2", "E3"}});
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields,
                new Object[][]{{"E2", "E3"}});

        epService.getEPRuntime().sendEvent(new SupportRecogBean("E4", 4));
        assertFalse(listener.isInvoked());
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields,
                new Object[][]{{"E2", "E3"}});

        epService.getEPRuntime().sendEvent(new SupportRecogBean("E5", 6));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields,
                new Object[][]{{"E4", "E5"}});
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields,
                new Object[][]{{"E2", "E3"}, {"E4", "E5"}});

        epService.getEPRuntime().sendEvent(new SupportRecogBean("E6", 10));
        assertFalse(listener.isInvoked());      // E5-E6 not a match since "skip past last row"
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields,
                new Object[][]{{"E2", "E3"}, {"E4", "E5"}});

        epService.getEPRuntime().sendEvent(new SupportRecogBean("E7", 9));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("E8", 4));
        assertFalse(listener.isInvoked());
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields,
                new Object[][]{{"E2", "E3"}, {"E4", "E5"}});

        stmt.stop();
    }

    private void runAssertionZeroToMany(EPServiceProvider epService) {
        String[] fields = "a_string,b0_string,b1_string,b2_string,c_string".split(",");
        String text = "select * from MyEvent#keepall " +
                "match_recognize (" +
                "  measures A.theString as a_string, " +
                "    B[0].theString as b0_string, " +
                "    B[1].theString as b1_string, " +
                "    B[2].theString as b2_string, " +
                "    C.theString as c_string" +
                "  all matches " +
                "  pattern (A B* C) " +
                "  define \n" +
                "    A as A.value = 10,\n" +
                "    B as B.value > 10,\n" +
                "    C as C.value < 10\n" +
                ") " +
                "order by a_string, c_string";

        EPStatement stmt = epService.getEPAdministrator().createEPL(text);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportRecogBean("E1", 12));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("E2", 10));
        assertFalse(listener.isInvoked());
        assertFalse(stmt.iterator().hasNext());

        epService.getEPRuntime().sendEvent(new SupportRecogBean("E3", 8));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields,
                new Object[][]{{"E2", null, null, null, "E3"}});
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields,
                new Object[][]{{"E2", null, null, null, "E3"}});

        epService.getEPRuntime().sendEvent(new SupportRecogBean("E4", 10));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("E5", 12));
        assertFalse(listener.isInvoked());
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields,
                new Object[][]{{"E2", null, null, null, "E3"}});

        epService.getEPRuntime().sendEvent(new SupportRecogBean("E6", 8));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields,
                new Object[][]{{"E4", "E5", null, null, "E6"}});
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields,
                new Object[][]{{"E2", null, null, null, "E3"}, {"E4", "E5", null, null, "E6"}});

        epService.getEPRuntime().sendEvent(new SupportRecogBean("E7", 10));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("E8", 12));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("E9", 12));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("E10", 12));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("E11", 9));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields,
                new Object[][]{{"E7", "E8", "E9", "E10", "E11"}});
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields,
                new Object[][]{{"E2", null, null, null, "E3"}, {"E4", "E5", null, null, "E6"}, {"E7", "E8", "E9", "E10", "E11"}});

        stmt.stop();

        // Zero-to-many unfiltered
        String epl = "select * from MyEvent match_recognize (" +
                "measures A as a, B as b, C as c " +
                "pattern (A C*? B) " +
                "define " +
                "A as typeof(A) = 'MyEventTypeA'," +
                "B as typeof(B) = 'MyEventTypeB'" +
                ")";
        stmt = epService.getEPAdministrator().createEPL(epl);
        stmt.destroy();
    }

    private void runAssertionOneToMany(EPServiceProvider epService) {
        String[] fields = "a_string,b0_string,b1_string,b2_string,c_string".split(",");
        String text = "select * from MyEvent#keepall " +
                "match_recognize (" +
                "  measures A.theString as a_string, " +
                "    B[0].theString as b0_string, " +
                "    B[1].theString as b1_string, " +
                "    B[2].theString as b2_string, " +
                "    C.theString as c_string" +
                "  all matches " +
                "  pattern (A B+ C) " +
                "  define \n" +
                "    A as (A.value = 10),\n" +
                "    B as (B.value > 10),\n" +
                "    C as (C.value < 10)\n" +
                ") " +
                "order by a_string, c_string";

        EPStatement stmt = epService.getEPAdministrator().createEPL(text);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportRecogBean("E1", 12));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("E2", 10));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("E3", 8));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("E4", 10));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("E5", 12));
        assertFalse(listener.isInvoked());
        assertFalse(stmt.iterator().hasNext());

        epService.getEPRuntime().sendEvent(new SupportRecogBean("E6", 8));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields,
                new Object[][]{{"E4", "E5", null, null, "E6"}});
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields,
                new Object[][]{{"E4", "E5", null, null, "E6"}});

        epService.getEPRuntime().sendEvent(new SupportRecogBean("E7", 10));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("E8", 12));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("E9", 12));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("E10", 12));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("E11", 9));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields,
                new Object[][]{{"E7", "E8", "E9", "E10", "E11"}});
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields,
                new Object[][]{{"E4", "E5", null, null, "E6"}, {"E7", "E8", "E9", "E10", "E11"}});

        stmt.stop();
    }

    private void runAssertionZeroToOne(EPServiceProvider epService) {
        String[] fields = "a_string,b_string,c_string".split(",");
        String text = "select * from MyEvent#keepall " +
                "match_recognize (" +
                "  measures A.theString as a_string, B.theString as b_string, " +
                "    C.theString as c_string" +
                "  all matches " +
                "  pattern (A B? C) " +
                "  define \n" +
                "    A as (A.value = 10),\n" +
                "    B as (B.value > 10),\n" +
                "    C as (C.value < 10)\n" +
                ") " +
                "order by a_string";

        EPStatement stmt = epService.getEPAdministrator().createEPL(text);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportRecogBean("E1", 12));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("E2", 10));
        assertFalse(listener.isInvoked());
        assertFalse(stmt.iterator().hasNext());

        epService.getEPRuntime().sendEvent(new SupportRecogBean("E3", 8));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields,
                new Object[][]{{"E2", null, "E3"}});
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields,
                new Object[][]{{"E2", null, "E3"}});

        epService.getEPRuntime().sendEvent(new SupportRecogBean("E4", 10));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("E5", 12));
        assertFalse(listener.isInvoked());
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields,
                new Object[][]{{"E2", null, "E3"}});

        epService.getEPRuntime().sendEvent(new SupportRecogBean("E6", 8));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields,
                new Object[][]{{"E4", "E5", "E6"}});
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields,
                new Object[][]{{"E2", null, "E3"}, {"E4", "E5", "E6"}});

        epService.getEPRuntime().sendEvent(new SupportRecogBean("E7", 10));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("E8", 12));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("E9", 12));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("E11", 9));
        assertFalse(listener.isInvoked());
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields,
                new Object[][]{{"E2", null, "E3"}, {"E4", "E5", "E6"}});

        stmt.stop();

        // test optional event not defined
        epService.getEPAdministrator().getConfiguration().addEventType("A", SupportBean_A.class);
        epService.getEPAdministrator().getConfiguration().addEventType("B", SupportBean_B.class);

        String epl = "select * from A match_recognize (" +
                "measures A.id as id, B.id as b_id " +
                "pattern (A B?) " +
                "define " +
                " A as typeof(A) = 'A'" +
                ")";
        epService.getEPAdministrator().createEPL(epl).addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean_A("A1"));
        assertTrue(listener.isInvoked());

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionPartitionBy(EPServiceProvider epService) {
        String[] fields = "a_string,a_value,b_value".split(",");
        String text = "select * from MyEvent#keepall " +
                "match_recognize (" +
                "  partition by theString" +
                "  measures A.theString as a_string, A.value as a_value, B.value as b_value " +
                "  all matches pattern (A B) " +
                "  define B as (B.value > A.value)" +
                ")" +
                " order by a_string";

        EPStatement stmt = epService.getEPAdministrator().createEPL(text);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportRecogBean("S1", 5));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("S2", 6));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("S3", 3));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("S4", 4));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("S1", 5));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("S2", 5));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("S1", 4));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("S4", -1));
        assertFalse(listener.isInvoked());
        assertFalse(stmt.iterator().hasNext());

        epService.getEPRuntime().sendEvent(new SupportRecogBean("S1", 6));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields,
                new Object[][]{{"S1", 4, 6}});
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields,
                new Object[][]{{"S1", 4, 6}});

        epService.getEPRuntime().sendEvent(new SupportRecogBean("S4", 10));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields,
                new Object[][]{{"S4", -1, 10}});
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields,
                new Object[][]{{"S1", 4, 6}, {"S4", -1, 10}});

        epService.getEPRuntime().sendEvent(new SupportRecogBean("S4", 11));
        assertFalse(listener.isInvoked());      // since skip past last row
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields,
                new Object[][]{{"S1", 4, 6}, {"S4", -1, 10}});

        epService.getEPRuntime().sendEvent(new SupportRecogBean("S3", 3));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("S4", -2));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("S3", 2));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("S1", 4));
        assertFalse(listener.isInvoked());
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields,
                new Object[][]{{"S1", 4, 6}, {"S4", -1, 10}});

        epService.getEPRuntime().sendEvent(new SupportRecogBean("S1", 7));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields,
                new Object[][]{{"S1", 4, 7}});
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields,
                new Object[][]{{"S1", 4, 6}, {"S1", 4, 7}, {"S4", -1, 10}});

        epService.getEPRuntime().sendEvent(new SupportRecogBean("S4", 12));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields,
                new Object[][]{{"S4", -2, 12}});
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields,
                new Object[][]{{"S1", 4, 6}, {"S1", 4, 7}, {"S4", -1, 10}, {"S4", -2, 12}});

        epService.getEPRuntime().sendEvent(new SupportRecogBean("S4", 12));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("S1", 7));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("S2", 4));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("S1", 5));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportRecogBean("S2", 5));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields,
                new Object[][]{{"S2", 4, 5}});
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields,
                new Object[][]{{"S1", 4, 6}, {"S1", 4, 7}, {"S2", 4, 5}, {"S4", -1, 10}, {"S4", -2, 12}});

        stmt.destroy();
    }

    private void runAssertionUnlimitedPartition(EPServiceProvider epService) {
        String text = "select * from MyEvent#keepall " +
                "match_recognize (" +
                "  partition by value" +
                "  measures A.theString as a_string " +
                "  pattern (A B) " +
                "  define " +
                "    A as (A.theString = 'A')," +
                "    B as (B.theString = 'B')" +
                ")";

        EPStatement stmt = epService.getEPAdministrator().createEPL(text);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        for (int i = 0; i < 5 * RegexPartitionStateRepoGroup.INITIAL_COLLECTION_MIN; i++) {
            epService.getEPRuntime().sendEvent(new SupportRecogBean("A", i));
            epService.getEPRuntime().sendEvent(new SupportRecogBean("B", i));
            assertTrue(listener.getAndClearIsInvoked());
        }

        for (int i = 0; i < 5 * RegexPartitionStateRepoGroup.INITIAL_COLLECTION_MIN; i++) {
            epService.getEPRuntime().sendEvent(new SupportRecogBean("A", i + 100000));
        }
        assertFalse(listener.getAndClearIsInvoked());
        for (int i = 0; i < 5 * RegexPartitionStateRepoGroup.INITIAL_COLLECTION_MIN; i++) {
            epService.getEPRuntime().sendEvent(new SupportRecogBean("B", i + 100000));
            assertTrue(listener.getAndClearIsInvoked());
        }

        stmt.destroy();
    }

    private void runAssertionConcatWithinAlter(EPServiceProvider epService) {
        String[] fields = "a_string,b_string,c_string,d_string".split(",");
        String text = "select * from MyEvent#keepall " +
                "match_recognize (" +
                "  measures A.theString as a_string, B.theString as b_string, C.theString as c_string, D.theString as d_string " +
                "  all matches pattern ( A B | C D ) " +
                "  define " +
                "    A as (A.value = 1)," +
                "    B as (B.value = 2)," +
                "    C as (C.value = 3)," +
                "    D as (D.value = 4)" +
                ")";

        EPStatement stmt = epService.getEPAdministrator().createEPL(text);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportRecogBean("E1", 3));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("E2", 5));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("E3", 4));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("E4", 3));
        assertFalse(listener.isInvoked());
        assertFalse(stmt.iterator().hasNext());

        epService.getEPRuntime().sendEvent(new SupportRecogBean("E5", 4));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields,
                new Object[][]{{null, null, "E4", "E5"}});
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields,
                new Object[][]{{null, null, "E4", "E5"}});

        epService.getEPRuntime().sendEvent(new SupportRecogBean("E1", 1));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("E1", 1));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("E2", 2));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields,
                new Object[][]{{"E1", "E2", null, null}});
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields,
                new Object[][]{{null, null, "E4", "E5"}, {"E1", "E2", null, null}});

        stmt.stop();
    }

    private void runAssertionAlterWithinConcat(EPServiceProvider epService) {
        String[] fields = "a_string,b_string,c_string,d_string".split(",");
        String text = "select * from MyEvent#keepall " +
                "match_recognize (" +
                "  measures A.theString as a_string, B.theString as b_string, C.theString as c_string, D.theString as d_string " +
                "  all matches pattern ( (A | B) (C | D) ) " +
                "  define " +
                "    A as (A.value = 1)," +
                "    B as (B.value = 2)," +
                "    C as (C.value = 3)," +
                "    D as (D.value = 4)" +
                ")";

        EPStatement stmt = epService.getEPAdministrator().createEPL(text);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportRecogBean("E1", 3));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("E2", 1));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("E3", 2));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("E4", 5));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("E5", 1));
        assertFalse(listener.isInvoked());
        assertFalse(stmt.iterator().hasNext());

        epService.getEPRuntime().sendEvent(new SupportRecogBean("E6", 3));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields,
                new Object[][]{{"E5", null, "E6", null}});
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields,
                new Object[][]{{"E5", null, "E6", null}});

        epService.getEPRuntime().sendEvent(new SupportRecogBean("E7", 2));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("E8", 3));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields,
                new Object[][]{{null, "E7", "E8", null}});
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields,
                new Object[][]{{"E5", null, "E6", null}, {null, "E7", "E8", null}});

        stmt.destroy();
    }

    private void runAssertionVariableMoreThenOnce(EPServiceProvider epService) {
        String[] fields = "a0,b,a1".split(",");
        String text = "select * from MyEvent#keepall " +
                "match_recognize (" +
                "  measures A[0].theString as a0, B.theString as b, A[1].theString as a1 " +
                "  all matches pattern ( A B A ) " +
                "  define " +
                "    A as (A.value = 1)," +
                "    B as (B.value = 2)" +
                ")";

        EPStatement stmt = epService.getEPAdministrator().createEPL(text);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportRecogBean("E1", 3));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("E2", 1));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("E3", 2));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("E4", 5));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("E5", 1));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("E6", 2));
        assertFalse(listener.isInvoked());
        assertFalse(stmt.iterator().hasNext());

        epService.getEPRuntime().sendEvent(new SupportRecogBean("E7", 1));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields,
                new Object[][]{{"E5", "E6", "E7"}});
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields,
                new Object[][]{{"E5", "E6", "E7"}});

        epService.getEPRuntime().sendEvent(new SupportRecogBean("E8", 2));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("E9", 1));
        assertFalse(listener.isInvoked());
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields,
                new Object[][]{{"E5", "E6", "E7"}});

        epService.getEPRuntime().sendEvent(new SupportRecogBean("E10", 2));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("E11", 1));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields,
                new Object[][]{{"E9", "E10", "E11"}});
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields,
                new Object[][]{{"E5", "E6", "E7"}, {"E9", "E10", "E11"}});

        stmt.destroy();
    }

    private void runAssertionRegex() {
        assertTrue("aq".matches("^aq|^id"));
        assertTrue("id".matches("^aq|^id"));
        assertTrue("ad".matches("a(q|i)?d"));
        assertTrue("aqd".matches("a(q|i)?d"));
        assertTrue("aid".matches("a(q|i)?d"));
        assertFalse("aed".matches("a(q|i)?d"));
        assertFalse("a".matches("(a(b?)c)?"));
    }
}
