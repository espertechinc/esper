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

package com.espertech.esper.multithread;

import com.espertech.esper.client.*;
import com.espertech.esper.support.bean.SupportBean;
import com.espertech.esper.support.util.SupportMTUpdateListener;
import org.junit.Assert;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.concurrent.Callable;
import java.util.List;

public class IsolateUnisolateCallable implements Callable
{
    private final int threadNum;
    private final EPServiceProvider engine;
    private final int loopCount;

    public IsolateUnisolateCallable(int threadNum, EPServiceProvider engine, int loopCount)
    {
        this.threadNum = threadNum;
        this.engine = engine;
        this.loopCount = loopCount;
    }

    public Object call() throws Exception
    {
        SupportMTUpdateListener listenerIsolated = new SupportMTUpdateListener();
        SupportMTUpdateListener listenerUnisolated = new SupportMTUpdateListener();
        EPStatement stmt = engine.getEPAdministrator().createEPL("select * from SupportBean");

        try
        {
            for (int i = 0; i < loopCount; i++)
            {
                EPServiceProviderIsolated isolated = engine.getEPServiceIsolated("i1");
                isolated.getEPAdministrator().addStatement(stmt);

                listenerIsolated.reset();
                stmt.addListener(listenerIsolated);
                Object theEvent = new SupportBean();
                //System.out.println("Sensing event : " + event + " by thread " + Thread.currentThread().getId());
                isolated.getEPRuntime().sendEvent(theEvent);
                findEvent(listenerIsolated, i, theEvent);
                stmt.removeAllListeners();

                isolated.getEPAdministrator().removeStatement(stmt);

                stmt.addListener(listenerUnisolated);
                theEvent = new SupportBean();
                engine.getEPRuntime().sendEvent(theEvent);
                findEvent(listenerUnisolated, i, theEvent);
                stmt.removeAllListeners();
            }
        }
        catch (Exception ex)
        {
            log.fatal("Error in thread " + threadNum, ex);
            return false;
        }
        return true;
    }

    private void findEvent(SupportMTUpdateListener listener, int loop, Object theEvent)
    {
        String message = "Failed in loop " + loop + " threads " + Thread.currentThread();
        Assert.assertTrue(message, listener.isInvoked());
        List<EventBean[]> eventBeans = listener.getNewDataListCopy();
        boolean found = false;
        for (EventBean[] events : eventBeans)
        {
            Assert.assertEquals(message, 1, events.length);
            if (events[0].getUnderlying() == theEvent)
            {
                found = true;
            }
        }
        Assert.assertTrue(message, found);
        listener.reset();
    }

    private static final Log log = LogFactory.getLog(IsolateUnisolateCallable.class);
}