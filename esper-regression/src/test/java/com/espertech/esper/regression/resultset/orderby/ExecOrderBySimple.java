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
package com.espertech.esper.regression.resultset.orderby;

import com.espertech.esper.client.*;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.soda.*;
import com.espertech.esper.client.time.CurrentTimeEvent;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBeanString;
import com.espertech.esper.supportregression.bean.SupportMarketDataBean;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.util.SerializableObjectCopier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static org.junit.Assert.*;

public class ExecOrderBySimple implements RegressionExecution {
    private static final Logger log = LoggerFactory.getLogger(ExecOrderBySimple.class);

    public void run(EPServiceProvider epService) throws Exception {
        runAssertionOrderByMultiDelivery(epService);
        runAssertionIterator(epService);
        runAssertionAcrossJoin(epService);
        runAssertionDescending_OM(epService);
        runAssertionDescending(epService);
        runAssertionExpressions(epService);
        runAssertionAliasesSimple(epService);
        runAssertionExpressionsJoin(epService);
        runAssertionMultipleKeys(epService);
        runAssertionAliases(epService);
        runAssertionMultipleKeysJoin(epService);
        runAssertionSimple(epService);
        runAssertionSimpleJoin(epService);
        runAssertionWildcard(epService);
        runAssertionWildcardJoin(epService);
        runAssertionNoOutputClauseView(epService);
        runAssertionNoOutputClauseJoin(epService);
        runAssertionInvalid(epService);
        runAssertionInvalidJoin(epService);
    }

    private void runAssertionOrderByMultiDelivery(EPServiceProvider epService) {
        // test for QWY-933597 or ESPER-409
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(0));
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);

        // try pattern
        SupportUpdateListener listener = new SupportUpdateListener();
        String stmtText = "select a.theString from pattern [every a=SupportBean(theString like 'A%') -> b=SupportBean(theString like 'B%')] order by a.theString desc";
        EPStatement stmtOne = epService.getEPAdministrator().createEPL(stmtText);
        stmtOne.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("A1", 1));
        epService.getEPRuntime().sendEvent(new SupportBean("A2", 2));
        epService.getEPRuntime().sendEvent(new SupportBean("B", 3));

        EventBean[] received = listener.getNewDataListFlattened();
        assertEquals(2, received.length);
        EPAssertionUtil.assertPropsPerRow(received, "a.theString".split(","), new Object[][]{{"A2"}, {"A1"}});

        // try pattern with output limit
        SupportUpdateListener listenerThree = new SupportUpdateListener();
        String stmtTextThree = "select a.theString from pattern [every a=SupportBean(theString like 'A%') -> b=SupportBean(theString like 'B%')] " +
                "output every 2 events order by a.theString desc";
        EPStatement stmtThree = epService.getEPAdministrator().createEPL(stmtTextThree);
        stmtThree.addListener(listenerThree);

        epService.getEPRuntime().sendEvent(new SupportBean("A1", 1));
        epService.getEPRuntime().sendEvent(new SupportBean("A2", 2));
        epService.getEPRuntime().sendEvent(new SupportBean("A3", 3));
        epService.getEPRuntime().sendEvent(new SupportBean("B", 3));

        EventBean[] receivedThree = listenerThree.getNewDataListFlattened();
        assertEquals(2, receivedThree.length);
        EPAssertionUtil.assertPropsPerRow(receivedThree, "a.theString".split(","), new Object[][]{{"A2"}, {"A1"}});

        // try grouped time window
        String stmtTextTwo = "select rstream theString from SupportBean#groupwin(theString)#time(10) order by theString desc";
        EPStatement stmtTwo = epService.getEPAdministrator().createEPL(stmtTextTwo);
        SupportUpdateListener listenerTwo = new SupportUpdateListener();
        stmtTwo.addListener(listenerTwo);

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(1000));
        epService.getEPRuntime().sendEvent(new SupportBean("A1", 1));
        epService.getEPRuntime().sendEvent(new SupportBean("A2", 1));

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(11000));
        EventBean[] receivedTwo = listenerTwo.getNewDataListFlattened();
        assertEquals(2, receivedTwo.length);
        EPAssertionUtil.assertPropsPerRow(receivedTwo, "theString".split(","), new Object[][]{{"A2"}, {"A1"}});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionIterator(EPServiceProvider epService) {
        String statementString = "select symbol, theString, price from " +
                SupportMarketDataBean.class.getName() + "#length(10) as one, " +
                SupportBeanString.class.getName() + "#length(100) as two " +
                "where one.symbol = two.theString " +
                "order by price";
        EPStatement statement = epService.getEPAdministrator().createEPL(statementString);
        sendJoinEvents(epService);
        sendEvent(epService, "CAT", 50);
        sendEvent(epService, "IBM", 49);
        sendEvent(epService, "CAT", 15);
        sendEvent(epService, "IBM", 100);
        EPAssertionUtil.assertPropsPerRow(statement.iterator(), new String[]{"symbol", "theString", "price"},
                new Object[][]{
                        {"CAT", "CAT", 15d},
                        {"IBM", "IBM", 49d},
                        {"CAT", "CAT", 50d},
                        {"IBM", "IBM", 100d},
                });

        sendEvent(epService, "KGB", 75);
        EPAssertionUtil.assertPropsPerRow(statement.iterator(), new String[]{"symbol", "theString", "price"},
                new Object[][]{
                        {"CAT", "CAT", 15d},
                        {"IBM", "IBM", 49d},
                        {"CAT", "CAT", 50d},
                        {"KGB", "KGB", 75d},
                        {"IBM", "IBM", 100d},
                });

        statement.destroy();
    }

    private void runAssertionAcrossJoin(EPServiceProvider epService) {
        String statementString = "select symbol, theString from " +
                SupportMarketDataBean.class.getName() + "#length(10) as one, " +
                SupportBeanString.class.getName() + "#length(100) as two " +
                "where one.symbol = two.theString " +
                "output every 6 events " +
                "order by price";
        SupportUpdateListener listener = new SupportUpdateListener();
        SymbolPricesVolumes spv = new SymbolPricesVolumes();
        createAndSend(epService, statementString, listener);
        sendJoinEvents(epService);
        orderValuesByPriceJoin(spv);
        assertValues(listener, spv.symbols, "symbol");
        assertValues(listener, spv.symbols, "theString");
        assertOnlyProperties(listener, Arrays.asList(new String[]{"symbol", "theString"}));
        clearValuesDropStmt(epService, spv);

        statementString = "select symbol from " +
                SupportMarketDataBean.class.getName() + "#length(10) as one, " +
                SupportBeanString.class.getName() + "#length(100) as two " +
                "where one.symbol = two.theString " +
                "output every 6 events " +
                "order by theString, price";
        createAndSend(epService, statementString, listener);
        sendJoinEvents(epService);
        orderValuesBySymbolPrice(spv);
        assertValues(listener, spv.symbols, "symbol");
        assertOnlyProperties(listener, Arrays.asList(new String[]{"symbol"}));
        clearValuesDropStmt(epService, spv);

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionDescending_OM(EPServiceProvider epService) throws Exception {
        String stmtText = "select symbol from " +
                SupportMarketDataBean.class.getName() + "#length(5) " +
                "output every 6 events " +
                "order by price desc";

        EPStatementObjectModel model = new EPStatementObjectModel();
        model.setSelectClause(SelectClause.create("symbol"));
        model.setFromClause(FromClause.create(FilterStream.create(SupportMarketDataBean.class.getName()).addView("length", Expressions.constant(5))));
        model.setOutputLimitClause(OutputLimitClause.create(6));
        model.setOrderByClause(OrderByClause.create().add("price", true));
        model = (EPStatementObjectModel) SerializableObjectCopier.copy(model);
        assertEquals(stmtText, model.toEPL());

        SupportUpdateListener listener = new SupportUpdateListener();
        EPStatement statement = epService.getEPAdministrator().create(model);
        statement.addListener(listener);
        sendEvent(epService, "IBM", 2);
        sendEvent(epService, "KGB", 1);
        sendEvent(epService, "CMU", 3);
        sendEvent(epService, "IBM", 6);
        sendEvent(epService, "CAT", 6);
        sendEvent(epService, "CAT", 5);

        SymbolPricesVolumes spv = new SymbolPricesVolumes();
        orderValuesByPriceDesc(spv);
        assertValues(listener, spv.symbols, "symbol");

        statement.destroy();
    }

    private void runAssertionDescending(EPServiceProvider epService) {
        String statementString = "select symbol from " +
                SupportMarketDataBean.class.getName() + "#length(5) " +
                "output every 6 events " +
                "order by price desc";
        SupportUpdateListener listener = new SupportUpdateListener();
        createAndSend(epService, statementString, listener);
        SymbolPricesVolumes spv = new SymbolPricesVolumes();
        orderValuesByPriceDesc(spv);
        assertValues(listener, spv.symbols, "symbol");
        clearValuesDropStmt(epService, spv);

        statementString = "select symbol from " +
                SupportMarketDataBean.class.getName() + "#length(5) " +
                "output every 6 events " +
                "order by price desc, symbol asc";
        createAndSend(epService, statementString, listener);
        orderValuesByPrice(spv);
        Collections.reverse(spv.symbols);
        assertValues(listener, spv.symbols, "symbol");
        clearValuesDropStmt(epService, spv);

        statementString = "select symbol from " +
                SupportMarketDataBean.class.getName() + "#length(5) " +
                "output every 6 events " +
                "order by price asc";
        createAndSend(epService, statementString, listener);
        orderValuesByPrice(spv);
        assertValues(listener, spv.symbols, "symbol");
        clearValuesDropStmt(epService, spv);

        statementString = "select symbol, volume from " +
                SupportMarketDataBean.class.getName() + "#length(5) " +
                "output every 6 events " +
                "order by symbol desc";
        createAndSend(epService, statementString, listener);
        orderValuesBySymbol(spv);
        Collections.reverse(spv.symbols);
        assertValues(listener, spv.symbols, "symbol");
        assertValues(listener, spv.volumes, "volume");
        clearValuesDropStmt(epService, spv);

        statementString = "select symbol, price from " +
                SupportMarketDataBean.class.getName() + "#length(5) " +
                "output every 6 events " +
                "order by symbol desc, price desc";
        createAndSend(epService, statementString, listener);
        orderValuesBySymbolPrice(spv);
        Collections.reverse(spv.symbols);
        Collections.reverse(spv.prices);
        assertValues(listener, spv.symbols, "symbol");
        assertValues(listener, spv.prices, "price");
        clearValuesDropStmt(epService, spv);

        statementString = "select symbol, price from " +
                SupportMarketDataBean.class.getName() + "#length(5) " +
                "output every 6 events " +
                "order by symbol, price";
        createAndSend(epService, statementString, listener);
        orderValuesBySymbolPrice(spv);
        assertValues(listener, spv.symbols, "symbol");
        assertValues(listener, spv.prices, "price");
        clearValuesDropStmt(epService, spv);
    }

    private void runAssertionExpressions(EPServiceProvider epService) {
        String statementString = "select symbol from " +
                SupportMarketDataBean.class.getName() + "#length(10) " +
                "output every 6 events " +
                "order by (price * 6) + 5";
        SupportUpdateListener listener = new SupportUpdateListener();
        createAndSend(epService, statementString, listener);
        SymbolPricesVolumes spv = new SymbolPricesVolumes();
        orderValuesByPrice(spv);
        assertValues(listener, spv.symbols, "symbol");
        assertOnlyProperties(listener, Arrays.asList(new String[]{"symbol"}));
        clearValuesDropStmt(epService, spv);

        statementString = "select symbol, price from " +
                SupportMarketDataBean.class.getName() + "#length(10) " +
                "output every 6 events " +
                "order by (price * 6) + 5, price";
        createAndSend(epService, statementString, listener);
        orderValuesByPrice(spv);
        assertValues(listener, spv.symbols, "symbol");
        assertOnlyProperties(listener, Arrays.asList(new String[]{"symbol", "price"}));
        clearValuesDropStmt(epService, spv);

        statementString = "select symbol, 1+volume*23 from " +
                SupportMarketDataBean.class.getName() + "#length(10) " +
                "output every 6 events " +
                "order by (price * 6) + 5, price, volume";
        createAndSend(epService, statementString, listener);
        orderValuesByPrice(spv);
        assertValues(listener, spv.symbols, "symbol");
        assertOnlyProperties(listener, Arrays.asList(new String[]{"symbol", "1+volume*23"}));
        clearValuesDropStmt(epService, spv);

        statementString = "select symbol from " +
                SupportMarketDataBean.class.getName() + "#length(10) " +
                "output every 6 events " +
                "order by volume*price, symbol";
        createAndSend(epService, statementString, listener);
        orderValuesBySymbol(spv);
        assertValues(listener, spv.symbols, "symbol");
        assertOnlyProperties(listener, Arrays.asList(new String[]{"symbol"}));
        clearValuesDropStmt(epService, spv);
    }

    private void runAssertionAliasesSimple(EPServiceProvider epService) {
        String statementString = "select symbol as mySymbol from " +
                SupportMarketDataBean.class.getName() + "#length(5) " +
                "output every 6 events " +
                "order by mySymbol";
        SupportUpdateListener listener = new SupportUpdateListener();
        createAndSend(epService, statementString, listener);
        SymbolPricesVolumes spv = new SymbolPricesVolumes();
        orderValuesBySymbol(spv);
        assertValues(listener, spv.symbols, "mySymbol");
        assertOnlyProperties(listener, Arrays.asList(new String[]{"mySymbol"}));
        clearValuesDropStmt(epService, spv);

        statementString = "select symbol as mySymbol, price as myPrice from " +
                SupportMarketDataBean.class.getName() + "#length(5) " +
                "output every 6 events " +
                "order by myPrice";
        createAndSend(epService, statementString, listener);
        orderValuesByPrice(spv);
        assertValues(listener, spv.symbols, "mySymbol");
        assertValues(listener, spv.prices, "myPrice");
        assertOnlyProperties(listener, Arrays.asList(new String[]{"mySymbol", "myPrice"}));
        clearValuesDropStmt(epService, spv);

        statementString = "select symbol, price as myPrice from " +
                SupportMarketDataBean.class.getName() + "#length(10) " +
                "output every 6 events " +
                "order by (myPrice * 6) + 5, price";
        createAndSend(epService, statementString, listener);
        orderValuesByPrice(spv);
        assertValues(listener, spv.symbols, "symbol");
        assertOnlyProperties(listener, Arrays.asList(new String[]{"symbol", "myPrice"}));
        clearValuesDropStmt(epService, spv);

        statementString = "select symbol, 1+volume*23 as myVol from " +
                SupportMarketDataBean.class.getName() + "#length(10) " +
                "output every 6 events " +
                "order by (price * 6) + 5, price, myVol";
        createAndSend(epService, statementString, listener);
        orderValuesByPrice(spv);
        assertValues(listener, spv.symbols, "symbol");
        assertOnlyProperties(listener, Arrays.asList(new String[]{"symbol", "myVol"}));
        clearValuesDropStmt(epService, spv);
    }

    private void runAssertionExpressionsJoin(EPServiceProvider epService) {
        String statementString = "select symbol from " +
                SupportMarketDataBean.class.getName() + "#length(10) as one, " +
                SupportBeanString.class.getName() + "#length(100) as two " +
                "where one.symbol = two.theString " +
                "output every 6 events " +
                "order by (price * 6) + 5";
        SupportUpdateListener listener = new SupportUpdateListener();
        createAndSend(epService, statementString, listener);
        sendJoinEvents(epService);
        SymbolPricesVolumes spv = new SymbolPricesVolumes();
        orderValuesByPriceJoin(spv);
        assertValues(listener, spv.symbols, "symbol");
        assertOnlyProperties(listener, Arrays.asList(new String[]{"symbol"}));
        clearValuesDropStmt(epService, spv);

        statementString = "select symbol, price from " +
                SupportMarketDataBean.class.getName() + "#length(10) as one, " +
                SupportBeanString.class.getName() + "#length(100) as two " +
                "where one.symbol = two.theString " +
                "output every 6 events " +
                "order by (price * 6) + 5, price";
        createAndSend(epService, statementString, listener);
        sendJoinEvents(epService);
        orderValuesByPriceJoin(spv);
        assertValues(listener, spv.prices, "price");
        assertOnlyProperties(listener, Arrays.asList(new String[]{"symbol", "price"}));
        clearValuesDropStmt(epService, spv);

        statementString = "select symbol, 1+volume*23 from " +
                SupportMarketDataBean.class.getName() + "#length(10) as one, " +
                SupportBeanString.class.getName() + "#length(100) as two " +
                "where one.symbol = two.theString " +
                "output every 6 events " +
                "order by (price * 6) + 5, price, volume";
        createAndSend(epService, statementString, listener);
        sendJoinEvents(epService);
        orderValuesByPriceJoin(spv);
        assertValues(listener, spv.symbols, "symbol");
        assertOnlyProperties(listener, Arrays.asList(new String[]{"symbol", "1+volume*23"}));
        clearValuesDropStmt(epService, spv);

        statementString = "select symbol from " +
                SupportMarketDataBean.class.getName() + "#length(10) as one, " +
                SupportBeanString.class.getName() + "#length(100) as two " +
                "where one.symbol = two.theString " +
                "output every 6 events " +
                "order by volume*price, symbol";
        createAndSend(epService, statementString, listener);
        sendJoinEvents(epService);
        orderValuesBySymbol(spv);
        assertValues(listener, spv.symbols, "symbol");
        assertOnlyProperties(listener, Arrays.asList(new String[]{"symbol"}));
        clearValuesDropStmt(epService, spv);
    }

    private void runAssertionInvalid(EPServiceProvider epService) {
        SupportUpdateListener listener = new SupportUpdateListener();
        String statementString = "select symbol from " +
                SupportMarketDataBean.class.getName() + "#length(5) " +
                "output every 6 events " +
                "order by sum(price)";
        try {
            createAndSend(epService, statementString, listener);
            fail();
        } catch (EPStatementException ex) {
            // expected
        }

        statementString = "select sum(price) from " +
                SupportMarketDataBean.class.getName() + "#length(5) " +
                "output every 6 events " +
                "order by sum(price + 6)";
        try {
            createAndSend(epService, statementString, listener);
            fail();
        } catch (EPStatementException ex) {
            // expected
        }

        statementString = "select sum(price + 6) from " +
                SupportMarketDataBean.class.getName() + "#length(5) " +
                "output every 6 events " +
                "order by sum(price)";
        try {
            createAndSend(epService, statementString, listener);
            fail();
        } catch (EPStatementException ex) {
            // expected
        }
    }

    private void runAssertionInvalidJoin(EPServiceProvider epService) {
        SupportUpdateListener listener = new SupportUpdateListener();
        String statementString = "select symbol from " +
                SupportMarketDataBean.class.getName() + "#length(10) as one, " +
                SupportBeanString.class.getName() + "#length(100) as two " +
                "where one.symbol = two.theString " +
                "output every 6 events " +
                "order by sum(price)";
        try {
            createAndSend(epService, statementString, listener);
            fail();
        } catch (EPStatementException ex) {
            // expected
        }

        statementString = "select sum(price) from " +
                SupportMarketDataBean.class.getName() + "#length(10) as one, " +
                SupportBeanString.class.getName() + "#length(100) as two " +
                "where one.symbol = two.theString " +
                "output every 6 events " +
                "order by sum(price + 6)";
        try {
            createAndSend(epService, statementString, listener);
            fail();
        } catch (EPStatementException ex) {
            // expected
        }

        statementString = "select sum(price + 6) from " +
                SupportMarketDataBean.class.getName() + "#length(10) as one, " +
                SupportBeanString.class.getName() + "#length(100) as two " +
                "where one.symbol = two.theString " +
                "output every 6 events " +
                "order by sum(price)";
        try {
            createAndSend(epService, statementString, listener);
            fail();
        } catch (EPStatementException ex) {
            // expected
        }
    }

    private void runAssertionMultipleKeys(EPServiceProvider epService) {
        String statementString = "select symbol from " +
                SupportMarketDataBean.class.getName() + "#length(10) " +
                "output every 6 events " +
                "order by symbol, price";
        SupportUpdateListener listener = new SupportUpdateListener();
        createAndSend(epService, statementString, listener);
        SymbolPricesVolumes spv = new SymbolPricesVolumes();
        orderValuesBySymbolPrice(spv);
        assertValues(listener, spv.symbols, "symbol");
        assertOnlyProperties(listener, Arrays.asList(new String[]{"symbol"}));
        clearValuesDropStmt(epService, spv);

        statementString = "select symbol from " +
                SupportMarketDataBean.class.getName() + "#length(10) " +
                "output every 6 events " +
                "order by price, symbol, volume";
        createAndSend(epService, statementString, listener);
        orderValuesByPriceSymbol(spv);
        assertValues(listener, spv.symbols, "symbol");
        assertOnlyProperties(listener, Arrays.asList(new String[]{"symbol"}));
        clearValuesDropStmt(epService, spv);

        statementString = "select symbol, volume*2 from " +
                SupportMarketDataBean.class.getName() + "#length(10) " +
                "output every 6 events " +
                "order by price, volume";
        createAndSend(epService, statementString, listener);
        orderValuesByPrice(spv);
        assertValues(listener, spv.symbols, "symbol");
        assertOnlyProperties(listener, Arrays.asList(new String[]{"symbol", "volume*2"}));
        clearValuesDropStmt(epService, spv);
    }

    private void runAssertionAliases(EPServiceProvider epService) {
        String statementString = "select symbol as mySymbol from " +
                SupportMarketDataBean.class.getName() + "#length(5) " +
                "output every 6 events " +
                "order by mySymbol";
        SupportUpdateListener listener = new SupportUpdateListener();
        createAndSend(epService, statementString, listener);
        SymbolPricesVolumes spv = new SymbolPricesVolumes();
        orderValuesBySymbol(spv);
        assertValues(listener, spv.symbols, "mySymbol");
        assertOnlyProperties(listener, Arrays.asList(new String[]{"mySymbol"}));
        clearValuesDropStmt(epService, spv);

        statementString = "select symbol as mySymbol, price as myPrice from " +
                SupportMarketDataBean.class.getName() + "#length(5) " +
                "output every 6 events " +
                "order by myPrice";
        createAndSend(epService, statementString, listener);
        orderValuesByPrice(spv);
        assertValues(listener, spv.symbols, "mySymbol");
        assertValues(listener, spv.prices, "myPrice");
        assertOnlyProperties(listener, Arrays.asList(new String[]{"mySymbol", "myPrice"}));
        clearValuesDropStmt(epService, spv);

        statementString = "select symbol, price as myPrice from " +
                SupportMarketDataBean.class.getName() + "#length(10) " +
                "output every 6 events " +
                "order by (myPrice * 6) + 5, price";
        createAndSend(epService, statementString, listener);
        orderValuesByPrice(spv);
        assertValues(listener, spv.symbols, "symbol");
        assertOnlyProperties(listener, Arrays.asList(new String[]{"symbol", "myPrice"}));
        clearValuesDropStmt(epService, spv);

        statementString = "select symbol, 1+volume*23 as myVol from " +
                SupportMarketDataBean.class.getName() + "#length(10) " +
                "output every 6 events " +
                "order by (price * 6) + 5, price, myVol";
        createAndSend(epService, statementString, listener);
        orderValuesByPrice(spv);
        assertValues(listener, spv.symbols, "symbol");
        assertOnlyProperties(listener, Arrays.asList(new String[]{"symbol", "myVol"}));
        clearValuesDropStmt(epService, spv);

        statementString = "select symbol as mySymbol from " +
                SupportMarketDataBean.class.getName() + "#length(5) " +
                "order by price, mySymbol";
        createAndSend(epService, statementString, listener);
        spv.symbols.add("CAT");
        assertValues(listener, spv.symbols, "mySymbol");
        clearValues(spv);
        sendEvent(epService, "FOX", 10);
        spv.symbols.add("FOX");
        assertValues(listener, spv.symbols, "mySymbol");
        clearValuesDropStmt(epService, spv);
    }

    private void runAssertionMultipleKeysJoin(EPServiceProvider epService) {
        String statementString = "select symbol from " +
                SupportMarketDataBean.class.getName() + "#length(10) as one, " +
                SupportBeanString.class.getName() + "#length(100) as two " +
                "where one.symbol = two.theString " +
                "output every 6 events " +
                "order by symbol, price";
        SupportUpdateListener listener = new SupportUpdateListener();
        SymbolPricesVolumes spv = new SymbolPricesVolumes();
        createAndSend(epService, statementString, listener);
        sendJoinEvents(epService);
        orderValuesBySymbolPrice(spv);
        assertValues(listener, spv.symbols, "symbol");
        assertOnlyProperties(listener, Arrays.asList(new String[]{"symbol"}));
        clearValuesDropStmt(epService, spv);

        statementString = "select symbol from " +
                SupportMarketDataBean.class.getName() + "#length(10) as one, " +
                SupportBeanString.class.getName() + "#length(100) as two " +
                "where one.symbol = two.theString " +
                "output every 6 events " +
                "order by price, symbol, volume";
        createAndSend(epService, statementString, listener);
        sendJoinEvents(epService);
        orderValuesByPriceSymbol(spv);
        assertValues(listener, spv.symbols, "symbol");
        assertOnlyProperties(listener, Arrays.asList(new String[]{"symbol"}));
        clearValuesDropStmt(epService, spv);

        statementString = "select symbol, volume*2 from " +
                SupportMarketDataBean.class.getName() + "#length(10) as one, " +
                SupportBeanString.class.getName() + "#length(100) as two " +
                "where one.symbol = two.theString " +
                "output every 6 events " +
                "order by price, volume";
        createAndSend(epService, statementString, listener);
        sendJoinEvents(epService);
        orderValuesByPriceJoin(spv);
        assertValues(listener, spv.symbols, "symbol");
        assertOnlyProperties(listener, Arrays.asList(new String[]{"symbol", "volume*2"}));
        clearValuesDropStmt(epService, spv);
    }

    private void runAssertionSimple(EPServiceProvider epService) {
        String statementString = "select symbol from " +
                SupportMarketDataBean.class.getName() + "#length(5) " +
                "output every 6 events " +
                "order by price";
        SymbolPricesVolumes spv = new SymbolPricesVolumes();
        SupportUpdateListener listener = new SupportUpdateListener();
        createAndSend(epService, statementString, listener);
        orderValuesByPrice(spv);
        assertValues(listener, spv.symbols, "symbol");
        assertOnlyProperties(listener, Arrays.asList(new String[]{"symbol"}));
        clearValuesDropStmt(epService, spv);

        statementString = "select symbol, price from " +
                SupportMarketDataBean.class.getName() + "#length(5) " +
                "output every 6 events " +
                "order by price";
        createAndSend(epService, statementString, listener);
        orderValuesByPrice(spv);
        assertValues(listener, spv.symbols, "symbol");
        assertValues(listener, spv.prices, "price");
        assertOnlyProperties(listener, Arrays.asList(new String[]{"symbol", "price"}));
        clearValuesDropStmt(epService, spv);

        statementString = "select symbol, volume from " +
                SupportMarketDataBean.class.getName() + "#length(5) " +
                "output every 6 events " +
                "order by price";
        createAndSend(epService, statementString, listener);
        orderValuesByPrice(spv);
        assertValues(listener, spv.symbols, "symbol");
        assertValues(listener, spv.volumes, "volume");
        assertOnlyProperties(listener, Arrays.asList(new String[]{"symbol", "volume"}));
        clearValuesDropStmt(epService, spv);

        statementString = "select symbol, volume*2 from " +
                SupportMarketDataBean.class.getName() + "#length(5) " +
                "output every 6 events " +
                "order by price";
        createAndSend(epService, statementString, listener);
        orderValuesByPrice(spv);
        assertValues(listener, spv.symbols, "symbol");
        assertValues(listener, spv.volumes, "volume*2");
        assertOnlyProperties(listener, Arrays.asList(new String[]{"symbol", "volume*2"}));
        clearValuesDropStmt(epService, spv);

        statementString = "select symbol, volume from " +
                SupportMarketDataBean.class.getName() + "#length(5) " +
                "output every 6 events " +
                "order by symbol";
        createAndSend(epService, statementString, listener);
        orderValuesBySymbol(spv);
        assertValues(listener, spv.symbols, "symbol");
        assertValues(listener, spv.volumes, "volume");
        assertOnlyProperties(listener, Arrays.asList(new String[]{"symbol", "volume"}));
        clearValuesDropStmt(epService, spv);

        statementString = "select price from " +
                SupportMarketDataBean.class.getName() + "#length(5) " +
                "output every 6 events " +
                "order by symbol";
        createAndSend(epService, statementString, listener);
        orderValuesBySymbol(spv);
        assertValues(listener, spv.prices, "price");
        assertOnlyProperties(listener, Arrays.asList(new String[]{"price"}));
        clearValuesDropStmt(epService, spv);
    }

    private void runAssertionSimpleJoin(EPServiceProvider epService) {
        String statementString = "select symbol from " +
                SupportMarketDataBean.class.getName() + "#length(10) as one, " +
                SupportBeanString.class.getName() + "#length(100) as two " +
                "where one.symbol = two.theString " +
                "output every 6 events " +
                "order by price";
        SymbolPricesVolumes spv = new SymbolPricesVolumes();
        SupportUpdateListener listener = new SupportUpdateListener();
        createAndSend(epService, statementString, listener);
        sendJoinEvents(epService);
        orderValuesByPriceJoin(spv);
        assertValues(listener, spv.symbols, "symbol");
        assertOnlyProperties(listener, Arrays.asList(new String[]{"symbol"}));
        clearValuesDropStmt(epService, spv);

        statementString = "select symbol, price from " +
                SupportMarketDataBean.class.getName() + "#length(10) as one, " +
                SupportBeanString.class.getName() + "#length(100) as two " +
                "where one.symbol = two.theString " +
                "output every 6 events " +
                "order by price";
        createAndSend(epService, statementString, listener);
        sendJoinEvents(epService);
        orderValuesByPriceJoin(spv);
        assertValues(listener, spv.symbols, "symbol");
        assertValues(listener, spv.prices, "price");
        assertOnlyProperties(listener, Arrays.asList(new String[]{"symbol", "price"}));
        clearValuesDropStmt(epService, spv);

        statementString = "select symbol, volume from " +
                SupportMarketDataBean.class.getName() + "#length(10) as one, " +
                SupportBeanString.class.getName() + "#length(100) as two " +
                "where one.symbol = two.theString " +
                "output every 6 events " +
                "order by price";
        createAndSend(epService, statementString, listener);
        sendJoinEvents(epService);
        orderValuesByPriceJoin(spv);
        assertValues(listener, spv.symbols, "symbol");
        assertValues(listener, spv.volumes, "volume");
        assertOnlyProperties(listener, Arrays.asList(new String[]{"symbol", "volume"}));
        clearValuesDropStmt(epService, spv);

        statementString = "select symbol, volume*2 from " +
                SupportMarketDataBean.class.getName() + "#length(10) as one, " +
                SupportBeanString.class.getName() + "#length(100) as two " +
                "where one.symbol = two.theString " +
                "output every 6 events " +
                "order by price";
        createAndSend(epService, statementString, listener);
        sendJoinEvents(epService);
        orderValuesByPriceJoin(spv);
        assertValues(listener, spv.symbols, "symbol");
        assertValues(listener, spv.volumes, "volume*2");
        assertOnlyProperties(listener, Arrays.asList(new String[]{"symbol", "volume*2"}));
        clearValuesDropStmt(epService, spv);

        statementString = "select symbol, volume from " +
                SupportMarketDataBean.class.getName() + "#length(10) as one, " +
                SupportBeanString.class.getName() + "#length(100) as two " +
                "where one.symbol = two.theString " +
                "output every 6 events " +
                "order by symbol";
        createAndSend(epService, statementString, listener);
        sendJoinEvents(epService);
        orderValuesBySymbol(spv);
        assertValues(listener, spv.symbols, "symbol");
        assertValues(listener, spv.volumes, "volume");
        assertOnlyProperties(listener, Arrays.asList(new String[]{"symbol", "volume"}));
        clearValuesDropStmt(epService, spv);

        statementString = "select price from " +
                SupportMarketDataBean.class.getName() + "#length(10) as one, " +
                SupportBeanString.class.getName() + "#length(100) as two " +
                "where one.symbol = two.theString " +
                "output every 6 events " +
                "order by symbol, price";
        createAndSend(epService, statementString, listener);
        sendJoinEvents(epService);
        orderValuesBySymbolJoin(spv);
        assertValues(listener, spv.prices, "price");
        assertOnlyProperties(listener, Arrays.asList(new String[]{"price"}));
        clearValuesDropStmt(epService, spv);
    }

    private void runAssertionWildcard(EPServiceProvider epService) {
        String statementString = "select * from " +
                SupportMarketDataBean.class.getName() + "#length(5) " +
                "output every 6 events " +
                "order by price";
        SupportUpdateListener listener = new SupportUpdateListener();
        createAndSend(epService, statementString, listener);
        SymbolPricesVolumes spv = new SymbolPricesVolumes();
        orderValuesByPrice(spv);
        assertValues(listener, spv.symbols, "symbol");
        assertValues(listener, spv.prices, "price");
        assertValues(listener, spv.volumes, "volume");
        assertOnlyProperties(listener, Arrays.asList(new String[]{"symbol", "id", "volume", "price", "feed"}));
        clearValuesDropStmt(epService, spv);

        statementString = "select * from " +
                SupportMarketDataBean.class.getName() + "#length(5) " +
                "output every 6 events " +
                "order by symbol";
        createAndSend(epService, statementString, listener);
        orderValuesBySymbol(spv);
        assertValues(listener, spv.symbols, "symbol");
        assertValues(listener, spv.prices, "price");
        assertValues(listener, spv.volumes, "volume");
        assertOnlyProperties(listener, Arrays.asList(new String[]{"symbol", "volume", "price", "feed", "id"}));
        clearValuesDropStmt(epService, spv);
    }


    private void runAssertionWildcardJoin(EPServiceProvider epService) {
        String statementString = "select * from " +
                SupportMarketDataBean.class.getName() + "#length(10) as one, " +
                SupportBeanString.class.getName() + "#length(100) as two " +
                "where one.symbol = two.theString " +
                "output every 6 events " +
                "order by price";
        SymbolPricesVolumes spv = new SymbolPricesVolumes();
        SupportUpdateListener listener = new SupportUpdateListener();
        createAndSend(epService, statementString, listener);
        sendJoinEvents(epService);
        orderValuesByPriceJoin(spv);
        assertSymbolsJoinWildCard(listener, spv.symbols);
        clearValuesDropStmt(epService, spv);

        statementString = "select * from " +
                SupportMarketDataBean.class.getName() + "#length(10) as one, " +
                SupportBeanString.class.getName() + "#length(100) as two " +
                "where one.symbol = two.theString " +
                "output every 6 events " +
                "order by symbol, price";
        createAndSend(epService, statementString, listener);
        sendJoinEvents(epService);
        orderValuesBySymbolJoin(spv);
        assertSymbolsJoinWildCard(listener, spv.symbols);
        clearValuesDropStmt(epService, spv);
    }

    private void runAssertionNoOutputClauseView(EPServiceProvider epService) {
        SymbolPricesVolumes spv = new SymbolPricesVolumes();
        String statementString = "select symbol from " +
                SupportMarketDataBean.class.getName() + "#length(5) " +
                "order by price";
        SupportUpdateListener listener = new SupportUpdateListener();
        createAndSend(epService, statementString, listener);
        spv.symbols.add("CAT");
        assertValues(listener, spv.symbols, "symbol");
        clearValues(spv);
        sendEvent(epService, "FOX", 10);
        spv.symbols.add("FOX");
        assertValues(listener, spv.symbols, "symbol");
        clearValuesDropStmt(epService, spv);

        // Set start time
        sendTimeEvent(epService, 0);

        statementString = "select symbol from " +
                SupportMarketDataBean.class.getName() + "#time_batch(1 sec) " +
                "order by price";
        createAndSend(epService, statementString, listener);
        orderValuesByPrice(spv);
        sendTimeEvent(epService, 1000);
        assertValues(listener, spv.symbols, "symbol");
        assertOnlyProperties(listener, Arrays.asList(new String[]{"symbol"}));
        clearValuesDropStmt(epService, spv);
    }

    private void runAssertionNoOutputClauseJoin(EPServiceProvider epService) {
        String statementString = "select symbol from " +
                SupportMarketDataBean.class.getName() + "#length(10) as one, " +
                SupportBeanString.class.getName() + "#length(100) as two " +
                "where one.symbol = two.theString " +
                "order by price";
        SymbolPricesVolumes spv = new SymbolPricesVolumes();
        SupportUpdateListener listener = new SupportUpdateListener();
        createAndSend(epService, statementString, listener);
        sendJoinEvents(epService);
        spv.symbols.add("KGB");
        assertValues(listener, spv.symbols, "symbol");
        clearValues(spv);
        sendEvent(epService, "DOG", 10);
        spv.symbols.add("DOG");
        assertValues(listener, spv.symbols, "symbol");
        clearValuesDropStmt(epService, spv);

        // Set start time
        sendTimeEvent(epService, 0);

        statementString = "select symbol from " +
                SupportMarketDataBean.class.getName() + "#time_batch(1) as one, " +
                SupportBeanString.class.getName() + "#length(100) as two " +
                "where one.symbol = two.theString " +
                "order by price, symbol";
        createAndSend(epService, statementString, listener);
        sendJoinEvents(epService);
        orderValuesByPriceJoin(spv);
        sendTimeEvent(epService, 1000);
        assertValues(listener, spv.symbols, "symbol");
        assertOnlyProperties(listener, Arrays.asList(new String[]{"symbol"}));
        clearValuesDropStmt(epService, spv);
    }

    private void assertOnlyProperties(SupportUpdateListener listener, List<String> requiredProperties) {
        EventBean[] events = listener.getLastNewData();
        if (events == null || events.length == 0) {
            return;
        }
        EventType type = events[0].getEventType();
        List<String> actualProperties = new ArrayList<String>(Arrays.asList(type.getPropertyNames()));
        log.debug(".assertOnlyProperties actualProperties==" + actualProperties);
        assertTrue(actualProperties.containsAll(requiredProperties));
        actualProperties.removeAll(requiredProperties);
        assertTrue(actualProperties.isEmpty());
    }

    private void assertSymbolsJoinWildCard(SupportUpdateListener listener, List<String> symbols) {
        EventBean[] events = listener.getLastNewData();
        log.debug(".assertValuesMayConvert event type = " + events[0].getEventType());
        log.debug(".assertValuesMayConvert values: " + symbols);
        log.debug(".assertValuesMayConvert events.length==" + events.length);
        for (int i = 0; i < events.length; i++) {
            SupportMarketDataBean theEvent = (SupportMarketDataBean) events[i].get("one");
            assertEquals(symbols.get(i), theEvent.getSymbol());
        }
    }

    private void assertValues(SupportUpdateListener listener, List values, String valueName) {
        EventBean[] events = listener.getLastNewData();
        assertEquals(values.size(), events.length);
        log.debug(".assertValuesMayConvert values: " + values);
        for (int i = 0; i < events.length; i++) {
            log.debug(".assertValuesMayConvert events[" + i + "]==" + events[i].get(valueName));
            assertEquals(values.get(i), events[i].get(valueName));
        }
    }

    private void clearValuesDropStmt(EPServiceProvider epService, SymbolPricesVolumes spv) {
        epService.getEPAdministrator().destroyAllStatements();
        clearValues(spv);
    }

    private void clearValues(SymbolPricesVolumes spv) {
        spv.prices.clear();
        spv.volumes.clear();
        spv.symbols.clear();
    }

    private void createAndSend(EPServiceProvider epService, String statementString, SupportUpdateListener listener) {
        EPStatement statement = epService.getEPAdministrator().createEPL(statementString);
        statement.addListener(listener);
        sendEvent(epService, "IBM", 2);
        sendEvent(epService, "KGB", 1);
        sendEvent(epService, "CMU", 3);
        sendEvent(epService, "IBM", 6);
        sendEvent(epService, "CAT", 6);
        sendEvent(epService, "CAT", 5);
    }

    private void orderValuesByPrice(SymbolPricesVolumes spv) {
        spv.symbols.add(0, "KGB");
        spv.symbols.add(1, "IBM");
        spv.symbols.add(2, "CMU");
        spv.symbols.add(3, "CAT");
        spv.symbols.add(4, "IBM");
        spv.symbols.add(5, "CAT");
        spv.prices.add(0, 1d);
        spv.prices.add(1, 2d);
        spv.prices.add(2, 3d);
        spv.prices.add(3, 5d);
        spv.prices.add(4, 6d);
        spv.prices.add(5, 6d);
        spv.volumes.add(0, 0L);
        spv.volumes.add(1, 0L);
        spv.volumes.add(2, 0L);
        spv.volumes.add(3, 0L);
        spv.volumes.add(4, 0L);
        spv.volumes.add(5, 0L);
    }

    private void orderValuesByPriceDesc(SymbolPricesVolumes spv) {
        spv.symbols.add(0, "IBM");
        spv.symbols.add(1, "CAT");
        spv.symbols.add(2, "CAT");
        spv.symbols.add(3, "CMU");
        spv.symbols.add(4, "IBM");
        spv.symbols.add(5, "KGB");
        spv.prices.add(0, 6d);
        spv.prices.add(1, 6d);
        spv.prices.add(2, 5d);
        spv.prices.add(3, 3d);
        spv.prices.add(4, 2d);
        spv.prices.add(5, 1d);
        spv.volumes.add(0, 0L);
        spv.volumes.add(1, 0L);
        spv.volumes.add(2, 0L);
        spv.volumes.add(3, 0L);
        spv.volumes.add(4, 0L);
        spv.volumes.add(5, 0L);
    }

    private void orderValuesByPriceJoin(SymbolPricesVolumes spv) {
        spv.symbols.add(0, "KGB");
        spv.symbols.add(1, "IBM");
        spv.symbols.add(2, "CMU");
        spv.symbols.add(3, "CAT");
        spv.symbols.add(4, "CAT");
        spv.symbols.add(5, "IBM");
        spv.prices.add(0, 1d);
        spv.prices.add(1, 2d);
        spv.prices.add(2, 3d);
        spv.prices.add(3, 5d);
        spv.prices.add(4, 6d);
        spv.prices.add(5, 6d);
        spv.volumes.add(0, 0L);
        spv.volumes.add(1, 0L);
        spv.volumes.add(2, 0L);
        spv.volumes.add(3, 0L);
        spv.volumes.add(4, 0L);
        spv.volumes.add(5, 0L);
    }

    private void orderValuesByPriceSymbol(SymbolPricesVolumes spv) {
        spv.symbols.add(0, "KGB");
        spv.symbols.add(1, "IBM");
        spv.symbols.add(2, "CMU");
        spv.symbols.add(3, "CAT");
        spv.symbols.add(4, "CAT");
        spv.symbols.add(5, "IBM");
        spv.prices.add(0, 1d);
        spv.prices.add(1, 2d);
        spv.prices.add(2, 3d);
        spv.prices.add(3, 5d);
        spv.prices.add(4, 6d);
        spv.prices.add(5, 6d);
        spv.volumes.add(0, 0L);
        spv.volumes.add(1, 0L);
        spv.volumes.add(2, 0L);
        spv.volumes.add(3, 0L);
        spv.volumes.add(4, 0L);
        spv.volumes.add(5, 0L);
    }

    private void orderValuesBySymbol(SymbolPricesVolumes spv) {
        spv.symbols.add(0, "CAT");
        spv.symbols.add(1, "CAT");
        spv.symbols.add(2, "CMU");
        spv.symbols.add(3, "IBM");
        spv.symbols.add(4, "IBM");
        spv.symbols.add(5, "KGB");
        spv.prices.add(0, 6d);
        spv.prices.add(1, 5d);
        spv.prices.add(2, 3d);
        spv.prices.add(3, 2d);
        spv.prices.add(4, 6d);
        spv.prices.add(5, 1d);
        spv.volumes.add(0, 0L);
        spv.volumes.add(1, 0L);
        spv.volumes.add(2, 0L);
        spv.volumes.add(3, 0L);
        spv.volumes.add(4, 0L);
        spv.volumes.add(5, 0L);
    }

    private void orderValuesBySymbolJoin(SymbolPricesVolumes spv) {
        spv.symbols.add(0, "CAT");
        spv.symbols.add(1, "CAT");
        spv.symbols.add(2, "CMU");
        spv.symbols.add(3, "IBM");
        spv.symbols.add(4, "IBM");
        spv.symbols.add(5, "KGB");
        spv.prices.add(0, 5d);
        spv.prices.add(1, 6d);
        spv.prices.add(2, 3d);
        spv.prices.add(3, 2d);
        spv.prices.add(4, 6d);
        spv.prices.add(5, 1d);
        spv.volumes.add(0, 0L);
        spv.volumes.add(1, 0L);
        spv.volumes.add(2, 0L);
        spv.volumes.add(3, 0L);
        spv.volumes.add(4, 0L);
        spv.volumes.add(5, 0L);
    }

    private void orderValuesBySymbolPrice(SymbolPricesVolumes spv) {
        spv.symbols.add(0, "CAT");
        spv.symbols.add(1, "CAT");
        spv.symbols.add(2, "CMU");
        spv.symbols.add(3, "IBM");
        spv.symbols.add(4, "IBM");
        spv.symbols.add(5, "KGB");
        spv.prices.add(0, 5d);
        spv.prices.add(1, 6d);
        spv.prices.add(2, 3d);
        spv.prices.add(3, 2d);
        spv.prices.add(4, 6d);
        spv.prices.add(5, 1d);
        spv.volumes.add(0, 0L);
        spv.volumes.add(1, 0L);
        spv.volumes.add(2, 0L);
        spv.volumes.add(3, 0L);
        spv.volumes.add(4, 0L);
        spv.volumes.add(5, 0L);
    }

    private void sendEvent(EPServiceProvider epService, String symbol, double price) {
        SupportMarketDataBean bean = new SupportMarketDataBean(symbol, price, 0L, null);
        epService.getEPRuntime().sendEvent(bean);
    }

    private void sendTimeEvent(EPServiceProvider epService, int millis) {
        CurrentTimeEvent theEvent = new CurrentTimeEvent(millis);
        epService.getEPRuntime().sendEvent(theEvent);
    }

    private void sendJoinEvents(EPServiceProvider epService) {
        epService.getEPRuntime().sendEvent(new SupportBeanString("CAT"));
        epService.getEPRuntime().sendEvent(new SupportBeanString("IBM"));
        epService.getEPRuntime().sendEvent(new SupportBeanString("CMU"));
        epService.getEPRuntime().sendEvent(new SupportBeanString("KGB"));
        epService.getEPRuntime().sendEvent(new SupportBeanString("DOG"));
    }

    private static class SymbolPricesVolumes {
        protected List<String> symbols = new LinkedList<String>();
        protected List<Double> prices = new LinkedList<Double>();
        protected List<Long> volumes = new LinkedList<Long>();

    }
}
