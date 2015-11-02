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
import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.support.bean.SupportBeanString;
import com.espertech.esper.support.bean.SupportMarketDataBean;
import com.espertech.esper.support.client.SupportConfigFactory;
import junit.framework.TestCase;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class TestOrderByEventPerGroup extends TestCase {

	private static final Log log = LogFactory.getLog(TestOrderByEventPerGroup.class);
	private EPServiceProvider epService;
	private SupportUpdateListener testListener;

    public void setUp()
    {
        Configuration config = SupportConfigFactory.getConfiguration();
        epService = EPServiceProviderManager.getDefaultProvider(config);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
    }

    protected void tearDown() throws Exception {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
        testListener = null;
    }

    public void testNoHavingNoJoin()
	{
		String statementString = "select irstream symbol, sum(price) as mysum from " +
                                SupportMarketDataBean.class.getName() + ".win:length(20) " +
                                "group by symbol " +
                                "output every 6 events " +
                                "order by sum(price), symbol";
        EPStatement statement = epService.getEPAdministrator().createEPL(statementString);

        runAssertionNoHaving(statement);
    }

    public void testHavingNoJoin()
    {
		String statementString = "select irstream symbol, sum(price) as mysum from " +
                                    SupportMarketDataBean.class.getName() + ".win:length(20) " +
                                    "group by symbol " +
                                    "having sum(price) > 0 " +
                                    "output every 6 events " +
                                    "order by sum(price), symbol";
        EPStatement statement = epService.getEPAdministrator().createEPL(statementString);
        runAssertionHaving(statement);
	}

    public void testNoHavingJoin()
    {
    	String statementString = "select irstream symbol, sum(price) as mysum from " +
                            SupportMarketDataBean.class.getName() + ".win:length(20) as one, " +
                            SupportBeanString.class.getName() + ".win:length(100) as two " +
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

        runAssertionNoHaving(statement);
    }

    public void testHavingJoin()
    {
        String statementString = "select irstream symbol, sum(price) as mysum from " +
            SupportMarketDataBean.class.getName() + ".win:length(20) as one, " +
            SupportBeanString.class.getName() + ".win:length(100) as two " +
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

        runAssertionHaving(statement);
    }

    public void testHavingJoinAlias()
    {
        String statementString = "select irstream symbol, sum(price) as mysum from " +
            SupportMarketDataBean.class.getName() + ".win:length(20) as one, " +
            SupportBeanString.class.getName() + ".win:length(100) as two " +
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

        runAssertionHaving(statement);
    }

	public void testLast()
	{
		String statementString = "select irstream symbol, sum(price) as mysum from " +
                                    SupportMarketDataBean.class.getName() + ".win:length(20) " +
                                    "group by symbol " +
                                    "output last every 6 events " +
                                    "order by sum(price), symbol";
        EPStatement statement = epService.getEPAdministrator().createEPL(statementString);
        runAssertionLast(statement);
    }

    public void testLastJoin()
    {        
        String statementString = "select irstream symbol, sum(price) as mysum from " +
                                SupportMarketDataBean.class.getName() + ".win:length(20) as one, " +
                                SupportBeanString.class.getName() + ".win:length(100) as two " +
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

        runAssertionLast(statement);
    }

    public void testIteratorGroupByEventPerGroup()
	{
        String[] fields = new String[] {"symbol", "sumPrice"};
        String statementString = "select symbol, sum(price) as sumPrice from " +
    	            SupportMarketDataBean.class.getName() + ".win:length(10) as one, " +
    	            SupportBeanString.class.getName() + ".win:length(100) as two " +
                    "where one.symbol = two.theString " +
                    "group by symbol " +
                    "order by symbol";
        EPStatement statement = epService.getEPAdministrator().createEPL(statementString);

        epService.getEPRuntime().sendEvent(new SupportBeanString("CAT"));
        epService.getEPRuntime().sendEvent(new SupportBeanString("IBM"));
        epService.getEPRuntime().sendEvent(new SupportBeanString("CMU"));
        epService.getEPRuntime().sendEvent(new SupportBeanString("KGB"));
        epService.getEPRuntime().sendEvent(new SupportBeanString("DOG"));

        sendEvent("CAT", 50);
        sendEvent("IBM", 49);
        sendEvent("CAT", 15);
        sendEvent("IBM", 100);
        EPAssertionUtil.assertPropsPerRowAnyOrder(statement.iterator(), fields,
                new Object[][]{
                        {"CAT", 65d},
                        {"IBM", 149d},
                });

        sendEvent("KGB", 75);
        EPAssertionUtil.assertPropsPerRowAnyOrder(statement.iterator(), fields,
                new Object[][]{
                        {"CAT", 65d},
                        {"IBM", 149d},
                        {"KGB", 75d},
                });
    }

    private void sendEvent(String symbol, double price)
	{
	    SupportMarketDataBean bean = new SupportMarketDataBean(symbol, price, 0L, null);
	    epService.getEPRuntime().sendEvent(bean);
	}

    private void runAssertionLast(EPStatement statement)
    {
        String fields[] = "symbol,mysum".split(",");
        testListener = new SupportUpdateListener();
        statement.addListener(testListener);

        sendEvent("IBM", 3);
        sendEvent("IBM", 4);
        sendEvent("CMU", 1);
        sendEvent("CMU", 2);
        sendEvent("CAT", 5);
        sendEvent("CAT", 6);

        EPAssertionUtil.assertPropsPerRow(testListener.getLastNewData(), fields,
                new Object[][]{{"CMU", 3.0}, {"IBM", 7.0}, {"CAT", 11.0}});
        EPAssertionUtil.assertPropsPerRow(testListener.getLastOldData(), fields,
                new Object[][]{{"CAT", null}, {"CMU", null}, {"IBM", null},});

        sendEvent("IBM", 3);
        sendEvent("IBM", 4);
        sendEvent("CMU", 5);
        sendEvent("CMU", 5);
        sendEvent("DOG", 0);
        sendEvent("DOG", 1);

        EPAssertionUtil.assertPropsPerRow(testListener.getLastNewData(), fields,
                new Object[][]{{"DOG", 1.0}, {"CMU", 13.0}, {"IBM", 14.0}});
        EPAssertionUtil.assertPropsPerRow(testListener.getLastOldData(), fields,
                new Object[][]{{"DOG", null}, {"CMU", 3.0}, {"IBM", 7.0}});
    }

    private void runAssertionNoHaving(EPStatement statement)
    {
        String fields[] = "symbol,mysum".split(",");

        testListener = new SupportUpdateListener();
        statement.addListener(testListener);
        sendEvent("IBM", 3);
        sendEvent("IBM", 4);
        sendEvent("CMU", 1);
        sendEvent("CMU", 2);
        sendEvent("CAT", 5);
        sendEvent("CAT", 6);
        EPAssertionUtil.assertPropsPerRow(testListener.getLastNewData(), fields,
                new Object[][]{{"CMU", 1.0}, {"CMU", 3.0}, {"IBM", 3.0}, {"CAT", 5.0}, {"IBM", 7.0}, {"CAT", 11.0}});
        EPAssertionUtil.assertPropsPerRow(testListener.getLastOldData(), fields,
                new Object[][]{{"CAT", null}, {"CMU", null}, {"IBM", null}, {"CMU", 1.0}, {"IBM", 3.0}, {"CAT", 5.0}});
        testListener.reset();

        sendEvent("IBM", 3);
        sendEvent("IBM", 4);
        sendEvent("CMU", 5);
        sendEvent("CMU", 5);
        sendEvent("DOG", 0);
        sendEvent("DOG", 1);
        EPAssertionUtil.assertPropsPerRow(testListener.getLastNewData(), fields,
                new Object[][]{{"DOG", 0.0}, {"DOG", 1.0}, {"CMU", 8.0}, {"IBM", 10.0}, {"CMU", 13.0}, {"IBM", 14.0}});
        EPAssertionUtil.assertPropsPerRow(testListener.getLastOldData(), fields,
                new Object[][]{{"DOG", null}, {"DOG", 0.0}, {"CMU", 3.0}, {"IBM", 7.0}, {"CMU", 8.0}, {"IBM", 10.0}});
    }

    private void runAssertionHaving(EPStatement statement)
    {
        String fields[] = "symbol,mysum".split(",");
        testListener = new SupportUpdateListener();
        statement.addListener(testListener);
        sendEvent("IBM", 3);
        sendEvent("IBM", 4);
        sendEvent("CMU", 1);
        sendEvent("CMU", 2);
        sendEvent("CAT", 5);
        sendEvent("CAT", 6);

        EPAssertionUtil.assertPropsPerRow(testListener.getLastNewData(), fields,
                new Object[][]{{"CMU", 1.0}, {"CMU", 3.0}, {"IBM", 3.0}, {"CAT", 5.0}, {"IBM", 7.0}, {"CAT", 11.0}});
        EPAssertionUtil.assertPropsPerRow(testListener.getLastOldData(), fields,
                new Object[][]{{"CMU", 1.0}, {"IBM", 3.0}, {"CAT", 5.0}});
        testListener.reset();

        sendEvent("IBM", 3);
        sendEvent("IBM", 4);
        sendEvent("CMU", 5);
        sendEvent("CMU", 5);
        sendEvent("DOG", 0);
        sendEvent("DOG", 1);
        EPAssertionUtil.assertPropsPerRow(testListener.getLastNewData(), fields,
                new Object[][]{{"DOG", 1.0}, {"CMU", 8.0}, {"IBM", 10.0}, {"CMU", 13.0}, {"IBM", 14.0}});
        EPAssertionUtil.assertPropsPerRow(testListener.getLastOldData(), fields,
                new Object[][]{{"CMU", 3.0}, {"IBM", 7.0}, {"CMU", 8.0}, {"IBM", 10.0}});
    }
}
