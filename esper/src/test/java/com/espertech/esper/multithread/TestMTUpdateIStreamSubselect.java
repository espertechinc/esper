package com.espertech.esper.multithread;/*
 * *************************************************************************************
 *  Copyright (C) 2006-2015 EsperTech, Inc. All rights reserved.                       *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 * *************************************************************************************
 */

import com.espertech.esper.client.*;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.support.bean.SupportBean;
import com.espertech.esper.support.bean.SupportBean_S0;
import com.espertech.esper.support.client.SupportConfigFactory;
import junit.framework.TestCase;

import java.util.LinkedList;
import java.util.List;

public class TestMTUpdateIStreamSubselect extends TestCase
{
    private EPServiceProvider engine;
    private SupportUpdateListener listener;

    public void setUp()
    {
        Configuration config = SupportConfigFactory.getConfiguration();
        engine = EPServiceProviderManager.getProvider("TestMTUpdate", config);
        listener = new SupportUpdateListener();
    }

    public void tearDown()
    {
        engine.initialize();
    }

    public void testUpdateIStreamSubselect() throws InterruptedException
    {
        engine.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);
        engine.getEPAdministrator().getConfiguration().addEventType(SupportBean_S0.class);
        EPStatement stmt = engine.getEPAdministrator().createEPL("update istream SupportBean as sb " +
                        "set longPrimitive = (select count(*) from SupportBean_S0#keepall() as s0 where s0.p00 = sb.theString)");
        stmt.addListener(listener);

        // insert 5 data events for each symbol
        int numGroups = 20;
        int numRepeats = 5;
        for (int i = 0; i < numGroups; i++) {
            for (int j = 0; j < numRepeats; j++) {
                engine.getEPRuntime().sendEvent(new SupportBean_S0(i, "S0_" + i)); // S0_0 .. S0_19 each has 5 events
            }
        }

        List<Thread> threads = new LinkedList<Thread>();
        for (int i = 0; i < numGroups; i++ ){
            final int group = i;
            final Thread t = new Thread( new Runnable() {
                public void run() {
                    engine.getEPRuntime().sendEvent(new SupportBean("S0_" + group, 1));
                }
            });
            threads.add(t);
            t.start();
        }
        for( Thread t: threads ){
            t.join();
        }

        // validate results, price must be 5 for each symbol
        assertEquals(numGroups, listener.getNewDataList().size());
        for (EventBean[] newData : listener.getNewDataList()){
            SupportBean result = (SupportBean) (newData[0]).getUnderlying();
            assertEquals(numRepeats, result.getLongPrimitive());
        }
    }
}

