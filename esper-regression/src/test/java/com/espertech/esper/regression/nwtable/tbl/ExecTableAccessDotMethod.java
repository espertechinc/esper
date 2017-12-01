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
import com.espertech.esper.client.util.DateTime;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBean_S0;
import com.espertech.esper.supportregression.bean.SupportBean_S1;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.supportregression.util.SupportModelHelper;
import com.espertech.esper.support.SupportEventTypeAssertionEnum;
import com.espertech.esper.support.SupportEventTypeAssertionUtil;

import java.util.Collection;
import java.util.Collections;

import static org.junit.Assert.assertEquals;

public class ExecTableAccessDotMethod implements RegressionExecution {

    public void run(EPServiceProvider epService) throws Exception {
        for (Class clazz : new Class[]{SupportBean.class, SupportBean_S0.class, SupportBean_S1.class}) {
            epService.getEPAdministrator().getConfiguration().addEventType(clazz);
        }

        runAssertionPlainPropDatetimeAndEnumerationAndMethod(epService);
        runAssertionAggDatetimeAndEnumerationAndMethod(epService);
        runAssertionNestedDotMethod(epService);
    }

    private void runAssertionAggDatetimeAndEnumerationAndMethod(EPServiceProvider epService) {
        runAggregationWDatetimeEtc(epService, false, false);
        runAggregationWDatetimeEtc(epService, true, false);
        runAggregationWDatetimeEtc(epService, false, true);
        runAggregationWDatetimeEtc(epService, true, true);
    }

    private void runAssertionPlainPropDatetimeAndEnumerationAndMethod(EPServiceProvider epService) {
        runPlainPropertyWDatetimeEtc(epService, false, false);
        runPlainPropertyWDatetimeEtc(epService, true, false);
        runPlainPropertyWDatetimeEtc(epService, false, true);
        runPlainPropertyWDatetimeEtc(epService, true, true);
    }

    private void runPlainPropertyWDatetimeEtc(EPServiceProvider epService, boolean grouped, boolean soda) {

        String myBean = MyBean.class.getName();
        SupportModelHelper.createByCompileOrParse(epService, soda, "create objectarray schema MyEvent as (p0 string)");
        SupportModelHelper.createByCompileOrParse(epService, soda, "create objectarray schema PopulateEvent as (" +
                "key string, ts long" +
                ", mb " + myBean +
                ", mbarr " + myBean + "[]" +
                ", me MyEvent, mearr MyEvent[])");

        String eplDeclare = "create table varaggPWD (key string" + (grouped ? " primary key" : "") +
                ", ts long" +
                ", mb " + myBean +
                ", mbarr " + myBean + "[]" +
                ", me MyEvent, mearr MyEvent[])";
        SupportModelHelper.createByCompileOrParse(epService, soda, eplDeclare);

        String key = grouped ? "[\"E1\"]" : "";
        String eplSelect = "select " +
                "varaggPWD" + key + ".ts.getMinuteOfHour() as c0, " +
                "varaggPWD" + key + ".mb.getMyProperty() as c1, " +
                "varaggPWD" + key + ".mbarr.takeLast(1) as c2, " +
                "varaggPWD" + key + ".me.p0 as c3, " +
                "varaggPWD" + key + ".mearr.selectFrom(i => i.p0) as c4 " +
                "from SupportBean_S0";
        SupportUpdateListener listener = new SupportUpdateListener();
        SupportModelHelper.createByCompileOrParse(epService, soda, eplSelect).addListener(listener);

        String eplMerge = "on PopulateEvent merge varaggPWD " +
                "when not matched then insert " +
                "select key, ts, mb, mbarr, me, mearr";
        SupportModelHelper.createByCompileOrParse(epService, soda, eplMerge);

        Object[] event = makePopulateEvent();
        epService.getEPRuntime().sendEvent(event, "PopulateEvent");
        epService.getEPRuntime().sendEvent(new SupportBean_S0(0, "E1"));
        EventBean output = listener.assertOneGetNewAndReset();
        EPAssertionUtil.assertProps(output, "c0,c1,c3".split(","),
                new Object[]{55, "x", "p0value"});
        assertEquals(1, ((Collection) output.get("c2")).size());
        assertEquals("[0_p0, 1_p0]", output.get("c4").toString());

        epService.getEPAdministrator().destroyAllStatements();
    }

    private Object[] makePopulateEvent() {
        return new Object[]{
            "E1",
            DateTime.parseDefaultMSec("2002-05-30T09:55:00.000"), // ts
            new MyBean(),   // mb
            new MyBean[]{new MyBean(), new MyBean()},   // mbarr
            new Object[]{"p0value"},   // me
            new Object[][]{{"0_p0"}, {"1_p0"}}    // mearr
        };
    }

    private void runAggregationWDatetimeEtc(EPServiceProvider epService, boolean grouped, boolean soda) {

        String eplDeclare = "create table varaggWDE (" + (grouped ? "key string primary key, " : "") +
                "a1 lastever(long), a2 window(*) @type('SupportBean'))";
        SupportModelHelper.createByCompileOrParse(epService, soda, eplDeclare);

        String eplInto = "into table varaggWDE " +
                "select lastever(longPrimitive) as a1, window(*) as a2 from SupportBean#time(10 seconds)" +
                (grouped ? " group by theString" : "");
        EPStatement stmtInto = SupportModelHelper.createByCompileOrParse(epService, soda, eplInto);
        Object[][] expectedAggType = new Object[][]{{"a1", Long.class}, {"a2", SupportBean[].class}};
        SupportEventTypeAssertionUtil.assertEventTypeProperties(expectedAggType, stmtInto.getEventType(), SupportEventTypeAssertionEnum.NAME, SupportEventTypeAssertionEnum.TYPE);

        String key = grouped ? "[\"E1\"]" : "";
        String eplGet = "select varaggWDE" + key + ".a1.after(150L) as c0, " +
                "varaggWDE" + key + ".a2.countOf() as c1 from SupportBean_S0";
        EPStatement stmtGet = SupportModelHelper.createByCompileOrParse(epService, soda, eplGet);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmtGet.addListener(listener);
        Object[][] expectedGetType = new Object[][]{{"c0", Boolean.class}, {"c1", Integer.class}};
        SupportEventTypeAssertionUtil.assertEventTypeProperties(expectedGetType, stmtGet.getEventType(), SupportEventTypeAssertionEnum.NAME, SupportEventTypeAssertionEnum.TYPE);

        String[] fields = "c0,c1".split(",");
        makeSendBean(epService, "E1", 10, 100);
        epService.getEPRuntime().sendEvent(new SupportBean_S0(0));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{false, 1});

        makeSendBean(epService, "E1", 20, 200);
        epService.getEPRuntime().sendEvent(new SupportBean_S0(0));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{true, 2});

        epService.getEPAdministrator().destroyAllStatements();
        epService.getEPAdministrator().getConfiguration().removeEventType("table_varaggWDE__internal", false);
        epService.getEPAdministrator().getConfiguration().removeEventType("table_varaggWDE__public", false);
    }

    private void runAssertionNestedDotMethod(EPServiceProvider epService) {
        tryAssertionNestedDotMethod(epService, true, false);
        tryAssertionNestedDotMethod(epService, false, false);
        tryAssertionNestedDotMethod(epService, true, true);
        tryAssertionNestedDotMethod(epService, false, true);
    }

    private void tryAssertionNestedDotMethod(EPServiceProvider epService, boolean grouped, boolean soda) {

        String eplDeclare = "create table varaggNDM (" +
                (grouped ? "key string primary key, " : "") +
                "windowSupportBean window(*) @type('SupportBean'))";
        SupportModelHelper.createByCompileOrParse(epService, soda, eplDeclare);

        String eplInto = "into table varaggNDM " +
                "select window(*) as windowSupportBean from SupportBean#length(2)" +
                (grouped ? " group by theString" : "");
        SupportModelHelper.createByCompileOrParse(epService, soda, eplInto);

        String key = grouped ? "[\"E1\"]" : "";
        String eplSelect = "select " +
                "varaggNDM" + key + ".windowSupportBean.last(*).intPrimitive as c0, " +
                "varaggNDM" + key + ".windowSupportBean.window(*).countOf() as c1, " +
                "varaggNDM" + key + ".windowSupportBean.window(intPrimitive).take(1) as c2" +
                " from SupportBean_S0";
        EPStatement stmtSelect = SupportModelHelper.createByCompileOrParse(epService, soda, eplSelect);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmtSelect.addListener(listener);
        Object[][] expectedAggType = new Object[][]{{"c0", Integer.class}, {"c1", Integer.class}, {"c2", Collection.class}};
        SupportEventTypeAssertionUtil.assertEventTypeProperties(expectedAggType, stmtSelect.getEventType(), SupportEventTypeAssertionEnum.NAME, SupportEventTypeAssertionEnum.TYPE);

        String[] fields = "c0,c1,c2".split(",");
        makeSendBean(epService, "E1", 10, 0);
        epService.getEPRuntime().sendEvent(new SupportBean_S0(0));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{10, 1, Collections.singletonList(10)});

        makeSendBean(epService, "E1", 20, 0);
        epService.getEPRuntime().sendEvent(new SupportBean_S0(0));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{20, 2, Collections.singletonList(10)});

        makeSendBean(epService, "E1", 30, 0);
        epService.getEPRuntime().sendEvent(new SupportBean_S0(0));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{30, 2, Collections.singletonList(20)});

        epService.getEPAdministrator().destroyAllStatements();
        epService.getEPAdministrator().getConfiguration().removeEventType("table_varaggNDM__internal", false);
        epService.getEPAdministrator().getConfiguration().removeEventType("table_varaggNDM__public", false);
    }

    private void makeSendBean(EPServiceProvider epService, String theString, int intPrimitive, long longPrimitive) {
        SupportBean bean = new SupportBean(theString, intPrimitive);
        bean.setLongPrimitive(longPrimitive);
        epService.getEPRuntime().sendEvent(bean);
    }

    public static class MyBean {
        public String getMyProperty() {
            return "x";
        }
    }
}
