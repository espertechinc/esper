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

public class StmtMgmtCallable implements Callable {
    private final EPServiceProvider engine;
    private final Object[][] statements;
    private final int numRepeats;

    public StmtMgmtCallable(EPServiceProvider engine, Object[][] statements, int numRepeats) {
        this.engine = engine;
        this.statements = statements;
        this.numRepeats = numRepeats;
    }

    public Object call() throws Exception {
        try {
            for (int loop = 0; loop < numRepeats; loop++) {
                for (Object[] statement : statements) {
                    boolean isEPL = (Boolean) statement[0];
                    String statementText = (String) statement[1];

                    // Create EPL or pattern statement
                    EPStatement stmt;
                    ThreadLogUtil.trace("stmt create,", statementText);
                    if (isEPL) {
                        stmt = engine.getEPAdministrator().createEPL(statementText);
                    } else {
                        stmt = engine.getEPAdministrator().createPattern(statementText);
                    }
                    ThreadLogUtil.trace("stmt done,", stmt);

                    // Add listener
                    SupportMTUpdateListener listener = new SupportMTUpdateListener();
                    LogUpdateListener logListener;
                    if (isEPL) {
                        logListener = new LogUpdateListener(null);
                    } else {
                        logListener = new LogUpdateListener("a");
                    }
                    ThreadLogUtil.trace("adding listeners ", listener, logListener);
                    stmt.addListener(listener);
                    stmt.addListener(logListener);

                    Object theEvent = makeEvent();
                    ThreadLogUtil.trace("sending event ", theEvent);
                    engine.getEPRuntime().sendEvent(theEvent);

                    // Should have received one or more events, one of them must be mine
                    EventBean[] newEvents = listener.getNewDataListFlattened();
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
                    listener.reset();

                    // Stopping statement, the event should not be received, another event may however
                    ThreadLogUtil.trace("stop statement");
                    stmt.stop();
                    theEvent = makeEvent();
                    ThreadLogUtil.trace("send non-matching event ", theEvent);
                    engine.getEPRuntime().sendEvent(theEvent);

                    // Make sure the event was not received
                    newEvents = listener.getNewDataListFlattened();
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

    private static final Logger log = LoggerFactory.getLogger(StmtMgmtCallable.class);
}
