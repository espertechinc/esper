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

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.util.SafeIterator;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.runtime.client.EPRuntime;
import com.espertech.esper.runtime.client.EPStatement;
import junit.framework.AssertionFailedError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;

import static org.junit.Assert.assertTrue;

public class StmtIterateCallable implements Callable {
    private static final Logger log = LoggerFactory.getLogger(StmtIterateCallable.class);
    private final int threadNum;
    private final EPRuntime runtime;
    private final EPStatement[] stmt;
    private final int numRepeats;

    public StmtIterateCallable(int threadNum, EPRuntime runtime, EPStatement[] stmt, int numRepeats) {
        this.threadNum = threadNum;
        this.runtime = runtime;
        this.stmt = stmt;
        this.numRepeats = numRepeats;
    }

    public Object call() throws Exception {
        try {
            for (int loop = 0; loop < numRepeats; loop++) {
                log.info(".call Thread " + Thread.currentThread().getId() + " sending event " + loop);
                String id = Long.toString(threadNum * 100000000 + loop);
                SupportBean bean = new SupportBean(id, 0);
                runtime.getEventService().sendEventBean(bean, bean.getClass().getSimpleName());

                for (int i = 0; i < stmt.length; i++) {
                    log.info(".call Thread " + Thread.currentThread().getId() + " starting iterator " + loop);
                    SafeIterator<EventBean> it = stmt[i].safeIterator();
                    boolean found = false;
                    for (; it.hasNext(); ) {
                        EventBean theEvent = it.next();
                        if (theEvent.get("theString").equals(id)) {
                            found = true;
                        }
                    }
                    it.close();
                    assertTrue(found);
                    log.info(".call Thread " + Thread.currentThread().getId() + " end iterator " + loop);
                }
            }
        } catch (AssertionFailedError ex) {
            log.error("Assertion error in thread " + Thread.currentThread().getId(), ex);
            return false;
        } catch (Throwable t) {
            log.error("Error in thread " + Thread.currentThread().getId(), t);
            return false;
        }
        return true;
    }
}
