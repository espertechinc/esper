/*
 * *************************************************************************************
 *  Copyright (C) 2006-2015 EsperTech, Inc. All rights reserved.                       *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 * *************************************************************************************
 */

package com.espertech.esper.regression.nwtable;

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.util.DateTime;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBean_S0;
import com.espertech.esper.supportregression.bean.SupportBean_S1;
import com.espertech.esper.supportregression.client.SupportConfigFactory;
import com.espertech.esper.supportregression.event.EventTypeAssertionEnum;
import com.espertech.esper.supportregression.event.EventTypeAssertionUtil;
import com.espertech.esper.supportregression.util.SupportModelHelper;
import junit.framework.TestCase;

import java.util.Collection;
import java.util.Collections;

public class TestTableAccessDotMethod extends TestCase {

    private EPServiceProvider epService;
    private SupportUpdateListener listener;

    public void setUp() {
        epService = EPServiceProviderManager.getDefaultProvider(SupportConfigFactory.getConfiguration());
        epService.initialize();
        for (Class clazz : new Class[] {SupportBean.class, SupportBean_S0.class, SupportBean_S1.class}) {
            epService.getEPAdministrator().getConfiguration().addEventType(clazz);
        }
        listener = new SupportUpdateListener();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
    }

    public void tearDown() {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
        listener = null;
    }

    public void testAggDatetimeAndEnumerationAndMethod() {
        runAggregationWDatetimeEtc(false, false);
        runAggregationWDatetimeEtc(true, false);
        runAggregationWDatetimeEtc(false, true);
        runAggregationWDatetimeEtc(true, true);
    }

    public void testPlainPropDatetimeAndEnumerationAndMethod() {
        runPlainPropertyWDatetimeEtc(false, false);
        runPlainPropertyWDatetimeEtc(true, false);
        runPlainPropertyWDatetimeEtc(false, true);
        runPlainPropertyWDatetimeEtc(true, true);
    }

    private void runPlainPropertyWDatetimeEtc(boolean grouped, boolean soda) {

        String myBean = MyBean.class.getName();
        SupportModelHelper.createByCompileOrParse(epService, soda, "create objectarray schema MyEvent as (p0 string)");
        SupportModelHelper.createByCompileOrParse(epService, soda, "create objectarray schema PopulateEvent as (" +
                "key string, ts long" +
                ", mb " + myBean +
                ", mbarr " + myBean + "[]" +
                ", me MyEvent, mearr MyEvent[])");

        String eplDeclare = "create table varagg (key string" + (grouped ? " primary key" : "") +
                ", ts long" +
                ", mb " + myBean +
                ", mbarr " + myBean + "[]" +
                ", me MyEvent, mearr MyEvent[])";
        SupportModelHelper.createByCompileOrParse(epService, soda, eplDeclare);

        String key = grouped ? "[\"E1\"]" : "";
        String eplSelect = "select " +
                "varagg" + key + ".ts.getMinuteOfHour() as c0, " +
                "varagg" + key + ".mb.getMyProperty() as c1, " +
                "varagg" + key + ".mbarr.takeLast(1) as c2, " +
                "varagg" + key + ".me.p0 as c3, " +
                "varagg" + key + ".mearr.selectFrom(i => i.p0) as c4 " +
                "from SupportBean_S0";
        SupportModelHelper.createByCompileOrParse(epService, soda, eplSelect).addListener(listener);

        String eplMerge = "on PopulateEvent merge varagg " +
                "when not matched then insert " +
                "select key, ts, mb, mbarr, me, mearr";
        SupportModelHelper.createByCompileOrParse(epService, soda, eplMerge);

        Object[] event = makePopulateEvent();
        epService.getEPRuntime().sendEvent(event, "PopulateEvent");
        epService.getEPRuntime().sendEvent(new SupportBean_S0(0, "E1"));
        EventBean output = listener.assertOneGetNewAndReset();
        EPAssertionUtil.assertProps(output, "c0,c1,c3".split(","),
                new Object[] {55, "x", "p0value"});
        assertEquals(1, ((Collection) output.get("c2")).size());
        assertEquals("[0_p0, 1_p0]", output.get("c4").toString());

        epService.getEPAdministrator().destroyAllStatements();
    }

    private Object[] makePopulateEvent() {
        return new Object[] {
                "E1",
                DateTime.parseDefaultMSec("2002-05-30T09:55:00.000"), // ts
                new MyBean(),   // mb
                new MyBean[] {new MyBean(), new MyBean()},   // mbarr
                new Object[] {"p0value"},   // me
                new Object[][] {{"0_p0"}, {"1_p0"}}    // mearr
        };
    }

    private void runAggregationWDatetimeEtc(boolean grouped, boolean soda) {

        String eplDeclare = "create table varagg (" + (grouped ? "key string primary key, " : "") +
                "a1 lastever(long), a2 window(*) @type('SupportBean'))";
        SupportModelHelper.createByCompileOrParse(epService, soda, eplDeclare);

        String eplInto = "into table varagg " +
                "select lastever(longPrimitive) as a1, window(*) as a2 from SupportBean#time(10 seconds)" +
                (grouped ? " group by theString" : "");
        EPStatement stmtInto = SupportModelHelper.createByCompileOrParse(epService, soda, eplInto);
        Object[][] expectedAggType = new Object[][]{{"a1", long.class}, {"a2", SupportBean[].class}};
        EventTypeAssertionUtil.assertEventTypeProperties(expectedAggType, stmtInto.getEventType(), EventTypeAssertionEnum.NAME, EventTypeAssertionEnum.TYPE);

        String key = grouped ? "[\"E1\"]" : "";
        String eplGet = "select varagg" + key + ".a1.after(150L) as c0, " +
                "varagg" + key + ".a2.countOf() as c1 from SupportBean_S0";
        EPStatement stmtGet = SupportModelHelper.createByCompileOrParse(epService, soda, eplGet);
        stmtGet.addListener(listener);
        Object[][] expectedGetType = new Object[][]{{"c0", Boolean.class}, {"c1", Integer.class}};
        EventTypeAssertionUtil.assertEventTypeProperties(expectedGetType, stmtGet.getEventType(), EventTypeAssertionEnum.NAME, EventTypeAssertionEnum.TYPE);

        String[] fields = "c0,c1".split(",");
        makeSendBean("E1", 10, 100);
        epService.getEPRuntime().sendEvent(new SupportBean_S0(0));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {false, 1});

        makeSendBean("E1", 20, 200);
        epService.getEPRuntime().sendEvent(new SupportBean_S0(0));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {true, 2});

        epService.getEPAdministrator().destroyAllStatements();
        epService.getEPAdministrator().getConfiguration().removeEventType("table_varagg__internal", false);
        epService.getEPAdministrator().getConfiguration().removeEventType("table_varagg__public", false);
    }

    public void testNestedDotMethod() {
        runAssertionNestedDotMethod(true, false);
        runAssertionNestedDotMethod(false, false);
        runAssertionNestedDotMethod(true, true);
        runAssertionNestedDotMethod(false, true);
    }

    private void runAssertionNestedDotMethod(boolean grouped, boolean soda) {

        String eplDeclare = "create table varagg (" +
                (grouped ? "key string primary key, " : "") +
                "windowSupportBean window(*) @type('SupportBean'))";
        SupportModelHelper.createByCompileOrParse(epService, soda, eplDeclare);

        String eplInto = "into table varagg " +
                "select window(*) as windowSupportBean from SupportBean#length(2)" +
                (grouped ? " group by theString" : "");
        SupportModelHelper.createByCompileOrParse(epService, soda, eplInto);

        String key = grouped ? "[\"E1\"]" : "";
        String eplSelect = "select " +
                "varagg" + key + ".windowSupportBean.last(*).intPrimitive as c0, " +
                "varagg" + key + ".windowSupportBean.window(*).countOf() as c1, " +
                "varagg" + key + ".windowSupportBean.window(intPrimitive).take(1) as c2" +
                " from SupportBean_S0";
        EPStatement stmtSelect = SupportModelHelper.createByCompileOrParse(epService, soda, eplSelect);
        stmtSelect.addListener(listener);
        Object[][] expectedAggType = new Object[][]{{"c0", Integer.class}, {"c1", Integer.class}, {"c2", Collection.class}};
        EventTypeAssertionUtil.assertEventTypeProperties(expectedAggType, stmtSelect.getEventType(), EventTypeAssertionEnum.NAME, EventTypeAssertionEnum.TYPE);

        String[] fields = "c0,c1,c2".split(",");
        makeSendBean("E1", 10, 0);
        epService.getEPRuntime().sendEvent(new SupportBean_S0(0));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {10, 1, Collections.singletonList(10)});

        makeSendBean("E1", 20, 0);
        epService.getEPRuntime().sendEvent(new SupportBean_S0(0));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {20, 2, Collections.singletonList(10)});

        makeSendBean("E1", 30, 0);
        epService.getEPRuntime().sendEvent(new SupportBean_S0(0));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {30, 2, Collections.singletonList(20)});

        epService.getEPAdministrator().destroyAllStatements();
        epService.getEPAdministrator().getConfiguration().removeEventType("table_varagg__internal", false);
        epService.getEPAdministrator().getConfiguration().removeEventType("table_varagg__public", false);
    }

    private SupportBean makeSendBean(String theString, int intPrimitive, long longPrimitive) {
        SupportBean bean = new SupportBean(theString, intPrimitive);
        bean.setLongPrimitive(longPrimitive);
        epService.getEPRuntime().sendEvent(bean);
        return bean;
    }

    private static class MyBean {
        public String getMyProperty() {
            return "x";
        }
    }
}
