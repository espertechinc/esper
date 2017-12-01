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
package com.espertech.esper.regression.nwtable.tbl;

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBean_S0;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.supportregression.util.SupportMessageAssertUtil;
import com.espertech.esper.supportregression.util.SupportModelHelper;
import com.espertech.esper.support.SupportEventTypeAssertionEnum;
import com.espertech.esper.support.SupportEventTypeAssertionUtil;

public class ExecTableNonAccessDotSubqueryAndJoin implements RegressionExecution {

    public void run(EPServiceProvider epService) throws Exception {
        for (Class clazz : new Class[]{SupportBean.class, SupportBean_S0.class}) {
            epService.getEPAdministrator().getConfiguration().addEventType(clazz);
        }

        runAssertionUse(epService, false);
        runAssertionUse(epService, true);
    }

    private void runAssertionUse(EPServiceProvider epService, boolean soda) throws Exception {
        String eplCreate = "create table MyTable (" +
                "col0 string, " +
                "col1 sum(int), " +
                "col2 sorted(intPrimitive) @type('SupportBean'), " +
                "col3 int[], " +
                "col4 window(*) @type('SupportBean')" +
                ")";
        SupportModelHelper.createByCompileOrParse(epService, soda, eplCreate);

        String eplIntoTable = "into table MyTable select sum(intPrimitive) as col1, sorted() as col2, " +
                "window(*) as col4 from SupportBean#length(3)";
        EPStatement stmtIntoTable = SupportModelHelper.createByCompileOrParse(epService, soda, eplIntoTable);
        SupportBean[] sentSB = new SupportBean[2];
        sentSB[0] = makeSendSupportBean(epService, "E1", 20);
        sentSB[1] = makeSendSupportBean(epService, "E2", 21);
        stmtIntoTable.destroy();

        String eplMerge = "on SupportBean merge MyTable when matched then update set col3={1,2,4,2}, col0=\"x\"";
        EPStatement stmtMerge = SupportModelHelper.createByCompileOrParse(epService, soda, eplMerge);
        makeSendSupportBean(epService, null, -1);
        stmtMerge.destroy();

        String eplSelect = "select " +
                "col0 as c0_1, mt.col0 as c0_2, " +
                "col1 as c1_1, mt.col1 as c1_2, " +
                "col2 as c2_1, mt.col2 as c2_2, " +
                "col2.minBy() as c2_3, mt.col2.maxBy() as c2_4, " +
                "col2.sorted().firstOf() as c2_5, mt.col2.sorted().firstOf() as c2_6, " +
                "col3.mostFrequent() as c3_1, mt.col3.mostFrequent() as c3_2, " +
                "col4 as c4_1 " +
                "from SupportBean unidirectional, MyTable as mt";
        EPStatement stmtSelect = SupportModelHelper.createByCompileOrParse(epService, soda, eplSelect);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmtSelect.addListener(listener);

        Object[][] expectedType = new Object[][]{
                {"c0_1", String.class}, {"c0_2", String.class},
                {"c1_1", Integer.class}, {"c1_2", Integer.class},
                {"c2_1", SupportBean[].class}, {"c2_2", SupportBean[].class},
                {"c2_3", SupportBean.class}, {"c2_4", SupportBean.class},
                {"c2_5", SupportBean.class}, {"c2_6", SupportBean.class},
                {"c3_1", Integer.class}, {"c3_2", Integer.class},
                {"c4_1", SupportBean[].class}
        };
        SupportEventTypeAssertionUtil.assertEventTypeProperties(expectedType, stmtSelect.getEventType(), SupportEventTypeAssertionEnum.NAME, SupportEventTypeAssertionEnum.TYPE);

        makeSendSupportBean(epService, null, -1);
        EventBean event = listener.assertOneGetNewAndReset();
        EPAssertionUtil.assertProps(event, "c0_1,c0_2,c1_1,c1_2".split(","), new Object[]{"x", "x", 41, 41});
        EPAssertionUtil.assertProps(event, "c2_1,c2_2".split(","), new Object[]{sentSB, sentSB});
        EPAssertionUtil.assertProps(event, "c2_3,c2_4".split(","), new Object[]{sentSB[0], sentSB[1]});
        EPAssertionUtil.assertProps(event, "c2_5,c2_6".split(","), new Object[]{sentSB[0], sentSB[0]});
        EPAssertionUtil.assertProps(event, "c3_1,c3_2".split(","), new Object[]{2, 2});
        EPAssertionUtil.assertProps(event, "c4_1".split(","), new Object[]{sentSB});

        // unnamed column
        String eplSelectUnnamed = "select col2.sorted().firstOf(), mt.col2.sorted().firstOf()" +
                " from SupportBean unidirectional, MyTable mt";
        EPStatement stmtSelectUnnamed = epService.getEPAdministrator().createEPL(eplSelectUnnamed);
        Object[][] expectedTypeUnnamed = new Object[][]{{"col2.sorted().firstOf()", SupportBean.class},
            {"mt.col2.sorted().firstOf()", SupportBean.class}};
        SupportEventTypeAssertionUtil.assertEventTypeProperties(expectedTypeUnnamed, stmtSelectUnnamed.getEventType(), SupportEventTypeAssertionEnum.NAME, SupportEventTypeAssertionEnum.TYPE);

        // invalid: ambiguous resolution
        SupportMessageAssertUtil.tryInvalid(epService, "" +
                        "select col0 from SupportBean, MyTable, MyTable",
                "Error starting statement: Failed to validate select-clause expression 'col0': Ambiguous table column 'col0' should be prefixed by a stream name [");

        epService.getEPAdministrator().destroyAllStatements();
    }

    private SupportBean makeSendSupportBean(EPServiceProvider epService, String theString, int intPrimitive) {
        SupportBean b = new SupportBean(theString, intPrimitive);
        epService.getEPRuntime().sendEvent(b);
        return b;
    }
}
