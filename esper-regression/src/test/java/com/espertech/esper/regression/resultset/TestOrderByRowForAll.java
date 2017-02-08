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
package com.espertech.esper.regression.resultset;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.supportregression.bean.SupportBeanString;
import com.espertech.esper.supportregression.bean.SupportMarketDataBean;
import com.espertech.esper.supportregression.client.SupportConfigFactory;
import junit.framework.TestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;

public class TestOrderByRowForAll extends TestCase
{
	private static final Logger log = LoggerFactory.getLogger(TestOrderByRowForAll.class);
	private EPServiceProvider epService;

    public void setUp()
    {
        Configuration config = SupportConfigFactory.getConfiguration();
        epService = EPServiceProviderManager.getDefaultProvider(config);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
    }

    public void tearDown() {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
    }

    public void testIteratorAggregateRowForAll()
	{
        String[] fields = new String[] {"sumPrice"};
        String statementString = "select sum(price) as sumPrice from " +
    	            SupportMarketDataBean.class.getName() + "#length(10) as one, " +
    	            SupportBeanString.class.getName() + "#length(100) as two " +
                    "where one.symbol = two.theString " +
                    "order by price";
        EPStatement statement = epService.getEPAdministrator().createEPL(statementString);
        sendJoinEvents();
        sendEvent("CAT", 50);
        sendEvent("IBM", 49);
        sendEvent("CAT", 15);
        sendEvent("IBM", 100);
        EPAssertionUtil.assertPropsPerRow(statement.iterator(), fields, new Object[][]{{214d}});

        sendEvent("KGB", 75);
        EPAssertionUtil.assertPropsPerRow(statement.iterator(), fields, new Object[][]{{289d}});

        // JIRA ESPER-644 Infinite loop when restarting a statement
        epService.getEPAdministrator().getConfiguration().addEventType("FB", Collections.<String, Object>singletonMap("timeTaken", double.class));
        EPStatement stmt = epService.getEPAdministrator().createEPL("select avg(timeTaken) as timeTaken from FB order by timeTaken desc");
        stmt.stop();
        stmt.start();
    }

    private void sendEvent(String symbol, double price)
	{
	    SupportMarketDataBean bean = new SupportMarketDataBean(symbol, price, 0L, null);
	    epService.getEPRuntime().sendEvent(bean);
	}

	private void sendJoinEvents()
	{
		epService.getEPRuntime().sendEvent(new SupportBeanString("CAT"));
		epService.getEPRuntime().sendEvent(new SupportBeanString("IBM"));
		epService.getEPRuntime().sendEvent(new SupportBeanString("CMU"));
		epService.getEPRuntime().sendEvent(new SupportBeanString("KGB"));
		epService.getEPRuntime().sendEvent(new SupportBeanString("DOG"));
	}
}
