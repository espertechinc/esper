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

package com.espertech.esper.regression.resultset;

import com.espertech.esper.client.*;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.time.CurrentTimeEvent;
import com.espertech.esper.collection.UniformPair;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.support.bean.*;
import com.espertech.esper.support.client.SupportConfigFactory;
import junit.framework.TestCase;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class TestGroupByEventPerGroup extends TestCase
{
    private static String SYMBOL_DELL = "DELL";
    private static String SYMBOL_IBM = "IBM";

    private EPServiceProvider epService;
    private SupportUpdateListener listener;

    public void setUp()
    {
        listener = new SupportUpdateListener();
        Configuration config = SupportConfigFactory.getConfiguration();
        config.getEngineDefaults().getViewResources().setAllowMultipleExpiryPolicies(true);
        epService = EPServiceProviderManager.getDefaultProvider(config);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
    }

    protected void tearDown() throws Exception {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
        listener = null;
    }

    public void testCriteriaByDotMethod() {
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);
        String epl = "select sb.getTheString() as c0, sum(intPrimitive) as c1 from SupportBean.win:length_batch(2) as sb group by sb.getTheString()";
        epService.getEPAdministrator().createEPL(epl).addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 10));
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 20));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "c0,c1".split(","), new Object[] {"E1", 30});
    }

    public void testUnboundStreamIterate() {
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);

        // with output snapshot
        String[] fields = "c0,c1".split(",");
        EPStatement stmt = epService.getEPAdministrator().createEPL("select theString as c0, sum(intPrimitive) as c1 from SupportBean group by theString " +
                "output snapshot every 3 events");
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
    }

    public void testNamedWindowDelete()
    {
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean_A", SupportBean_A.class);
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean_B", SupportBean_B.class);
        epService.getEPAdministrator().createEPL("create window MyWindow.win:keepall() as select * from SupportBean");
        epService.getEPAdministrator().createEPL("insert into MyWindow select * from SupportBean");
        epService.getEPAdministrator().createEPL("on SupportBean_A a delete from MyWindow w where w.theString = a.id");
        epService.getEPAdministrator().createEPL("on SupportBean_B delete from MyWindow");

        String fields[] = "theString,mysum".split(",");
        String viewExpr = "@Hint('DISABLE_RECLAIM_GROUP') select theString, sum(intPrimitive) as mysum from MyWindow group by theString order by theString";
        EPStatement selectTestView = epService.getEPAdministrator().createEPL(viewExpr);
        selectTestView.addListener(listener);

        runAssertion(selectTestView, fields);

        selectTestView.destroy();
        epService.getEPRuntime().sendEvent(new SupportBean_B("delete"));

        viewExpr = "select theString, sum(intPrimitive) as mysum from MyWindow group by theString order by theString";
        selectTestView = epService.getEPAdministrator().createEPL(viewExpr);
        selectTestView.addListener(listener);

        runAssertion(selectTestView, fields);
    }

    public void testUnboundStreamUnlimitedKey()
    {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}

        // ESPER-396 Unbound stream and aggregating/grouping by unlimited key (i.e. timestamp) configurable state drop
        sendTimer(0);

        // After the oldest group is 60 second old, reclaim group older then  30 seconds
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);
        EPStatement stmtOne = epService.getEPAdministrator().createEPL("@Hint('reclaim_group_aged=30,reclaim_group_freq=5') select longPrimitive, count(*) from SupportBean group by longPrimitive");
        stmtOne.addListener(listener);

        for (int i = 0; i < 1000; i++)
        {
            sendTimer(1000 + i * 1000); // reduce factor if sending more events
            SupportBean theEvent = new SupportBean();
            theEvent.setLongPrimitive(i * 1000);
            epService.getEPRuntime().sendEvent(theEvent);

            //if (i % 100000 == 0)
            //{
            //    System.out.println("Sending event number " + i);
            //}
        }
        
        listener.reset();

        for (int i = 0; i < 964; i++)
        {
            SupportBean theEvent = new SupportBean();
            theEvent.setLongPrimitive(i * 1000);
            epService.getEPRuntime().sendEvent(theEvent);
            assertEquals("Failed at " + i, 1L, listener.assertOneGetNewAndReset().get("count(*)"));
        }

        for (int i = 965; i < 1000; i++)
        {
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

        for (int i = 0; i < 1000; i++)
        {
            sendTimer(2000000 + 1000 + i * 1000); // reduce factor if sending more events
            SupportBean theEvent = new SupportBean();
            theEvent.setLongPrimitive(i * 1000);
            epService.getEPRuntime().sendEvent(theEvent);

            if (i == 500)
            {
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

        for (int i = 0; i < 900; i++)
        {
            SupportBean theEvent = new SupportBean();
            theEvent.setLongPrimitive(i * 1000);
            epService.getEPRuntime().sendEvent(theEvent);
            assertEquals("Failed at " + i, 1L, listener.assertOneGetNewAndReset().get("count(*)"));
        }

        for (int i = 900; i < 1000; i++)
        {
            SupportBean theEvent = new SupportBean();
            theEvent.setLongPrimitive(i * 1000);
            epService.getEPRuntime().sendEvent(theEvent);
            assertEquals("Failed at " + i, 2L, listener.assertOneGetNewAndReset().get("count(*)"));
        }        

        stmtOne.destroy();

        // invalid tests
        tryInvalid("@Hint('reclaim_group_aged=xyz') select longPrimitive, count(*) from SupportBean group by longPrimitive",
                   "Error starting statement: Failed to parse hint parameter value 'xyz' as a double-typed seconds value or variable name [@Hint('reclaim_group_aged=xyz') select longPrimitive, count(*) from SupportBean group by longPrimitive]");
        tryInvalid("@Hint('reclaim_group_aged=30,reclaim_group_freq=xyz') select longPrimitive, count(*) from SupportBean group by longPrimitive",
                   "Error starting statement: Failed to parse hint parameter value 'xyz' as a double-typed seconds value or variable name [@Hint('reclaim_group_aged=30,reclaim_group_freq=xyz') select longPrimitive, count(*) from SupportBean group by longPrimitive]");
        epService.getEPAdministrator().getConfiguration().addVariable("MyVar", String.class, "");
        tryInvalid("@Hint('reclaim_group_aged=MyVar') select longPrimitive, count(*) from SupportBean group by longPrimitive",
                   "Error starting statement: Variable type of variable 'MyVar' is not numeric [@Hint('reclaim_group_aged=MyVar') select longPrimitive, count(*) from SupportBean group by longPrimitive]");
        tryInvalid("@Hint('reclaim_group_aged=-30,reclaim_group_freq=30') select longPrimitive, count(*) from SupportBean group by longPrimitive",
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

    private void runAssertion(EPStatement selectTestView, String[] fields)
    {
        epService.getEPRuntime().sendEvent(new SupportBean("A", 100));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"A", 100});

        epService.getEPRuntime().sendEvent(new SupportBean("B", 20));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"B", 20});

        epService.getEPRuntime().sendEvent(new SupportBean("A", 101));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"A", 201});

        epService.getEPRuntime().sendEvent(new SupportBean("B", 21));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"B", 41});
        EPAssertionUtil.assertPropsPerRow(selectTestView.iterator(), fields, new Object[][]{{"A", 201}, {"B", 41}});

        epService.getEPRuntime().sendEvent(new SupportBean_A("A"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"A", null});
        EPAssertionUtil.assertPropsPerRow(selectTestView.iterator(), fields, new Object[][]{{"B", 41}});

        epService.getEPRuntime().sendEvent(new SupportBean("A", 102));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"A", 102});
        EPAssertionUtil.assertPropsPerRow(selectTestView.iterator(), fields, new Object[][]{{"A", 102}, {"B", 41}});

        epService.getEPRuntime().sendEvent(new SupportBean_A("B"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"B", null});
        EPAssertionUtil.assertPropsPerRow(selectTestView.iterator(), fields, new Object[][]{{"A", 102}});

        epService.getEPRuntime().sendEvent(new SupportBean("B", 22));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"B", 22});
        EPAssertionUtil.assertPropsPerRow(selectTestView.iterator(), fields, new Object[][]{{"A", 102}, {"B", 22}});
    }

    public void testAggregateGroupedProps()
    {
        // test for ESPER-185
        String fields[] = "mycount".split(",");
        String viewExpr = "select irstream count(price) as mycount " +
                          "from " + SupportMarketDataBean.class.getName() + ".win:length(5) " +
                          "group by price";

        EPStatement selectTestView = epService.getEPAdministrator().createEPL(viewExpr);
        selectTestView.addListener(listener);

        sendEvent(SYMBOL_DELL, 10);
        EPAssertionUtil.assertProps(listener.getLastNewData()[0], fields, new Object[]{1L});
        EPAssertionUtil.assertProps(listener.getLastOldData()[0], fields, new Object[]{0L});
        EPAssertionUtil.assertPropsPerRow(selectTestView.iterator(), fields, new Object[][]{{1L}});
        listener.reset();

        sendEvent(SYMBOL_DELL, 11);
        EPAssertionUtil.assertProps(listener.getLastNewData()[0], fields, new Object[]{1L});
        EPAssertionUtil.assertProps(listener.getLastOldData()[0], fields, new Object[]{0L});
        EPAssertionUtil.assertPropsPerRow(selectTestView.iterator(), fields, new Object[][]{{1L}, {1L}});
        listener.reset();

        sendEvent(SYMBOL_IBM, 10);
        EPAssertionUtil.assertProps(listener.getLastNewData()[0], fields, new Object[]{2L});
        EPAssertionUtil.assertProps(listener.getLastOldData()[0], fields, new Object[]{1L});
        EPAssertionUtil.assertPropsPerRow(selectTestView.iterator(), fields, new Object[][]{{2L}, {1L}});
        listener.reset();
    }

    public void testAggregateGroupedPropsPerGroup()
    {
        // test for ESPER-185
        String fields[] = "mycount".split(",");
        String viewExpr = "select irstream count(price) as mycount " +
                          "from " + SupportMarketDataBean.class.getName() + ".win:length(5) " +
                          "group by symbol, price";

        EPStatement selectTestView = epService.getEPAdministrator().createEPL(viewExpr);
        selectTestView.addListener(listener);

        sendEvent(SYMBOL_DELL, 10);
        EPAssertionUtil.assertProps(listener.getLastNewData()[0], fields, new Object[]{1L});
        EPAssertionUtil.assertProps(listener.getLastOldData()[0], fields, new Object[]{0L});
        EPAssertionUtil.assertPropsPerRow(selectTestView.iterator(), fields, new Object[][]{{1L}});
        listener.reset();

        sendEvent(SYMBOL_DELL, 11);
        EPAssertionUtil.assertProps(listener.getLastNewData()[0], fields, new Object[]{1L});
        EPAssertionUtil.assertProps(listener.getLastOldData()[0], fields, new Object[]{0L});
        EPAssertionUtil.assertPropsPerRow(selectTestView.iterator(), fields, new Object[][]{{1L}, {1L}});
        listener.reset();

        sendEvent(SYMBOL_DELL, 10);
        EPAssertionUtil.assertProps(listener.getLastNewData()[0], fields, new Object[]{2L});
        EPAssertionUtil.assertProps(listener.getLastOldData()[0], fields, new Object[]{1L});
        EPAssertionUtil.assertPropsPerRow(selectTestView.iterator(), fields, new Object[][]{{2L}, {1L}});
        listener.reset();

        sendEvent(SYMBOL_IBM, 10);
        EPAssertionUtil.assertProps(listener.getLastNewData()[0], fields, new Object[]{1L});
        EPAssertionUtil.assertProps(listener.getLastOldData()[0], fields, new Object[]{0L});
        EPAssertionUtil.assertPropsPerRow(selectTestView.iterator(), fields, new Object[][]{{2L}, {1L}, {1L}});
        listener.reset();
    }

    public void testAggregationOverGroupedProps()
    {
        // test for ESPER-185
        String fields[] = "symbol,price,mycount".split(",");
        String viewExpr = "select irstream symbol,price,count(price) as mycount " +
                          "from " + SupportMarketDataBean.class.getName() + ".win:length(5) " +
                          "group by symbol, price order by symbol asc";

        EPStatement selectTestView = epService.getEPAdministrator().createEPL(viewExpr);
        selectTestView.addListener(listener);

        sendEvent(SYMBOL_DELL, 10);
        EPAssertionUtil.assertProps(listener.getLastNewData()[0], fields, new Object[]{"DELL", 10.0, 1L});
        EPAssertionUtil.assertProps(listener.getLastOldData()[0], fields, new Object[]{"DELL", 10.0, 0L});
        EPAssertionUtil.assertPropsPerRow(selectTestView.iterator(), fields, new Object[][]{{"DELL", 10.0, 1L}});
        listener.reset();

        sendEvent(SYMBOL_DELL, 11);
        EPAssertionUtil.assertProps(listener.getLastNewData()[0], fields, new Object[]{"DELL", 11.0, 1L});
        EPAssertionUtil.assertProps(listener.getLastOldData()[0], fields, new Object[]{"DELL", 11.0, 0L});
        EPAssertionUtil.assertPropsPerRow(selectTestView.iterator(), fields, new Object[][]{{"DELL", 10.0, 1L}, {"DELL", 11.0, 1L}});
        listener.reset();

        sendEvent(SYMBOL_DELL, 10);
        EPAssertionUtil.assertProps(listener.getLastNewData()[0], fields, new Object[]{"DELL", 10.0, 2L});
        EPAssertionUtil.assertProps(listener.getLastOldData()[0], fields, new Object[]{"DELL", 10.0, 1L});
        EPAssertionUtil.assertPropsPerRow(selectTestView.iterator(), fields, new Object[][]{{"DELL", 10.0, 2L}, {"DELL", 11.0, 1L}});
        listener.reset();

        sendEvent(SYMBOL_IBM, 5);
        assertEquals(1, listener.getNewDataList().size());
        EPAssertionUtil.assertProps(listener.getLastNewData()[0], fields, new Object[]{"IBM", 5.0, 1L});
        EPAssertionUtil.assertProps(listener.getLastOldData()[0], fields, new Object[]{"IBM", 5.0, 0L});
        EPAssertionUtil.assertPropsPerRow(selectTestView.iterator(), fields, new Object[][]{{"DELL", 10.0, 2L}, {"DELL", 11.0, 1L}, {"IBM", 5.0, 1L}});
        listener.reset();

        sendEvent(SYMBOL_IBM, 5);
        assertEquals(1, listener.getLastNewData().length);
        EPAssertionUtil.assertProps(listener.getLastNewData()[0], fields, new Object[]{"IBM", 5.0, 2L});
        EPAssertionUtil.assertProps(listener.getLastOldData()[0], fields, new Object[]{"IBM", 5.0, 1L});
        EPAssertionUtil.assertPropsPerRow(selectTestView.iterator(), fields, new Object[][]{{"DELL", 10.0, 2L}, {"DELL", 11.0, 1L}, {"IBM", 5.0, 2L}});
        listener.reset();

        sendEvent(SYMBOL_IBM, 5);
        assertEquals(2, listener.getLastNewData().length);
        EPAssertionUtil.assertProps(listener.getLastNewData()[1], fields, new Object[]{"IBM", 5.0, 3L});
        EPAssertionUtil.assertProps(listener.getLastOldData()[1], fields, new Object[]{"IBM", 5.0, 2L});
        EPAssertionUtil.assertProps(listener.getLastNewData()[0], fields, new Object[]{"DELL", 10.0, 1L});
        EPAssertionUtil.assertProps(listener.getLastOldData()[0], fields, new Object[]{"DELL", 10.0, 2L});
        EPAssertionUtil.assertPropsPerRow(selectTestView.iterator(), fields, new Object[][]{{"DELL", 11.0, 1L}, {"DELL", 10.0, 1L}, {"IBM", 5.0, 3L}});
        listener.reset();

        sendEvent(SYMBOL_IBM, 5);
        assertEquals(2, listener.getLastNewData().length);
        EPAssertionUtil.assertProps(listener.getLastNewData()[1], fields, new Object[]{"IBM", 5.0, 4L});
        EPAssertionUtil.assertProps(listener.getLastOldData()[1], fields, new Object[]{"IBM", 5.0, 3L});
        EPAssertionUtil.assertProps(listener.getLastNewData()[0], fields, new Object[]{"DELL", 11.0, 0L});
        EPAssertionUtil.assertProps(listener.getLastOldData()[0], fields, new Object[]{"DELL", 11.0, 1L});
        EPAssertionUtil.assertPropsPerRow(selectTestView.iterator(), fields, new Object[][]{{"DELL", 10.0, 1L}, {"IBM", 5.0, 4L}});
        listener.reset();
    }

    public void testSumOneView()
    {
        String viewExpr = "select irstream symbol," +
                                 "sum(price) as mySum," +
                                 "avg(price) as myAvg " +
                          "from " + SupportMarketDataBean.class.getName() + ".win:length(3) " +
                          "where symbol='DELL' or symbol='IBM' or symbol='GE' " +
                          "group by symbol";

        EPStatement selectTestView = epService.getEPAdministrator().createEPL(viewExpr);
        selectTestView.addListener(listener);

        runAssertion(selectTestView);
    }

    public void testSumJoin()
    {
        String viewExpr = "select irstream symbol," +
                                 "sum(price) as mySum," +
                                 "avg(price) as myAvg " +
                          "from " + SupportBeanString.class.getName() + ".win:length(100) as one, " +
                                    SupportMarketDataBean.class.getName() + ".win:length(3) as two " +
                          "where (symbol='DELL' or symbol='IBM' or symbol='GE') " +
                          "       and one.theString = two.symbol " +
                          "group by symbol";

        EPStatement selectTestView = epService.getEPAdministrator().createEPL(viewExpr);
        selectTestView.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBeanString(SYMBOL_DELL));
        epService.getEPRuntime().sendEvent(new SupportBeanString(SYMBOL_IBM));
        epService.getEPRuntime().sendEvent(new SupportBeanString("AAA"));

        runAssertion(selectTestView);
    }

    public void testUniqueInBatch()
    {
        String stmtOne = "insert into MyStream select symbol, price from " +
                SupportMarketDataBean.class.getName() + ".win:time_batch(1 sec)";
        epService.getEPAdministrator().createEPL(stmtOne);
        sendTimer(0);

        String viewExpr = "select symbol " +
                          "from MyStream.win:time_batch(1 sec).std:unique(symbol) " +
                          "group by symbol";

        EPStatement selectTestView = epService.getEPAdministrator().createEPL(viewExpr);
        selectTestView.addListener(listener);

        sendEvent("IBM", 100);
        sendEvent("IBM", 101);
        sendEvent("IBM", 102);
        sendTimer(1000);
        assertFalse(listener.isInvoked());

        sendTimer(2000);
        UniformPair<EventBean[]> received = listener.getDataListsFlattened();
        assertEquals("IBM", received.getFirst()[0].get("symbol"));
    }

    private void runAssertion(EPStatement selectTestView)
    {
        String[] fields = new String[] {"symbol", "mySum", "myAvg"};
        EPAssertionUtil.assertPropsPerRowAnyOrder(selectTestView.iterator(), fields, null);

        // assert select result type
        assertEquals(String.class, selectTestView.getEventType().getPropertyType("symbol"));
        assertEquals(Double.class, selectTestView.getEventType().getPropertyType("mySum"));
        assertEquals(Double.class, selectTestView.getEventType().getPropertyType("myAvg"));

        sendEvent(SYMBOL_DELL, 10);
        assertEvents(SYMBOL_DELL,
                null, null,
                10d, 10d);
        EPAssertionUtil.assertPropsPerRowAnyOrder(selectTestView.iterator(), fields, new Object[][]{{"DELL", 10d, 10d}});

        sendEvent(SYMBOL_DELL, 20);
        assertEvents(SYMBOL_DELL,
                10d, 10d,
                30d, 15d);
        EPAssertionUtil.assertPropsPerRowAnyOrder(selectTestView.iterator(), fields, new Object[][]{{"DELL", 30d, 15d}});

        sendEvent(SYMBOL_DELL, 100);
        assertEvents(SYMBOL_DELL,
                30d, 15d,
                130d, 130d/3d);
        EPAssertionUtil.assertPropsPerRowAnyOrder(selectTestView.iterator(), fields, new Object[][]{{"DELL", 130d, 130d / 3d}});

        sendEvent(SYMBOL_DELL, 50);
        assertEvents(SYMBOL_DELL,
                130d, 130/3d,
                170d, 170/3d);    // 20 + 100 + 50
        EPAssertionUtil.assertPropsPerRowAnyOrder(selectTestView.iterator(), fields, new Object[][]{{"DELL", 170d, 170d / 3d}});

        sendEvent(SYMBOL_DELL, 5);
        assertEvents(SYMBOL_DELL,
                170d, 170/3d,
                155d, 155/3d);    // 100 + 50 + 5
        EPAssertionUtil.assertPropsPerRowAnyOrder(selectTestView.iterator(), fields, new Object[][]{{"DELL", 155d, 155d / 3d}});

        sendEvent("AAA", 1000);
        assertEvents(SYMBOL_DELL,
                155d, 155d/3,
                55d, 55d/2);    // 50 + 5
        EPAssertionUtil.assertPropsPerRowAnyOrder(selectTestView.iterator(), fields, new Object[][]{{"DELL", 55d, 55d / 2d}});

        sendEvent(SYMBOL_IBM, 70);
        assertEvents(SYMBOL_DELL,
                55d, 55/2d,
                5, 5,
                SYMBOL_IBM,
                null, null,
                70, 70);    // Dell:5
        EPAssertionUtil.assertPropsPerRowAnyOrder(selectTestView.iterator(), fields, new Object[][]{
                {"DELL", 5d, 5d}, {"IBM", 70d, 70d}});

        sendEvent("AAA", 2000);
        assertEvents(SYMBOL_DELL,
                5d, 5d,
                null, null);
        EPAssertionUtil.assertPropsPerRowAnyOrder(selectTestView.iterator(), fields, new Object[][]{
                {"IBM", 70d, 70d}});

        sendEvent("AAA", 3000);
        assertFalse(listener.isInvoked());

        sendEvent("AAA", 4000);
        assertEvents(SYMBOL_IBM,
                70d, 70d,
                null, null);
        EPAssertionUtil.assertPropsPerRowAnyOrder(selectTestView.iterator(), fields, null);
    }

    private void assertEvents(String symbol,
                              Double oldSum, Double oldAvg,
                              Double newSum, Double newAvg)
    {
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

    private void assertEvents(String symbolOne,
                              Double oldSumOne, Double oldAvgOne,
                              double newSumOne, double newAvgOne,
                              String symbolTwo,
                              Double oldSumTwo, Double oldAvgTwo,
                              double newSumTwo, double newAvgTwo)
    {
        EPAssertionUtil.assertPropsPerRowAnyOrder(listener.getAndResetDataListsFlattened(),
                "mySum,myAvg".split(","),
                new Object[][] {{newSumOne, newAvgOne}, {newSumTwo, newAvgTwo}},
                new Object[][] {{oldSumOne, oldAvgOne}, {oldSumTwo, oldAvgTwo}});
    }

    private void sendEvent(String symbol, double price)
	{
	    SupportMarketDataBean bean = new SupportMarketDataBean(symbol, price, 0L, null);
	    epService.getEPRuntime().sendEvent(bean);
	}

    private void sendTimer(long timeInMSec)
    {
        CurrentTimeEvent theEvent = new CurrentTimeEvent(timeInMSec);
        EPRuntime runtime = epService.getEPRuntime();
        runtime.sendEvent(theEvent);
    }

    private void tryInvalid(String epl, String message)
    {
        try
        {
            epService.getEPAdministrator().createEPL(epl);
            fail();
        }
        catch (EPStatementException ex)
        {
            assertEquals(message, ex.getMessage());
        }
    }

    private static final Log log = LogFactory.getLog(TestGroupByEventPerGroup.class);
}
