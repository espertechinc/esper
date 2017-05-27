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
import com.espertech.esper.supportregression.bean.SupportBean;
import junit.framework.AssertionFailedError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;

public class StmtUpdateSendCallable implements Callable {
    private static final Logger log = LoggerFactory.getLogger(StmtUpdateSendCallable.class);
    private final int threadNum;
    private final EPServiceProvider engine;
    private final int numRepeats;

    public StmtUpdateSendCallable(int threadNum, EPServiceProvider engine, int numRepeats) {
        this.threadNum = threadNum;
        this.engine = engine;
        this.numRepeats = numRepeats;
    }

    public Object call() throws Exception {
        try {
            log.info(".call Thread " + Thread.currentThread().getId() + " sending " + numRepeats + " events");
            for (int loop = 0; loop < numRepeats; loop++) {
                String id = Long.toString(threadNum * 100000000 + loop);
                SupportBean bean = new SupportBean(id, 0);
                engine.getEPRuntime().sendEvent(bean);
            }
            log.info(".call Thread " + Thread.currentThread().getId() + " completed.");
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
