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
package com.espertech.esper.regression.epl.other;

import com.espertech.esper.client.EPOnDemandQueryResult;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.soda.EPStatementObjectModel;
import com.espertech.esper.client.soda.FilterStream;
import com.espertech.esper.client.soda.FromClause;
import com.espertech.esper.client.soda.SelectClause;
import com.espertech.esper.client.time.CurrentTimeEvent;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBean_A;
import com.espertech.esper.supportregression.bean.SupportBean_N;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class ExecEPLDistinct implements RegressionExecution {
    public void run(EPServiceProvider epService) throws Exception {
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean_A", SupportBean_A.class);
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean_N", SupportBean_N.class);

        runAssertionWildcardJoinPattern(epService);
        runAssertionOnDemandAndOnSelect(epService);
        runAssertionSubquery(epService);
        runAssertionBeanEventWildcardThisProperty(epService);
        runAssertionBeanEventWildcardSODA(epService);
        runAssertionBeanEventWildcardPlusCols(epService);
        runAssertionMapEventWildcard(epService);
        runAssertionOutputSimpleColumn(epService);
        runAssertionOutputLimitEveryColumn(epService);
        runAssertionOutputRateSnapshotColumn(epService);
        runAssertionBatchWindow(epService);
        runAssertionBatchWindowJoin(epService);
        runAssertionBatchWindowInsertInto(epService);
    }

    private void runAssertionWildcardJoinPattern(EPServiceProvider epService) {
        String epl = "select distinct * from " +
                "SupportBean(intPrimitive=0) as fooB unidirectional " +
                "inner join " +
                "pattern [" +
                "every-distinct(fooA.theString) fooA=SupportBean(intPrimitive=1)" +
                "->" +
                "every-distinct(wooA.theString) wooA=SupportBean(intPrimitive=2)" +
                " where timer:within(1 hour)" +
                "]#time(1 hour) as fooWooPair " +
                "on fooB.longPrimitive = fooWooPair.fooA.longPrimitive";

        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        sendEvent(epService, "E1", 1, 10L);
        sendEvent(epService, "E1", 2, 10L);

        sendEvent(epService, "E2", 1, 10L);
        sendEvent(epService, "E2", 2, 10L);

        sendEvent(epService, "E3", 1, 10L);
        sendEvent(epService, "E3", 2, 10L);

        sendEvent(epService, "Query", 0, 10L);
        assertTrue(listener.isInvoked());

        stmt.destroy();
    }

    private void sendEvent(EPServiceProvider epService, String theString, int intPrimitive, long longPrimitive) {
        SupportBean bean = new SupportBean(theString, intPrimitive);
        bean.setLongPrimitive(longPrimitive);
        epService.getEPRuntime().sendEvent(bean);
    }

    private void runAssertionOnDemandAndOnSelect(EPServiceProvider epService) {
        String[] fields = new String[]{"theString", "intPrimitive"};
        epService.getEPAdministrator().createEPL("create window MyWindow#keepall as select * from SupportBean");
        epService.getEPAdministrator().createEPL("insert into MyWindow select * from SupportBean");

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 2));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 2));
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));

        String query = "select distinct theString, intPrimitive from MyWindow order by theString, intPrimitive";
        EPOnDemandQueryResult result = epService.getEPRuntime().executeQuery(query);
        EPAssertionUtil.assertPropsPerRow(result.getArray(), fields, new Object[][]{{"E1", 1}, {"E1", 2}, {"E2", 2}});

        EPStatement stmt = epService.getEPAdministrator().createEPL("on SupportBean_A select distinct theString, intPrimitive from MyWindow order by theString, intPrimitive asc");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean_A("x"));
        EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), fields, new Object[][]{{"E1", 1}, {"E1", 2}, {"E2", 2}});

        stmt.destroy();
    }

    private void runAssertionSubquery(EPServiceProvider epService) {
        String[] fields = new String[]{"theString", "intPrimitive"};
        EPStatement stmt = epService.getEPAdministrator().createEPL("select * from SupportBean where theString in (select distinct id from SupportBean_A#keepall)");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean_A("E1"));
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 2));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E1", 2});

        epService.getEPRuntime().sendEvent(new SupportBean_A("E1"));
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 3));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E1", 3});

        stmt.destroy();
    }

    // Since the "this" property will always be unique, this test verifies that condition
    private void runAssertionBeanEventWildcardThisProperty(EPServiceProvider epService) {
        String[] fields = new String[]{"theString", "intPrimitive"};
        String statementText = "select distinct * from SupportBean#keepall";
        EPStatement stmt = epService.getEPAdministrator().createEPL(statementText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, new Object[][]{{"E1", 1}});

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 2));
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, new Object[][]{{"E1", 1}, {"E2", 2}});

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, new Object[][]{{"E1", 1}, {"E2", 2}, {"E1", 1}});

        stmt.destroy();
    }

    private void runAssertionBeanEventWildcardSODA(EPServiceProvider epService) {
        String[] fields = new String[]{"id"};
        String statementText = "select distinct * from SupportBean_A#keepall";
        EPStatement stmt = epService.getEPAdministrator().createEPL(statementText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean_A("E1"));
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, new Object[][]{{"E1"}});

        epService.getEPRuntime().sendEvent(new SupportBean_A("E2"));
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, new Object[][]{{"E1"}, {"E2"}});

        epService.getEPRuntime().sendEvent(new SupportBean_A("E1"));
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, new Object[][]{{"E1"}, {"E2"}});

        EPStatementObjectModel model = epService.getEPAdministrator().compileEPL(statementText);
        assertEquals(statementText, model.toEPL());

        model = new EPStatementObjectModel();
        model.setSelectClause(SelectClause.createWildcard().distinct(true));
        model.setFromClause(FromClause.create(FilterStream.create("SupportBean_A")));
        assertEquals("select distinct * from SupportBean_A", model.toEPL());

        stmt.destroy();
    }

    private void runAssertionBeanEventWildcardPlusCols(EPServiceProvider epService) {
        String[] fields = new String[]{"intPrimitive", "val1", "val2"};
        String statementText = "select distinct *, intBoxed%5 as val1, intBoxed as val2 from SupportBean_N#keepall";
        EPStatement stmt = epService.getEPAdministrator().createEPL(statementText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean_N(1, 8));
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, new Object[][]{{1, 3, 8}});

        epService.getEPRuntime().sendEvent(new SupportBean_N(1, 3));
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, new Object[][]{{1, 3, 8}, {1, 3, 3}});

        epService.getEPRuntime().sendEvent(new SupportBean_N(1, 8));
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, new Object[][]{{1, 3, 8}, {1, 3, 3}});

        stmt.destroy();
    }

    private void runAssertionMapEventWildcard(EPServiceProvider epService) {
        Map<String, Object> def = new HashMap<>();
        def.put("k1", String.class);
        def.put("v1", int.class);
        epService.getEPAdministrator().getConfiguration().addEventType("MyMapType", def);

        String[] fields = new String[]{"k1", "v1"};
        String statementText = "select distinct * from MyMapType#keepall";
        EPStatement stmt = epService.getEPAdministrator().createEPL(statementText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        sendMapEvent(epService, "E1", 1);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, new Object[][]{{"E1", 1}});

        sendMapEvent(epService, "E2", 2);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, new Object[][]{{"E1", 1}, {"E2", 2}});

        sendMapEvent(epService, "E1", 1);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, new Object[][]{{"E1", 1}, {"E2", 2}});

        stmt.destroy();
    }

    private void runAssertionOutputSimpleColumn(EPServiceProvider epService) {
        String[] fields = new String[]{"theString", "intPrimitive"};
        String statementText = "select distinct theString, intPrimitive from SupportBean#keepall";
        EPStatement stmt = epService.getEPAdministrator().createEPL(statementText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        tryAssertionSimpleColumn(epService, listener, stmt, fields);
        stmt.destroy();

        // test join
        statementText = "select distinct theString, intPrimitive from SupportBean#keepall a, SupportBean_A#keepall b where a.theString = b.id";
        stmt = epService.getEPAdministrator().createEPL(statementText);
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean_A("E1"));
        epService.getEPRuntime().sendEvent(new SupportBean_A("E2"));
        tryAssertionSimpleColumn(epService, listener, stmt, fields);

        stmt.destroy();
    }

    private void runAssertionOutputLimitEveryColumn(EPServiceProvider epService) {
        String[] fields = new String[]{"theString", "intPrimitive"};
        String statementText = "@IterableUnbound select distinct theString, intPrimitive from SupportBean output every 3 events";
        EPStatement stmt = epService.getEPAdministrator().createEPL(statementText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        tryAssertionOutputEvery(epService, listener, stmt, fields);
        stmt.destroy();

        // test join
        statementText = "select distinct theString, intPrimitive from SupportBean#lastevent a, SupportBean_A#keepall b where a.theString = b.id output every 3 events";
        stmt = epService.getEPAdministrator().createEPL(statementText);
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean_A("E1"));
        epService.getEPRuntime().sendEvent(new SupportBean_A("E2"));
        tryAssertionOutputEvery(epService, listener, stmt, fields);

        stmt.destroy();
    }

    private void runAssertionOutputRateSnapshotColumn(EPServiceProvider epService) {
        String[] fields = new String[]{"theString", "intPrimitive"};
        String statementText = "select distinct theString, intPrimitive from SupportBean#keepall output snapshot every 3 events order by theString asc";
        EPStatement stmt = epService.getEPAdministrator().createEPL(statementText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        tryAssertionSnapshotColumn(epService, listener, stmt, fields);
        stmt.destroy();

        statementText = "select distinct theString, intPrimitive from SupportBean#keepall a, SupportBean_A#keepall b where a.theString = b.id output snapshot every 3 events order by theString asc";
        stmt = epService.getEPAdministrator().createEPL(statementText);
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean_A("E1"));
        epService.getEPRuntime().sendEvent(new SupportBean_A("E2"));
        epService.getEPRuntime().sendEvent(new SupportBean_A("E3"));
        tryAssertionSnapshotColumn(epService, listener, stmt, fields);

        stmt.destroy();
    }

    private void runAssertionBatchWindow(EPServiceProvider epService) {
        String[] fields = new String[]{"theString", "intPrimitive"};
        String statementText = "select distinct theString, intPrimitive from SupportBean#length_batch(3)";
        EPStatement stmt = epService.getEPAdministrator().createEPL(statementText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, new Object[][]{{"E1", 1}});
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 2));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][]{{"E1", 1}, {"E2", 2}});

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 2));
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 2));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][]{{"E2", 2}, {"E1", 1}});

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 3));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 3));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 3));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][]{{"E2", 3}});

        stmt.destroy();

        // test batch window with aggregation
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(0));
        String[] fieldsTwo = new String[]{"c1", "c2"};
        String epl = "insert into ABC select distinct theString as c1, first(intPrimitive) as c2 from SupportBean#time_batch(1 second)";
        EPStatement stmtTwo = epService.getEPAdministrator().createEPL(epl);
        stmtTwo.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 2));

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(1000));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fieldsTwo, new Object[][]{{"E1", 1}, {"E2", 1}});

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(2000));
        assertFalse(listener.isInvoked());

        stmtTwo.destroy();
    }

    private void runAssertionBatchWindowJoin(EPServiceProvider epService) {
        String[] fields = new String[]{"theString", "intPrimitive"};
        String statementText = "select distinct theString, intPrimitive from SupportBean#length_batch(3) a, SupportBean_A#keepall b where a.theString = b.id";
        EPStatement stmt = epService.getEPAdministrator().createEPL(statementText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean_A("E1"));
        epService.getEPRuntime().sendEvent(new SupportBean_A("E2"));

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 2));
        EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), fields, new Object[][]{{"E1", 1}, {"E2", 2}});

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 2));
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 2));
        EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), fields, new Object[][]{{"E2", 2}, {"E1", 1}});

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 3));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 3));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 3));
        EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), fields, new Object[][]{{"E2", 3}});

        stmt.destroy();
    }

    private void runAssertionBatchWindowInsertInto(EPServiceProvider epService) {
        String[] fields = new String[]{"theString", "intPrimitive"};
        String statementText = "insert into MyStream select distinct theString, intPrimitive from SupportBean#length_batch(3)";
        epService.getEPAdministrator().createEPL(statementText);

        statementText = "select * from MyStream";
        EPStatement stmt = epService.getEPAdministrator().createEPL(statementText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E1", 1});

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 2));
        epService.getEPRuntime().sendEvent(new SupportBean("E3", 3));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 2));
        EPAssertionUtil.assertProps(listener.getNewDataListFlattened()[0], fields, new Object[]{"E2", 2});
        EPAssertionUtil.assertProps(listener.getNewDataListFlattened()[1], fields, new Object[]{"E3", 3});

        stmt.destroy();
    }

    private void tryAssertionOutputEvery(EPServiceProvider epService, SupportUpdateListener listener, EPStatement stmt, String[] fields) {
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, new Object[][]{{"E1", 1}});
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 2));
        EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), fields, new Object[][]{{"E1", 1}, {"E2", 2}});
        listener.reset();

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 2));
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 2));
        EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), fields, new Object[][]{{"E2", 2}, {"E1", 1}});
        listener.reset();

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 3));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 3));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 3));
        EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), fields, new Object[][]{{"E2", 3}});
        listener.reset();
    }

    private void tryAssertionSimpleColumn(EPServiceProvider epService, SupportUpdateListener listener, EPStatement stmt, String[] fields) {
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, new Object[][]{{"E1", 1}});
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E1", 1});

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, new Object[][]{{"E1", 1}});
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E1", 1});

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 1));
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, new Object[][]{{"E1", 1}, {"E2", 1}});
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E2", 1});

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 2));
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, new Object[][]{{"E1", 1}, {"E2", 1}, {"E1", 2}});
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E1", 2});

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 2));
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, new Object[][]{{"E1", 1}, {"E2", 1}, {"E1", 2}, {"E2", 2}});
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E2", 2});

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 2));
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, new Object[][]{{"E1", 1}, {"E2", 1}, {"E1", 2}, {"E2", 2}});
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E2", 2});

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, new Object[][]{{"E1", 1}, {"E2", 1}, {"E1", 2}, {"E2", 2}});
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E1", 1});
    }

    private void tryAssertionSnapshotColumn(EPServiceProvider epService, SupportUpdateListener listener, EPStatement stmt, String[] fields) {
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, new Object[][]{{"E1", 1}});
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 2));
        EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), fields, new Object[][]{{"E1", 1}, {"E2", 2}});
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, new Object[][]{{"E1", 1}, {"E2", 2}});

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 2));
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 2));
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, new Object[][]{{"E1", 1}, {"E2", 2}});
        EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), fields, new Object[][]{{"E1", 1}, {"E2", 2}});
        listener.reset();

        epService.getEPRuntime().sendEvent(new SupportBean("E3", 3));
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 2));
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, new Object[][]{{"E1", 1}, {"E2", 2}, {"E3", 3}});
        EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), fields, new Object[][]{{"E1", 1}, {"E2", 2}, {"E3", 3}});
        listener.reset();
    }

    private void sendMapEvent(EPServiceProvider epService, String s, int i) {
        Map<String, Object> def = new HashMap<>();
        def.put("k1", s);
        def.put("v1", i);
        epService.getEPRuntime().sendEvent(def, "MyMapType");
    }
}