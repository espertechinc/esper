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

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.soda.*;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.support.bean.SupportBeanString;
import com.espertech.esper.support.bean.SupportMarketDataBean;
import com.espertech.esper.support.client.SupportConfigFactory;
import com.espertech.esper.util.SerializableObjectCopier;
import junit.framework.TestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestGroupByMaxMin extends TestCase
{
    private static String SYMBOL_DELL = "DELL";
    private static String SYMBOL_IBM = "IBM";

    private EPServiceProvider epService;
    private SupportUpdateListener testListener;

    public void setUp()
    {
        testListener = new SupportUpdateListener();
        epService = EPServiceProviderManager.getDefaultProvider(SupportConfigFactory.getConfiguration());
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
    }

    protected void tearDown() throws Exception {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
        testListener = null;
    }

    public void testMinMaxView()
    {
        String viewExpr = "select irstream symbol, " +
                                  "min(all volume) as minVol," +
                                  "max(all volume) as maxVol," +
                                  "min(distinct volume) as minDistVol," +
                                  "max(distinct volume) as maxDistVol" +
                          " from " + SupportMarketDataBean.class.getName() + ".win:length(3) " +
                          "where symbol='DELL' or symbol='IBM' or symbol='GE' " +
                          "group by symbol";

        EPStatement selectTestView = epService.getEPAdministrator().createEPL(viewExpr);
        selectTestView.addListener(testListener);

        runAssertion(selectTestView);
    }

    public void testMinMaxView_OM() throws Exception
    {
        EPStatementObjectModel model = new EPStatementObjectModel();
        model.setSelectClause(SelectClause.create().streamSelector(StreamSelector.RSTREAM_ISTREAM_BOTH)
            .add("symbol")
            .add(Expressions.min("volume"), "minVol")
            .add(Expressions.max("volume"), "maxVol")
            .add(Expressions.minDistinct("volume"), "minDistVol")
            .add(Expressions.maxDistinct("volume"), "maxDistVol")
            );
        model.setFromClause(FromClause.create(FilterStream.create(SupportMarketDataBean.class.getName()).addView("win", "length", Expressions.constant(3))));
        model.setWhereClause(Expressions.or()
                .add(Expressions.eq("symbol", "DELL"))
                .add(Expressions.eq("symbol", "IBM"))
                .add(Expressions.eq("symbol", "GE")) );
        model.setGroupByClause(GroupByClause.create("symbol"));
        model = (EPStatementObjectModel) SerializableObjectCopier.copy(model);

        String viewExpr = "select irstream symbol, " +
                                  "min(volume) as minVol, " +
                                  "max(volume) as maxVol, " +
                                  "min(distinct volume) as minDistVol, " +
                                  "max(distinct volume) as maxDistVol " +
                          "from " + SupportMarketDataBean.class.getName() + ".win:length(3) " +
                          "where symbol=\"DELL\" or symbol=\"IBM\" or symbol=\"GE\" " +
                          "group by symbol";
        assertEquals(viewExpr, model.toEPL());

        EPStatement selectTestView = epService.getEPAdministrator().create(model);
        selectTestView.addListener(testListener);

        runAssertion(selectTestView);
    }

    public void testMinMaxView_Compile()
    {
        String viewExpr = "select irstream symbol, " +
                                  "min(volume) as minVol, " +
                                  "max(volume) as maxVol, " +
                                  "min(distinct volume) as minDistVol, " +
                                  "max(distinct volume) as maxDistVol " +
                          "from " + SupportMarketDataBean.class.getName() + ".win:length(3) " +
                          "where symbol=\"DELL\" or symbol=\"IBM\" or symbol=\"GE\" " +
                          "group by symbol";
        EPStatementObjectModel model = epService.getEPAdministrator().compileEPL(viewExpr);
        assertEquals(viewExpr, model.toEPL());

        EPStatement selectTestView = epService.getEPAdministrator().create(model);
        selectTestView.addListener(testListener);

        runAssertion(selectTestView);
    }

    public void testMinMaxJoin()
    {
        String viewExpr = "select irstream symbol, " +
                                  "min(volume) as minVol," +
                                  "max(volume) as maxVol," +
                                  "min(distinct volume) as minDistVol," +
                                  "max(distinct volume) as maxDistVol" +
                          " from " + SupportBeanString.class.getName() + ".win:length(100) as one, " +
                                    SupportMarketDataBean.class.getName() + ".win:length(3) as two " +
                          "where (symbol='DELL' or symbol='IBM' or symbol='GE') " +
                          "  and one.theString = two.symbol " +
                          "group by symbol";

        EPStatement selectTestView = epService.getEPAdministrator().createEPL(viewExpr);
        selectTestView.addListener(testListener);

        epService.getEPRuntime().sendEvent(new SupportBeanString(SYMBOL_DELL));
        epService.getEPRuntime().sendEvent(new SupportBeanString(SYMBOL_IBM));

        runAssertion(selectTestView);
    }

    public void testMinNoGroupHaving()
    {
        String stmtText = "select symbol from " + SupportMarketDataBean.class.getName() + ".win:time(5 sec) " +
                          "having volume > min(volume) * 1.3";

        EPStatement selectTestView = epService.getEPAdministrator().createEPL(stmtText);
        selectTestView.addListener(testListener);

        sendEvent("DELL", 100L);
        sendEvent("DELL", 105L);
        sendEvent("DELL", 100L);
        assertFalse(testListener.isInvoked());

        sendEvent("DELL", 131L);
        assertEquals("DELL", testListener.assertOneGetNewAndReset().get("symbol"));

        sendEvent("DELL", 132L);
        assertEquals("DELL", testListener.assertOneGetNewAndReset().get("symbol"));

        sendEvent("DELL", 129L);
        assertFalse(testListener.isInvoked());
    }

    public void testMinNoGroupSelectHaving()
    {
        String stmtText = "select symbol, min(volume) as mymin from " + SupportMarketDataBean.class.getName() + ".win:length(5) " +
                          "having volume > min(volume) * 1.3";

        EPStatement selectTestView = epService.getEPAdministrator().createEPL(stmtText);
        selectTestView.addListener(testListener);

        sendEvent("DELL", 100L);
        sendEvent("DELL", 105L);
        sendEvent("DELL", 100L);
        assertFalse(testListener.isInvoked());

        sendEvent("DELL", 131L);
        EventBean theEvent = testListener.assertOneGetNewAndReset();
        assertEquals("DELL", theEvent.get("symbol"));
        assertEquals(100L, theEvent.get("mymin"));

        sendEvent("DELL", 132L);
        theEvent = testListener.assertOneGetNewAndReset();
        assertEquals("DELL", theEvent.get("symbol"));
        assertEquals(100L, theEvent.get("mymin"));

        sendEvent("DELL", 129L);
        sendEvent("DELL", 125L);
        sendEvent("DELL", 125L);
        assertFalse(testListener.isInvoked());

        sendEvent("DELL", 170L);
        theEvent = testListener.assertOneGetNewAndReset();
        assertEquals("DELL", theEvent.get("symbol"));
        assertEquals(125L, theEvent.get("mymin"));
    }

    private void runAssertion(EPStatement selectTestView)
    {
        // assert select result type
        assertEquals(String.class, selectTestView.getEventType().getPropertyType("symbol"));
        assertEquals(Long.class, selectTestView.getEventType().getPropertyType("minVol"));
        assertEquals(Long.class, selectTestView.getEventType().getPropertyType("maxVol"));
        assertEquals(Long.class, selectTestView.getEventType().getPropertyType("minDistVol"));
        assertEquals(Long.class, selectTestView.getEventType().getPropertyType("maxDistVol"));

        sendEvent(SYMBOL_DELL, 50L);
        assertEvents(SYMBOL_DELL, null, null, null, null,
                SYMBOL_DELL, 50L, 50L, 50L, 50L
                );

        sendEvent(SYMBOL_DELL, 30L);
        assertEvents(SYMBOL_DELL, 50L, 50L, 50L, 50L,
                SYMBOL_DELL, 30L, 50L, 30L, 50L
                );

        sendEvent(SYMBOL_DELL, 30L);
        assertEvents(SYMBOL_DELL, 30L, 50L, 30L, 50L,
                SYMBOL_DELL, 30L, 50L, 30L, 50L
                );

        sendEvent(SYMBOL_DELL, 90L);
        assertEvents(SYMBOL_DELL, 30L, 50L, 30L, 50L,
                SYMBOL_DELL, 30L, 90L, 30L, 90L
                );

        sendEvent(SYMBOL_DELL, 100L);
        assertEvents(SYMBOL_DELL, 30L, 90L, 30L, 90L,
                SYMBOL_DELL, 30L, 100L, 30L, 100L
                );

        sendEvent(SYMBOL_IBM, 20L);
        sendEvent(SYMBOL_IBM, 5L);
        sendEvent(SYMBOL_IBM, 15L);
        sendEvent(SYMBOL_IBM, 18L);
        assertEvents(SYMBOL_IBM, 5L, 20L, 5L, 20L,
                SYMBOL_IBM, 5L, 18L, 5L, 18L
                );

        sendEvent(SYMBOL_IBM, null);
        assertEvents(SYMBOL_IBM, 5L, 18L, 5L, 18L,
                SYMBOL_IBM, 15L, 18L, 15L, 18L
                );

        sendEvent(SYMBOL_IBM, null);
        assertEvents(SYMBOL_IBM, 15L, 18L, 15L, 18L,
                SYMBOL_IBM, 18L, 18L, 18L, 18L
                );

        sendEvent(SYMBOL_IBM, null);
        assertEvents(SYMBOL_IBM, 18L, 18L, 18L, 18L,
                SYMBOL_IBM, null, null, null, null
                );
    }

    private void assertEvents(String symbolOld, Long minVolOld, Long maxVolOld, Long minDistVolOld, Long maxDistVolOld,
                              String symbolNew, Long minVolNew, Long maxVolNew, Long minDistVolNew, Long maxDistVolNew)
    {
        EventBean[] oldData = testListener.getLastOldData();
        EventBean[] newData = testListener.getLastNewData();

        assertEquals(1, oldData.length);
        assertEquals(1, newData.length);

        assertEquals(symbolOld, oldData[0].get("symbol"));
        assertEquals(minVolOld, oldData[0].get("minVol"));
        assertEquals(maxVolOld, oldData[0].get("maxVol"));
        assertEquals(minDistVolOld, oldData[0].get("minDistVol"));
        assertEquals(maxDistVolOld, oldData[0].get("maxDistVol"));

        assertEquals(symbolNew, newData[0].get("symbol"));
        assertEquals(minVolNew, newData[0].get("minVol"));
        assertEquals(maxVolNew, newData[0].get("maxVol"));
        assertEquals(minDistVolNew, newData[0].get("minDistVol"));
        assertEquals(maxDistVolNew, newData[0].get("maxDistVol"));

        testListener.reset();
        assertFalse(testListener.isInvoked());
    }

    private void sendEvent(String symbol, Long volume)
    {
        SupportMarketDataBean bean = new SupportMarketDataBean(symbol, 0, volume, null);
        epService.getEPRuntime().sendEvent(bean);
    }

    private static final Logger log = LoggerFactory.getLogger(TestGroupByMaxMin.class);
}
