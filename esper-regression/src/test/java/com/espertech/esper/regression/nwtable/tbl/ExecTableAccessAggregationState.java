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
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBean_S0;
import com.espertech.esper.supportregression.bean.SupportBean_S1;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.supportregression.util.SupportModelHelper;
import com.espertech.esper.support.SupportEventTypeAssertionEnum;
import com.espertech.esper.support.SupportEventTypeAssertionUtil;

import static org.junit.Assert.assertEquals;

public class ExecTableAccessAggregationState implements RegressionExecution {

    public void run(EPServiceProvider epService) throws Exception {
        for (Class clazz : new Class[]{SupportBean.class, SupportBean_S0.class, SupportBean_S1.class}) {
            epService.getEPAdministrator().getConfiguration().addEventType(clazz);
        }

        runAssertionNestedMultivalueAccess(epService, false, false);
        runAssertionNestedMultivalueAccess(epService, true, false);
        runAssertionNestedMultivalueAccess(epService, false, true);
        runAssertionNestedMultivalueAccess(epService, true, true);

        runAssertionAccessAggShare(epService);
    }

    private void runAssertionNestedMultivalueAccess(EPServiceProvider epService, boolean grouped, boolean soda) {

        String eplDeclare = "create table varagg (" +
                (grouped ? "key string primary key, " : "") + "windowSupportBean window(*) @type('SupportBean'))";
        SupportModelHelper.createByCompileOrParse(epService, soda, eplDeclare);

        String eplInto = "into table varagg " +
                "select window(*) as windowSupportBean from SupportBean#length(2)" +
                (grouped ? " group by theString" : "");
        SupportModelHelper.createByCompileOrParse(epService, soda, eplInto);

        String key = grouped ? "[\"E1\"]" : "";
        String eplSelect = "select " +
                "varagg" + key + ".windowSupportBean.last(*) as c0, " +
                "varagg" + key + ".windowSupportBean.window(*) as c1, " +
                "varagg" + key + ".windowSupportBean.first(*) as c2, " +
                "varagg" + key + ".windowSupportBean.last(intPrimitive) as c3, " +
                "varagg" + key + ".windowSupportBean.window(intPrimitive) as c4, " +
                "varagg" + key + ".windowSupportBean.first(intPrimitive) as c5" +
                " from SupportBean_S0";
        EPStatement stmtSelect = SupportModelHelper.createByCompileOrParse(epService, soda, eplSelect);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmtSelect.addListener(listener);
        Object[][] expectedAggType = new Object[][]{
                {"c0", SupportBean.class}, {"c1", SupportBean[].class}, {"c2", SupportBean.class},
                {"c3", Integer.class}, {"c4", Integer[].class}, {"c5", Integer.class}};
        SupportEventTypeAssertionUtil.assertEventTypeProperties(expectedAggType, stmtSelect.getEventType(), SupportEventTypeAssertionEnum.NAME, SupportEventTypeAssertionEnum.TYPE);

        String[] fields = "c0,c1,c2,c3,c4,c5".split(",");
        SupportBean b1 = makeSendBean(epService, "E1", 10);
        epService.getEPRuntime().sendEvent(new SupportBean_S0(0));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields,
                new Object[]{b1, new Object[]{b1}, b1, 10, new int[]{10}, 10});

        SupportBean b2 = makeSendBean(epService, "E1", 20);
        epService.getEPRuntime().sendEvent(new SupportBean_S0(0));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields,
                new Object[]{b2, new Object[]{b1, b2}, b1, 20, new int[]{10, 20}, 10});

        SupportBean b3 = makeSendBean(epService, "E1", 30);
        epService.getEPRuntime().sendEvent(new SupportBean_S0(0));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields,
                new Object[]{b3, new Object[]{b2, b3}, b2, 30, new int[]{20, 30}, 20});

        epService.getEPAdministrator().destroyAllStatements();
        epService.getEPAdministrator().getConfiguration().removeEventType("table_varagg__internal", false);
        epService.getEPAdministrator().getConfiguration().removeEventType("table_varagg__public", false);
    }

    private void runAssertionAccessAggShare(EPServiceProvider epService) {
        epService.getEPAdministrator().createEPL("create table varagg (" +
                "mywin window(*) @type(SupportBean))");

        EPStatement stmtAgg = epService.getEPAdministrator().createEPL("into table varagg " +
                "select window(sb.*) as mywin from SupportBean#time(10 sec) as sb");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmtAgg.addListener(listener);
        assertEquals(SupportBean[].class, stmtAgg.getEventType().getPropertyType("mywin"));

        EPStatement stmtGet = epService.getEPAdministrator().createEPL("select varagg.mywin as c0 from SupportBean_S0");
        stmtGet.addListener(listener);
        assertEquals(SupportBean[].class, stmtGet.getEventType().getPropertyType("c0"));

        SupportBean b1 = makeSendBean(epService, "E1", 10);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "mywin".split(","), new Object[]{new SupportBean[]{b1}});

        epService.getEPRuntime().sendEvent(new SupportBean_S0(1));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "c0".split(","), new Object[]{new Object[]{b1}});

        SupportBean b2 = makeSendBean(epService, "E2", 20);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "mywin".split(","), new Object[]{new SupportBean[]{b1, b2}});

        epService.getEPRuntime().sendEvent(new SupportBean_S0(2));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "c0".split(","), new Object[]{new Object[]{b1, b2}});
    }

    private SupportBean makeSendBean(EPServiceProvider epService, String theString, int intPrimitive) {
        SupportBean bean = new SupportBean(theString, intPrimitive);
        epService.getEPRuntime().sendEvent(bean);
        return bean;
    }
}
