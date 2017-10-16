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
package com.espertech.esper.regression.resultset.querytype;

import com.espertech.esper.client.*;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.time.CurrentTimeEvent;
import com.espertech.esper.collection.UniformPair;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.supportregression.bean.*;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import static com.espertech.esper.supportregression.util.SupportMessageAssertUtil.tryInvalid;
import static org.junit.Assert.*;

public class ExecQuerytypeRowPerGroup implements RegressionExecution {
    private final static String SYMBOL_DELL = "DELL";
    private final static String SYMBOL_IBM = "IBM";

    public void configure(Configuration configuration) throws Exception {
        configuration.getEngineDefaults().getViewResources().setAllowMultipleExpiryPolicies(true);
    }

    public void run(EPServiceProvider epService) throws Exception {
        runAssertionCriteriaByDotMethod(epService);
        runAssertionUnboundStreamIterate(epService);
        runAssertionNamedWindowDelete(epService);
        if (!InstrumentationHelper.ENABLED) {
            runAssertionUnboundStreamUnlimitedKey(epService);
        }
        runAssertionAggregateGroupedProps(epService);
        runAssertionAggregateGroupedPropsPerGroup(epService);
        runAssertionAggregationOverGroupedProps(epService);
        runAssertionSumOneView(epService);
        runAssertionSumJoin(epService);
        runAssertionUniqueInBatch(epService);
        runAssertionSelectAvgExprGroupBy(epService);
    }

    private void runAssertionSelectAvgExprGroupBy(EPServiceProvider epService) {
        String stmtText = "select istream avg(price) as aprice, symbol from " + SupportMarketDataBean.class.getName()
                + "#length(2) group by symbol";
        EPStatement statement = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);
        String[] fields = "aprice,symbol".split(",");

        sendEvent(epService, "A", 1);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {1.0, "A"});

        sendEvent(epService, "B", 3);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {3.0, "B"});

        sendEvent(epService, "B", 5);
        EPAssertionUtil.assertPropsPerRowAnyOrder(listener.getAndResetLastNewData(), fields, new Object[][] {{null, "A"}, {4.0, "B"}});

        sendEvent(epService, "A", 10);
        EPAssertionUtil.assertPropsPerRowAnyOrder(listener.getAndResetLastNewData(), fields, new Object[][] {{10.0, "A"}, {5.0, "B"}});

        sendEvent(epService, "A", 20);
        EPAssertionUtil.assertPropsPerRowAnyOrder(listener.getAndResetLastNewData(), fields, new Object[][] {{15.0, "A"}, {null, "B"}});

        statement.destroy();
    }

    private void runAssertionCriteriaByDotMethod(EPServiceProvider epService) {
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);
        String epl = "select sb.getTheString() as c0, sum(intPrimitive) as c1 from SupportBean#length_batch(2) as sb group by sb.getTheString()";
        SupportUpdateListener listener = new SupportUpdateListener();
        epService.getEPAdministrator().createEPL(epl).addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 10));
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 20));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "c0,c1".split(","), new Object[]{"E1", 30});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionUnboundStreamIterate(EPServiceProvider epService) {
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);
        String[] fields = "c0,c1".split(",");

        // with output snapshot
        EPStatement stmt = epService.getEPAdministrator().createEPL("select theString as c0, sum(intPrimitive) as c1 from SupportBean group by theString " +
                "output snapshot every 3 events");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 10));
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"E1", 10}});
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 20));
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"E1", 10}, {"E2", 20}});
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 11));
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"E1", 21}, {"E2", 20}});
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][]{{"E1", 21}, {"E2", 20}});

        epService.getEPRuntime().sendEvent(new SupportBean("E0", 30));
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"E1", 21}, {"E2", 20}, {"E0", 30}});
        assertFalse(listener.isInvoked());

        stmt.destroy();

        // with order-by
        stmt = epService.getEPAdministrator().createEPL("select theString as c0, sum(intPrimitive) as c1 from SupportBean group by theString " +
                "output snapshot every 3 events order by theString asc");
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 10));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 20));
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 11));
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"E1", 21}, {"E2", 20}});
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][]{{"E1", 21}, {"E2", 20}});

        epService.getEPRuntime().sendEvent(new SupportBean("E0", 30));
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"E0", 30}, {"E1", 21}, {"E2", 20}});
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean("E3", 40));
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"E0", 30}, {"E1", 21}, {"E2", 20}, {"E3", 40}});
        assertFalse(listener.isInvoked());

        stmt.destroy();

        // test un-grouped case
        stmt = epService.getEPAdministrator().createEPL("select null as c0, sum(intPrimitive) as c1 from SupportBean output snapshot every 3 events");
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 10));
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{null, 10}});
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 20));
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{null, 30}});
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 11));
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{null, 41}});
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][]{{null, 41}});

        stmt.destroy();

        // test reclaim
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(1000));
        stmt = epService.getEPAdministrator().createEPL("@Hint('reclaim_group_aged=1,reclaim_group_freq=1') select theString as c0, sum(intPrimitive) as c1 from SupportBean group by theString " +
                "output snapshot every 3 events");
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 10));

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(1500));
        epService.getEPRuntime().sendEvent(new SupportBean("E0", 11));

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(1800));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 12));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][]{{"E1", 10}, {"E0", 11}, {"E2", 12}});
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"E1", 10}, {"E0", 11}, {"E2", 12}});

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(2200));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 13));
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"E0", 11}, {"E2", 25}});

        stmt.destroy();
    }

    private void runAssertionNamedWindowDelete(EPServiceProvider epService) {
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean_A", SupportBean_A.class);
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean_B", SupportBean_B.class);
        epService.getEPAdministrator().createEPL("create window MyWindow#keepall as select * from SupportBean");
        epService.getEPAdministrator().createEPL("insert into MyWindow select * from SupportBean");
        epService.getEPAdministrator().createEPL("on SupportBean_A a delete from MyWindow w where w.theString = a.id");
        epService.getEPAdministrator().createEPL("on SupportBean_B delete from MyWindow");

        String[] fields = "theString,mysum".split(",");
        String epl = "@Hint('DISABLE_RECLAIM_GROUP') select theString, sum(intPrimitive) as mysum from MyWindow group by theString order by theString";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        tryAssertionNamedWindowDelete(epService, listener, stmt, fields);

        stmt.destroy();
        epService.getEPRuntime().sendEvent(new SupportBean_B("delete"));

        epl = "select theString, sum(intPrimitive) as mysum from MyWindow group by theString order by theString";
        stmt = epService.getEPAdministrator().createEPL(epl);
        stmt.addListener(listener);

        tryAssertionNamedWindowDelete(epService, listener, stmt, fields);

        stmt.destroy();
    }

    private void runAssertionUnboundStreamUnlimitedKey(EPServiceProvider epService) {
        // ESPER-396 Unbound stream and aggregating/grouping by unlimited key (i.e. timestamp) configurable state drop
        sendTimer(epService, 0);

        // After the oldest group is 60 second old, reclaim group older then  30 seconds
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);
        EPStatement stmtOne = epService.getEPAdministrator().createEPL("@Hint('reclaim_group_aged=30,reclaim_group_freq=5') select longPrimitive, count(*) from SupportBean group by longPrimitive");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmtOne.addListener(listener);

        for (int i = 0; i < 1000; i++) {
            sendTimer(epService, 1000 + i * 1000); // reduce factor if sending more events
            SupportBean theEvent = new SupportBean();
            theEvent.setLongPrimitive(i * 1000);
            epService.getEPRuntime().sendEvent(theEvent);

            //if (i % 100000 == 0)
            //{
            //    System.out.println("Sending event number " + i);
            //}
        }

        listener.reset();

        for (int i = 0; i < 964; i++) {
            SupportBean theEvent = new SupportBean();
            theEvent.setLongPrimitive(i * 1000);
            epService.getEPRuntime().sendEvent(theEvent);
            assertEquals("Failed at " + i, 1L, listener.assertOneGetNewAndReset().get("count(*)"));
        }

        for (int i = 965; i < 1000; i++) {
            SupportBean theEvent = new SupportBean();
            theEvent.setLongPrimitive(i * 1000);
            epService.getEPRuntime().sendEvent(theEvent);
            assertEquals("Failed at " + i, 2L, listener.assertOneGetNewAndReset().get("count(*)"));
        }

        // no frequency provided
        epService.getEPAdministrator().createEPL("@Hint('reclaim_group_aged=30') select longPrimitive, count(*) from SupportBean group by longPrimitive");
        epService.getEPRuntime().sendEvent(new SupportBean());

        epService.getEPAdministrator().createEPL("create variable int myAge = 10");
        epService.getEPAdministrator().createEPL("create variable int myFreq = 10");

        stmtOne.destroy();
        stmtOne = epService.getEPAdministrator().createEPL("@Hint('reclaim_group_aged=myAge,reclaim_group_freq=myFreq') select longPrimitive, count(*) from SupportBean group by longPrimitive");
        stmtOne.addListener(listener);

        for (int i = 0; i < 1000; i++) {
            sendTimer(epService, 2000000 + 1000 + i * 1000); // reduce factor if sending more events
            SupportBean theEvent = new SupportBean();
            theEvent.setLongPrimitive(i * 1000);
            epService.getEPRuntime().sendEvent(theEvent);

            if (i == 500) {
                epService.getEPRuntime().setVariableValue("myAge", 60);
                epService.getEPRuntime().setVariableValue("myFreq", 90);
            }

            /*
            if (i % 100000 == 0)
            {
                System.out.println("Sending event number " + i);
            }
            */
        }

        listener.reset();

        for (int i = 0; i < 900; i++) {
            SupportBean theEvent = new SupportBean();
            theEvent.setLongPrimitive(i * 1000);
            epService.getEPRuntime().sendEvent(theEvent);
            assertEquals("Failed at " + i, 1L, listener.assertOneGetNewAndReset().get("count(*)"));
        }

        for (int i = 900; i < 1000; i++) {
            SupportBean theEvent = new SupportBean();
            theEvent.setLongPrimitive(i * 1000);
            epService.getEPRuntime().sendEvent(theEvent);
            assertEquals("Failed at " + i, 2L, listener.assertOneGetNewAndReset().get("count(*)"));
        }

        stmtOne.destroy();

        // invalid tests
        tryInvalid(epService, "@Hint('reclaim_group_aged=xyz') select longPrimitive, count(*) from SupportBean group by longPrimitive",
                "Error starting statement: Failed to parse hint parameter value 'xyz' as a double-typed seconds value or variable name [@Hint('reclaim_group_aged=xyz') select longPrimitive, count(*) from SupportBean group by longPrimitive]");
        tryInvalid(epService, "@Hint('reclaim_group_aged=30,reclaim_group_freq=xyz') select longPrimitive, count(*) from SupportBean group by longPrimitive",
                "Error starting statement: Failed to parse hint parameter value 'xyz' as a double-typed seconds value or variable name [@Hint('reclaim_group_aged=30,reclaim_group_freq=xyz') select longPrimitive, count(*) from SupportBean group by longPrimitive]");
        epService.getEPAdministrator().getConfiguration().addVariable("MyVar", String.class, "");
        tryInvalid(epService, "@Hint('reclaim_group_aged=MyVar') select longPrimitive, count(*) from SupportBean group by longPrimitive",
                "Error starting statement: Variable type of variable 'MyVar' is not numeric [@Hint('reclaim_group_aged=MyVar') select longPrimitive, count(*) from SupportBean group by longPrimitive]");
        tryInvalid(epService, "@Hint('reclaim_group_aged=-30,reclaim_group_freq=30') select longPrimitive, count(*) from SupportBean group by longPrimitive",
                "Error starting statement: Hint parameter value '-30' is an invalid value, expecting a double-typed seconds value or variable name [@Hint('reclaim_group_aged=-30,reclaim_group_freq=30') select longPrimitive, count(*) from SupportBean group by longPrimitive]");

        /**
         * Test natural timer - long running test to be commented out.
         */
        /*
        epService = EPServiceProviderManager.getProvider(this.getClass().getName());
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);
        EPStatement stmtOne = epService.getEPAdministrator().createEPL("@Hint('reclaim_group_aged=1,reclaim_group_freq=1') select longPrimitive, count(*) from SupportBean group by longPrimitive");

        int count = 0;
        while(true)
        {
            SupportBean event = new SupportBean();
            event.setLongPrimitive(System.currentTimeMillis());
            epService.getEPRuntime().sendEvent(new SupportBean());
            count++;
            if (count % 100000 == 0)
            {
                System.out.println("Sending event number " + count);
            }
        }
        */
    }

    private void tryAssertionNamedWindowDelete(EPServiceProvider epService, SupportUpdateListener listener, EPStatement stmt, String[] fields) {
        epService.getEPRuntime().sendEvent(new SupportBean("A", 100));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"A", 100});

        epService.getEPRuntime().sendEvent(new SupportBean("B", 20));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"B", 20});

        epService.getEPRuntime().sendEvent(new SupportBean("A", 101));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"A", 201});

        epService.getEPRuntime().sendEvent(new SupportBean("B", 21));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"B", 41});
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"A", 201}, {"B", 41}});

        epService.getEPRuntime().sendEvent(new SupportBean_A("A"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"A", null});
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"B", 41}});

        epService.getEPRuntime().sendEvent(new SupportBean("A", 102));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"A", 102});
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"A", 102}, {"B", 41}});

        epService.getEPRuntime().sendEvent(new SupportBean_A("B"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"B", null});
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"A", 102}});

        epService.getEPRuntime().sendEvent(new SupportBean("B", 22));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"B", 22});
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"A", 102}, {"B", 22}});
    }

    private void runAssertionAggregateGroupedProps(EPServiceProvider epService) {
        // test for ESPER-185
        String[] fields = "mycount".split(",");
        String epl = "select irstream count(price) as mycount " +
                "from " + SupportMarketDataBean.class.getName() + "#length(5) " +
                "group by price";

        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        sendEvent(epService, SYMBOL_DELL, 10);
        EPAssertionUtil.assertProps(listener.getLastNewData()[0], fields, new Object[]{1L});
        EPAssertionUtil.assertProps(listener.getLastOldData()[0], fields, new Object[]{0L});
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{1L}});
        listener.reset();

        sendEvent(epService, SYMBOL_DELL, 11);
        EPAssertionUtil.assertProps(listener.getLastNewData()[0], fields, new Object[]{1L});
        EPAssertionUtil.assertProps(listener.getLastOldData()[0], fields, new Object[]{0L});
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{1L}, {1L}});
        listener.reset();

        sendEvent(epService, SYMBOL_IBM, 10);
        EPAssertionUtil.assertProps(listener.getLastNewData()[0], fields, new Object[]{2L});
        EPAssertionUtil.assertProps(listener.getLastOldData()[0], fields, new Object[]{1L});
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{2L}, {1L}});
        listener.reset();

        stmt.destroy();
    }

    private void runAssertionAggregateGroupedPropsPerGroup(EPServiceProvider epService) {
        // test for ESPER-185
        String[] fields = "mycount".split(",");
        String epl = "select irstream count(price) as mycount " +
                "from " + SupportMarketDataBean.class.getName() + "#length(5) " +
                "group by symbol, price";

        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        sendEvent(epService, SYMBOL_DELL, 10);
        EPAssertionUtil.assertProps(listener.getLastNewData()[0], fields, new Object[]{1L});
        EPAssertionUtil.assertProps(listener.getLastOldData()[0], fields, new Object[]{0L});
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{1L}});
        listener.reset();

        sendEvent(epService, SYMBOL_DELL, 11);
        EPAssertionUtil.assertProps(listener.getLastNewData()[0], fields, new Object[]{1L});
        EPAssertionUtil.assertProps(listener.getLastOldData()[0], fields, new Object[]{0L});
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{1L}, {1L}});
        listener.reset();

        sendEvent(epService, SYMBOL_DELL, 10);
        EPAssertionUtil.assertProps(listener.getLastNewData()[0], fields, new Object[]{2L});
        EPAssertionUtil.assertProps(listener.getLastOldData()[0], fields, new Object[]{1L});
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{2L}, {1L}});
        listener.reset();

        sendEvent(epService, SYMBOL_IBM, 10);
        EPAssertionUtil.assertProps(listener.getLastNewData()[0], fields, new Object[]{1L});
        EPAssertionUtil.assertProps(listener.getLastOldData()[0], fields, new Object[]{0L});
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{2L}, {1L}, {1L}});
        listener.reset();

        stmt.destroy();
    }

    private void runAssertionAggregationOverGroupedProps(EPServiceProvider epService) {
        // test for ESPER-185
        String[] fields = "symbol,price,mycount".split(",");
        String epl = "select irstream symbol,price,count(price) as mycount " +
                "from " + SupportMarketDataBean.class.getName() + "#length(5) " +
                "group by symbol, price order by symbol asc";

        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        sendEvent(epService, SYMBOL_DELL, 10);
        EPAssertionUtil.assertProps(listener.getLastNewData()[0], fields, new Object[]{"DELL", 10.0, 1L});
        EPAssertionUtil.assertProps(listener.getLastOldData()[0], fields, new Object[]{"DELL", 10.0, 0L});
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"DELL", 10.0, 1L}});
        listener.reset();

        sendEvent(epService, SYMBOL_DELL, 11);
        EPAssertionUtil.assertProps(listener.getLastNewData()[0], fields, new Object[]{"DELL", 11.0, 1L});
        EPAssertionUtil.assertProps(listener.getLastOldData()[0], fields, new Object[]{"DELL", 11.0, 0L});
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"DELL", 10.0, 1L}, {"DELL", 11.0, 1L}});
        listener.reset();

        sendEvent(epService, SYMBOL_DELL, 10);
        EPAssertionUtil.assertProps(listener.getLastNewData()[0], fields, new Object[]{"DELL", 10.0, 2L});
        EPAssertionUtil.assertProps(listener.getLastOldData()[0], fields, new Object[]{"DELL", 10.0, 1L});
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"DELL", 10.0, 2L}, {"DELL", 11.0, 1L}});
        listener.reset();

        sendEvent(epService, SYMBOL_IBM, 5);
        assertEquals(1, listener.getNewDataList().size());
        EPAssertionUtil.assertProps(listener.getLastNewData()[0], fields, new Object[]{"IBM", 5.0, 1L});
        EPAssertionUtil.assertProps(listener.getLastOldData()[0], fields, new Object[]{"IBM", 5.0, 0L});
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"DELL", 10.0, 2L}, {"DELL", 11.0, 1L}, {"IBM", 5.0, 1L}});
        listener.reset();

        sendEvent(epService, SYMBOL_IBM, 5);
        assertEquals(1, listener.getLastNewData().length);
        EPAssertionUtil.assertProps(listener.getLastNewData()[0], fields, new Object[]{"IBM", 5.0, 2L});
        EPAssertionUtil.assertProps(listener.getLastOldData()[0], fields, new Object[]{"IBM", 5.0, 1L});
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"DELL", 10.0, 2L}, {"DELL", 11.0, 1L}, {"IBM", 5.0, 2L}});
        listener.reset();

        sendEvent(epService, SYMBOL_IBM, 5);
        assertEquals(2, listener.getLastNewData().length);
        EPAssertionUtil.assertProps(listener.getLastNewData()[1], fields, new Object[]{"IBM", 5.0, 3L});
        EPAssertionUtil.assertProps(listener.getLastOldData()[1], fields, new Object[]{"IBM", 5.0, 2L});
        EPAssertionUtil.assertProps(listener.getLastNewData()[0], fields, new Object[]{"DELL", 10.0, 1L});
        EPAssertionUtil.assertProps(listener.getLastOldData()[0], fields, new Object[]{"DELL", 10.0, 2L});
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"DELL", 11.0, 1L}, {"DELL", 10.0, 1L}, {"IBM", 5.0, 3L}});
        listener.reset();

        sendEvent(epService, SYMBOL_IBM, 5);
        assertEquals(2, listener.getLastNewData().length);
        EPAssertionUtil.assertProps(listener.getLastNewData()[1], fields, new Object[]{"IBM", 5.0, 4L});
        EPAssertionUtil.assertProps(listener.getLastOldData()[1], fields, new Object[]{"IBM", 5.0, 3L});
        EPAssertionUtil.assertProps(listener.getLastNewData()[0], fields, new Object[]{"DELL", 11.0, 0L});
        EPAssertionUtil.assertProps(listener.getLastOldData()[0], fields, new Object[]{"DELL", 11.0, 1L});
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"DELL", 10.0, 1L}, {"IBM", 5.0, 4L}});
        listener.reset();

        stmt.destroy();
    }

    private void runAssertionSumOneView(EPServiceProvider epService) {
        String epl = "select irstream symbol," +
                "sum(price) as mySum," +
                "avg(price) as myAvg " +
                "from " + SupportMarketDataBean.class.getName() + "#length(3) " +
                "where symbol='DELL' or symbol='IBM' or symbol='GE' " +
                "group by symbol";

        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        tryAssertionSum(epService, stmt, listener);

        stmt.destroy();
    }

    private void runAssertionSumJoin(EPServiceProvider epService) {
        String epl = "select irstream symbol," +
                "sum(price) as mySum," +
                "avg(price) as myAvg " +
                "from " + SupportBeanString.class.getName() + "#length(100) as one, " +
                SupportMarketDataBean.class.getName() + "#length(3) as two " +
                "where (symbol='DELL' or symbol='IBM' or symbol='GE') " +
                "       and one.theString = two.symbol " +
                "group by symbol";

        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBeanString(SYMBOL_DELL));
        epService.getEPRuntime().sendEvent(new SupportBeanString(SYMBOL_IBM));
        epService.getEPRuntime().sendEvent(new SupportBeanString("AAA"));

        tryAssertionSum(epService, stmt, listener);

        stmt.destroy();
    }

    private void runAssertionUniqueInBatch(EPServiceProvider epService) {
        String stmtOne = "insert into MyStream select symbol, price from " +
                SupportMarketDataBean.class.getName() + "#time_batch(1 sec)";
        epService.getEPAdministrator().createEPL(stmtOne);
        sendTimer(epService, 0);

        String epl = "select symbol " +
                "from MyStream#time_batch(1 sec)#unique(symbol) " +
                "group by symbol";

        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        sendEvent(epService, "IBM", 100);
        sendEvent(epService, "IBM", 101);
        sendEvent(epService, "IBM", 102);
        sendTimer(epService, 1000);
        assertFalse(listener.isInvoked());

        sendTimer(epService, 2000);
        UniformPair<EventBean[]> received = listener.getDataListsFlattened();
        assertEquals("IBM", received.getFirst()[0].get("symbol"));

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void tryAssertionSum(EPServiceProvider epService, EPStatement stmt, SupportUpdateListener listener) {
        String[] fields = new String[]{"symbol", "mySum", "myAvg"};
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, null);

        // assert select result type
        assertEquals(String.class, stmt.getEventType().getPropertyType("symbol"));
        assertEquals(Double.class, stmt.getEventType().getPropertyType("mySum"));
        assertEquals(Double.class, stmt.getEventType().getPropertyType("myAvg"));

        sendEvent(epService, SYMBOL_DELL, 10);
        assertEvents(listener, SYMBOL_DELL,
                null, null,
                10d, 10d);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, new Object[][]{{"DELL", 10d, 10d}});

        sendEvent(epService, SYMBOL_DELL, 20);
        assertEvents(listener, SYMBOL_DELL,
                10d, 10d,
                30d, 15d);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, new Object[][]{{"DELL", 30d, 15d}});

        sendEvent(epService, SYMBOL_DELL, 100);
        assertEvents(listener, SYMBOL_DELL,
                30d, 15d,
                130d, 130d / 3d);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, new Object[][]{{"DELL", 130d, 130d / 3d}});

        sendEvent(epService, SYMBOL_DELL, 50);
        assertEvents(listener, SYMBOL_DELL,
                130d, 130 / 3d,
                170d, 170 / 3d);    // 20 + 100 + 50
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, new Object[][]{{"DELL", 170d, 170d / 3d}});

        sendEvent(epService, SYMBOL_DELL, 5);
        assertEvents(listener, SYMBOL_DELL,
                170d, 170 / 3d,
                155d, 155 / 3d);    // 100 + 50 + 5
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, new Object[][]{{"DELL", 155d, 155d / 3d}});

        sendEvent(epService, "AAA", 1000);
        assertEvents(listener, SYMBOL_DELL,
                155d, 155d / 3,
                55d, 55d / 2);    // 50 + 5
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, new Object[][]{{"DELL", 55d, 55d / 2d}});

        sendEvent(epService, SYMBOL_IBM, 70);
        assertEvents(listener, SYMBOL_DELL,
                55d, 55 / 2d,
                5, 5,
                SYMBOL_IBM,
                null, null,
                70, 70);    // Dell:5
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, new Object[][]{
                {"DELL", 5d, 5d}, {"IBM", 70d, 70d}});

        sendEvent(epService, "AAA", 2000);
        assertEvents(listener, SYMBOL_DELL,
                5d, 5d,
                null, null);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, new Object[][]{
                {"IBM", 70d, 70d}});

        sendEvent(epService, "AAA", 3000);
        assertFalse(listener.isInvoked());

        sendEvent(epService, "AAA", 4000);
        assertEvents(listener, SYMBOL_IBM,
                70d, 70d,
                null, null);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, null);
    }

    private void assertEvents(SupportUpdateListener listener, String symbol,
                              Double oldSum, Double oldAvg,
                              Double newSum, Double newAvg) {
        EventBean[] oldData = listener.getLastOldData();
        EventBean[] newData = listener.getLastNewData();

        assertEquals(1, oldData.length);
        assertEquals(1, newData.length);

        assertEquals(symbol, oldData[0].get("symbol"));
        assertEquals(oldSum, oldData[0].get("mySum"));
        assertEquals(oldAvg, oldData[0].get("myAvg"));

        assertEquals(symbol, newData[0].get("symbol"));
        assertEquals(newSum, newData[0].get("mySum"));
        assertEquals("newData myAvg wrong", newAvg, newData[0].get("myAvg"));

        listener.reset();
        assertFalse(listener.isInvoked());
    }

    private void assertEvents(SupportUpdateListener listener, String symbolOne,
                              Double oldSumOne, Double oldAvgOne,
                              double newSumOne, double newAvgOne,
                              String symbolTwo,
                              Double oldSumTwo, Double oldAvgTwo,
                              double newSumTwo, double newAvgTwo) {
        EPAssertionUtil.assertPropsPerRowAnyOrder(listener.getAndResetDataListsFlattened(),
                "mySum,myAvg".split(","),
                new Object[][]{{newSumOne, newAvgOne}, {newSumTwo, newAvgTwo}},
                new Object[][]{{oldSumOne, oldAvgOne}, {oldSumTwo, oldAvgTwo}});
    }

    private void sendEvent(EPServiceProvider epService, String symbol, double price) {
        SupportMarketDataBean bean = new SupportMarketDataBean(symbol, price, 0L, null);
        epService.getEPRuntime().sendEvent(bean);
    }

    private void sendTimer(EPServiceProvider epService, long timeInMSec) {
        CurrentTimeEvent theEvent = new CurrentTimeEvent(timeInMSec);
        EPRuntime runtime = epService.getEPRuntime();
        runtime.sendEvent(theEvent);
    }
}
