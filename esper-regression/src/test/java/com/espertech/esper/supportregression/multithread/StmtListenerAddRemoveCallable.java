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
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.supportregression.bean.SupportMarketDataBean;
import com.espertech.esper.supportregression.util.LogUpdateListener;
import com.espertech.esper.supportregression.util.SupportMTUpdateListener;
import com.espertech.esper.util.ThreadLogUtil;
import junit.framework.AssertionFailedError;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;

public class StmtListenerAddRemoveCallable implements Callable {
    private final EPServiceProvider engine;
    private final EPStatement stmt;
    private final boolean isEPL;
    private final int numRepeats;

    public StmtListenerAddRemoveCallable(EPServiceProvider engine, EPStatement stmt, boolean isEPL, int numRepeats) {
        this.engine = engine;
        this.stmt = stmt;
        this.isEPL = isEPL;
        this.numRepeats = numRepeats;
    }

    public Object call() throws Exception {
        try {
            for (int loop = 0; loop < numRepeats; loop++) {
                // Add assertListener
                SupportMTUpdateListener assertListener = new SupportMTUpdateListener();
                LogUpdateListener logListener;
                if (isEPL) {
                    logListener = new LogUpdateListener(null);
                } else {
                    logListener = new LogUpdateListener("a");
                }
                ThreadLogUtil.trace("adding listeners ", assertListener, logListener);
                stmt.addListener(assertListener);
                stmt.addListener(logListener);

                // send event
                Object theEvent = makeEvent();
                ThreadLogUtil.trace("sending event ", theEvent);
                engine.getEPRuntime().sendEvent(theEvent);

                // Should have received one or more events, one of them must be mine
                EventBean[] newEvents = assertListener.getNewDataListFlattened();
                Assert.assertTrue("No event received", newEvents.length >= 1);
                ThreadLogUtil.trace("assert received, size is", newEvents.length);
                boolean found = false;
                for (int i = 0; i < newEvents.length; i++) {
                    Object underlying = newEvents[i].getUnderlying();
                    if (!isEPL) {
                        underlying = newEvents[i].get("a");
                    }
                    if (underlying == theEvent) {
                        found = true;
                    }
                }
                Assert.assertTrue(found);
                assertListener.reset();

                // Remove assertListener
                ThreadLogUtil.trace("removing assertListener");
                stmt.removeListener(assertListener);
                stmt.removeListener(logListener);

                // Send another event
                theEvent = makeEvent();
                ThreadLogUtil.trace("send non-matching event ", theEvent);
                engine.getEPRuntime().sendEvent(theEvent);

                // Make sure the event was not received
                newEvents = assertListener.getNewDataListFlattened();
                found = false;
                for (int i = 0; i < newEvents.length; i++) {
                    Object underlying = newEvents[i].getUnderlying();
                    if (!isEPL) {
                        underlying = newEvents[i].get("a");
                    }
                    if (underlying == theEvent) {
                        found = true;
                    }
                }
                Assert.assertFalse(found);
            }
        } catch (AssertionFailedError ex) {
            log.error("Assertion error in thread " + Thread.currentThread().getId(), ex);
            return false;
        } catch (Exception ex) {
            log.error("Error in thread " + Thread.currentThread().getId(), ex);
            return false;
        }
        return true;
    }

    private SupportMarketDataBean makeEvent() {
        SupportMarketDataBean theEvent = new SupportMarketDataBean("IBM", 50, 1000L, "RT");
        return theEvent;
    }

    private static final Logger log = LoggerFactory.getLogger(StmtListenerAddRemoveCallable.class);
}
