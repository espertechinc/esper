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
package com.espertech.esper.regressionlib.support.multithread;

import com.espertech.esper.common.client.EPCompiled;
import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.util.ThreadLogUtil;
import com.espertech.esper.regressionlib.support.bean.SupportMarketDataBean;
import com.espertech.esper.regressionlib.support.util.LogUpdateListener;
import com.espertech.esper.regressionlib.support.util.SupportMTUpdateListener;
import com.espertech.esper.runtime.client.EPDeployment;
import com.espertech.esper.runtime.client.EPRuntime;
import junit.framework.AssertionFailedError;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;

public class StmtMgmtCallable implements Callable {
    private final EPRuntime runtime;
    private final StmtMgmtCallablePair[] statements;
    private final int numRepeats;

    public StmtMgmtCallable(EPRuntime runtime, StmtMgmtCallablePair[] statements, int numRepeats) {
        this.runtime = runtime;
        this.statements = statements;
        this.numRepeats = numRepeats;
    }

    public Object call() throws Exception {
        try {
            for (int loop = 0; loop < numRepeats; loop++) {
                for (StmtMgmtCallablePair statement : statements) {
                    String statementText = statement.getEpl();
                    EPCompiled compiled = statement.getCompiled();

                    // Create EPL or pattern statement
                    ThreadLogUtil.trace("stmt create,", statementText);
                    EPDeployment deployed = runtime.getDeploymentService().deploy(compiled);
                    ThreadLogUtil.trace("stmt done,", statementText);

                    // Add listener
                    SupportMTUpdateListener listener = new SupportMTUpdateListener();
                    LogUpdateListener logListener = new LogUpdateListener(null);
                    ThreadLogUtil.trace("adding listeners ", listener, logListener);
                    deployed.getStatements()[0].addListener(listener);
                    deployed.getStatements()[0].addListener(logListener);

                    Object theEvent = makeEvent();
                    ThreadLogUtil.trace("sending event ", theEvent);
                    runtime.getEventService().sendEventBean(theEvent, theEvent.getClass().getSimpleName());

                    // Should have received one or more events, one of them must be mine
                    EventBean[] newEvents = listener.getNewDataListFlattened();
                    Assert.assertTrue("No event received", newEvents.length >= 1);
                    ThreadLogUtil.trace("assert received, size is", newEvents.length);
                    boolean found = false;
                    for (int i = 0; i < newEvents.length; i++) {
                        Object underlying = newEvents[i].getUnderlying();
                        if (underlying == theEvent) {
                            found = true;
                        }
                    }
                    Assert.assertTrue(found);
                    listener.reset();

                    // Stopping statement, the event should not be received, another event may however
                    ThreadLogUtil.trace("stop statement");
                    runtime.getDeploymentService().undeploy(deployed.getDeploymentId());
                    theEvent = makeEvent();
                    ThreadLogUtil.trace("send non-matching event ", theEvent);
                    runtime.getEventService().sendEventBean(theEvent, theEvent.getClass().getSimpleName());

                    // Make sure the event was not received
                    newEvents = listener.getNewDataListFlattened();
                    found = false;
                    for (int i = 0; i < newEvents.length; i++) {
                        Object underlying = newEvents[i].getUnderlying();
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
        return new SupportMarketDataBean("IBM", 50, 1000L, "RT");
    }

    private static final Logger log = LoggerFactory.getLogger(StmtMgmtCallable.class);
}
