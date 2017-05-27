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
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.supportregression.rowrecog.SupportRecogBean;

public class ExecRowRecogAggregation implements RegressionExecution {

    public void run(EPServiceProvider epService) throws Exception {
        epService.getEPAdministrator().getConfiguration().addEventType("MyEvent", SupportRecogBean.class);

        runAssertionMeasureAggregation(epService);
        runAssertionMeasureAggregationPartitioned(epService);
    }

    private void runAssertionMeasureAggregation(EPServiceProvider epService) {
        String text = "select * from MyEvent#keepall " +
                "match_recognize (" +
                "  measures A.theString as a_string, " +
                "       C.theString as c_string, " +
                "       max(B.value) as maxb, " +
                "       min(B.value) as minb, " +
                "       2*min(B.value) as minb2x, " +
                "       last(B.value) as lastb, " +
                "       first(B.value) as firstb," +
                "       count(B.value) as countb " +
                "  all matches pattern (A B* C) " +
                "  define " +
                "   A as (A.value = 0)," +
                "   B as (B.value != 1)," +
                "   C as (C.value = 1)" +
                ") " +
                "order by a_string";

        EPStatement stmt = epService.getEPAdministrator().createEPL(text);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        String[] fields = "a_string,c_string,maxb,minb,minb2x,firstb,lastb,countb".split(",");
        epService.getEPRuntime().sendEvent(new SupportRecogBean("E1", 0));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("E2", 1));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields,
                new Object[][]{{"E1", "E2", null, null, null, null, null, 0L}});
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields,
                new Object[][]{{"E1", "E2", null, null, null, null, null, 0L}});

        epService.getEPRuntime().sendEvent(new SupportRecogBean("E3", 0));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("E4", 5));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("E5", 3));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("E6", 1));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields,
                new Object[][]{{"E3", "E6", 5, 3, 6, 5, 3, 2L}});
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields,
                new Object[][]{{"E1", "E2", null, null, null, null, null, 0L}, {"E3", "E6", 5, 3, 6, 5, 3, 2L}});

        epService.getEPRuntime().sendEvent(new SupportRecogBean("E7", 0));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("E8", 4));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("E9", -1));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("E10", 7));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("E11", 2));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("E12", 1));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields,
                new Object[][]{{"E7", "E12", 7, -1, -2, 4, 2, 4L}});
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields,
                new Object[][]{{"E1", "E2", null, null, null, null, null, 0L},
                    {"E3", "E6", 5, 3, 6, 5, 3, 2L},
                    {"E7", "E12", 7, -1, -2, 4, 2, 4L},
                });

        stmt.destroy();
    }

    private void runAssertionMeasureAggregationPartitioned(EPServiceProvider epService) {
        String text = "select * from MyEvent#keepall " +
                "match_recognize (" +
                "  partition by cat" +
                "  measures A.cat as cat, A.theString as a_string, " +
                "       D.theString as d_string, " +
                "       sum(C.value) as sumc, " +
                "       sum(B.value) as sumb, " +
                "       sum(B.value + A.value) as sumaplusb, " +
                "       sum(C.value + A.value) as sumaplusc " +
                "  all matches pattern (A B B C C D) " +
                "  define " +
                "   A as (A.value >= 10)," +
                "   B as (B.value > 1)," +
                "   C as (C.value < -1)," +
                "   D as (D.value = 999)" +
                ") order by cat";

        EPStatement stmt = epService.getEPAdministrator().createEPL(text);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        String[] fields = "a_string,d_string,sumb,sumc,sumaplusb,sumaplusc".split(",");
        epService.getEPRuntime().sendEvent(new SupportRecogBean("E1", "x", 10));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("E2", "y", 20));

        epService.getEPRuntime().sendEvent(new SupportRecogBean("E3", "x", 7));     // B
        epService.getEPRuntime().sendEvent(new SupportRecogBean("E4", "y", 5));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("E5", "x", 8));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("E6", "y", 2));

        epService.getEPRuntime().sendEvent(new SupportRecogBean("E7", "x", -2));    // C
        epService.getEPRuntime().sendEvent(new SupportRecogBean("E8", "y", -7));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("E9", "x", -5));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("E10", "y", -4));

        epService.getEPRuntime().sendEvent(new SupportRecogBean("E11", "y", 999));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields,
                new Object[][]{{"E2", "E11", 7, -11, 47, 29}});
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields,
                new Object[][]{{"E2", "E11", 7, -11, 47, 29}});

        epService.getEPRuntime().sendEvent(new SupportRecogBean("E12", "x", 999));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields,
                new Object[][]{{"E1", "E12", 15, -7, 35, 13}});
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields,
                new Object[][]{{"E1", "E12", 15, -7, 35, 13}, {"E2", "E11", 7, -11, 47, 29}});

        stmt.destroy();
    }
}
