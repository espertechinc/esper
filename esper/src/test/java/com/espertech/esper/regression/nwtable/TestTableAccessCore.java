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

import com.espertech.esper.client.*;
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

import java.util.Collection;
import java.util.Map;

public class TestTableAccessCore extends TestCase {
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

    public void testIntegerIndexedPropertyLookAlike() {
        runAssertionIntegerIndexedPropertyLookAlike(false);
        runAssertionIntegerIndexedPropertyLookAlike(true);
    }

    private void runAssertionIntegerIndexedPropertyLookAlike(boolean soda) {
        String eplDeclare = "create table varagg (key int primary key, myevents window(*) @type('SupportBean'))";
        SupportModelHelper.createByCompileOrParse(epService, soda, eplDeclare);

        String eplInto = "into table varagg select window(*) as myevents from SupportBean.win:length(3) group by intPrimitive";
        SupportModelHelper.createByCompileOrParse(epService, soda, eplInto);

        String eplSelect = "select varagg[1] as c0, varagg[1].myevents as c1, varagg[1].myevents.last(*) as c2 from SupportBean_S0";
        EPStatement stmt = SupportModelHelper.createByCompileOrParse(epService, soda, eplSelect);
        stmt.addListener(listener);

        SupportBean e1 = makeSendBean("E1", 1, 10L);
        SupportBean e2 = makeSendBean("E2", 1, 20L);

        epService.getEPRuntime().sendEvent(new SupportBean_S0(0));
        assertIntegerIndexed(listener.assertOneGetNewAndReset(), new SupportBean[] {e1, e2});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void assertIntegerIndexed(EventBean event, SupportBean[] events) {
        EPAssertionUtil.assertEqualsExactOrder(events, (Object[]) event.get("c0.myevents"));
        EPAssertionUtil.assertEqualsExactOrder(events, (Object[]) event.get("c1"));
        assertSame(events[events.length - 1], event.get("c2"));
    }

    public void testFilterBehavior() {
        epService.getEPAdministrator().createEPL("create table varagg (total count(*))");
        epService.getEPAdministrator().createEPL("into table varagg select count(*) as total from SupportBean_S0");
        epService.getEPAdministrator().createEPL("select * from SupportBean(varagg.total = intPrimitive)").addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean_S0(0));

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        assertTrue(listener.getAndClearIsInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean_S0(0));

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 2));
        assertTrue(listener.getAndClearIsInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 3));
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        assertFalse(listener.getAndClearIsInvoked());
    }

    public void testExprSelectClauseRenderingUnnamedCol() {
        epService.getEPAdministrator().createEPL("create table varagg (" +
                "key string primary key, theEvents window(*) @type(SupportBean))");

        EPStatement stmtSelect = epService.getEPAdministrator().createEPL("select " +
                "varagg.keys()," +
                "varagg[p00].theEvents," +
                "varagg[p00]," +
                "varagg[p00].theEvents.last(*)," +
                "varagg[p00].theEvents.window(*).take(1) from SupportBean_S0");

        Object[][] expectedAggType = new Object[][]{
                {"varagg.keys()", Object[].class},
                {"varagg[p00].theEvents", SupportBean[].class},
                {"varagg[p00]", Map.class},
                {"varagg[p00].theEvents.last(*)", SupportBean.class},
                {"varagg[p00].theEvents.window(*).take(1)", Collection.class},
        };
        EventTypeAssertionUtil.assertEventTypeProperties(expectedAggType, stmtSelect.getEventType(), EventTypeAssertionEnum.NAME, EventTypeAssertionEnum.TYPE);
    }

    public void testTopLevelReadGrouped2Keys() {
        runAssertionTopLevelReadGrouped2Keys(false);
        runAssertionTopLevelReadGrouped2Keys(true);
    }

    private void runAssertionTopLevelReadGrouped2Keys(boolean soda) {
        SupportModelHelper.createByCompileOrParse(epService, soda, "create objectarray schema MyEvent as (c0 int, c1 string, c2 int)");
        SupportModelHelper.createByCompileOrParse(epService, soda, "create table windowAndTotal (" +
                "keyi int primary key, keys string primary key, thewindow window(*) @type('MyEvent'), thetotal sum(int))");
        SupportModelHelper.createByCompileOrParse(epService, soda, "into table windowAndTotal " +
                "select window(*) as thewindow, sum(c2) as thetotal from MyEvent.win:length(2) group by c0, c1");

        EPStatement stmtSelect = SupportModelHelper.createByCompileOrParse(epService, soda, "select windowAndTotal[id,p00] as val0 from SupportBean_S0");
        stmtSelect.addListener(listener);
        assertTopLevelTypeInfo(stmtSelect);

        Object[] e1 = new Object[] {10, "G1", 100};
        epService.getEPRuntime().sendEvent(e1, "MyEvent");

        String[] fieldsInner = "thewindow,thetotal".split(",");
        epService.getEPRuntime().sendEvent(new SupportBean_S0(10, "G1"));
        EPAssertionUtil.assertPropsMap((Map) listener.assertOneGetNewAndReset().get("val0"), fieldsInner, new Object[][] {e1}, 100);

        Object[] e2 = new Object[] {20, "G2", 200};
        epService.getEPRuntime().sendEvent(e2, "MyEvent");

        epService.getEPRuntime().sendEvent(new SupportBean_S0(20, "G2"));
        EPAssertionUtil.assertPropsMap((Map) listener.assertOneGetNewAndReset().get("val0"), fieldsInner, new Object[][]{e2}, 200);

        Object[] e3 = new Object[] {20, "G2", 300};
        epService.getEPRuntime().sendEvent(e3, "MyEvent");

        epService.getEPRuntime().sendEvent(new SupportBean_S0(10, "G1"));
        EPAssertionUtil.assertPropsMap((Map) listener.assertOneGetNewAndReset().get("val0"), fieldsInner, null, null);
        epService.getEPRuntime().sendEvent(new SupportBean_S0(20, "G2"));
        EPAssertionUtil.assertPropsMap((Map) listener.assertOneGetNewAndReset().get("val0"), fieldsInner, new Object[][] {e2, e3}, 500);

        // test typable output
        stmtSelect.destroy();
        EPStatement stmtConvert = epService.getEPAdministrator().createEPL("insert into AggBean select windowAndTotal[20, 'G2'] as val0 from SupportBean_S0");
        stmtConvert.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean_S0(0));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "val0.thewindow,val0.thetotal".split(","), new Object[]{new Object[][]{e2, e3}, 500});

        epService.getEPAdministrator().destroyAllStatements();
    }

    public void testTopLevelReadUnGrouped() {
        epService.getEPAdministrator().getConfiguration().addEventType(AggBean.class);
        epService.getEPAdministrator().createEPL("create objectarray schema MyEvent(c0 int)");
        epService.getEPAdministrator().createEPL("create table windowAndTotal (" +
                "thewindow window(*) @type(MyEvent), thetotal sum(int))");
        epService.getEPAdministrator().createEPL("into table windowAndTotal " +
                "select window(*) as thewindow, sum(c0) as thetotal from MyEvent.win:length(2)");

        EPStatement stmt = epService.getEPAdministrator().createEPL("select windowAndTotal as val0 from SupportBean_S0");
        stmt.addListener(listener);

        Object[] e1 = new Object[] {10};
        epService.getEPRuntime().sendEvent(e1, "MyEvent");

        String[] fieldsInner = "thewindow,thetotal".split(",");
        epService.getEPRuntime().sendEvent(new SupportBean_S0(0));
        EPAssertionUtil.assertPropsMap((Map) listener.assertOneGetNewAndReset().get("val0"), fieldsInner, new Object[][] {e1}, 10);

        Object[] e2 = new Object[] {20};
        epService.getEPRuntime().sendEvent(e2, "MyEvent");

        epService.getEPRuntime().sendEvent(new SupportBean_S0(1));
        EPAssertionUtil.assertPropsMap((Map) listener.assertOneGetNewAndReset().get("val0"), fieldsInner, new Object[][] {e1, e2}, 30);

        Object[] e3 = new Object[] {30};
        epService.getEPRuntime().sendEvent(e3, "MyEvent");

        epService.getEPRuntime().sendEvent(new SupportBean_S0(2));
        EPAssertionUtil.assertPropsMap((Map) listener.assertOneGetNewAndReset().get("val0"), fieldsInner, new Object[][] {e2, e3}, 50);

        // test typable output
        stmt.destroy();
        EPStatement stmtConvert = epService.getEPAdministrator().createEPL("insert into AggBean select windowAndTotal as val0 from SupportBean_S0");
        stmtConvert.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean_S0(2));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "val0.thewindow,val0.thetotal".split(","), new Object[]{new Object[][]{e2, e3}, 50});
    }

    public void testExpressionAliasAndDecl() {
        epService.getEPAdministrator().createEPL("create expression sumi {a -> sum(intPrimitive)}");
        epService.getEPAdministrator().createEPL("create expression sumd alias for {sum(doublePrimitive)}");
        epService.getEPAdministrator().createEPL("create table varagg (" +
                "sumi sum(int), sumd sum(double), sumf sum(float), suml sum(long))");
        epService.getEPAdministrator().createEPL("expression suml alias for {sum(longPrimitive)} " +
                "into table varagg " +
                "select suml, sum(floatPrimitive) as sumf, sumd, sumi(sb) from SupportBean as sb");

        makeSendBean("E1", 10, 100L, 1000d, 10000f);

        String fields = "varagg.sumi,varagg.sumd,varagg.sumf,varagg.suml";
        epService.getEPAdministrator().createEPL("select " + fields + " from SupportBean_S0").addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean_S0(1));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields.split(","), new Object[] {10, 1000d, 10000f, 100L});

        makeSendBean("E1", 11, 101L, 1001d, 10001f);

        epService.getEPRuntime().sendEvent(new SupportBean_S0(1));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields.split(","), new Object[]{21, 2001d, 20001f, 201L});
    }

    public void testGroupedTwoKeyNoContext() throws Exception {
        String eplDeclare = "create table varTotal (key0 string primary key, key1 int primary key, total sum(long), cnt count(*))";
        epService.getEPAdministrator().createEPL(eplDeclare);

        String eplBind =
                "into table varTotal " +
                "select sum(longPrimitive) as total, count(*) as cnt " +
                "from SupportBean group by theString, intPrimitive";
        epService.getEPAdministrator().createEPL(eplBind);

        String eplUse = "select varTotal[p00, id].total as c0, varTotal[p00, id].cnt as c1 from SupportBean_S0";
        epService.getEPAdministrator().createEPL(eplUse).addListener(listener);

        makeSendBean("E1", 10, 100);

        String[] fields = "c0,c1".split(",");
        epService.getEPRuntime().sendEvent(new SupportBean_S0(10, "E1"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {100L, 1L});
        epService.getEPRuntime().sendEvent(new SupportBean_S0(0, "E1"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {null, null});
        epService.getEPRuntime().sendEvent(new SupportBean_S0(10, "E2"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {null, null});
    }

    public void testGroupedThreeKeyNoContext() throws Exception {
        String eplDeclare = "create table varTotal (key0 string primary key, key1 int primary key," +
                "key2 long primary key, total sum(double), cnt count(*))";
        epService.getEPAdministrator().createEPL(eplDeclare);

        String eplBind = "into table varTotal " +
                         "select sum(doublePrimitive) as total, count(*) as cnt " +
                         "from SupportBean group by theString, intPrimitive, longPrimitive";
        epService.getEPAdministrator().createEPL(eplBind);

        String[] fields = "c0,c1".split(",");
        String eplUse = "select varTotal[p00, id, 100L].total as c0, varTotal[p00, id, 100L].cnt as c1 from SupportBean_S0";
        epService.getEPAdministrator().createEPL(eplUse).addListener(listener);

        makeSendBean("E1", 10, 100, 1000);

        epService.getEPRuntime().sendEvent(new SupportBean_S0(10, "E1"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {1000.0, 1L});

        makeSendBean("E1", 10, 100, 1001);

        epService.getEPRuntime().sendEvent(new SupportBean_S0(10, "E1"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {2001.0, 2L});
    }

    public void testGroupedSingleKeyNoContext() throws Exception {
        runAssertionGroupedSingleKeyNoContext(false);
        runAssertionGroupedSingleKeyNoContext(true);
    }

    private void runAssertionGroupedSingleKeyNoContext(boolean soda) throws Exception {
        String eplDeclare = "create table varTotal (key string primary key, total sum(int))";
        SupportModelHelper.createByCompileOrParse(epService, soda, eplDeclare);

        String eplBind = "into table varTotal " +
                "select theString, sum(intPrimitive) as total from SupportBean group by theString";
        SupportModelHelper.createByCompileOrParse(epService, soda, eplBind);

        String eplUse = "select p00 as c0, varTotal[p00].total as c1 from SupportBean_S0";
        SupportModelHelper.createByCompileOrParse(epService, soda, eplUse).addListener(listener);

        runAssertionTopLevelSingle();

        epService.getEPAdministrator().destroyAllStatements();
    }

    public void testUngroupedWContext() throws Exception {
        String eplPart =
                "create context PartitionedByString partition by theString from SupportBean, p00 from SupportBean_S0;\n" +
                "context PartitionedByString create table varTotal (total sum(int));\n" +
                "context PartitionedByString into table varTotal select sum(intPrimitive) as total from SupportBean;\n" +
                "@Name('L') context PartitionedByString select p00 as c0, varTotal.total as c1 from SupportBean_S0;\n";
        epService.getEPAdministrator().getDeploymentAdmin().parseDeploy(eplPart);
        epService.getEPAdministrator().getStatement("L").addListener(listener);

        runAssertionTopLevelSingle();
    }

    public void testOrderOfAggregationsAndPush() {
        runAssertionOrderOfAggs(true);
        runAssertionOrderOfAggs(false);
    }

    public void testMultiStmtContributing() {
        runAssertionMultiStmtContributingDifferentAggs(false);
        runAssertionMultiStmtContributingDifferentAggs(true);

        // contribute to the same aggregation
        epService.getEPAdministrator().createEPL("create table sharedagg (total sum(int))");
        epService.getEPAdministrator().createEPL("into table sharedagg " +
                "select p00 as c0, sum(id) as total from SupportBean_S0").addListener(listener);
        epService.getEPAdministrator().createEPL("into table sharedagg " +
                "select p10 as c0, sum(id) as total from SupportBean_S1").addListener(listener);
        epService.getEPAdministrator().createEPL("select theString as c0, sharedagg.total as total from SupportBean").addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean_S0(10, "A"));
        assertMultiStmtContributingTotal("A", 10);

        epService.getEPRuntime().sendEvent(new SupportBean_S1(-5, "B"));
        assertMultiStmtContributingTotal("B", 5);

        epService.getEPRuntime().sendEvent(new SupportBean_S0(2, "C"));
        assertMultiStmtContributingTotal("C", 7);
    }

    private void assertMultiStmtContributingTotal(String c0, int total) {
        String[] fields = "c0,total".split(",");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{c0, total});

        epService.getEPRuntime().sendEvent(new SupportBean(c0, 0));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{c0, total});
    }

    private void runAssertionMultiStmtContributingDifferentAggs(boolean grouped) {
        String eplDeclare = "create table varagg (" +
                (grouped ? "key string primary key," : "") +
                "s0sum sum(int), s0cnt count(*), s0win window(*) @type(SupportBean_S0)," +
                "s1sum sum(int), s1cnt count(*), s1win window(*) @type(SupportBean_S1)" +
                ")";
        epService.getEPAdministrator().createEPL(eplDeclare);

        String[] fieldsSelect = "c0,c1,c2,c3,c4,c5".split(",");
        String eplSelectUngrouped = "select varagg.s0sum as c0, varagg.s0cnt as c1," +
                "varagg.s0win as c2, varagg.s1sum as c3, varagg.s1cnt as c4," +
                "varagg.s1win as c5 from SupportBean";
        String eplSelectGrouped = "select varagg[theString].s0sum as c0, varagg[theString].s0cnt as c1," +
                "varagg[theString].s0win as c2, varagg[theString].s1sum as c3, varagg[theString].s1cnt as c4," +
                "varagg[theString].s1win as c5 from SupportBean";
        epService.getEPAdministrator().createEPL(grouped ? eplSelectGrouped : eplSelectUngrouped).addListener(listener);

        SupportUpdateListener listenerOne = new SupportUpdateListener();
        String[] fieldsOne = "s0sum,s0cnt,s0win".split(",");
        String eplBindOne = "into table varagg select sum(id) as s0sum, count(*) as s0cnt, window(*) as s0win from SupportBean_S0.win:length(2) " +
                (grouped ? "group by p00" : "");
        epService.getEPAdministrator().createEPL(eplBindOne).addListener(listenerOne);

        SupportUpdateListener listenerTwo = new SupportUpdateListener();
        String[] fieldsTwo = "s1sum,s1cnt,s1win".split(",");
        String eplBindTwo = "into table varagg select sum(id) as s1sum, count(*) as s1cnt, window(*) as s1win from SupportBean_S1.win:length(2) " +
                (grouped ? "group by p10" : "");
        epService.getEPAdministrator().createEPL(eplBindTwo).addListener(listenerTwo);

        // contribute S1
        SupportBean_S1 s1_1 = makeSendS1(10, "G1");
        EPAssertionUtil.assertProps(listenerTwo.assertOneGetNewAndReset(), fieldsTwo, new Object[] {10,1L,new Object[] {s1_1}});

        epService.getEPRuntime().sendEvent(new SupportBean("G1", 0));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fieldsSelect,
                new Object[] {null,0L,null,10,1L,new Object[] {s1_1}});

        // contribute S0
        SupportBean_S0 s0_1 = makeSendS0(20, "G1");
        EPAssertionUtil.assertProps(listenerOne.assertOneGetNewAndReset(), fieldsOne, new Object[] {20,1L,new Object[] {s0_1}});

        epService.getEPRuntime().sendEvent(new SupportBean("G1", 0));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fieldsSelect,
                new Object[] {20,1L,new Object[] {s0_1},10,1L,new Object[] {s1_1}});

        // contribute S1 and S0
        SupportBean_S1 s1_2 = makeSendS1(11, "G1");
        EPAssertionUtil.assertProps(listenerTwo.assertOneGetNewAndReset(), fieldsTwo, new Object[] {21,2L,new Object[] {s1_1, s1_2}});
        SupportBean_S0 s0_2 = makeSendS0(21, "G1");
        EPAssertionUtil.assertProps(listenerOne.assertOneGetNewAndReset(), fieldsOne, new Object[] {41,2L,new Object[] {s0_1, s0_2}});

        epService.getEPRuntime().sendEvent(new SupportBean("G1", 0));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fieldsSelect,
                new Object[] {41,2L,new Object[] {s0_1, s0_2},21,2L,new Object[] {s1_1, s1_2}});

        // contribute S1 and S0 (leave)
        SupportBean_S1 s1_3 = makeSendS1(12, "G1");
        EPAssertionUtil.assertProps(listenerTwo.assertOneGetNewAndReset(), fieldsTwo, new Object[] {23,2L,new Object[] {s1_2, s1_3}});
        SupportBean_S0 s0_3 = makeSendS0(22, "G1");
        EPAssertionUtil.assertProps(listenerOne.assertOneGetNewAndReset(), fieldsOne, new Object[] {43,2L,new Object[] {s0_2, s0_3}});

        epService.getEPRuntime().sendEvent(new SupportBean("G1", 0));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fieldsSelect,
                new Object[] {43,2L,new Object[] {s0_2, s0_3},23,2L,new Object[] {s1_2, s1_3}});

        epService.getEPAdministrator().destroyAllStatements();
        epService.getEPAdministrator().getConfiguration().removeEventType("table_varagg__internal", false);
        epService.getEPAdministrator().getConfiguration().removeEventType("table_varagg__public", false);
    }

    private SupportBean_S1 makeSendS1(int id, String p10) {
        SupportBean_S1 bean = new SupportBean_S1(id, p10);
        epService.getEPRuntime().sendEvent(bean);
        return bean;
    }

    private SupportBean_S0 makeSendS0(int id, String p00) {
        SupportBean_S0 bean = new SupportBean_S0(id, p00);
        epService.getEPRuntime().sendEvent(bean);
        return bean;
    }

    private void runAssertionOrderOfAggs(boolean ungrouped) {

        String eplDeclare = "create table varagg (" + (ungrouped ? "" : "key string primary key, ") +
                "sumint sum(int), " +
                "sumlong sum(long), " +
                "mysort sorted(intPrimitive) @type(SupportBean)," +
                "mywindow window(*) @type(SupportBean)" +
                ")";
        epService.getEPAdministrator().createEPL(eplDeclare);

        String[] fieldsTable = "sumint,sumlong,mywindow,mysort".split(",");
        SupportUpdateListener listenerIntoTable = new SupportUpdateListener();
        String eplSelect = "into table varagg select " +
                "sum(longPrimitive) as sumlong, " +
                "sum(intPrimitive) as sumint, " +
                "window(*) as mywindow," +
                "sorted() as mysort " +
                "from SupportBean.win:length(2) " +
                (ungrouped ? "" : "group by theString ");
        epService.getEPAdministrator().createEPL(eplSelect).addListener(listenerIntoTable);

        String[] fieldsSelect = "c0,c1,c2,c3".split(",");
        String groupKey = ungrouped ? "" : "['E1']";
        epService.getEPAdministrator().createEPL("select " +
                "varagg" + groupKey + ".sumint as c0, " +
                "varagg" + groupKey + ".sumlong as c1," +
                "varagg" + groupKey + ".mywindow as c2," +
                "varagg"  + groupKey + ".mysort as c3 from SupportBean_S0").addListener(listener);

        SupportBean e1 = makeSendBean("E1", 10, 100);
        EPAssertionUtil.assertProps(listenerIntoTable.assertOneGetNewAndReset(), fieldsTable,
                new Object[] {10, 100L, new Object[] {e1}, new Object[] {e1}});

        SupportBean e2 = makeSendBean("E1", 5, 50);
        EPAssertionUtil.assertProps(listenerIntoTable.assertOneGetNewAndReset(), fieldsTable,
                new Object[] {15, 150L, new Object[] {e1, e2}, new Object[] {e2, e1}});

        epService.getEPRuntime().sendEvent(new SupportBean_S0(0));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fieldsSelect,
            new Object[] {15, 150L, new Object[] {e1, e2}, new Object[] {e2, e1}});

        SupportBean e3 = makeSendBean("E1", 12, 120);
        EPAssertionUtil.assertProps(listenerIntoTable.assertOneGetNewAndReset(), fieldsTable,
                new Object[] {17, 170L, new Object[] {e2, e3}, new Object[] {e2, e3}});

        epService.getEPRuntime().sendEvent(new SupportBean_S0(0));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fieldsSelect,
                new Object[] {17, 170L, new Object[] {e2, e3}, new Object[] {e2, e3}});

        epService.getEPAdministrator().destroyAllStatements();
        epService.getEPAdministrator().getConfiguration().removeEventType("table_varagg__internal", false);
        epService.getEPAdministrator().getConfiguration().removeEventType("table_varagg__public", false);
    }

    public void testGroupedMixedMethodAndAccess() throws Exception {
        runAssertionGroupedMixedMethodAndAccess(false);
        runAssertionGroupedMixedMethodAndAccess(true);
    }

    public void testNamedWindowAndFireAndForget() throws Exception {
        String epl = "create window MyWindow.win:length(2) as SupportBean;\n" +
                     "insert into MyWindow select * from SupportBean;\n" +
                     "create table varagg (total sum(int));\n" +
                     "into table varagg select sum(intPrimitive) as total from MyWindow;\n";
        epService.getEPAdministrator().getDeploymentAdmin().parseDeploy(epl);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 10));
        EPOnDemandQueryResult resultSelect = epService.getEPRuntime().executeQuery("select varagg.total as c0 from MyWindow");
        assertEquals(10, resultSelect.getArray()[0].get("c0"));

        EPOnDemandQueryResult resultDelete = epService.getEPRuntime().executeQuery("delete from MyWindow where varagg.total = intPrimitive");
        assertEquals(1, resultDelete.getArray().length);

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 20));
        EPOnDemandQueryResult resultUpdate = epService.getEPRuntime().executeQuery("update MyWindow set doublePrimitive = 100 where varagg.total = intPrimitive");
        assertEquals(100d, resultUpdate.getArray()[0].get("doublePrimitive"));

        EPOnDemandQueryResult resultInsert = epService.getEPRuntime().executeQuery("insert into MyWindow (theString, intPrimitive) values ('A', varagg.total)");
        EPAssertionUtil.assertProps(resultInsert.getArray()[0], "theString,intPrimitive".split(","), new Object[] {"A", 20});
    }

    public void testSubquery() {
        epService.getEPAdministrator().createEPL("create table subquery_var_agg (key string primary key, total count(*))");
        epService.getEPAdministrator().createEPL("select (select subquery_var_agg[p00].total from SupportBean_S0.std:lastevent()) as c0 " +
                "from SupportBean_S1").addListener(listener);
        epService.getEPAdministrator().createEPL("into table subquery_var_agg select count(*) as total from SupportBean group by theString");

        epService.getEPRuntime().sendEvent(new SupportBean("E1", -1));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(0, "E1"));
        epService.getEPRuntime().sendEvent(new SupportBean_S1(1));
        assertEquals(1L, listener.assertOneGetNewAndReset().get("c0"));

        epService.getEPRuntime().sendEvent(new SupportBean("E1", -1));
        epService.getEPRuntime().sendEvent(new SupportBean_S1(2));
        assertEquals(2L, listener.assertOneGetNewAndReset().get("c0"));
    }

    public void testOnMergeExpressions() {
        epService.getEPAdministrator().createEPL("create table the_table (key string primary key, total count(*), value int)");
        epService.getEPAdministrator().createEPL("into table the_table select count(*) as total from SupportBean group by theString");
        epService.getEPAdministrator().createEPL("on SupportBean_S0 as s0 " +
                "merge the_table as tt " +
                "where s0.p00 = tt.key " +
                "when matched and the_table[s0.p00].total > 0" +
                "  then update set value = 1");
        epService.getEPAdministrator().createEPL("select the_table[p10].value as c0 from SupportBean_S1").addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", -1));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(0, "E1"));

        epService.getEPRuntime().sendEvent(new SupportBean_S1(0, "E1"));
        assertEquals(1, listener.assertOneGetNewAndReset().get("c0"));
    }

    private void runAssertionGroupedMixedMethodAndAccess(boolean soda) throws Exception {
        String eplDeclare = "create table varMyAgg (" +
                "key string primary key, " +
                "c0 count(*), " +
                "c1 count(distinct object), " +
                "c2 window(*) @type('SupportBean'), " +
                "c3 sum(long)" +
                ")";
        SupportModelHelper.createByCompileOrParse(epService, soda, eplDeclare);

        String eplBind = "into table varMyAgg select " +
                "count(*) as c0, " +
                "count(distinct intPrimitive) as c1, " +
                "window(*) as c2, " +
                "sum(longPrimitive) as c3 " +
                "from SupportBean.win:length(3) group by theString";
        SupportModelHelper.createByCompileOrParse(epService, soda, eplBind);

        String eplSelect = "select " +
                "varMyAgg[p00].c0 as c0, " +
                "varMyAgg[p00].c1 as c1, " +
                "varMyAgg[p00].c2 as c2, " +
                "varMyAgg[p00].c3 as c3" +
                " from SupportBean_S0";
        EPStatement stmtSelect = SupportModelHelper.createByCompileOrParse(epService, soda, eplSelect);
        stmtSelect.addListener(listener);
        String[] fields = "c0,c1,c2,c3".split(",");

        assertEquals(Long.class, stmtSelect.getEventType().getPropertyType("c0"));
        assertEquals(Long.class, stmtSelect.getEventType().getPropertyType("c1"));
        assertEquals(SupportBean[].class, stmtSelect.getEventType().getPropertyType("c2"));
        assertEquals(Long.class, stmtSelect.getEventType().getPropertyType("c3"));

        SupportBean b1 = makeSendBean("E1", 10, 100);
        SupportBean b2 = makeSendBean("E1", 11, 101);
        SupportBean b3 = makeSendBean("E1", 10, 102);

        epService.getEPRuntime().sendEvent(new SupportBean_S0(0, "E1"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields,
                new Object[] {3L, 2L, new SupportBean[] {b1, b2, b3}, 303L});

        epService.getEPRuntime().sendEvent(new SupportBean_S0(0, "E2"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields,
                new Object[] {null, null, null, null});

        SupportBean b4 = makeSendBean("E2", 20, 200);
        epService.getEPRuntime().sendEvent(new SupportBean_S0(0, "E2"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields,
                new Object[] {1L, 1L, new SupportBean[] {b4}, 200L});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionTopLevelSingle() {
        sendEventsAndAssert("A", 10, "A", 10);
        sendEventsAndAssert("A", 11, "A", 21);
        sendEventsAndAssert("B", 20, "A", 21);
        sendEventsAndAssert("B", 21, "B", 41);
        sendEventsAndAssert("C", 30, "A", 21);
        sendEventsAndAssert("D", 40, "C", 30);

        String[] fields = "c0,c1".split(",");
        int[] expected = new int[]{21, 41, 30, 40};
        int count = 0;
        for (String p00 : "A,B,C,D".split(",")) {
            epService.getEPRuntime().sendEvent(new SupportBean_S0(0, p00));
            EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {p00, expected[count]});
            count++;
        }

        epService.getEPRuntime().sendEvent(new SupportBean_S0(0, "A"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"A", 21});
    }

    private void sendEventsAndAssert(String theString, int intPrimitive, String p00, int total) {
        String[] fields = "c0,c1".split(",");
        epService.getEPRuntime().sendEvent(new SupportBean(theString, intPrimitive));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(0, p00));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {p00, total});
    }

    private SupportBean makeSendBean(String theString, int intPrimitive, long longPrimitive) {
        return makeSendBean(theString, intPrimitive, longPrimitive, -1);
    }

    private SupportBean makeSendBean(String theString, int intPrimitive, long longPrimitive, double doublePrimitive) {
        return makeSendBean(theString, intPrimitive, longPrimitive, doublePrimitive, -1);
    }

    private SupportBean makeSendBean(String theString, int intPrimitive, long longPrimitive, double doublePrimitive, float floatPrimitive) {
        SupportBean bean = new SupportBean(theString, intPrimitive);
        bean.setLongPrimitive(longPrimitive);
        bean.setDoublePrimitive(doublePrimitive);
        bean.setFloatPrimitive(floatPrimitive);
        epService.getEPRuntime().sendEvent(bean);
        return bean;
    }

    private void assertTopLevelTypeInfo(EPStatement stmt) {
        assertEquals(Map.class, stmt.getEventType().getPropertyType("val0"));
        FragmentEventType fragType = stmt.getEventType().getFragmentType("val0");
        assertFalse(fragType.isIndexed());
        assertFalse(fragType.isNative());
        assertEquals(Object[][].class, fragType.getFragmentType().getPropertyType("thewindow"));
        assertEquals(Integer.class, fragType.getFragmentType().getPropertyType("thetotal"));
    }

    private static class AggSubBean {
        private int thetotal;
        private Object[][] thewindow;

        public int getThetotal() {
            return thetotal;
        }

        public void setThetotal(int thetotal) {
            this.thetotal = thetotal;
        }

        public Object[][] getThewindow() {
            return thewindow;
        }

        public void setThewindow(Object[][] thewindow) {
            this.thewindow = thewindow;
        }
    }

    private static class AggBean {
        private AggSubBean val0;

        public AggSubBean getVal0() {
            return val0;
        }

        public void setVal0(AggSubBean val0) {
            this.val0 = val0;
        }
    }
}
