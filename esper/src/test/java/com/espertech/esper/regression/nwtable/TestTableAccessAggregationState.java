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
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.support.bean.SupportBean;
import com.espertech.esper.support.bean.SupportBean_S0;
import com.espertech.esper.support.bean.SupportBean_S1;
import com.espertech.esper.support.client.SupportConfigFactory;
import com.espertech.esper.support.event.EventTypeAssertionEnum;
import com.espertech.esper.support.event.EventTypeAssertionUtil;
import com.espertech.esper.support.util.SupportModelHelper;
import junit.framework.TestCase;

public class TestTableAccessAggregationState extends TestCase {
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

    public void testNestedMultivalueAccess() {
        runAssertionNestedMultivalueAccess(false, false);
        runAssertionNestedMultivalueAccess(true, false);
        runAssertionNestedMultivalueAccess(false, true);
        runAssertionNestedMultivalueAccess(true, true);
    }

    private void runAssertionNestedMultivalueAccess(boolean grouped, boolean soda) {

        String eplDeclare = "create table varagg (" +
                (grouped ? "key string primary key, " : "") + "windowSupportBean window(*) @type('SupportBean'))";
        SupportModelHelper.createByCompileOrParse(epService, soda, eplDeclare);

        String eplInto = "into table varagg " +
                "select window(*) as windowSupportBean from SupportBean.win:length(2)" +
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
        stmtSelect.addListener(listener);
        Object[][] expectedAggType = new Object[][]{
                {"c0", SupportBean.class}, {"c1", SupportBean[].class}, {"c2", SupportBean.class},
                {"c3", int.class}, {"c4", int[].class}, {"c5", int.class}};
        EventTypeAssertionUtil.assertEventTypeProperties(expectedAggType, stmtSelect.getEventType(), EventTypeAssertionEnum.NAME, EventTypeAssertionEnum.TYPE);

        String[] fields = "c0,c1,c2,c3,c4,c5".split(",");
        SupportBean b1 = makeSendBean("E1", 10);
        epService.getEPRuntime().sendEvent(new SupportBean_S0(0));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields,
                new Object[] {b1, new Object[] {b1}, b1, 10, new int[] {10}, 10});

        SupportBean b2 = makeSendBean("E1", 20);
        epService.getEPRuntime().sendEvent(new SupportBean_S0(0));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields,
                new Object[] {b2, new Object[] {b1, b2}, b1, 20, new int[] {10, 20}, 10});

        SupportBean b3 = makeSendBean("E1", 30);
        epService.getEPRuntime().sendEvent(new SupportBean_S0(0));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields,
                new Object[] {b3, new Object[] {b2, b3}, b2, 30, new int[] {20, 30}, 20});

        epService.getEPAdministrator().destroyAllStatements();
        epService.getEPAdministrator().getConfiguration().removeEventType("table_varagg__internal", false);
        epService.getEPAdministrator().getConfiguration().removeEventType("table_varagg__public", false);
    }

    public void testAccessAggShare() {
        epService.getEPAdministrator().createEPL("create table varagg (" +
                "mywin window(*) @type(SupportBean))");

        EPStatement stmtAgg = epService.getEPAdministrator().createEPL("into table varagg " +
                "select window(sb.*) as mywin from SupportBean.win:time(10 sec) as sb");
        stmtAgg.addListener(listener);
        assertEquals(SupportBean[].class, stmtAgg.getEventType().getPropertyType("mywin"));

        EPStatement stmtGet = epService.getEPAdministrator().createEPL("select varagg.mywin as c0 from SupportBean_S0");
        stmtGet.addListener(listener);
        assertEquals(SupportBean[].class, stmtGet.getEventType().getPropertyType("c0"));

        SupportBean b1 = makeSendBean("E1", 10);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "mywin".split(","), new Object[]{new SupportBean[] {b1}});

        epService.getEPRuntime().sendEvent(new SupportBean_S0(1));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "c0".split(","), new Object[]{new Object[]{b1}});

        SupportBean b2 = makeSendBean("E2", 20);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "mywin".split(","), new Object[]{new SupportBean[] {b1, b2}});

        epService.getEPRuntime().sendEvent(new SupportBean_S0(2));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "c0".split(","), new Object[] {new Object[] {b1, b2}});
    }

    private SupportBean makeSendBean(String theString, int intPrimitive) {
        SupportBean bean = new SupportBean(theString, intPrimitive);
        epService.getEPRuntime().sendEvent(bean);
        return bean;
    }
}
