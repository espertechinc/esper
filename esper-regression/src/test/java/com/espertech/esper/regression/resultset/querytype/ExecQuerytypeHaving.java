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

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.soda.*;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBeanString;
import com.espertech.esper.supportregression.bean.SupportMarketDataBean;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.util.SerializableObjectCopier;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.*;

public class ExecQuerytypeHaving implements RegressionExecution {
    private final static String SYMBOL_DELL = "DELL";

    public void configure(Configuration configuration) throws Exception {
        configuration.getEngineDefaults().getLogging().setEnableCode(true);
    }

    public void run(EPServiceProvider epService) throws Exception {
        runAssertionHavingWildcardSelect(epService);
        runAssertionStatementOM(epService);
        runAssertionStatement(epService);
        runAssertionStatementJoin(epService);
        runAssertionSumHavingNoAggregatedProp(epService);
        runAssertionNoAggregationJoinHaving(epService);
        runAssertionNoAggregationJoinWhere(epService);
        runAssertionSubstreamSelectHaving(epService);
        runAssertionHavingSum(epService);
        runAssertionHavingSumIStream(epService);
    }

    private void runAssertionHavingWildcardSelect(EPServiceProvider epService) {
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);
        String epl = "select * " +
                "from SupportBean#length_batch(2) " +
                "where intPrimitive>0 " +
                "having count(*)=2";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 0));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 0));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean("E3", 1));
        epService.getEPRuntime().sendEvent(new SupportBean("E4", 1));
        assertTrue(listener.getAndClearIsInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean("E3", 0));
        epService.getEPRuntime().sendEvent(new SupportBean("E4", 1));
        assertFalse(listener.getAndClearIsInvoked());

        stmt.destroy();
    }

    private void runAssertionStatementOM(EPServiceProvider epService) throws Exception {
        EPStatementObjectModel model = new EPStatementObjectModel();
        model.setSelectClause(SelectClause.create("symbol", "price").streamSelector(StreamSelector.RSTREAM_ISTREAM_BOTH).add(Expressions.avg("price"), "avgPrice"));
        model.setFromClause(FromClause.create(FilterStream.create(SupportMarketDataBean.class.getName()).addView("length", Expressions.constant(5))));
        model.setHavingClause(Expressions.lt(Expressions.property("price"), Expressions.avg("price")));
        model = (EPStatementObjectModel) SerializableObjectCopier.copy(model);

        String epl = "select irstream symbol, price, avg(price) as avgPrice " +
                "from " + SupportMarketDataBean.class.getName() + "#length(5) " +
                "having price<avg(price)";
        assertEquals(epl, model.toEPL());

        EPStatement stmt = epService.getEPAdministrator().create(model);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        tryAssertion(epService, listener, stmt);

        stmt.destroy();
    }

    private void runAssertionStatement(EPServiceProvider epService) {
        String epl = "select irstream symbol, price, avg(price) as avgPrice " +
                "from " + SupportMarketDataBean.class.getName() + "#length(5) " +
                "having price < avg(price)";

        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        tryAssertion(epService, listener, stmt);

        stmt.destroy();
    }

    private void runAssertionStatementJoin(EPServiceProvider epService) {
        String epl = "select irstream symbol, price, avg(price) as avgPrice " +
                "from " + SupportBeanString.class.getName() + "#length(100) as one, " +
                SupportMarketDataBean.class.getName() + "#length(5) as two " +
                "where one.theString = two.symbol " +
                "having price < avg(price)";

        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBeanString(SYMBOL_DELL));

        tryAssertion(epService, listener, stmt);

        stmt.destroy();
    }

    private void runAssertionSumHavingNoAggregatedProp(EPServiceProvider epService) {
        String epl = "select irstream symbol, price, avg(price) as avgPrice " +
                "from " + SupportMarketDataBean.class.getName() + "#length(5) as two " +
                "having volume < avg(price)";
        epService.getEPAdministrator().createEPL(epl).destroy();
    }

    private void runAssertionNoAggregationJoinHaving(EPServiceProvider epService) {
        runNoAggregationJoin(epService, "having");
    }

    private void runAssertionNoAggregationJoinWhere(EPServiceProvider epService) {
        runNoAggregationJoin(epService, "where");
    }

    private void runAssertionSubstreamSelectHaving(EPServiceProvider epService) {
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);
        String stmtText = "insert into MyStream select quote.* from SupportBean#length(14) quote having avg(intPrimitive) >= 3\n";
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("abc", 2));
        assertFalse(listener.isInvoked());
        epService.getEPRuntime().sendEvent(new SupportBean("abc", 2));
        assertFalse(listener.isInvoked());
        epService.getEPRuntime().sendEvent(new SupportBean("abc", 3));
        assertFalse(listener.isInvoked());
        epService.getEPRuntime().sendEvent(new SupportBean("abc", 5));
        assertTrue(listener.isInvoked());

        stmt.destroy();
    }

    private void runNoAggregationJoin(EPServiceProvider epService, String filterClause) {
        String epl = "select irstream a.price as aPrice, b.price as bPrice, Math.max(a.price, b.price) - Math.min(a.price, b.price) as spread " +
                "from " + SupportMarketDataBean.class.getName() + "(symbol='SYM1')#length(1) as a, " +
                SupportMarketDataBean.class.getName() + "(symbol='SYM2')#length(1) as b " +
                filterClause + " Math.max(a.price, b.price) - Math.min(a.price, b.price) >= 1.4";

        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        sendPriceEvent(epService, "SYM1", 20);
        assertFalse(listener.isInvoked());

        sendPriceEvent(epService, "SYM2", 10);
        assertNewSpreadEvent(listener, 20, 10, 10);

        sendPriceEvent(epService, "SYM2", 20);
        assertOldSpreadEvent(listener, 20, 10, 10);

        sendPriceEvent(epService, "SYM2", 20);
        sendPriceEvent(epService, "SYM2", 20);
        sendPriceEvent(epService, "SYM1", 20);
        assertFalse(listener.isInvoked());

        sendPriceEvent(epService, "SYM1", 18.7);
        assertFalse(listener.isInvoked());

        sendPriceEvent(epService, "SYM2", 20);
        assertFalse(listener.isInvoked());

        sendPriceEvent(epService, "SYM1", 18.5);
        assertNewSpreadEvent(listener, 18.5, 20, 1.5d);

        sendPriceEvent(epService, "SYM2", 16);
        assertOldNewSpreadEvent(listener, 18.5, 20, 1.5d, 18.5, 16, 2.5d);

        sendPriceEvent(epService, "SYM1", 12);
        assertOldNewSpreadEvent(listener, 18.5, 16, 2.5d, 12, 16, 4);

        stmt.destroy();
    }

    private void assertOldNewSpreadEvent(SupportUpdateListener listener, double oldaprice, double oldbprice, double oldspread,
                                         double newaprice, double newbprice, double newspread) {
        assertEquals(1, listener.getOldDataList().size());
        assertEquals(1, listener.getLastOldData().length);
        assertEquals(1, listener.getNewDataList().size());   // since event null is put into the list
        assertEquals(1, listener.getLastNewData().length);

        EventBean oldEvent = listener.getLastOldData()[0];
        EventBean newEvent = listener.getLastNewData()[0];

        compareSpreadEvent(oldEvent, oldaprice, oldbprice, oldspread);
        compareSpreadEvent(newEvent, newaprice, newbprice, newspread);

        listener.reset();
    }

    private void assertOldSpreadEvent(SupportUpdateListener listener, double aprice, double bprice, double spread) {
        assertEquals(1, listener.getOldDataList().size());
        assertEquals(1, listener.getLastOldData().length);
        assertEquals(1, listener.getNewDataList().size());   // since event null is put into the list
        Assert.assertNull(listener.getLastNewData());

        EventBean theEvent = listener.getLastOldData()[0];

        compareSpreadEvent(theEvent, aprice, bprice, spread);
        listener.reset();
    }

    private void assertNewSpreadEvent(SupportUpdateListener listener, double aprice, double bprice, double spread) {
        assertEquals(1, listener.getNewDataList().size());
        assertEquals(1, listener.getLastNewData().length);
        assertEquals(1, listener.getOldDataList().size());
        Assert.assertNull(listener.getLastOldData());

        EventBean theEvent = listener.getLastNewData()[0];
        compareSpreadEvent(theEvent, aprice, bprice, spread);
        listener.reset();
    }

    private void compareSpreadEvent(EventBean theEvent, double aprice, double bprice, double spread) {
        assertEquals(aprice, theEvent.get("aPrice"));
        assertEquals(bprice, theEvent.get("bPrice"));
        assertEquals(spread, theEvent.get("spread"));
    }

    private void sendPriceEvent(EPServiceProvider epService, String symbol, double price) {
        epService.getEPRuntime().sendEvent(new SupportMarketDataBean(symbol, price, -1L, null));
    }

    private void tryAssertion(EPServiceProvider epService, SupportUpdateListener listener, EPStatement stmt) {
        // assert select result type
        assertEquals(String.class, stmt.getEventType().getPropertyType("symbol"));
        assertEquals(Double.class, stmt.getEventType().getPropertyType("price"));
        assertEquals(Double.class, stmt.getEventType().getPropertyType("avgPrice"));

        sendEvent(epService, SYMBOL_DELL, 10);
        assertFalse(listener.isInvoked());

        sendEvent(epService, SYMBOL_DELL, 5);
        assertNewEvents(listener, SYMBOL_DELL, 5d, 7.5d);

        sendEvent(epService, SYMBOL_DELL, 15);
        assertFalse(listener.isInvoked());

        sendEvent(epService, SYMBOL_DELL, 8);  // avg = (10 + 5 + 15 + 8) / 4 = 38/4=9.5
        assertNewEvents(listener, SYMBOL_DELL, 8d, 9.5d);

        sendEvent(epService, SYMBOL_DELL, 10);  // avg = (10 + 5 + 15 + 8 + 10) / 5 = 48/5=9.5
        assertFalse(listener.isInvoked());

        sendEvent(epService, SYMBOL_DELL, 6);  // avg = (5 + 15 + 8 + 10 + 6) / 5 = 44/5=8.8
        // no old event posted, old event falls above current avg price
        assertNewEvents(listener, SYMBOL_DELL, 6d, 8.8d);

        sendEvent(epService, SYMBOL_DELL, 12);  // avg = (15 + 8 + 10 + 6 + 12) / 5 = 51/5=10.2
        assertOldEvents(listener, SYMBOL_DELL, 5d, 10.2d);
    }

    private void runAssertionHavingSum(EPServiceProvider epService) {
        String epl = "select irstream sum(myEvent.intPrimitive) as mysum from pattern [every myEvent=" + SupportBean.class.getName() +
                "] having sum(myEvent.intPrimitive) = 2";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        sendEvent(epService, 1);
        assertFalse(listener.isInvoked());

        sendEvent(epService, 1);
        assertEquals(2, listener.assertOneGetNewAndReset().get("mysum"));

        sendEvent(epService, 1);
        assertEquals(2, listener.assertOneGetOldAndReset().get("mysum"));
    }

    private void runAssertionHavingSumIStream(EPServiceProvider epService) {
        String epl = "select istream sum(myEvent.intPrimitive) as mysum from pattern [every myEvent=" + SupportBean.class.getName() +
                "] having sum(myEvent.intPrimitive) = 2";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        sendEvent(epService, 1);
        assertFalse(listener.isInvoked());

        sendEvent(epService, 1);
        assertEquals(2, listener.assertOneGetNewAndReset().get("mysum"));

        sendEvent(epService, 1);
        assertFalse(listener.isInvoked());
    }

    private void assertNewEvents(SupportUpdateListener listener, String symbol,
                                 Double newPrice, Double newAvgPrice
    ) {
        EventBean[] oldData = listener.getLastOldData();
        EventBean[] newData = listener.getLastNewData();

        assertNull(oldData);
        assertEquals(1, newData.length);

        assertEquals(symbol, newData[0].get("symbol"));
        assertEquals(newPrice, newData[0].get("price"));
        assertEquals(newAvgPrice, newData[0].get("avgPrice"));

        listener.reset();
    }

    private void assertOldEvents(SupportUpdateListener listener, String symbol,
                                 Double oldPrice, Double oldAvgPrice
    ) {
        EventBean[] oldData = listener.getLastOldData();
        EventBean[] newData = listener.getLastNewData();

        assertNull(newData);
        assertEquals(1, oldData.length);

        assertEquals(symbol, oldData[0].get("symbol"));
        assertEquals(oldPrice, oldData[0].get("price"));
        assertEquals(oldAvgPrice, oldData[0].get("avgPrice"));

        listener.reset();
    }

    private void sendEvent(EPServiceProvider epService, int intPrimitive) {
        SupportBean bean = new SupportBean();
        bean.setIntPrimitive(intPrimitive);
        epService.getEPRuntime().sendEvent(bean);
    }

    private void sendEvent(EPServiceProvider epService, String symbol, double price) {
        SupportMarketDataBean bean = new SupportMarketDataBean(symbol, price, 0L, null);
        epService.getEPRuntime().sendEvent(bean);
    }

    private static final Logger log = LoggerFactory.getLogger(ExecQuerytypeHaving.class);
}
