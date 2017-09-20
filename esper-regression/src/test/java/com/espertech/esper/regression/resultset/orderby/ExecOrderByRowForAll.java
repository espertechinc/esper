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
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBeanString;
import com.espertech.esper.supportregression.bean.SupportBean_A;
import com.espertech.esper.supportregression.bean.SupportMarketDataBean;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import java.util.Collections;

import static junit.framework.TestCase.assertFalse;

public class ExecOrderByRowForAll implements RegressionExecution {
    public void run(EPServiceProvider epService) throws Exception {
        runAssertionNoOutputRateNonJoin(epService);
        runAssertionNoOutputRateJoin(epService);
        runAssertionOutputDefault(epService, false);
        runAssertionOutputDefault(epService, true);
    }

    private void runAssertionOutputDefault(EPServiceProvider epService, boolean join) {
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean_A.class);
        String epl = "select irstream sum(intPrimitive) as c0, last(theString) as c1 from SupportBean#length(2) " +
                (join ? ",SupportBean_A#keepall " : "") +
                "output every 3 events order by sum(intPrimitive) desc";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean_A("A1"));
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 10));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 11));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean("E3", 12));

        String[] fields = "c0,c1".split(",");
        EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), fields, new Object[][] {{23, "E3"}, {21, "E2"}, {10, "E1"}});
        EPAssertionUtil.assertPropsPerRow(listener.getLastOldData(), fields, new Object[][] {{21, "E2"}, {10, "E1"}, {null, null}});

        stmt.destroy();
    }

    private void runAssertionNoOutputRateNonJoin(EPServiceProvider epService) {
        // JIRA ESPER-644 Infinite loop when restarting a statement
        epService.getEPAdministrator().getConfiguration().addEventType("FB", Collections.<String, Object>singletonMap("timeTaken", double.class));
        EPStatement stmt = epService.getEPAdministrator().createEPL("select avg(timeTaken) as timeTaken from FB order by timeTaken desc");
        stmt.stop();
        stmt.start();
        stmt.destroy();
    }

    private void runAssertionNoOutputRateJoin(EPServiceProvider epService) {
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

        statement.destroy();
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
