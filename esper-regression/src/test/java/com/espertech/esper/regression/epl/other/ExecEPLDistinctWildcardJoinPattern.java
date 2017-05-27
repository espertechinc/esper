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
package com.espertech.esper.regression.epl.other;

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.scopetest.SupportSubscriberMRD;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ExecEPLDistinctWildcardJoinPattern implements RegressionExecution {
    public void run(EPServiceProvider epService) throws Exception {
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);

        String epl = "select distinct * from " +
                "SupportBean(intPrimitive=0) as fooB unidirectional " +
                "inner join " +
                "pattern [" +
                "every-distinct(fooA.theString) fooA=SupportBean(intPrimitive=1)" +
                "->" +
                "every-distinct(wooA.theString) wooA=SupportBean(intPrimitive=2)" +
                " where timer:within(1 hour)" +
                "]#time(1 hour) as fooWooPair " +
                "on fooB.longPrimitive = fooWooPair.fooA.longPrimitive" +
                " order by fooWooPair.wooA.theString asc";

        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportSubscriberMRD subscriber = new SupportSubscriberMRD();
        stmt.setSubscriber(subscriber);

        sendEvent(epService, "E1", 1, 10L);
        sendEvent(epService, "E2", 2, 10L);
        sendEvent(epService, "E3", 2, 10L);
        sendEvent(epService, "Query", 0, 10L);

        assertTrue(subscriber.isInvoked());
        assertEquals(1, subscriber.getInsertStreamList().size());
        Object[][] inserted = subscriber.getInsertStreamList().get(0);
        assertEquals(2, inserted.length);
        assertEquals("Query", ((SupportBean) inserted[0][0]).getTheString());
        assertEquals("Query", ((SupportBean) inserted[1][0]).getTheString());
        Map mapOne = (Map) inserted[0][1];
        assertEquals("E2", ((EventBean) mapOne.get("wooA")).get("theString"));
        assertEquals("E1", ((EventBean) mapOne.get("fooA")).get("theString"));
        Map mapTwo = (Map) inserted[1][1];
        assertEquals("E3", ((EventBean) mapTwo.get("wooA")).get("theString"));
        assertEquals("E1", ((EventBean) mapTwo.get("fooA")).get("theString"));
    }

    private void sendEvent(EPServiceProvider epService, String theString, int intPrimitive, long longPrimitive) {
        SupportBean bean = new SupportBean(theString, intPrimitive);
        bean.setLongPrimitive(longPrimitive);
        epService.getEPRuntime().sendEvent(bean);
    }
}
