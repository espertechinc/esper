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
package com.espertech.esper.supportregression.multithread;

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderIsolated;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.util.SupportMTUpdateListener;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.Callable;

public class IsolateUnisolateCallable implements Callable {
    private final int threadNum;
    private final EPServiceProvider engine;
    private final int loopCount;

    public IsolateUnisolateCallable(int threadNum, EPServiceProvider engine, int loopCount) {
        this.threadNum = threadNum;
        this.engine = engine;
        this.loopCount = loopCount;
    }

    public Object call() throws Exception {
        SupportMTUpdateListener listenerIsolated = new SupportMTUpdateListener();
        SupportMTUpdateListener listenerUnisolated = new SupportMTUpdateListener();
        EPStatement stmt = engine.getEPAdministrator().createEPL("select * from SupportBean");

        try {
            for (int i = 0; i < loopCount; i++) {
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
        } catch (Exception ex) {
            log.error("Error in thread " + threadNum, ex);
            return false;
        }
        return true;
    }

    private void findEvent(SupportMTUpdateListener listener, int loop, Object theEvent) {
        String message = "Failed in loop " + loop + " threads " + Thread.currentThread();
        Assert.assertTrue(message, listener.isInvoked());
        List<EventBean[]> eventBeans = listener.getNewDataListCopy();
        boolean found = false;
        for (EventBean[] events : eventBeans) {
            Assert.assertEquals(message, 1, events.length);
            if (events[0].getUnderlying() == theEvent) {
                found = true;
            }
        }
        Assert.assertTrue(message, found);
        listener.reset();
    }

    private static final Logger log = LoggerFactory.getLogger(IsolateUnisolateCallable.class);
}
