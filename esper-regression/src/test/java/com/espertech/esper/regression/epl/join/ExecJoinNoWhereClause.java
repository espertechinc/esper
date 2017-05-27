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
package com.espertech.esper.regression.epl.join;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportMarketDataBean;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import static org.junit.Assert.assertEquals;

public class ExecJoinNoWhereClause implements RegressionExecution {
    public void configure(Configuration configuration) throws Exception {
        configuration.getEngineDefaults().getThreading().setListenerDispatchPreserveOrder(false);
        configuration.getEngineDefaults().getViewResources().setShareViews(false);
    }

    public void run(EPServiceProvider epService) throws Exception {
        runAssertionJoinWInnerKeywordWOOnClause(epService);
        runAssertionJoinNoWhereClause(epService);
    }

    private void runAssertionJoinWInnerKeywordWOOnClause(EPServiceProvider epService) {
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);

        String[] fields = "a.theString,b.theString".split(",");
        String epl = "select * from SupportBean(theString like 'A%')#length(3) as a inner join SupportBean(theString like 'B%')#length(3) as b " +
                "where a.intPrimitive = b.intPrimitive";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        sendEvent(epService, "A1", 1);
        sendEvent(epService, "A2", 2);
        sendEvent(epService, "A3", 3);
        sendEvent(epService, "B2", 2);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"A2", "B2"});

        stmt.destroy();
    }

    private void runAssertionJoinNoWhereClause(EPServiceProvider epService) {
        String[] fields = new String[]{"stream_0.volume", "stream_1.longBoxed"};
        String joinStatement = "select * from " +
                SupportMarketDataBean.class.getName() + "#length(3)," +
                SupportBean.class.getName() + "()#length(3)";

        EPStatement stmt = epService.getEPAdministrator().createEPL(joinStatement);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        Object[] setOne = new Object[5];
        Object[] setTwo = new Object[5];
        for (int i = 0; i < setOne.length; i++) {
            setOne[i] = new SupportMarketDataBean("IBM", 0, (long) i, "");

            SupportBean theEvent = new SupportBean();
            theEvent.setLongBoxed((long) i);
            setTwo[i] = theEvent;
        }

        // Send 2 events, should join on second one
        sendEvent(epService, setOne[0]);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, null);

        sendEvent(epService, setTwo[0]);
        assertEquals(1, listener.getLastNewData().length);
        assertEquals(setOne[0], listener.getLastNewData()[0].get("stream_0"));
        assertEquals(setTwo[0], listener.getLastNewData()[0].get("stream_1"));
        listener.reset();
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields,
                new Object[][]{{0L, 0L}});

        sendEvent(epService, setOne[1]);
        sendEvent(epService, setOne[2]);
        sendEvent(epService, setTwo[1]);
        assertEquals(3, listener.getLastNewData().length);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields,
                new Object[][]{{0L, 0L},
                    {1L, 0L},
                    {2L, 0L},
                    {0L, 1L},
                    {1L, 1L},
                    {2L, 1L}});

        stmt.destroy();
    }

    private void sendEvent(EPServiceProvider epService, String theString, int intPrimitive) {
        sendEvent(epService, new SupportBean(theString, intPrimitive));
    }

    private void sendEvent(EPServiceProvider epService, Object theEvent) {
        epService.getEPRuntime().sendEvent(theEvent);
    }
}
