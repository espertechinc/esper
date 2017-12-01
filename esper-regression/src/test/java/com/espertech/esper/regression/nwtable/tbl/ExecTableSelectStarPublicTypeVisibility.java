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

import com.espertech.esper.client.*;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.time.CurrentTimeEvent;
import com.espertech.esper.core.service.EPServiceProviderSPI;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBean_S0;
import com.espertech.esper.supportregression.bean.SupportBean_S1;
import com.espertech.esper.supportregression.bean.SupportBean_S2;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.supportregression.subscriber.SupportSubscriberMultirowObjectArrayNStmt;
import com.espertech.esper.support.SupportEventTypeAssertionEnum;
import com.espertech.esper.support.SupportEventTypeAssertionUtil;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class ExecTableSelectStarPublicTypeVisibility implements RegressionExecution {

    public void run(EPServiceProvider epService) throws Exception {
        for (Class clazz : new Class[]{SupportBean.class, SupportBean_S0.class, SupportBean_S1.class, SupportBean_S2.class}) {
            epService.getEPAdministrator().getConfiguration().addEventType(clazz);
        }

        AtomicLong currentTime = new AtomicLong(0);
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(currentTime.get()));
        epService.getEPAdministrator().createEPL("@name('create') create table MyTable as (\n" +
                "key string primary key,\n" +
                "totalInt sum(int),\n" +
                "p0 string,\n" +
                "winsb window(*) @type(SupportBean),\n" +
                "totalLong sum(long),\n" +
                "p1 string,\n" +
                "winsb0 window(*) @type(SupportBean_S0)\n" +
                ")");
        Object[][] expectedType = new Object[][]{
                {"key", String.class},
                {"totalInt", Integer.class},
                {"p0", String.class},
                {"winsb", SupportBean[].class},
                {"totalLong", Long.class},
                {"p1", String.class},
                {"winsb0", SupportBean_S0[].class},
        };

        epService.getEPAdministrator().createEPL("into table MyTable " +
                "select sum(intPrimitive) as totalInt, sum(longPrimitive) as totalLong," +
                "window(*) as winsb from SupportBean#keepall group by theString");
        epService.getEPAdministrator().createEPL("into table MyTable " +
                "select window(*) as winsb0 from SupportBean_S0#keepall group by p00");
        epService.getEPAdministrator().createEPL("on SupportBean_S1 " +
                "merge MyTable where p10 = key when matched then " +
                "update set p0 = p11, p1 = p12");

        SupportBean e1Sb = makeSupportBean("G1", 10, 100);
        epService.getEPRuntime().sendEvent(e1Sb); // update some aggs

        SupportBean_S0 e2Sb0 = new SupportBean_S0(5, "G1");
        epService.getEPRuntime().sendEvent(e2Sb0); // update more aggs

        epService.getEPRuntime().sendEvent(new SupportBean_S1(6, "G1", "a", "b")); // merge more values

        Object[] rowValues = {"G1", 10, "a", new SupportBean[]{e1Sb}, 100L, "b", new SupportBean_S0[]{e2Sb0}};
        runAssertionSubqueryWindowAgg(epService, rowValues);
        runAssertionOnSelectWindowAgg(epService, expectedType, rowValues);
        runAssertionSubquerySelectStar(epService, rowValues);
        runAssertionSubquerySelectWEnumMethod(epService, rowValues);
        runAssertionIterateCreateTable(epService, expectedType, rowValues, epService.getEPAdministrator().getStatement("create"));
        runAssertionJoinSelectStar(epService, expectedType, rowValues);
        runAssertionJoinSelectStreamName(epService, expectedType, rowValues);
        runAssertionJoinSelectStreamStarNamed(epService, expectedType, rowValues);
        runAssertionJoinSelectStreamStarUnnamed(epService, expectedType, rowValues);
        runAssertionInsertIntoBean(epService, rowValues);
        runAssertionSingleRowFunc(epService, rowValues);
        runAssertionOutputSnapshot(epService, expectedType, rowValues, currentTime);
        runAssertionFireAndForgetSelectStar(epService, expectedType, rowValues);
        runAssertionFireAndForgetInsertUpdateDelete(epService, expectedType);
    }

    private void runAssertionSubqueryWindowAgg(EPServiceProvider epService, Object[] rowValues) {
        EPStatement stmt = epService.getEPAdministrator().createEPL("select " +
                "(select window(mt.*) from MyTable as mt) as c0," +
                "(select first(mt.*) from MyTable as mt) as c1" +
                " from SupportBean_S2");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        epService.getEPRuntime().sendEvent(new SupportBean_S2(0));
        EventBean event = listener.assertOneGetNewAndReset();
        assertEventUnd(((Object[][]) event.get("c0"))[0], rowValues);
        assertEventUnd(event.get("c1"), rowValues);
        stmt.destroy();
    }

    private void runAssertionOnSelectWindowAgg(EPServiceProvider epService, Object[][] expectedType, Object[] rowValues) {
        EPStatement stmt = epService.getEPAdministrator().createEPL("on SupportBean_S2 select " +
                "window(win.*) as c0," +
                "last(win.*) as c1, " +
                "first(win.*) as c2, " +
                "first(p1) as c3," +
                "window(p1) as c4," +
                "sorted(p1) as c5," +
                "minby(p1) as c6" +
                " from MyTable as win");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean_S2(0));
        EventBean event = listener.assertOneGetNewAndReset();
        for (String col : "c1,c2,c6".split(",")) {
            assertEventUnd(event.get(col), rowValues);
        }
        for (String col : "c0,c5".split(",")) {
            assertEventUnd(((Object[][]) event.get(col))[0], rowValues);
        }
        assertEquals("b", event.get("c3"));
        EPAssertionUtil.assertEqualsExactOrder(new String[]{"b"}, (String[]) event.get("c4"));

        stmt.destroy();
    }

    private void runAssertionOutputSnapshot(EPServiceProvider epService, Object[][] expectedType, Object[] rowValues, AtomicLong currentTime) {
        EPStatement stmt = epService.getEPAdministrator().createEPL("select * from MyTable output snapshot every 1 second");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        assertEventType(stmt.getEventType(), expectedType);

        currentTime.set(currentTime.get() + 1000L);
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(currentTime.get()));
        EventBean event = listener.assertOneGetNewAndReset();
        assertEventTypeAndEvent(event.getEventType(), expectedType, event.getUnderlying(), rowValues);
    }

    private void runAssertionFireAndForgetInsertUpdateDelete(EPServiceProvider epService, Object[][] expectedType) {
        EPOnDemandQueryResult result = epService.getEPRuntime().executeQuery("insert into MyTable(key) values ('dummy')");
        assertEventType(result.getEventType(), expectedType);

        result = epService.getEPRuntime().executeQuery("delete from MyTable where key = 'dummy'");
        assertEventType(result.getEventType(), expectedType);

        result = epService.getEPRuntime().executeQuery("update MyTable set key='dummy' where key='dummy'");
        assertEventType(result.getEventType(), expectedType);
    }

    private void runAssertionIterateCreateTable(EPServiceProvider epService, Object[][] expectedType, Object[] rowValues, EPStatement stmtCreate) {
        assertEventTypeAndEvent(stmtCreate.getEventType(), expectedType, stmtCreate.iterator().next().getUnderlying(), rowValues);
    }

    private void runAssertionSingleRowFunc(EPServiceProvider epService, Object[] rowValues) {
        // try join passing of params
        String eplJoin = "select " +
                this.getClass().getName() + ".myServiceEventBean(mt) as c0, " +
                this.getClass().getName() + ".myServiceObjectArray(mt) as c1 " +
                "from SupportBean_S2, MyTable as mt";
        EPStatement stmtJoin = epService.getEPAdministrator().createEPL(eplJoin);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmtJoin.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean_S2(0));
        EventBean result = listener.assertOneGetNewAndReset();
        assertEventUnd(result.get("c0"), rowValues);
        assertEventUnd(result.get("c1"), rowValues);
        stmtJoin.destroy();

        // try subquery
        epService.getEPAdministrator().getConfiguration().addPlugInSingleRowFunction("pluginServiceEventBean", this.getClass().getName(), "myServiceEventBean");
        String eplSubquery = "select (select pluginServiceEventBean(mt) from MyTable as mt) as c0 " +
                "from SupportBean_S2";
        EPStatement stmtSubquery = epService.getEPAdministrator().createEPL(eplSubquery);
        stmtSubquery.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean_S2(0));
        result = listener.assertOneGetNewAndReset();
        assertEventUnd(result.get("c0"), rowValues);
        stmtSubquery.destroy();
    }

    private void runAssertionInsertIntoBean(EPServiceProvider epService, Object[] rowValues) {
        epService.getEPAdministrator().getConfiguration().addEventType(MyBeanCtor.class);
        String epl = "insert into MyBeanCtor select * from SupportBean_S2, MyTable";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean_S2(0));
        assertEventUnd(listener.assertOneGetNewAndReset().get("arr"), rowValues);

        stmt.destroy();
    }

    private void runAssertionSubquerySelectWEnumMethod(EPServiceProvider epService, Object[] rowValues) {
        String epl = "select (select * from MyTable).where(v=>v.key = 'G1') as mt from SupportBean_S2";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        assertEquals(Collection.class, stmt.getEventType().getPropertyType("mt"));

        epService.getEPRuntime().sendEvent(new SupportBean_S2(0));
        Collection coll = (Collection) listener.assertOneGetNewAndReset().get("mt");
        assertEventUnd(coll.iterator().next(), rowValues);

        stmt.destroy();
    }

    private void runAssertionSubquerySelectStar(EPServiceProvider epService, Object[] rowValues) {
        String eplFiltered = "select (select * from MyTable where key = 'G1') as mt from SupportBean_S2";
        runAssertionSubquerySelectStar(epService, rowValues, eplFiltered);

        String eplUnfiltered = "select (select * from MyTable) as mt from SupportBean_S2";
        runAssertionSubquerySelectStar(epService, rowValues, eplUnfiltered);

        // With @eventbean
        String eplEventBean = "select (select * from MyTable) @eventbean as mt from SupportBean_S2";
        EPStatement stmt = epService.getEPAdministrator().createEPL(eplEventBean);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        assertEquals(Object[][].class, stmt.getEventType().getPropertyType("mt"));
        assertSame(getTablePublicType(epService, "MyTable"), stmt.getEventType().getFragmentType("mt").getFragmentType());

        epService.getEPRuntime().sendEvent(new SupportBean_S2(0));
        EventBean event = listener.assertOneGetNewAndReset();
        Object[][] value = (Object[][]) event.get("mt");
        assertEventUnd(value[0], rowValues);
        assertSame(getTablePublicType(epService, "MyTable"), ((EventBean[]) event.getFragment("mt"))[0].getEventType());

        stmt.destroy();
    }

    private void runAssertionSubquerySelectStar(EPServiceProvider epService, Object[] rowValues, String epl) {
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        assertEquals(Object[].class, stmt.getEventType().getPropertyType("mt"));

        epService.getEPRuntime().sendEvent(new SupportBean_S2(0));
        EventBean event = listener.assertOneGetNewAndReset();
        assertEventUnd(event.get("mt"), rowValues);

        stmt.destroy();
    }

    private void runAssertionJoinSelectStreamStarUnnamed(EPServiceProvider epService, Object[][] expectedType, Object[] rowValues) {
        String joinEpl = "select mt.* from MyTable as mt, SupportBean_S2 where key = p20";
        EPStatement stmt = epService.getEPAdministrator().createEPL(joinEpl);
        SupportUpdateListener listener = new SupportUpdateListener();
        SupportSubscriberMultirowObjectArrayNStmt subscriber = new SupportSubscriberMultirowObjectArrayNStmt();
        stmt.addListener(listener);
        stmt.setSubscriber(subscriber);

        assertEventType(stmt.getEventType(), expectedType);

        // listener assertion
        epService.getEPRuntime().sendEvent(new SupportBean_S2(0, "G1"));
        EventBean event = listener.assertOneGetNewAndReset();
        assertEventTypeAndEvent(event.getEventType(), expectedType, event.getUnderlying(), rowValues);

        // subscriber assertion
        Object[][] newData = subscriber.getAndResetIndicateArr().get(0).getFirst();
        assertEventUnd(newData[0][0], rowValues);

        stmt.destroy();
    }

    private void runAssertionJoinSelectStreamStarNamed(EPServiceProvider epService, Object[][] expectedType, Object[] rowValues) {
        String joinEpl = "select mt.* as mymt from MyTable as mt, SupportBean_S2 where key = p20";
        EPStatement stmt = epService.getEPAdministrator().createEPL(joinEpl);
        SupportUpdateListener listener = new SupportUpdateListener();
        SupportSubscriberMultirowObjectArrayNStmt subscriber = new SupportSubscriberMultirowObjectArrayNStmt();
        stmt.addListener(listener);
        stmt.setSubscriber(subscriber);

        assertEventType(stmt.getEventType().getFragmentType("mymt").getFragmentType(), expectedType);

        // listener assertion
        epService.getEPRuntime().sendEvent(new SupportBean_S2(0, "G1"));
        EventBean event = listener.assertOneGetNewAndReset();
        assertEventTypeAndEvent(event.getEventType().getFragmentType("mymt").getFragmentType(),
                expectedType, event.get("mymt"), rowValues);

        // subscriber assertion
        Object[][] newData = subscriber.getAndResetIndicateArr().get(0).getFirst();
        assertEventUnd(newData[0][0], rowValues);

        stmt.destroy();
    }

    private void runAssertionJoinSelectStreamName(EPServiceProvider epService, Object[][] expectedType, Object[] rowValues) {
        String joinEpl = "select mt from MyTable as mt, SupportBean_S2 where key = p20";
        EPStatement stmt = epService.getEPAdministrator().createEPL(joinEpl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        assertEventType(stmt.getEventType().getFragmentType("mt").getFragmentType(), expectedType);

        epService.getEPRuntime().sendEvent(new SupportBean_S2(0, "G1"));
        EventBean event = listener.assertOneGetNewAndReset();
        assertEventTypeAndEvent(event.getEventType().getFragmentType("mt").getFragmentType(),
                expectedType, event.get("mt"), rowValues);

        stmt.destroy();
    }

    private void runAssertionJoinSelectStar(EPServiceProvider epService, Object[][] expectedType, Object[] rowValues) {
        String joinEpl = "select * from MyTable, SupportBean_S2 where key = p20";
        EPStatement stmt = epService.getEPAdministrator().createEPL(joinEpl);
        SupportUpdateListener listener = new SupportUpdateListener();
        SupportSubscriberMultirowObjectArrayNStmt subscriber = new SupportSubscriberMultirowObjectArrayNStmt();
        stmt.addListener(listener);
        stmt.setSubscriber(subscriber);

        assertEventType(stmt.getEventType().getFragmentType("stream_0").getFragmentType(), expectedType);

        // listener assertion
        epService.getEPRuntime().sendEvent(new SupportBean_S2(0, "G1"));
        EventBean event = listener.assertOneGetNewAndReset();
        assertEventTypeAndEvent(event.getEventType().getFragmentType("stream_0").getFragmentType(),
                expectedType, event.get("stream_0"), rowValues);

        // subscriber assertion
        Object[][] newData = subscriber.getAndResetIndicateArr().get(0).getFirst();
        assertEventUnd(newData[0][0], rowValues);

        stmt.destroy();
    }

    private void runAssertionFireAndForgetSelectStar(EPServiceProvider epService, Object[][] expectedType, Object[] rowValues) {
        EPOnDemandQueryResult result = epService.getEPRuntime().executeQuery("select * from MyTable where key = 'G1'");
        assertEventTypeAndEvent(result.getEventType(), expectedType, result.getArray()[0].getUnderlying(), rowValues);
    }

    private void assertEventTypeAndEvent(EventType eventType, Object[][] expectedType, Object underlying, Object[] expectedValues) {
        assertEventType(eventType, expectedType);
        assertEventUnd(underlying, expectedValues);
    }

    private void assertEventUnd(Object underlying, Object[] expectedValues) {
        Object[] und = (Object[]) underlying;
        EPAssertionUtil.assertEqualsExactOrder(expectedValues, und);
    }

    private void assertEventType(EventType eventType, Object[][] expectedType) {
        SupportEventTypeAssertionUtil.assertEventTypeProperties(expectedType, eventType, SupportEventTypeAssertionEnum.NAME, SupportEventTypeAssertionEnum.TYPE);
    }

    private SupportBean makeSupportBean(String theString, int intPrimitive, int longPrimitive) {
        SupportBean supportBean = new SupportBean(theString, intPrimitive);
        supportBean.setLongPrimitive(longPrimitive);
        return supportBean;
    }

    public static Object[] myServiceEventBean(EventBean event) {
        return (Object[]) event.getUnderlying();
    }

    public static Object[] myServiceObjectArray(Object[] data) {
        return data;
    }

    public EventType getTablePublicType(EPServiceProvider epService, String tableName) {
        return ((EPServiceProviderSPI) epService).getServicesContext().getTableService().getTableMetadata(tableName).getPublicEventType();
    }

    public static final class MyBeanCtor {
        private final SupportBean_S2 sb;
        private final Object[] arr;

        public MyBeanCtor(SupportBean_S2 sb, Object[] arr) {
            this.sb = sb;
            this.arr = arr;
        }

        public SupportBean_S2 getSb() {
            return sb;
        }

        public Object[] getArr() {
            return arr;
        }
    }
}
