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
import com.espertech.esper.supportregression.bean.SupportBeanString;
import com.espertech.esper.supportregression.bean.SupportMarketDataBean;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import java.util.Collections;

public class ExecOrderByRowForAll implements RegressionExecution {
    public void run(EPServiceProvider epService) throws Exception {
        String[] fields = new String[]{"sumPrice"};
        String statementString = "select sum(price) as sumPrice from " +
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
        EPAssertionUtil.assertPropsPerRow(statement.iterator(), fields, new Object[][]{{214d}});

        sendEvent(epService, "KGB", 75);
        EPAssertionUtil.assertPropsPerRow(statement.iterator(), fields, new Object[][]{{289d}});

        // JIRA ESPER-644 Infinite loop when restarting a statement
        epService.getEPAdministrator().getConfiguration().addEventType("FB", Collections.<String, Object>singletonMap("timeTaken", double.class));
        EPStatement stmt = epService.getEPAdministrator().createEPL("select avg(timeTaken) as timeTaken from FB order by timeTaken desc");
        stmt.stop();
        stmt.start();
    }

    private void sendEvent(EPServiceProvider epService, String symbol, double price) {
        SupportMarketDataBean bean = new SupportMarketDataBean(symbol, price, 0L, null);
        epService.getEPRuntime().sendEvent(bean);
    }

    private void sendJoinEvents(EPServiceProvider epService) {
        epService.getEPRuntime().sendEvent(new SupportBeanString("CAT"));
        epService.getEPRuntime().sendEvent(new SupportBeanString("IBM"));
        epService.getEPRuntime().sendEvent(new SupportBeanString("CMU"));
        epService.getEPRuntime().sendEvent(new SupportBeanString("KGB"));
        epService.getEPRuntime().sendEvent(new SupportBeanString("DOG"));
    }
}
