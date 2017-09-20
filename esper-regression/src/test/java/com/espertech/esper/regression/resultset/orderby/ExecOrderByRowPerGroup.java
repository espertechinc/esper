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

public class ExecOrderByRowPerGroup implements RegressionExecution {

    public void run(EPServiceProvider epService) throws Exception {
        runAssertionNoHavingNoJoin(epService);
        runAssertionHavingNoJoin(epService);
        runAssertionNoHavingJoin(epService);
        runAssertionHavingJoin(epService);
        runAssertionHavingJoinAlias(epService);
        runAssertionLast(epService);
        runAssertionLastJoin(epService);
        runAssertionIteratorRowPerGroup(epService);
    }

    private void runAssertionNoHavingNoJoin(EPServiceProvider epService) {
        String statementString = "select irstream symbol, sum(price) as mysum from " +
                SupportMarketDataBean.class.getName() + "#length(20) " +
                "group by symbol " +
                "output every 6 events " +
                "order by sum(price), symbol";
        EPStatement statement = epService.getEPAdministrator().createEPL(statementString);
        tryAssertionNoHaving(epService, statement);
        statement.destroy();
    }

    private void runAssertionHavingNoJoin(EPServiceProvider epService) {
        String statementString = "select irstream symbol, sum(price) as mysum from " +
                SupportMarketDataBean.class.getName() + "#length(20) " +
                "group by symbol " +
                "having sum(price) > 0 " +
                "output every 6 events " +
                "order by sum(price), symbol";
        EPStatement statement = epService.getEPAdministrator().createEPL(statementString);
        tryAssertionHaving(epService, statement);
        statement.destroy();
    }

    private void runAssertionNoHavingJoin(EPServiceProvider epService) {
        String statementString = "select irstream symbol, sum(price) as mysum from " +
                SupportMarketDataBean.class.getName() + "#length(20) as one, " +
                SupportBeanString.class.getName() + "#length(100) as two " +
                "where one.symbol = two.theString " +
                "group by symbol " +
                "output every 6 events " +
                "order by sum(price), symbol";
        EPStatement statement = epService.getEPAdministrator().createEPL(statementString);

        epService.getEPRuntime().sendEvent(new SupportBeanString("CAT"));
        epService.getEPRuntime().sendEvent(new SupportBeanString("IBM"));
        epService.getEPRuntime().sendEvent(new SupportBeanString("CMU"));
        epService.getEPRuntime().sendEvent(new SupportBeanString("KGB"));
        epService.getEPRuntime().sendEvent(new SupportBeanString("DOG"));

        tryAssertionNoHaving(epService, statement);

        statement.destroy();
    }

    private void runAssertionHavingJoin(EPServiceProvider epService) {
        String statementString = "select irstream symbol, sum(price) as mysum from " +
                SupportMarketDataBean.class.getName() + "#length(20) as one, " +
                SupportBeanString.class.getName() + "#length(100) as two " +
                "where one.symbol = two.theString " +
                "group by symbol " +
                "having sum(price) > 0 " +
                "output every 6 events " +
                "order by sum(price), symbol";

        EPStatement statement = epService.getEPAdministrator().createEPL(statementString);
        epService.getEPRuntime().sendEvent(new SupportBeanString("CAT"));
        epService.getEPRuntime().sendEvent(new SupportBeanString("IBM"));
        epService.getEPRuntime().sendEvent(new SupportBeanString("CMU"));
        epService.getEPRuntime().sendEvent(new SupportBeanString("KGB"));
        epService.getEPRuntime().sendEvent(new SupportBeanString("DOG"));

        tryAssertionHaving(epService, statement);

        statement.destroy();
    }

    private void runAssertionHavingJoinAlias(EPServiceProvider epService) {
        String statementString = "select irstream symbol, sum(price) as mysum from " +
                SupportMarketDataBean.class.getName() + "#length(20) as one, " +
                SupportBeanString.class.getName() + "#length(100) as two " +
                "where one.symbol = two.theString " +
                "group by symbol " +
                "having sum(price) > 0 " +
                "output every 6 events " +
                "order by mysum, symbol";

        EPStatement statement = epService.getEPAdministrator().createEPL(statementString);
        epService.getEPRuntime().sendEvent(new SupportBeanString("CAT"));
        epService.getEPRuntime().sendEvent(new SupportBeanString("IBM"));
        epService.getEPRuntime().sendEvent(new SupportBeanString("CMU"));
        epService.getEPRuntime().sendEvent(new SupportBeanString("KGB"));
        epService.getEPRuntime().sendEvent(new SupportBeanString("DOG"));

        tryAssertionHaving(epService, statement);

        statement.destroy();
    }

    private void runAssertionLast(EPServiceProvider epService) {
        String statementString = "select irstream symbol, sum(price) as mysum from " +
                SupportMarketDataBean.class.getName() + "#length(20) " +
                "group by symbol " +
                "output last every 6 events " +
                "order by sum(price), symbol";
        EPStatement statement = epService.getEPAdministrator().createEPL(statementString);
        tryAssertionLast(epService, statement);
        statement.destroy();
    }

    private void runAssertionLastJoin(EPServiceProvider epService) {
        String statementString = "select irstream symbol, sum(price) as mysum from " +
                SupportMarketDataBean.class.getName() + "#length(20) as one, " +
                SupportBeanString.class.getName() + "#length(100) as two " +
                "where one.symbol = two.theString " +
                "group by symbol " +
                "output last every 6 events " +
                "order by sum(price), symbol";

        EPStatement statement = epService.getEPAdministrator().createEPL(statementString);

        epService.getEPRuntime().sendEvent(new SupportBeanString("CAT"));
        epService.getEPRuntime().sendEvent(new SupportBeanString("IBM"));
        epService.getEPRuntime().sendEvent(new SupportBeanString("CMU"));
        epService.getEPRuntime().sendEvent(new SupportBeanString("KGB"));
        epService.getEPRuntime().sendEvent(new SupportBeanString("DOG"));

        tryAssertionLast(epService, statement);

        statement.destroy();
    }

    private void runAssertionIteratorRowPerGroup(EPServiceProvider epService) {
        String[] fields = new String[]{"symbol", "sumPrice"};
        String statementString = "select symbol, sum(price) as sumPrice from " +
                SupportMarketDataBean.class.getName() + "#length(10) as one, " +
                SupportBeanString.class.getName() + "#length(100) as two " +
                "where one.symbol = two.theString " +
                "group by symbol " +
                "order by symbol";
        EPStatement statement = epService.getEPAdministrator().createEPL(statementString);

        epService.getEPRuntime().sendEvent(new SupportBeanString("CAT"));
        epService.getEPRuntime().sendEvent(new SupportBeanString("IBM"));
        epService.getEPRuntime().sendEvent(new SupportBeanString("CMU"));
        epService.getEPRuntime().sendEvent(new SupportBeanString("KGB"));
        epService.getEPRuntime().sendEvent(new SupportBeanString("DOG"));

        sendEvent(epService, "CAT", 50);
        sendEvent(epService, "IBM", 49);
        sendEvent(epService, "CAT", 15);
        sendEvent(epService, "IBM", 100);
        EPAssertionUtil.assertPropsPerRowAnyOrder(statement.iterator(), fields,
                new Object[][]{
                        {"CAT", 65d},
                        {"IBM", 149d},
                });

        sendEvent(epService, "KGB", 75);
        EPAssertionUtil.assertPropsPerRowAnyOrder(statement.iterator(), fields,
                new Object[][]{
                        {"CAT", 65d},
                        {"IBM", 149d},
                        {"KGB", 75d},
                });

        statement.destroy();
    }

    private void sendEvent(EPServiceProvider epService, String symbol, double price) {
        SupportMarketDataBean bean = new SupportMarketDataBean(symbol, price, 0L, null);
        epService.getEPRuntime().sendEvent(bean);
    }

    private void tryAssertionLast(EPServiceProvider epService, EPStatement statement) {
        String[] fields = "symbol,mysum".split(",");
        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);

        sendEvent(epService, "IBM", 3);
        sendEvent(epService, "IBM", 4);
        sendEvent(epService, "CMU", 1);
        sendEvent(epService, "CMU", 2);
        sendEvent(epService, "CAT", 5);
        sendEvent(epService, "CAT", 6);

        EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), fields,
                new Object[][]{{"CMU", 3.0}, {"IBM", 7.0}, {"CAT", 11.0}});
        EPAssertionUtil.assertPropsPerRow(listener.getLastOldData(), fields,
                new Object[][]{{"CAT", null}, {"CMU", null}, {"IBM", null}});

        sendEvent(epService, "IBM", 3);
        sendEvent(epService, "IBM", 4);
        sendEvent(epService, "CMU", 5);
        sendEvent(epService, "CMU", 5);
        sendEvent(epService, "DOG", 0);
        sendEvent(epService, "DOG", 1);

        EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), fields,
                new Object[][]{{"DOG", 1.0}, {"CMU", 13.0}, {"IBM", 14.0}});
        EPAssertionUtil.assertPropsPerRow(listener.getLastOldData(), fields,
                new Object[][]{{"DOG", null}, {"CMU", 3.0}, {"IBM", 7.0}});
    }

    private void tryAssertionNoHaving(EPServiceProvider epService, EPStatement statement) {
        String[] fields = "symbol,mysum".split(",");

        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);
        sendEvent(epService, "IBM", 3);
        sendEvent(epService, "IBM", 4);
        sendEvent(epService, "CMU", 1);
        sendEvent(epService, "CMU", 2);
        sendEvent(epService, "CAT", 5);
        sendEvent(epService, "CAT", 6);
        EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), fields,
                new Object[][]{{"CMU", 1.0}, {"CMU", 3.0}, {"IBM", 3.0}, {"CAT", 5.0}, {"IBM", 7.0}, {"CAT", 11.0}});
        EPAssertionUtil.assertPropsPerRow(listener.getLastOldData(), fields,
                new Object[][]{{"CAT", null}, {"CMU", null}, {"IBM", null}, {"CMU", 1.0}, {"IBM", 3.0}, {"CAT", 5.0}});
        listener.reset();

        sendEvent(epService, "IBM", 3);
        sendEvent(epService, "IBM", 4);
        sendEvent(epService, "CMU", 5);
        sendEvent(epService, "CMU", 5);
        sendEvent(epService, "DOG", 0);
        sendEvent(epService, "DOG", 1);
        EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), fields,
                new Object[][]{{"DOG", 0.0}, {"DOG", 1.0}, {"CMU", 8.0}, {"IBM", 10.0}, {"CMU", 13.0}, {"IBM", 14.0}});
        EPAssertionUtil.assertPropsPerRow(listener.getLastOldData(), fields,
                new Object[][]{{"DOG", null}, {"DOG", 0.0}, {"CMU", 3.0}, {"IBM", 7.0}, {"CMU", 8.0}, {"IBM", 10.0}});
    }

    private void tryAssertionHaving(EPServiceProvider epService, EPStatement statement) {
        String[] fields = "symbol,mysum".split(",");
        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);
        sendEvent(epService, "IBM", 3);
        sendEvent(epService, "IBM", 4);
        sendEvent(epService, "CMU", 1);
        sendEvent(epService, "CMU", 2);
        sendEvent(epService, "CAT", 5);
        sendEvent(epService, "CAT", 6);

        EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), fields,
                new Object[][]{{"CMU", 1.0}, {"CMU", 3.0}, {"IBM", 3.0}, {"CAT", 5.0}, {"IBM", 7.0}, {"CAT", 11.0}});
        EPAssertionUtil.assertPropsPerRow(listener.getLastOldData(), fields,
                new Object[][]{{"CMU", 1.0}, {"IBM", 3.0}, {"CAT", 5.0}});
        listener.reset();

        sendEvent(epService, "IBM", 3);
        sendEvent(epService, "IBM", 4);
        sendEvent(epService, "CMU", 5);
        sendEvent(epService, "CMU", 5);
        sendEvent(epService, "DOG", 0);
        sendEvent(epService, "DOG", 1);
        EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), fields,
                new Object[][]{{"DOG", 1.0}, {"CMU", 8.0}, {"IBM", 10.0}, {"CMU", 13.0}, {"IBM", 14.0}});
        EPAssertionUtil.assertPropsPerRow(listener.getLastOldData(), fields,
                new Object[][]{{"CMU", 3.0}, {"IBM", 7.0}, {"CMU", 8.0}, {"IBM", 10.0}});
    }
}
