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
import com.espertech.esper.supportregression.bean.SupportBeanString;
import com.espertech.esper.supportregression.bean.SupportMarketDataBean;
import com.espertech.esper.supportregression.execution.RegressionExecution;

public class ExecOrderByRowPerEvent implements RegressionExecution {
    public void run(EPServiceProvider epService) throws Exception {
        runAssertionIteratorAggregateRowPerEvent(epService);
        runAssertionAliases(epService);
        runAssertionRowPerEventJoinOrderFunction(epService);
        runAssertionRowPerEventOrderFunction(epService);
        runAssertionRowPerEventSum(epService);
        runAssertionRowPerEventMaxSum(epService);
        runAssertionRowPerEventSumHaving(epService);
        runAssertionAggOrderWithSum(epService);
        runAssertionRowPerEventJoin(epService);
        runAssertionRowPerEventJoinMax(epService);
        runAssertionAggHaving(epService);
    }

    private void runAssertionIteratorAggregateRowPerEvent(EPServiceProvider epService) {
        String[] fields = new String[]{"symbol", "sumPrice"};
        String statementString = "select symbol, sum(price) as sumPrice from " +
                SupportMarketDataBean.class.getName() + "#length(10) as one, " +
                SupportBeanString.class.getName() + "#length(100) as two " +
                "where one.symbol = two.theString " +
                "order by symbol";
        EPStatement statement = epService.getEPAdministrator().createEPL(statementString);

        epService.getEPRuntime().sendEvent(new SupportBeanString("CAT"));
        epService.getEPRuntime().sendEvent(new SupportBeanString("IBM"));
        epService.getEPRuntime().sendEvent(new SupportBeanString("KGB"));

        sendEvent(epService, "CAT", 50);
        sendEvent(epService, "IBM", 49);
        sendEvent(epService, "CAT", 15);
        sendEvent(epService, "IBM", 100);
        EPAssertionUtil.assertPropsPerRow(statement.iterator(), fields,
                new Object[][]{
                        {"CAT", 214d},
                        {"CAT", 214d},
                        {"IBM", 214d},
                        {"IBM", 214d},
                });

        sendEvent(epService, "KGB", 75);
        EPAssertionUtil.assertPropsPerRow(statement.iterator(), fields,
                new Object[][]{
                        {"CAT", 289d},
                        {"CAT", 289d},
                        {"IBM", 289d},
                        {"IBM", 289d},
                        {"KGB", 289d},
                });

        statement.destroy();
    }

    private void runAssertionAliases(EPServiceProvider epService) {
        String statementString = "select symbol as mySymbol, sum(price) as mySum from " +
                SupportMarketDataBean.class.getName() + "#length(10) " +
                "output every 6 events " +
                "order by mySymbol";

        SupportUpdateListener listener = new SupportUpdateListener();
        EPStatement statement = epService.getEPAdministrator().createEPL(statementString);
        statement.addListener(listener);

        sendEvent(epService, "IBM", 3);
        sendEvent(epService, "IBM", 4);
        sendEvent(epService, "CMU", 1);
        sendEvent(epService, "CMU", 2);
        sendEvent(epService, "CAT", 5);
        sendEvent(epService, "CAT", 6);

        String[] fields = "mySymbol,mySum".split(",");
        EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), fields, new Object[][]{
                {"CAT", 15.0}, {"CAT", 21.0}, {"CMU", 8.0}, {"CMU", 10.0}, {"IBM", 3.0}, {"IBM", 7.0}});

        statement.destroy();
    }

    private void runAssertionRowPerEventJoinOrderFunction(EPServiceProvider epService) {
        String statementString = "select symbol, sum(price) from " +
                SupportMarketDataBean.class.getName() + "#length(10) as one, " +
                SupportBeanString.class.getName() + "#length(100) as two " +
                "where one.symbol = two.theString " +
                "output every 6 events " +
                "order by volume*sum(price), symbol";

        SupportUpdateListener listener = new SupportUpdateListener();
        EPStatement statement = epService.getEPAdministrator().createEPL(statementString);
        statement.addListener(listener);
        sendEvent(epService, "IBM", 2);
        sendEvent(epService, "KGB", 1);
        sendEvent(epService, "CMU", 3);
        sendEvent(epService, "IBM", 6);
        sendEvent(epService, "CAT", 6);
        sendEvent(epService, "CAT", 5);

        epService.getEPRuntime().sendEvent(new SupportBeanString("CAT"));
        epService.getEPRuntime().sendEvent(new SupportBeanString("IBM"));
        epService.getEPRuntime().sendEvent(new SupportBeanString("CMU"));
        epService.getEPRuntime().sendEvent(new SupportBeanString("KGB"));
        epService.getEPRuntime().sendEvent(new SupportBeanString("DOG"));

        String[] fields = "symbol".split(",");
        EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), fields, new Object[][]{
                {"CAT"}, {"CAT"}, {"CMU"}, {"IBM"}, {"IBM"}, {"KGB"}});

        statement.destroy();
    }

    private void runAssertionRowPerEventOrderFunction(EPServiceProvider epService) {
        String statementString = "select symbol, sum(price) from " +
                SupportMarketDataBean.class.getName() + "#length(10) " +
                "output every 6 events " +
                "order by volume*sum(price), symbol";

        SupportUpdateListener listener = new SupportUpdateListener();
        EPStatement statement = epService.getEPAdministrator().createEPL(statementString);
        statement.addListener(listener);

        sendEvent(epService, "IBM", 2);
        sendEvent(epService, "KGB", 1);
        sendEvent(epService, "CMU", 3);
        sendEvent(epService, "IBM", 6);
        sendEvent(epService, "CAT", 6);
        sendEvent(epService, "CAT", 5);

        String[] fields = "symbol".split(",");
        EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), fields, new Object[][]{
                {"CAT"}, {"CAT"}, {"CMU"}, {"IBM"}, {"IBM"}, {"KGB"}});

        statement.destroy();
    }

    private void runAssertionRowPerEventSum(EPServiceProvider epService) {
        String statementString = "select symbol, sum(price) from " +
                SupportMarketDataBean.class.getName() + "#length(10) " +
                "output every 6 events " +
                "order by symbol";

        SupportUpdateListener listener = new SupportUpdateListener();
        EPStatement statement = epService.getEPAdministrator().createEPL(statementString);
        statement.addListener(listener);

        sendEvent(epService, "IBM", 3);
        sendEvent(epService, "IBM", 4);
        sendEvent(epService, "CMU", 1);
        sendEvent(epService, "CMU", 2);
        sendEvent(epService, "CAT", 5);
        sendEvent(epService, "CAT", 6);

        String[] fields = "symbol,sum(price)".split(",");
        EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), fields, new Object[][]{
                {"CAT", 15.0}, {"CAT", 21.0}, {"CMU", 8.0}, {"CMU", 10.0}, {"IBM", 3.0}, {"IBM", 7.0}});

        statement.destroy();
    }

    private void runAssertionRowPerEventMaxSum(EPServiceProvider epService) {
        String statementString = "select symbol, max(sum(price)) from " +
                SupportMarketDataBean.class.getName() + "#length(10) " +
                "output every 6 events " +
                "order by symbol";

        SupportUpdateListener listener = new SupportUpdateListener();
        EPStatement statement = epService.getEPAdministrator().createEPL(statementString);
        statement.addListener(listener);

        sendEvent(epService, "IBM", 3);
        sendEvent(epService, "IBM", 4);
        sendEvent(epService, "CMU", 1);
        sendEvent(epService, "CMU", 2);
        sendEvent(epService, "CAT", 5);
        sendEvent(epService, "CAT", 6);

        String[] fields = "symbol,max(sum(price))".split(",");
        EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), fields, new Object[][]{
                {"CAT", 15.0}, {"CAT", 21.0}, {"CMU", 8.0}, {"CMU", 10.0}, {"IBM", 3.0}, {"IBM", 7.0}});

        statement.destroy();
    }

    private void runAssertionRowPerEventSumHaving(EPServiceProvider epService) {
        String statementString = "select symbol, sum(price) from " +
                SupportMarketDataBean.class.getName() + "#length(10) " +
                "having sum(price) > 0 " +
                "output every 6 events " +
                "order by symbol";

        SupportUpdateListener listener = new SupportUpdateListener();
        EPStatement statement = epService.getEPAdministrator().createEPL(statementString);
        statement.addListener(listener);

        sendEvent(epService, "IBM", 3);
        sendEvent(epService, "IBM", 4);
        sendEvent(epService, "CMU", 1);
        sendEvent(epService, "CMU", 2);
        sendEvent(epService, "CAT", 5);
        sendEvent(epService, "CAT", 6);

        String[] fields = "symbol,sum(price)".split(",");
        EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), fields, new Object[][]{
                {"CAT", 15.0}, {"CAT", 21.0}, {"CMU", 8.0}, {"CMU", 10.0}, {"IBM", 3.0}, {"IBM", 7.0}});

        statement.destroy();
    }

    private void runAssertionAggOrderWithSum(EPServiceProvider epService) {
        String statementString = "select symbol, sum(price) from " +
                SupportMarketDataBean.class.getName() + "#length(10) " +
                "output every 6 events " +
                "order by symbol, sum(price)";

        SupportUpdateListener listener = new SupportUpdateListener();
        EPStatement statement = epService.getEPAdministrator().createEPL(statementString);
        statement.addListener(listener);

        sendEvent(epService, "IBM", 3);
        sendEvent(epService, "IBM", 4);
        sendEvent(epService, "CMU", 1);
        sendEvent(epService, "CMU", 2);
        sendEvent(epService, "CAT", 5);
        sendEvent(epService, "CAT", 6);

        String[] fields = "symbol,sum(price)".split(",");
        EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), fields, new Object[][]{
                {"CAT", 15.0}, {"CAT", 21.0}, {"CMU", 8.0}, {"CMU", 10.0}, {"IBM", 3.0}, {"IBM", 7.0}});

        statement.destroy();
    }

    private void runAssertionRowPerEventJoin(EPServiceProvider epService) {
        String statementString = "select symbol, sum(price) from " +
                SupportMarketDataBean.class.getName() + "#length(10) as one, " +
                SupportBeanString.class.getName() + "#length(100) as two " +
                "where one.symbol = two.theString " +
                "output every 6 events " +
                "order by symbol, sum(price)";

        SupportUpdateListener listener = new SupportUpdateListener();
        EPStatement statement = epService.getEPAdministrator().createEPL(statementString);
        statement.addListener(listener);

        sendEvent(epService, "IBM", 3);
        sendEvent(epService, "IBM", 4);
        sendEvent(epService, "CMU", 1);
        sendEvent(epService, "CMU", 2);
        sendEvent(epService, "CAT", 5);
        sendEvent(epService, "CAT", 6);

        epService.getEPRuntime().sendEvent(new SupportBeanString("CAT"));
        epService.getEPRuntime().sendEvent(new SupportBeanString("IBM"));
        epService.getEPRuntime().sendEvent(new SupportBeanString("CMU"));

        String[] fields = "symbol,sum(price)".split(",");
        EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), fields, new Object[][]{
                {"CAT", 11.0}, {"CAT", 11.0}, {"CMU", 21.0}, {"CMU", 21.0}, {"IBM", 18.0}, {"IBM", 18.0}});

        statement.destroy();
    }

    private void runAssertionRowPerEventJoinMax(EPServiceProvider epService) {
        String statementString = "select symbol, max(sum(price)) from " +
                SupportMarketDataBean.class.getName() + "#length(10) as one, " +
                SupportBeanString.class.getName() + "#length(100) as two " +
                "where one.symbol = two.theString " +
                "output every 6 events " +
                "order by symbol";

        SupportUpdateListener listener = new SupportUpdateListener();
        EPStatement statement = epService.getEPAdministrator().createEPL(statementString);
        statement.addListener(listener);

        sendEvent(epService, "IBM", 3);
        sendEvent(epService, "IBM", 4);
        sendEvent(epService, "CMU", 1);
        sendEvent(epService, "CMU", 2);
        sendEvent(epService, "CAT", 5);
        sendEvent(epService, "CAT", 6);

        epService.getEPRuntime().sendEvent(new SupportBeanString("CAT"));
        epService.getEPRuntime().sendEvent(new SupportBeanString("IBM"));
        epService.getEPRuntime().sendEvent(new SupportBeanString("CMU"));

        String[] fields = "symbol,max(sum(price))".split(",");
        EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), fields, new Object[][]{
                {"CAT", 11.0}, {"CAT", 11.0}, {"CMU", 21.0}, {"CMU", 21.0}, {"IBM", 18.0}, {"IBM", 18.0}});

        statement.destroy();
    }

    private void runAssertionAggHaving(EPServiceProvider epService) {
        String statementString = "select symbol, sum(price) from " +
                SupportMarketDataBean.class.getName() + "#length(10) as one, " +
                SupportBeanString.class.getName() + "#length(100) as two " +
                "where one.symbol = two.theString " +
                "having sum(price) > 0 " +
                "output every 6 events " +
                "order by symbol";
        SupportUpdateListener listener = new SupportUpdateListener();
        EPStatement statement = epService.getEPAdministrator().createEPL(statementString);
        statement.addListener(listener);

        sendEvent(epService, "IBM", 3);
        sendEvent(epService, "IBM", 4);
        sendEvent(epService, "CMU", 1);
        sendEvent(epService, "CMU", 2);
        sendEvent(epService, "CAT", 5);
        sendEvent(epService, "CAT", 6);

        epService.getEPRuntime().sendEvent(new SupportBeanString("CAT"));
        epService.getEPRuntime().sendEvent(new SupportBeanString("IBM"));
        epService.getEPRuntime().sendEvent(new SupportBeanString("CMU"));

        String[] fields = "symbol,sum(price)".split(",");
        EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), fields, new Object[][]{
                {"CAT", 11.0}, {"CAT", 11.0}, {"CMU", 21.0}, {"CMU", 21.0}, {"IBM", 18.0}, {"IBM", 18.0}});

        statement.destroy();
    }

    private void sendEvent(EPServiceProvider epService, String symbol, double price) {
        SupportMarketDataBean bean = new SupportMarketDataBean(symbol, price, 0L, null);
        epService.getEPRuntime().sendEvent(bean);
    }
}
