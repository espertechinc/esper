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

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.soda.*;
import com.espertech.esper.supportregression.bean.SupportBeanString;
import com.espertech.esper.supportregression.bean.SupportMarketDataBean;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.util.SerializableObjectCopier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ExecOrderByAggregateGrouped implements RegressionExecution {
    public void run(EPServiceProvider epService) throws Exception {
        runAssertionAliasesAggregationCompile(epService);
        runAssertionAliasesAggregationOM(epService);
        runAssertionAliases(epService);
        runAssertionGroupBySwitch(epService);
        runAssertionGroupBySwitchJoin(epService);
        runAssertionLastJoin(epService);
        runAssertionIterator(epService);
        runAssertionLast(epService);
    }

    private void runAssertionAliasesAggregationCompile(EPServiceProvider epService) throws Exception {
        String statementString = "select symbol, volume, sum(price) as mySum from " +
                SupportMarketDataBean.class.getName() + "#length(20) " +
                "group by symbol " +
                "output every 6 events " +
                "order by sum(price), symbol";

        EPStatementObjectModel model = epService.getEPAdministrator().compileEPL(statementString);
        model = (EPStatementObjectModel) SerializableObjectCopier.copy(model);
        assertEquals(statementString, model.toEPL());

        SupportUpdateListener testListener = new SupportUpdateListener();
        EPStatement statement = epService.getEPAdministrator().create(model);
        statement.addListener(testListener);

        tryAssertionDefault(epService, testListener);

        statement.destroy();
    }

    private void runAssertionAliasesAggregationOM(EPServiceProvider epService) throws Exception {
        EPStatementObjectModel model = new EPStatementObjectModel();
        model.setSelectClause(SelectClause.create("symbol", "volume").add(Expressions.sum("price"), "mySum"));
        model.setFromClause(FromClause.create(FilterStream.create(SupportMarketDataBean.class.getName()).addView(View.create("length", Expressions.constant(20)))));
        model.setGroupByClause(GroupByClause.create("symbol"));
        model.setOutputLimitClause(OutputLimitClause.create(6));
        model.setOrderByClause(OrderByClause.create(Expressions.sum("price")).add("symbol", false));
        model = (EPStatementObjectModel) SerializableObjectCopier.copy(model);

        String statementString = "select symbol, volume, sum(price) as mySum from " +
                SupportMarketDataBean.class.getName() + "#length(20) " +
                "group by symbol " +
                "output every 6 events " +
                "order by sum(price), symbol";

        assertEquals(statementString, model.toEPL());

        SupportUpdateListener testListener = new SupportUpdateListener();
        EPStatement statement = epService.getEPAdministrator().create(model);
        statement.addListener(testListener);

        tryAssertionDefault(epService, testListener);

        statement.destroy();
    }

    private void runAssertionAliases(EPServiceProvider epService) {
        String statementString = "select symbol, volume, sum(price) as mySum from " +
                SupportMarketDataBean.class.getName() + "#length(20) " +
                "group by symbol " +
                "output every 6 events " +
                "order by mySum, symbol";

        EPStatement statement = epService.getEPAdministrator().createEPL(statementString);
        SupportUpdateListener testListener = new SupportUpdateListener();
        statement.addListener(testListener);

        tryAssertionDefault(epService, testListener);

        statement.destroy();
    }

    private void runAssertionGroupBySwitch(EPServiceProvider epService) {
        // Instead of the row-per-group behavior, these should
        // get row-per-event behavior since there are properties
        // in the order-by that are not in the select expression.
        String statementString = "select symbol, sum(price) from " +
                SupportMarketDataBean.class.getName() + "#length(20) " +
                "group by symbol " +
                "output every 6 events " +
                "order by sum(price), symbol, volume";

        EPStatement statement = epService.getEPAdministrator().createEPL(statementString);
        SupportUpdateListener testListener = new SupportUpdateListener();
        statement.addListener(testListener);

        tryAssertionDefaultNoVolume(epService, testListener);

        statement.destroy();
    }

    private void runAssertionGroupBySwitchJoin(EPServiceProvider epService) {
        String statementString = "select symbol, sum(price) from " +
                SupportMarketDataBean.class.getName() + "#length(20) as one, " +
                SupportBeanString.class.getName() + "#length(100) as two " +
                "where one.symbol = two.theString " +
                "group by symbol " +
                "output every 6 events " +
                "order by sum(price), symbol, volume";

        EPStatement statement = epService.getEPAdministrator().createEPL(statementString);
        SupportUpdateListener testListener = new SupportUpdateListener();
        statement.addListener(testListener);

        epService.getEPRuntime().sendEvent(new SupportBeanString("CAT"));
        epService.getEPRuntime().sendEvent(new SupportBeanString("IBM"));
        epService.getEPRuntime().sendEvent(new SupportBeanString("CMU"));
        epService.getEPRuntime().sendEvent(new SupportBeanString("KGB"));
        epService.getEPRuntime().sendEvent(new SupportBeanString("DOG"));

        tryAssertionDefaultNoVolume(epService, testListener);

        statement.destroy();
    }

    private void runAssertionLast(EPServiceProvider epService) {
        String statementString = "select symbol, volume, sum(price) from " +
                SupportMarketDataBean.class.getName() + "#length(20) " +
                "group by symbol " +
                "output last every 6 events " +
                "order by sum(price)";

        EPStatement statement = epService.getEPAdministrator().createEPL(statementString);
        SupportUpdateListener testListener = new SupportUpdateListener();
        statement.addListener(testListener);

        tryAssertionLast(epService, testListener);

        statement.destroy();
    }

    private void runAssertionLastJoin(EPServiceProvider epService) {
        String statementString = "select symbol, volume, sum(price) from " +
                SupportMarketDataBean.class.getName() + "#length(20) as one, " +
                SupportBeanString.class.getName() + "#length(100) as two " +
                "where one.symbol = two.theString " +
                "group by symbol " +
                "output last every 6 events " +
                "order by sum(price)";

        EPStatement statement = epService.getEPAdministrator().createEPL(statementString);
        SupportUpdateListener testListener = new SupportUpdateListener();
        statement.addListener(testListener);

        epService.getEPRuntime().sendEvent(new SupportBeanString("CAT"));
        epService.getEPRuntime().sendEvent(new SupportBeanString("IBM"));
        epService.getEPRuntime().sendEvent(new SupportBeanString("CMU"));
        epService.getEPRuntime().sendEvent(new SupportBeanString("KGB"));
        epService.getEPRuntime().sendEvent(new SupportBeanString("DOG"));

        tryAssertionLast(epService, testListener);

        statement.destroy();
    }

    private void tryAssertionLast(EPServiceProvider epService, SupportUpdateListener testListener) {
        sendEvent(epService, "IBM", 101, 3);
        sendEvent(epService, "IBM", 102, 4);
        sendEvent(epService, "CMU", 103, 1);
        sendEvent(epService, "CMU", 104, 2);
        sendEvent(epService, "CAT", 105, 5);
        sendEvent(epService, "CAT", 106, 6);

        String[] fields = "symbol,volume,sum(price)".split(",");
        EPAssertionUtil.assertPropsPerRow(testListener.getLastNewData(), fields,
                new Object[][]{{"CMU", 104L, 3.0}, {"IBM", 102L, 7.0}, {"CAT", 106L, 11.0}});
        assertNull(testListener.getLastOldData());

        sendEvent(epService, "IBM", 201, 3);
        sendEvent(epService, "IBM", 202, 4);
        sendEvent(epService, "CMU", 203, 5);
        sendEvent(epService, "CMU", 204, 5);
        sendEvent(epService, "DOG", 205, 0);
        sendEvent(epService, "DOG", 206, 1);

        EPAssertionUtil.assertPropsPerRow(testListener.getLastNewData(), fields,
                new Object[][]{{"DOG", 206L, 1.0}, {"CMU", 204L, 13.0}, {"IBM", 202L, 14.0}});
        assertNull(testListener.getLastOldData());
    }


    private void runAssertionIterator(EPServiceProvider epService) {
        String[] fields = new String[]{"symbol", "theString", "sumPrice"};
        String statementString = "select symbol, theString, sum(price) as sumPrice from " +
                SupportMarketDataBean.class.getName() + "#length(10) as one, " +
                SupportBeanString.class.getName() + "#length(100) as two " +
                "where one.symbol = two.theString " +
                "group by symbol " +
                "order by symbol";
        EPStatement statement = epService.getEPAdministrator().createEPL(statementString);
        sendJoinEvents(epService);
        sendEvent(epService, "CAT", 50);
        sendEvent(epService, "IBM", 49);
        sendEvent(epService, "CAT", 15);
        sendEvent(epService, "IBM", 100);
        EPAssertionUtil.assertPropsPerRowAnyOrder(statement.iterator(), fields,
                new Object[][]{
                        {"CAT", "CAT", 65d},
                        {"CAT", "CAT", 65d},
                        {"IBM", "IBM", 149d},
                        {"IBM", "IBM", 149d},
                });

        sendEvent(epService, "KGB", 75);
        EPAssertionUtil.assertPropsPerRowAnyOrder(statement.iterator(), fields,
                new Object[][]{
                        {"CAT", "CAT", 65d},
                        {"CAT", "CAT", 65d},
                        {"IBM", "IBM", 149d},
                        {"IBM", "IBM", 149d},
                        {"KGB", "KGB", 75d},
                });

        statement.destroy();
    }

    private void sendEvent(EPServiceProvider epService, String symbol, double price) {
        SupportMarketDataBean bean = new SupportMarketDataBean(symbol, price, 0L, null);
        epService.getEPRuntime().sendEvent(bean);
    }

    private void sendEvent(EPServiceProvider epService, String symbol, long volume, double price) {
        SupportMarketDataBean bean = new SupportMarketDataBean(symbol, price, volume, null);
        epService.getEPRuntime().sendEvent(bean);
    }

    private void sendJoinEvents(EPServiceProvider epService) {
        epService.getEPRuntime().sendEvent(new SupportBeanString("CAT"));
        epService.getEPRuntime().sendEvent(new SupportBeanString("IBM"));
        epService.getEPRuntime().sendEvent(new SupportBeanString("CMU"));
        epService.getEPRuntime().sendEvent(new SupportBeanString("KGB"));
        epService.getEPRuntime().sendEvent(new SupportBeanString("DOG"));
    }

    private void tryAssertionDefault(EPServiceProvider epService, SupportUpdateListener testListener) {
        sendEvent(epService, "IBM", 110, 3);
        sendEvent(epService, "IBM", 120, 4);
        sendEvent(epService, "CMU", 130, 1);
        sendEvent(epService, "CMU", 140, 2);
        sendEvent(epService, "CAT", 150, 5);
        sendEvent(epService, "CAT", 160, 6);

        String[] fields = "symbol,volume,mySum".split(",");
        EPAssertionUtil.assertPropsPerRow(testListener.getLastNewData(), fields,
            new Object[][]{{"CMU", 130L, 1.0}, {"CMU", 140L, 3.0}, {"IBM", 110L, 3.0},
                {"CAT", 150L, 5.0}, {"IBM", 120L, 7.0}, {"CAT", 160L, 11.0}});
        assertNull(testListener.getLastOldData());
    }

    private void tryAssertionDefaultNoVolume(EPServiceProvider epService, SupportUpdateListener testListener) {
        sendEvent(epService, "IBM", 110, 3);
        sendEvent(epService, "IBM", 120, 4);
        sendEvent(epService, "CMU", 130, 1);
        sendEvent(epService, "CMU", 140, 2);
        sendEvent(epService, "CAT", 150, 5);
        sendEvent(epService, "CAT", 160, 6);

        String[] fields = "symbol,sum(price)".split(",");
        EPAssertionUtil.assertPropsPerRow(testListener.getLastNewData(), fields,
            new Object[][]{{"CMU", 1.0}, {"CMU", 3.0}, {"IBM", 3.0},
                {"CAT", 5.0}, {"IBM", 7.0}, {"CAT", 11.0}});
        assertNull(testListener.getLastOldData());
    }
}
