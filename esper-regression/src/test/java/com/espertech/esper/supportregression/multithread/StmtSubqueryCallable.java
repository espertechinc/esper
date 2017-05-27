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
import com.espertech.esper.supportregression.bean.SupportBean_S0;
import com.espertech.esper.supportregression.bean.SupportBean_S1;
import junit.framework.AssertionFailedError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;

public class StmtSubqueryCallable implements Callable {
    private final int threadNum;
    private final EPServiceProvider engine;
    private final int numRepeats;

    public StmtSubqueryCallable(int threadNum, EPServiceProvider engine, int numRepeats) {
        this.threadNum = threadNum;
        this.engine = engine;
        this.numRepeats = numRepeats;
    }

    public Object call() throws Exception {
        try {
            for (int loop = 0; loop < numRepeats; loop++) {
                int id = threadNum * 10000000 + loop;
                Object eventS0 = new SupportBean_S0(id);
                Object eventS1 = new SupportBean_S1(id);

                engine.getEPRuntime().sendEvent(eventS0);
                engine.getEPRuntime().sendEvent(eventS1);
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

    private static final Logger log = LoggerFactory.getLogger(StmtSubqueryCallable.class);
}
