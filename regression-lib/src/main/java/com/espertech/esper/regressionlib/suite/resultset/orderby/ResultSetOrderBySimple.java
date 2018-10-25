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
package com.espertech.esper.regressionlib.suite.resultset.orderby;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.client.soda.*;
import com.espertech.esper.common.internal.util.SerializableObjectCopier;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.support.bean.SupportBeanString;
import com.espertech.esper.regressionlib.support.bean.SupportMarketDataBean;
import com.espertech.esper.runtime.client.scopetest.SupportUpdateListener;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil.tryInvalidCompile;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ResultSetOrderBySimple {
    private static final Logger log = LoggerFactory.getLogger(ResultSetOrderBySimple.class);

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ResultSetOrderByMultiDelivery());
        execs.add(new ResultSetIterator());
        execs.add(new ResultSetAcrossJoin());
        execs.add(new ResultSetDescendingOM());
        execs.add(new ResultSetDescending());
        execs.add(new ResultSetExpressions());
        execs.add(new ResultSetAliasesSimple());
        execs.add(new ResultSetExpressionsJoin());
        execs.add(new ResultSetMultipleKeys());
        execs.add(new ResultSetAliases());
        execs.add(new ResultSetMultipleKeysJoin());
        execs.add(new ResultSetSimple());
        execs.add(new ResultSetSimpleJoin());
        execs.add(new ResultSetWildcard());
        execs.add(new ResultSetWildcardJoin());
        execs.add(new ResultSetNoOutputClauseView());
        execs.add(new ResultSetNoOutputClauseJoin());
        execs.add(new ResultSetInvalid());
        return execs;
    }

    private static class ResultSetOrderByMultiDelivery implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            // test for QWY-933597 or ESPER-409
            env.advanceTime(0);

            // try pattern
            String epl = "@name('s0') select a.theString from pattern [every a=SupportBean(theString like 'A%') -> b=SupportBean(theString like 'B%')] order by a.theString desc";
            env.compileDeploy(epl).addListener("s0");

            env.sendEventBean(new SupportBean("A1", 1));

            env.milestone(1);

            env.sendEventBean(new SupportBean("A2", 2));
            env.sendEventBean(new SupportBean("B", 3));

            EventBean[] received = env.listener("s0").getNewDataListFlattened();
            assertEquals(2, received.length);
            EPAssertionUtil.assertPropsPerRow(received, "a.theString".split(","), new Object[][]{{"A2"}, {"A1"}});

            env.undeployAll();

            // try pattern with output limit
            epl = "@name('s0') select a.theString from pattern [every a=SupportBean(theString like 'A%') -> b=SupportBean(theString like 'B%')] " +
                "output every 3 events order by a.theString desc";
            env.compileDeploy(epl).addListener("s0");

            env.sendEventBean(new SupportBean("A1", 1));
            env.sendEventBean(new SupportBean("A2", 2));

            env.milestone(1);

            env.sendEventBean(new SupportBean("A3", 3));
            env.sendEventBean(new SupportBean("B", 3));

            EventBean[] receivedThree = env.listener("s0").getNewDataListFlattened();
            assertEquals(3, receivedThree.length);
            EPAssertionUtil.assertPropsPerRow(receivedThree, "a.theString".split(","), new Object[][]{{"A3"}, {"A2"}, {"A1"}});

            env.undeployAll();

            // try grouped time window
            epl = "@name('s0') select rstream theString from SupportBean#groupwin(theString)#time(10) order by theString desc";
            env.compileDeploy(epl).addListener("s0");

            env.advanceTime(1000);
            env.sendEventBean(new SupportBean("A1", 1));
            env.sendEventBean(new SupportBean("A2", 1));

            env.milestone(2);

            env.advanceTime(11000);
            EventBean[] receivedTwo = env.listener("s0").getNewDataListFlattened();
            assertEquals(2, receivedTwo.length);
            EPAssertionUtil.assertPropsPerRow(receivedTwo, "theString".split(","), new Object[][]{{"A2"}, {"A1"}});

            env.undeployAll();
        }
    }

    private static class ResultSetIterator implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            AtomicInteger milestone = new AtomicInteger();
            String epl = "@name('s0') select symbol, theString, price from " +
                "SupportMarketDataBean#length(10) as one, " +
                "SupportBeanString#length(100) as two " +
                "where one.symbol = two.theString " +
                "order by price";
            env.compileDeploy(epl).addListener("s0");

            sendJoinEvents(env, milestone);
            sendEvent(env, "CAT", 50);

            env.milestoneInc(milestone);

            sendEvent(env, "IBM", 49);
            sendEvent(env, "CAT", 15);
            sendEvent(env, "IBM", 100);
            EPAssertionUtil.assertPropsPerRow(env.iterator("s0"), new String[]{"symbol", "theString", "price"},
                new Object[][]{
                    {"CAT", "CAT", 15d},
                    {"IBM", "IBM", 49d},
                    {"CAT", "CAT", 50d},
                    {"IBM", "IBM", 100d},
                });

            env.milestoneInc(milestone);

            sendEvent(env, "KGB", 75);
            EPAssertionUtil.assertPropsPerRow(env.iterator("s0"), new String[]{"symbol", "theString", "price"},
                new Object[][]{
                    {"CAT", "CAT", 15d},
                    {"IBM", "IBM", 49d},
                    {"CAT", "CAT", 50d},
                    {"KGB", "KGB", 75d},
                    {"IBM", "IBM", 100d},
                });

            env.undeployAll();
        }
    }

    private static class ResultSetAcrossJoin implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            AtomicInteger milestone = new AtomicInteger();
            String epl = "@name('s0') select symbol, theString from " +
                "SupportMarketDataBean#length(10) as one, " +
                "SupportBeanString#length(100) as two " +
                "where one.symbol = two.theString " +
                "output every 6 events " +
                "order by price";
            SymbolPricesVolumes spv = new SymbolPricesVolumes();
            createAndSend(env, epl, milestone);

            env.milestoneInc(milestone);

            sendJoinEvents(env, milestone);
            orderValuesByPriceJoin(spv);
            assertValues(env, spv.symbols, "symbol");
            assertValues(env, spv.symbols, "theString");
            assertOnlyProperties(env, Arrays.asList(new String[]{"symbol", "theString"}));
            clearValuesDropStmt(env, spv);

            epl = "@name('s0') select symbol from " +
                "SupportMarketDataBean#length(10) as one, " +
                "SupportBeanString#length(100) as two " +
                "where one.symbol = two.theString " +
                "output every 6 events " +
                "order by theString, price";
            createAndSend(env, epl, milestone);
            sendJoinEvents(env, milestone);
            orderValuesBySymbolPrice(spv);
            assertValues(env, spv.symbols, "symbol");
            assertOnlyProperties(env, Arrays.asList(new String[]{"symbol"}));
            clearValuesDropStmt(env, spv);

            env.undeployAll();
        }
    }

    private static class ResultSetDescendingOM implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmtText = "select symbol from " +
                "SupportMarketDataBean#length(5) " +
                "output every 6 events " +
                "order by price desc";

            EPStatementObjectModel model = new EPStatementObjectModel();
            model.setSelectClause(SelectClause.create("symbol"));
            model.setFromClause(FromClause.create(FilterStream.create(SupportMarketDataBean.class.getSimpleName()).addView("length", Expressions.constant(5))));
            model.setOutputLimitClause(OutputLimitClause.create(6));
            model.setOrderByClause(OrderByClause.create().add("price", true));
            model = SerializableObjectCopier.copyMayFail(model);
            Assert.assertEquals(stmtText, model.toEPL());

            model.setAnnotations(Collections.singletonList(AnnotationPart.nameAnnotation("s0")));
            env.compileDeploy(model).addListener("s0");

            sendEvent(env, "IBM", 2);
            sendEvent(env, "KGB", 1);

            env.milestone(0);

            sendEvent(env, "CMU", 3);
            sendEvent(env, "IBM", 6);
            sendEvent(env, "CAT", 6);

            env.milestone(1);

            sendEvent(env, "CAT", 5);

            SymbolPricesVolumes spv = new SymbolPricesVolumes();
            orderValuesByPriceDesc(spv);
            assertValues(env, spv.symbols, "symbol");

            env.undeployAll();
        }
    }

    private static class ResultSetDescending implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            AtomicInteger milestone = new AtomicInteger();
            String epl = "@name('s0') select symbol from " +
                "SupportMarketDataBean#length(5) " +
                "output every 6 events " +
                "order by price desc";
            createAndSend(env, epl, milestone);
            SymbolPricesVolumes spv = new SymbolPricesVolumes();
            orderValuesByPriceDesc(spv);
            assertValues(env, spv.symbols, "symbol");
            clearValuesDropStmt(env, spv);

            epl = "@name('s0') select symbol from " +
                "SupportMarketDataBean#length(5) " +
                "output every 6 events " +
                "order by price desc, symbol asc";
            createAndSend(env, epl, milestone);
            orderValuesByPrice(spv);
            Collections.reverse(spv.symbols);
            assertValues(env, spv.symbols, "symbol");
            clearValuesDropStmt(env, spv);

            epl = "@name('s0') select symbol from " +
                "SupportMarketDataBean#length(5) " +
                "output every 6 events " +
                "order by price asc";
            createAndSend(env, epl, milestone);
            orderValuesByPrice(spv);
            assertValues(env, spv.symbols, "symbol");
            clearValuesDropStmt(env, spv);

            epl = "@name('s0') select symbol, volume from " +
                "SupportMarketDataBean#length(5) " +
                "output every 6 events " +
                "order by symbol desc";
            createAndSend(env, epl, milestone);
            orderValuesBySymbol(spv);
            Collections.reverse(spv.symbols);
            assertValues(env, spv.symbols, "symbol");
            assertValues(env, spv.volumes, "volume");
            clearValuesDropStmt(env, spv);

            epl = "@name('s0') select symbol, price from " +
                "SupportMarketDataBean#length(5) " +
                "output every 6 events " +
                "order by symbol desc, price desc";
            createAndSend(env, epl, milestone);
            orderValuesBySymbolPrice(spv);
            Collections.reverse(spv.symbols);
            Collections.reverse(spv.prices);
            assertValues(env, spv.symbols, "symbol");
            assertValues(env, spv.prices, "price");
            clearValuesDropStmt(env, spv);

            epl = "@name('s0') select symbol, price from " +
                "SupportMarketDataBean#length(5) " +
                "output every 6 events " +
                "order by symbol, price";
            createAndSend(env, epl, milestone);
            orderValuesBySymbolPrice(spv);
            assertValues(env, spv.symbols, "symbol");
            assertValues(env, spv.prices, "price");
            clearValuesDropStmt(env, spv);
        }
    }

    private static class ResultSetExpressions implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            AtomicInteger milestone = new AtomicInteger();
            String epl = "@name('s0') select symbol from " +
                "SupportMarketDataBean#length(10) " +
                "output every 6 events " +
                "order by (price * 6) + 5";
            createAndSend(env, epl, milestone);
            SymbolPricesVolumes spv = new SymbolPricesVolumes();
            orderValuesByPrice(spv);
            assertValues(env, spv.symbols, "symbol");
            assertOnlyProperties(env, Arrays.asList(new String[]{"symbol"}));
            clearValuesDropStmt(env, spv);

            epl = "@name('s0') select symbol, price from " +
                "SupportMarketDataBean#length(10) " +
                "output every 6 events " +
                "order by (price * 6) + 5, price";
            createAndSend(env, epl, milestone);
            orderValuesByPrice(spv);
            assertValues(env, spv.symbols, "symbol");
            assertOnlyProperties(env, Arrays.asList(new String[]{"symbol", "price"}));
            clearValuesDropStmt(env, spv);

            epl = "@name('s0') select symbol, 1+volume*23 from " +
                "SupportMarketDataBean#length(10) " +
                "output every 6 events " +
                "order by (price * 6) + 5, price, volume";
            createAndSend(env, epl, milestone);
            orderValuesByPrice(spv);
            assertValues(env, spv.symbols, "symbol");
            assertOnlyProperties(env, Arrays.asList(new String[]{"symbol", "1+volume*23"}));
            clearValuesDropStmt(env, spv);

            epl = "@name('s0') select symbol from " +
                "SupportMarketDataBean#length(10) " +
                "output every 6 events " +
                "order by volume*price, symbol";
            createAndSend(env, epl, milestone);
            orderValuesBySymbol(spv);
            assertValues(env, spv.symbols, "symbol");
            assertOnlyProperties(env, Arrays.asList(new String[]{"symbol"}));
            clearValuesDropStmt(env, spv);
        }
    }

    private static class ResultSetAliasesSimple implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            AtomicInteger milestone = new AtomicInteger();
            String epl = "@name('s0') select symbol as mySymbol from " +
                "SupportMarketDataBean#length(5) " +
                "output every 6 events " +
                "order by mySymbol";
            SupportUpdateListener listener = new SupportUpdateListener();
            createAndSend(env, epl, milestone);
            SymbolPricesVolumes spv = new SymbolPricesVolumes();
            orderValuesBySymbol(spv);
            assertValues(env, spv.symbols, "mySymbol");
            assertOnlyProperties(env, Arrays.asList(new String[]{"mySymbol"}));
            clearValuesDropStmt(env, spv);

            epl = "@name('s0') select symbol as mySymbol, price as myPrice from " +
                "SupportMarketDataBean#length(5) " +
                "output every 6 events " +
                "order by myPrice";
            createAndSend(env, epl, milestone);
            orderValuesByPrice(spv);
            assertValues(env, spv.symbols, "mySymbol");
            assertValues(env, spv.prices, "myPrice");
            assertOnlyProperties(env, Arrays.asList(new String[]{"mySymbol", "myPrice"}));
            clearValuesDropStmt(env, spv);

            epl = "@name('s0') select symbol, price as myPrice from " +
                "SupportMarketDataBean#length(10) " +
                "output every 6 events " +
                "order by (myPrice * 6) + 5, price";
            createAndSend(env, epl, milestone);
            orderValuesByPrice(spv);
            assertValues(env, spv.symbols, "symbol");
            assertOnlyProperties(env, Arrays.asList(new String[]{"symbol", "myPrice"}));
            clearValuesDropStmt(env, spv);

            epl = "@name('s0') select symbol, 1+volume*23 as myVol from " +
                "SupportMarketDataBean#length(10) " +
                "output every 6 events " +
                "order by (price * 6) + 5, price, myVol";
            createAndSend(env, epl, milestone);
            orderValuesByPrice(spv);
            assertValues(env, spv.symbols, "symbol");
            assertOnlyProperties(env, Arrays.asList(new String[]{"symbol", "myVol"}));
            clearValuesDropStmt(env, spv);
        }
    }

    private static class ResultSetExpressionsJoin implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            AtomicInteger milestone = new AtomicInteger();
            String epl = "@name('s0') select symbol from " +
                "SupportMarketDataBean#length(10) as one, " +
                "SupportBeanString#length(100) as two " +
                "where one.symbol = two.theString " +
                "output every 6 events " +
                "order by (price * 6) + 5";
            createAndSend(env, epl, milestone);
            sendJoinEvents(env, milestone);
            SymbolPricesVolumes spv = new SymbolPricesVolumes();
            orderValuesByPriceJoin(spv);
            assertValues(env, spv.symbols, "symbol");
            assertOnlyProperties(env, Arrays.asList(new String[]{"symbol"}));
            clearValuesDropStmt(env, spv);

            epl = "@name('s0') select symbol, price from " +
                "SupportMarketDataBean#length(10) as one, " +
                "SupportBeanString#length(100) as two " +
                "where one.symbol = two.theString " +
                "output every 6 events " +
                "order by (price * 6) + 5, price";
            createAndSend(env, epl, milestone);
            sendJoinEvents(env, milestone);
            orderValuesByPriceJoin(spv);
            assertValues(env, spv.prices, "price");
            assertOnlyProperties(env, Arrays.asList(new String[]{"symbol", "price"}));
            clearValuesDropStmt(env, spv);

            epl = "@name('s0') select symbol, 1+volume*23 from " +
                "SupportMarketDataBean#length(10) as one, " +
                "SupportBeanString#length(100) as two " +
                "where one.symbol = two.theString " +
                "output every 6 events " +
                "order by (price * 6) + 5, price, volume";
            createAndSend(env, epl, milestone);
            sendJoinEvents(env, milestone);
            orderValuesByPriceJoin(spv);
            assertValues(env, spv.symbols, "symbol");
            assertOnlyProperties(env, Arrays.asList(new String[]{"symbol", "1+volume*23"}));
            clearValuesDropStmt(env, spv);

            epl = "@name('s0') select symbol from " +
                "SupportMarketDataBean#length(10) as one, " +
                "SupportBeanString#length(100) as two " +
                "where one.symbol = two.theString " +
                "output every 6 events " +
                "order by volume*price, symbol";
            createAndSend(env, epl, milestone);
            sendJoinEvents(env, milestone);
            orderValuesBySymbol(spv);
            assertValues(env, spv.symbols, "symbol");
            assertOnlyProperties(env, Arrays.asList(new String[]{"symbol"}));
            clearValuesDropStmt(env, spv);
        }
    }

    private static class ResultSetInvalid implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String message = "Aggregate functions in the order-by clause must also occur in the select expression";
            String epl = "@name('s0') select symbol from " +
                "SupportMarketDataBean#length(5) " +
                "output every 6 events " +
                "order by sum(price)";
            tryInvalidCompile(env, epl, message);

            epl = "@name('s0') select sum(price) from " +
                "SupportMarketDataBean#length(5) " +
                "output every 6 events " +
                "order by sum(price + 6)";
            tryInvalidCompile(env, epl, message);

            epl = "@name('s0') select sum(price + 6) from " +
                "SupportMarketDataBean#length(5) " +
                "output every 6 events " +
                "order by sum(price)";
            tryInvalidCompile(env, epl, message);

            epl = "@name('s0') select symbol from " +
                "SupportMarketDataBean#length(10) as one, " +
                "SupportBeanString#length(100) as two " +
                "where one.symbol = two.theString " +
                "output every 6 events " +
                "order by sum(price)";
            tryInvalidCompile(env, epl, message);

            epl = "@name('s0') select sum(price) from " +
                "SupportMarketDataBean#length(10) as one, " +
                "SupportBeanString#length(100) as two " +
                "where one.symbol = two.theString " +
                "output every 6 events " +
                "order by sum(price + 6)";
            tryInvalidCompile(env, epl, message);

            epl = "@name('s0') select sum(price + 6) from " +
                "SupportMarketDataBean#length(10) as one, " +
                "SupportBeanString#length(100) as two " +
                "where one.symbol = two.theString " +
                "output every 6 events " +
                "order by sum(price)";
            tryInvalidCompile(env, epl, message);
        }
    }

    private static class ResultSetMultipleKeys implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            AtomicInteger milestone = new AtomicInteger();
            String epl = "@name('s0') select symbol from " +
                "SupportMarketDataBean#length(10) " +
                "output every 6 events " +
                "order by symbol, price";
            createAndSend(env, epl, milestone);
            SymbolPricesVolumes spv = new SymbolPricesVolumes();
            orderValuesBySymbolPrice(spv);
            assertValues(env, spv.symbols, "symbol");
            assertOnlyProperties(env, Arrays.asList(new String[]{"symbol"}));
            clearValuesDropStmt(env, spv);

            epl = "@name('s0') select symbol from " +
                "SupportMarketDataBean#length(10) " +
                "output every 6 events " +
                "order by price, symbol, volume";
            createAndSend(env, epl, milestone);
            orderValuesByPriceSymbol(spv);
            assertValues(env, spv.symbols, "symbol");
            assertOnlyProperties(env, Arrays.asList(new String[]{"symbol"}));
            clearValuesDropStmt(env, spv);

            epl = "@name('s0') select symbol, volume*2 from " +
                "SupportMarketDataBean#length(10) " +
                "output every 6 events " +
                "order by price, volume";
            createAndSend(env, epl, milestone);
            orderValuesByPrice(spv);
            assertValues(env, spv.symbols, "symbol");
            assertOnlyProperties(env, Arrays.asList(new String[]{"symbol", "volume*2"}));
            clearValuesDropStmt(env, spv);
        }
    }

    private static class ResultSetAliases implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            AtomicInteger milestone = new AtomicInteger();
            String epl = "@name('s0') select symbol as mySymbol from " +
                "SupportMarketDataBean#length(5) " +
                "output every 6 events " +
                "order by mySymbol";
            createAndSend(env, epl, milestone);
            SymbolPricesVolumes spv = new SymbolPricesVolumes();
            orderValuesBySymbol(spv);
            assertValues(env, spv.symbols, "mySymbol");
            assertOnlyProperties(env, Arrays.asList(new String[]{"mySymbol"}));
            clearValuesDropStmt(env, spv);

            epl = "@name('s0') select symbol as mySymbol, price as myPrice from " +
                "SupportMarketDataBean#length(5) " +
                "output every 6 events " +
                "order by myPrice";
            createAndSend(env, epl, milestone);
            orderValuesByPrice(spv);
            assertValues(env, spv.symbols, "mySymbol");
            assertValues(env, spv.prices, "myPrice");
            assertOnlyProperties(env, Arrays.asList(new String[]{"mySymbol", "myPrice"}));
            clearValuesDropStmt(env, spv);

            epl = "@name('s0') select symbol, price as myPrice from " +
                "SupportMarketDataBean#length(10) " +
                "output every 6 events " +
                "order by (myPrice * 6) + 5, price";
            createAndSend(env, epl, milestone);
            orderValuesByPrice(spv);
            assertValues(env, spv.symbols, "symbol");
            assertOnlyProperties(env, Arrays.asList(new String[]{"symbol", "myPrice"}));
            clearValuesDropStmt(env, spv);

            epl = "@name('s0') select symbol, 1+volume*23 as myVol from " +
                "SupportMarketDataBean#length(10) " +
                "output every 6 events " +
                "order by (price * 6) + 5, price, myVol";
            createAndSend(env, epl, milestone);
            orderValuesByPrice(spv);
            assertValues(env, spv.symbols, "symbol");
            assertOnlyProperties(env, Arrays.asList(new String[]{"symbol", "myVol"}));
            clearValuesDropStmt(env, spv);

            epl = "@name('s0') select symbol as mySymbol from " +
                "SupportMarketDataBean#length(5) " +
                "order by price, mySymbol";
            createAndSend(env, epl, milestone);
            spv.symbols.add("CAT");
            assertValues(env, spv.symbols, "mySymbol");
            clearValues(spv);
            sendEvent(env, "FOX", 10);
            spv.symbols.add("FOX");
            assertValues(env, spv.symbols, "mySymbol");
            clearValuesDropStmt(env, spv);
        }
    }

    private static class ResultSetMultipleKeysJoin implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            AtomicInteger milestone = new AtomicInteger();
            String epl = "@name('s0') select symbol from " +
                "SupportMarketDataBean#length(10) as one, " +
                "SupportBeanString#length(100) as two " +
                "where one.symbol = two.theString " +
                "output every 6 events " +
                "order by symbol, price";
            SymbolPricesVolumes spv = new SymbolPricesVolumes();
            createAndSend(env, epl, milestone);
            sendJoinEvents(env, milestone);
            orderValuesBySymbolPrice(spv);
            assertValues(env, spv.symbols, "symbol");
            assertOnlyProperties(env, Arrays.asList(new String[]{"symbol"}));
            clearValuesDropStmt(env, spv);

            epl = "@name('s0') select symbol from " +
                "SupportMarketDataBean#length(10) as one, " +
                "SupportBeanString#length(100) as two " +
                "where one.symbol = two.theString " +
                "output every 6 events " +
                "order by price, symbol, volume";
            createAndSend(env, epl, milestone);
            sendJoinEvents(env, milestone);
            orderValuesByPriceSymbol(spv);
            assertValues(env, spv.symbols, "symbol");
            assertOnlyProperties(env, Arrays.asList(new String[]{"symbol"}));
            clearValuesDropStmt(env, spv);

            epl = "@name('s0') select symbol, volume*2 from " +
                "SupportMarketDataBean#length(10) as one, " +
                "SupportBeanString#length(100) as two " +
                "where one.symbol = two.theString " +
                "output every 6 events " +
                "order by price, volume";
            createAndSend(env, epl, milestone);
            sendJoinEvents(env, milestone);
            orderValuesByPriceJoin(spv);
            assertValues(env, spv.symbols, "symbol");
            assertOnlyProperties(env, Arrays.asList(new String[]{"symbol", "volume*2"}));
            clearValuesDropStmt(env, spv);
        }
    }

    private static class ResultSetSimple implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            AtomicInteger milestone = new AtomicInteger();
            String epl = "@name('s0') select symbol from " +
                "SupportMarketDataBean#length(5) " +
                "output every 6 events " +
                "order by price";
            SymbolPricesVolumes spv = new SymbolPricesVolumes();
            createAndSend(env, epl, milestone);
            orderValuesByPrice(spv);
            assertValues(env, spv.symbols, "symbol");
            assertOnlyProperties(env, Arrays.asList(new String[]{"symbol"}));
            clearValuesDropStmt(env, spv);

            epl = "@name('s0') select symbol, price from " +
                "SupportMarketDataBean#length(5) " +
                "output every 6 events " +
                "order by price";
            createAndSend(env, epl, milestone);
            orderValuesByPrice(spv);
            assertValues(env, spv.symbols, "symbol");
            assertValues(env, spv.prices, "price");
            assertOnlyProperties(env, Arrays.asList(new String[]{"symbol", "price"}));
            clearValuesDropStmt(env, spv);

            epl = "@name('s0') select symbol, volume from " +
                "SupportMarketDataBean#length(5) " +
                "output every 6 events " +
                "order by price";
            createAndSend(env, epl, milestone);
            orderValuesByPrice(spv);
            assertValues(env, spv.symbols, "symbol");
            assertValues(env, spv.volumes, "volume");
            assertOnlyProperties(env, Arrays.asList(new String[]{"symbol", "volume"}));
            clearValuesDropStmt(env, spv);

            epl = "@name('s0') select symbol, volume*2 from " +
                "SupportMarketDataBean#length(5) " +
                "output every 6 events " +
                "order by price";
            createAndSend(env, epl, milestone);
            orderValuesByPrice(spv);
            assertValues(env, spv.symbols, "symbol");
            assertValues(env, spv.volumes, "volume*2");
            assertOnlyProperties(env, Arrays.asList(new String[]{"symbol", "volume*2"}));
            clearValuesDropStmt(env, spv);

            epl = "@name('s0') select symbol, volume from " +
                "SupportMarketDataBean#length(5) " +
                "output every 6 events " +
                "order by symbol";
            createAndSend(env, epl, milestone);
            orderValuesBySymbol(spv);
            assertValues(env, spv.symbols, "symbol");
            assertValues(env, spv.volumes, "volume");
            assertOnlyProperties(env, Arrays.asList(new String[]{"symbol", "volume"}));
            clearValuesDropStmt(env, spv);

            epl = "@name('s0') select price from " +
                "SupportMarketDataBean#length(5) " +
                "output every 6 events " +
                "order by symbol";
            createAndSend(env, epl, milestone);
            orderValuesBySymbol(spv);
            assertValues(env, spv.prices, "price");
            assertOnlyProperties(env, Arrays.asList(new String[]{"price"}));
            clearValuesDropStmt(env, spv);
        }
    }

    private static class ResultSetSimpleJoin implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            AtomicInteger milestone = new AtomicInteger();
            String epl = "@name('s0') select symbol from " +
                "SupportMarketDataBean#length(10) as one, " +
                "SupportBeanString#length(100) as two " +
                "where one.symbol = two.theString " +
                "output every 6 events " +
                "order by price";
            SymbolPricesVolumes spv = new SymbolPricesVolumes();
            createAndSend(env, epl, milestone);
            sendJoinEvents(env, milestone);
            orderValuesByPriceJoin(spv);
            assertValues(env, spv.symbols, "symbol");
            assertOnlyProperties(env, Arrays.asList(new String[]{"symbol"}));
            clearValuesDropStmt(env, spv);

            epl = "@name('s0') select symbol, price from " +
                "SupportMarketDataBean#length(10) as one, " +
                "SupportBeanString#length(100) as two " +
                "where one.symbol = two.theString " +
                "output every 6 events " +
                "order by price";
            createAndSend(env, epl, milestone);
            sendJoinEvents(env, milestone);
            orderValuesByPriceJoin(spv);
            assertValues(env, spv.symbols, "symbol");
            assertValues(env, spv.prices, "price");
            assertOnlyProperties(env, Arrays.asList(new String[]{"symbol", "price"}));
            clearValuesDropStmt(env, spv);

            epl = "@name('s0') select symbol, volume from " +
                "SupportMarketDataBean#length(10) as one, " +
                "SupportBeanString#length(100) as two " +
                "where one.symbol = two.theString " +
                "output every 6 events " +
                "order by price";
            createAndSend(env, epl, milestone);
            sendJoinEvents(env, milestone);
            orderValuesByPriceJoin(spv);
            assertValues(env, spv.symbols, "symbol");
            assertValues(env, spv.volumes, "volume");
            assertOnlyProperties(env, Arrays.asList(new String[]{"symbol", "volume"}));
            clearValuesDropStmt(env, spv);

            epl = "@name('s0') select symbol, volume*2 from " +
                "SupportMarketDataBean#length(10) as one, " +
                "SupportBeanString#length(100) as two " +
                "where one.symbol = two.theString " +
                "output every 6 events " +
                "order by price";
            createAndSend(env, epl, milestone);
            sendJoinEvents(env, milestone);
            orderValuesByPriceJoin(spv);
            assertValues(env, spv.symbols, "symbol");
            assertValues(env, spv.volumes, "volume*2");
            assertOnlyProperties(env, Arrays.asList(new String[]{"symbol", "volume*2"}));
            clearValuesDropStmt(env, spv);

            epl = "@name('s0') select symbol, volume from " +
                "SupportMarketDataBean#length(10) as one, " +
                "SupportBeanString#length(100) as two " +
                "where one.symbol = two.theString " +
                "output every 6 events " +
                "order by symbol";
            createAndSend(env, epl, milestone);
            sendJoinEvents(env, milestone);
            orderValuesBySymbol(spv);
            assertValues(env, spv.symbols, "symbol");
            assertValues(env, spv.volumes, "volume");
            assertOnlyProperties(env, Arrays.asList(new String[]{"symbol", "volume"}));
            clearValuesDropStmt(env, spv);

            epl = "@name('s0') select price from " +
                "SupportMarketDataBean#length(10) as one, " +
                "SupportBeanString#length(100) as two " +
                "where one.symbol = two.theString " +
                "output every 6 events " +
                "order by symbol, price";
            createAndSend(env, epl, milestone);
            sendJoinEvents(env, milestone);
            orderValuesBySymbolJoin(spv);
            assertValues(env, spv.prices, "price");
            assertOnlyProperties(env, Arrays.asList(new String[]{"price"}));
            clearValuesDropStmt(env, spv);
        }
    }

    private static class ResultSetWildcard implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            AtomicInteger milestone = new AtomicInteger();
            String epl = "@name('s0') select * from " +
                "SupportMarketDataBean#length(5) " +
                "output every 6 events " +
                "order by price";
            createAndSend(env, epl, milestone);
            SymbolPricesVolumes spv = new SymbolPricesVolumes();
            orderValuesByPrice(spv);
            assertValues(env, spv.symbols, "symbol");
            assertValues(env, spv.prices, "price");
            assertValues(env, spv.volumes, "volume");
            assertOnlyProperties(env, Arrays.asList(new String[]{"symbol", "id", "volume", "price", "feed"}));
            clearValuesDropStmt(env, spv);

            epl = "@name('s0') select * from " +
                "SupportMarketDataBean#length(5) " +
                "output every 6 events " +
                "order by symbol";
            createAndSend(env, epl, milestone);
            orderValuesBySymbol(spv);
            assertValues(env, spv.symbols, "symbol");
            assertValues(env, spv.prices, "price");
            assertValues(env, spv.volumes, "volume");
            assertOnlyProperties(env, Arrays.asList(new String[]{"symbol", "volume", "price", "feed", "id"}));
            clearValuesDropStmt(env, spv);
        }

    }

    private static class ResultSetWildcardJoin implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            AtomicInteger milestone = new AtomicInteger();
            String epl = "@name('s0') select * from " +
                "SupportMarketDataBean#length(10) as one, " +
                "SupportBeanString#length(100) as two " +
                "where one.symbol = two.theString " +
                "output every 6 events " +
                "order by price";
            SymbolPricesVolumes spv = new SymbolPricesVolumes();
            createAndSend(env, epl, milestone);
            sendJoinEvents(env, milestone);
            orderValuesByPriceJoin(spv);
            assertSymbolsJoinWildCard(env, spv.symbols);
            clearValuesDropStmt(env, spv);

            epl = "@name('s0') select * from " +
                "SupportMarketDataBean#length(10) as one, " +
                "SupportBeanString#length(100) as two " +
                "where one.symbol = two.theString " +
                "output every 6 events " +
                "order by symbol, price";
            createAndSend(env, epl, milestone);
            sendJoinEvents(env, milestone);
            orderValuesBySymbolJoin(spv);
            assertSymbolsJoinWildCard(env, spv.symbols);
            clearValuesDropStmt(env, spv);
        }
    }

    private static class ResultSetNoOutputClauseView implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            AtomicInteger milestone = new AtomicInteger();
            SymbolPricesVolumes spv = new SymbolPricesVolumes();
            String epl = "@name('s0') select symbol from " +
                "SupportMarketDataBean#length(5) " +
                "order by price";
            SupportUpdateListener listener = new SupportUpdateListener();
            createAndSend(env, epl, milestone);
            spv.symbols.add("CAT");
            assertValues(env, spv.symbols, "symbol");
            clearValues(spv);
            sendEvent(env, "FOX", 10);
            spv.symbols.add("FOX");
            assertValues(env, spv.symbols, "symbol");
            clearValuesDropStmt(env, spv);

            // Set start time
            sendTimeEvent(env, 0);

            epl = "@name('s0') select symbol from " +
                "SupportMarketDataBean#time_batch(1 sec) " +
                "order by price";
            createAndSend(env, epl, milestone);
            orderValuesByPrice(spv);
            sendTimeEvent(env, 1000);
            assertValues(env, spv.symbols, "symbol");
            assertOnlyProperties(env, Arrays.asList(new String[]{"symbol"}));
            clearValuesDropStmt(env, spv);
        }
    }

    private static class ResultSetNoOutputClauseJoin implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            AtomicInteger milestone = new AtomicInteger();
            String epl = "@name('s0') select symbol from " +
                "SupportMarketDataBean#length(10) as one, " +
                "SupportBeanString#length(100) as two " +
                "where one.symbol = two.theString " +
                "order by price";
            SymbolPricesVolumes spv = new SymbolPricesVolumes();
            SupportUpdateListener listener = new SupportUpdateListener();
            createAndSend(env, epl, milestone);
            sendJoinEvents(env, milestone);
            spv.symbols.add("KGB");
            assertValues(env, spv.symbols, "symbol");
            clearValues(spv);
            sendEvent(env, "DOG", 10);
            spv.symbols.add("DOG");
            assertValues(env, spv.symbols, "symbol");
            clearValuesDropStmt(env, spv);

            // Set start time
            sendTimeEvent(env, 0);

            epl = "@name('s0') select symbol from " +
                "SupportMarketDataBean#time_batch(1) as one, " +
                "SupportBeanString#length(100) as two " +
                "where one.symbol = two.theString " +
                "order by price, symbol";
            createAndSend(env, epl, milestone);
            sendJoinEvents(env, milestone);
            orderValuesByPriceJoin(spv);
            sendTimeEvent(env, 1000);
            assertValues(env, spv.symbols, "symbol");
            assertOnlyProperties(env, Arrays.asList(new String[]{"symbol"}));
            clearValuesDropStmt(env, spv);
        }
    }

    private static void assertOnlyProperties(RegressionEnvironment env, List<String> requiredProperties) {
        EventBean[] events = env.listener("s0").getLastNewData();
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

    private static void assertSymbolsJoinWildCard(RegressionEnvironment env, List<String> symbols) {
        EventBean[] events = env.listener("s0").getLastNewData();
        log.debug(".assertValuesMayConvert event type = " + events[0].getEventType());
        log.debug(".assertValuesMayConvert values: " + symbols);
        log.debug(".assertValuesMayConvert events.length==" + events.length);
        for (int i = 0; i < events.length; i++) {
            SupportMarketDataBean theEvent = (SupportMarketDataBean) events[i].get("one");
            Assert.assertEquals(symbols.get(i), theEvent.getSymbol());
        }
    }

    private static void assertValues(RegressionEnvironment env, List values, String valueName) {
        EventBean[] events = env.listener("s0").getLastNewData();
        assertEquals(values.size(), events.length);
        log.debug(".assertValuesMayConvert values: " + values);
        for (int i = 0; i < events.length; i++) {
            log.debug(".assertValuesMayConvert events[" + i + "]==" + events[i].get(valueName));
            Assert.assertEquals(values.get(i), events[i].get(valueName));
        }
    }

    private static void clearValuesDropStmt(RegressionEnvironment env, SymbolPricesVolumes spv) {
        env.undeployAll();
        clearValues(spv);
    }

    private static void clearValues(SymbolPricesVolumes spv) {
        spv.prices.clear();
        spv.volumes.clear();
        spv.symbols.clear();
    }

    private static void createAndSend(RegressionEnvironment env, String epl, AtomicInteger milestone) {
        env.compileDeploy(epl).addListener("s0");
        sendEvent(env, "IBM", 2);
        sendEvent(env, "KGB", 1);
        sendEvent(env, "CMU", 3);
        sendEvent(env, "IBM", 6);

        env.milestoneInc(milestone);

        sendEvent(env, "CAT", 6);
        sendEvent(env, "CAT", 5);
    }

    private static void orderValuesByPrice(SymbolPricesVolumes spv) {
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

    private static void orderValuesByPriceDesc(SymbolPricesVolumes spv) {
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

    private static void orderValuesByPriceJoin(SymbolPricesVolumes spv) {
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

    private static void orderValuesByPriceSymbol(SymbolPricesVolumes spv) {
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

    private static void orderValuesBySymbol(SymbolPricesVolumes spv) {
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

    private static void orderValuesBySymbolJoin(SymbolPricesVolumes spv) {
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

    private static void orderValuesBySymbolPrice(SymbolPricesVolumes spv) {
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

    private static void sendEvent(RegressionEnvironment env, String symbol, double price) {
        SupportMarketDataBean bean = new SupportMarketDataBean(symbol, price, 0L, null);
        env.sendEventBean(bean);
    }

    private static void sendTimeEvent(RegressionEnvironment env, int millis) {
        env.advanceTime(millis);
    }

    private static void sendJoinEvents(RegressionEnvironment env, AtomicInteger milestone) {
        env.sendEventBean(new SupportBeanString("CAT"));
        env.sendEventBean(new SupportBeanString("IBM"));
        env.milestoneInc(milestone);
        env.sendEventBean(new SupportBeanString("CMU"));
        env.sendEventBean(new SupportBeanString("KGB"));
        env.sendEventBean(new SupportBeanString("DOG"));
    }

    private static class SymbolPricesVolumes {
        protected List<String> symbols = new LinkedList<String>();
        protected List<Double> prices = new LinkedList<Double>();
        protected List<Long> volumes = new LinkedList<Long>();

    }
}
