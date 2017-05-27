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
import com.espertech.esper.client.soda.EPStatementObjectModel;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.supportregression.rowrecog.SupportRecogBean;
import com.espertech.esper.util.SerializableObjectCopier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class ExecRowRecogAfter implements RegressionExecution {

    public void run(EPServiceProvider epService) throws Exception {
        epService.getEPAdministrator().getConfiguration().addEventType("MyEvent", SupportRecogBean.class);

        runAssertionAfterCurrentRow(epService);
        runAssertionAfterNextRow(epService);
        runAssertionSkipToNextRow(epService);
        runAssertionVariableMoreThenOnce(epService);
        runAssertionSkipToNextRowPartitioned(epService);
        runAssertionAfterSkipPastLast(epService);
    }

    private void runAssertionAfterCurrentRow(EPServiceProvider epService) throws Exception {
        String text = "select * from MyEvent#keepall " +
                "match_recognize (" +
                " measures A.theString as a, B[0].theString as b0, B[1].theString as b1" +
                " after match skip to current row" +
                " pattern (A B*)" +
                " define" +
                " A as A.theString like \"A%\"," +
                " B as B.theString like \"B%\"" +
                ")";

        EPStatement stmt = epService.getEPAdministrator().createEPL(text);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        tryAssertionAfterCurrentRow(epService, listener, stmt);

        stmt.destroy();
        EPStatementObjectModel model = epService.getEPAdministrator().compileEPL(text);
        SerializableObjectCopier.copy(model);
        assertEquals(text, model.toEPL());
        stmt = epService.getEPAdministrator().create(model);
        stmt.addListener(listener);
        assertEquals(text, stmt.getText());

        tryAssertionAfterCurrentRow(epService, listener, stmt);

        stmt.destroy();
    }

    private void tryAssertionAfterCurrentRow(EPServiceProvider epService, SupportUpdateListener listener, EPStatement stmt) {
        String[] fields = "a,b0,b1".split(",");

        epService.getEPRuntime().sendEvent(new SupportRecogBean("A1", 1));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields,
                new Object[][]{{"A1", null, null}});
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields,
                new Object[][]{{"A1", null, null}});

        // since the first match skipped past A, we do not match again
        epService.getEPRuntime().sendEvent(new SupportRecogBean("B1", 2));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields,
                new Object[][]{{"A1", "B1", null}});
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields,
                new Object[][]{{"A1", "B1", null}});
    }

    private void runAssertionAfterNextRow(EPServiceProvider epService) {
        String[] fields = "a,b0,b1".split(",");
        String text = "select * from MyEvent#keepall " +
                "match_recognize (" +
                "  measures A.theString as a, B[0].theString as b0, B[1].theString as b1" +
                "  AFTER MATCH SKIP TO NEXT ROW " +
                "  pattern (A B*) " +
                "  define " +
                "    A as A.theString like 'A%'," +
                "    B as B.theString like 'B%'" +
                ")";

        EPStatement stmt = epService.getEPAdministrator().createEPL(text);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportRecogBean("A1", 1));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields,
                new Object[][]{{"A1", null, null}});
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields,
                new Object[][]{{"A1", null, null}});

        // since the first match skipped past A, we do not match again
        epService.getEPRuntime().sendEvent(new SupportRecogBean("B1", 2));
        assertFalse(listener.isInvoked());  // incremental skips to next 
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields,
                new Object[][]{{"A1", "B1", null}});

        stmt.destroy();
    }

    private void runAssertionSkipToNextRow(EPServiceProvider epService) {
        String[] fields = "a_string,b_string".split(",");
        String text = "select * from MyEvent#keepall " +
                "match_recognize (" +
                "  measures A.theString as a_string, B.theString as b_string " +
                "  all matches " +
                "  after match skip to next row " +
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
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields,
                new Object[][]{{"E5", "E6"}});
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields,
                new Object[][]{{"E2", "E3"}, {"E4", "E5"}, {"E5", "E6"}});

        epService.getEPRuntime().sendEvent(new SupportRecogBean("E7", 9));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("E8", 4));
        assertFalse(listener.isInvoked());
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields,
                new Object[][]{{"E2", "E3"}, {"E4", "E5"}, {"E5", "E6"}});

        stmt.stop();
    }

    private void runAssertionVariableMoreThenOnce(EPServiceProvider epService) {
        String[] fields = "a0,b,a1".split(",");
        String text = "select * from MyEvent#keepall " +
                "match_recognize (" +
                "  measures A[0].theString as a0, B.theString as b, A[1].theString as a1 " +
                "  all matches " +
                "  after match skip to next row " +
                "  pattern ( A B A ) " +
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
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields,
                new Object[][]{{"E7", "E8", "E9"}});
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields,
                new Object[][]{{"E5", "E6", "E7"}, {"E7", "E8", "E9"}});

        stmt.destroy();
    }

    private void runAssertionSkipToNextRowPartitioned(EPServiceProvider epService) {
        String[] fields = "a_string,a_value,b_value".split(",");
        String text = "select * from MyEvent#keepall " +
                "match_recognize (" +
                "  partition by theString" +
                "  measures A.theString as a_string, A.value as a_value, B.value as b_value " +
                "  all matches " +
                "  after match skip to next row " +
                "  pattern (A B) " +
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
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields,
                new Object[][]{{"S4", 10, 11}});
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields,
                new Object[][]{{"S1", 4, 6}, {"S4", -1, 10}, {"S4", 10, 11}});

        epService.getEPRuntime().sendEvent(new SupportRecogBean("S3", 3));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("S4", -1));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("S3", 2));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("S1", 4));
        assertFalse(listener.isInvoked());
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields,
                new Object[][]{{"S1", 4, 6}, {"S4", -1, 10}, {"S4", 10, 11}});

        epService.getEPRuntime().sendEvent(new SupportRecogBean("S1", 7));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields,
                new Object[][]{{"S1", 4, 7}});
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields,
                new Object[][]{{"S1", 4, 6}, {"S1", 4, 7}, {"S4", -1, 10}, {"S4", 10, 11}});

        epService.getEPRuntime().sendEvent(new SupportRecogBean("S4", 12));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields,
                new Object[][]{{"S4", -1, 12}});
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields,
                new Object[][]{{"S1", 4, 6}, {"S1", 4, 7}, {"S4", -1, 10}, {"S4", 10, 11}, {"S4", -1, 12}});

        epService.getEPRuntime().sendEvent(new SupportRecogBean("S4", 12));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("S1", 7));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("S2", 4));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("S1", 5));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportRecogBean("S2", 5));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields,
                new Object[][]{{"S2", 4, 5}});
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields,
                new Object[][]{{"S1", 4, 6}, {"S1", 4, 7}, {"S2", 4, 5}, {"S4", -1, 10}, {"S4", 10, 11}, {"S4", -1, 12}});

        stmt.destroy();
    }

    private void runAssertionAfterSkipPastLast(EPServiceProvider epService) {
        String[] fields = "a_string,b_string".split(",");
        String text = "select * from MyEvent#keepall " +
                "match_recognize (" +
                "  measures A.theString as a_string, B.theString as b_string " +
                "  all matches " +
                "  after match skip past last row" +
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
}
