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

package com.espertech.esper.regression.epl;

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.support.bean.*;
import com.espertech.esper.support.client.SupportConfigFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import junit.framework.TestCase;

public class TestPerf5StreamJoin extends TestCase
{
    private EPServiceProvider epService;
    private SupportUpdateListener updateListener;

    public void setUp()
    {
        epService = EPServiceProviderManager.getDefaultProvider(SupportConfigFactory.getConfiguration());
        epService.initialize();
        updateListener = new SupportUpdateListener();
    }

    protected void tearDown() throws Exception {
        updateListener = null;
    }

    public void testPerfAllProps()
    {
        String statement = "select * from " +
            SupportBean_S0.class.getName() + ".win:length(100000) as s0," +
            SupportBean_S1.class.getName() + ".win:length(100000) as s1," +
            SupportBean_S2.class.getName() + ".win:length(100000) as s2," +
            SupportBean_S3.class.getName() + ".win:length(100000) as s3," +
            SupportBean_S4.class.getName() + ".win:length(100000) as s4" +
            " where s0.p00 = s1.p10 " +
               "and s1.p10 = s2.p20 " +
               "and s2.p20 = s3.p30 " +
               "and s3.p30 = s4.p40 ";

        EPStatement joinView = epService.getEPAdministrator().createEPL(statement);
        joinView.addListener(updateListener);

        log.info(".testPerfAllProps Preloading events");
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 1000; i++)
        {
            sendEvents(new int[] {0,0,0,0,0}, new String[] {"s0"+i, "s1"+i, "s2"+i, "s3"+i, "s4"+i});
        }

        long endTime = System.currentTimeMillis();
        log.info(".testPerfAllProps delta=" + (endTime - startTime));
        assertTrue((endTime - startTime) < 1500);

        // test if join returns data
        assertNull(updateListener.getLastNewData());
        String[] propertyValues = new String[] {"x", "x", "x", "x", "x"};
        int[] ids = new int[] { 1, 2, 3, 4, 5 };
        sendEvents(ids, propertyValues);
        assertEventsReceived(ids);
    }

    private void assertEventsReceived(int[] expectedIds)
    {
        assertEquals(1, updateListener.getLastNewData().length);
        assertNull(updateListener.getLastOldData());
        EventBean theEvent = updateListener.getLastNewData()[0];
        assertEquals(expectedIds[0], ((SupportBean_S0) theEvent.get("s0")).getId());
        assertEquals(expectedIds[1], ((SupportBean_S1) theEvent.get("s1")).getId());
        assertEquals(expectedIds[2], ((SupportBean_S2) theEvent.get("s2")).getId());
        assertEquals(expectedIds[3], ((SupportBean_S3) theEvent.get("s3")).getId());
        assertEquals(expectedIds[4], ((SupportBean_S4) theEvent.get("s4")).getId());
    }

    private void sendEvent(Object theEvent)
    {
        epService.getEPRuntime().sendEvent(theEvent);
    }

    private void sendEvents(int[] ids, String[] propertyValues)
    {
        sendEvent(new SupportBean_S0(ids[0], propertyValues[0]));
        sendEvent(new SupportBean_S1(ids[1], propertyValues[1]));
        sendEvent(new SupportBean_S2(ids[2], propertyValues[2]));
        sendEvent(new SupportBean_S3(ids[3], propertyValues[3]));
        sendEvent(new SupportBean_S4(ids[4], propertyValues[4]));
    }

    private static final Log log = LogFactory.getLog(TestPerf5StreamJoin.class);
}
