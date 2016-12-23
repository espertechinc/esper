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

package com.espertech.esper.regression.multithread;

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.support.bean.SupportMarketDataBean;
import com.espertech.esper.support.bean.SupportBean_A;

import java.util.concurrent.Callable;
import java.util.List;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StmtNamedWindowDeleteCallable implements Callable
{
    private final EPServiceProvider engine;
    private final int numRepeats;
    private final String threadKey;

    public StmtNamedWindowDeleteCallable(String threadKey, EPServiceProvider engine, int numRepeats)
    {
        this.engine = engine;
        this.numRepeats = numRepeats;
        this.threadKey = threadKey;
    }

    public Object call() throws Exception
    {
        List<String> eventKeys = new ArrayList<String>(numRepeats);
        try
        {
            for (int loop = 0; loop < numRepeats; loop++)
            {
                // Insert event into named window
                String theEvent = "E" + threadKey + "_" + loop;
                eventKeys.add(theEvent);
                sendMarketBean(theEvent, 0);

                // delete same event
                sendSupportBean_A(theEvent);
            }
        }
        catch (Exception ex)
        {
            log.error("Error in thread " + Thread.currentThread().getId(), ex);
            return null;
        }
        return eventKeys;
    }

    private SupportBean_A sendSupportBean_A(String id)
    {
        SupportBean_A bean = new SupportBean_A(id);
        engine.getEPRuntime().sendEvent(bean);
        return bean;
    }

    private void sendMarketBean(String symbol, long volume)
    {
        SupportMarketDataBean bean = new SupportMarketDataBean(symbol, 0, volume, "");
        engine.getEPRuntime().sendEvent(bean);
    }

    private static final Logger log = LoggerFactory.getLogger(StmtNamedWindowDeleteCallable.class);
}
