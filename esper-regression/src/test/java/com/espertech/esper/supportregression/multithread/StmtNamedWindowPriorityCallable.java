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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;

public class StmtNamedWindowPriorityCallable implements Callable {
    private final int threadNum;
    private final EPServiceProvider engine;
    private final int numRepeats;

    public StmtNamedWindowPriorityCallable(int threadNum, EPServiceProvider engine, int numRepeats) {
        this.threadNum = threadNum;
        this.engine = engine;
        this.numRepeats = numRepeats;
    }

    public Object call() throws Exception {
        try {
            int offset = threadNum + 1000000;
            for (int i = 0; i < numRepeats; i++) {
                engine.getEPRuntime().sendEvent(new SupportBean_S0(i + offset, "c0_" + i + offset, "p01_" + i + offset));
                engine.getEPRuntime().sendEvent(new SupportBean_S1(i + offset, "c0_" + i + offset, "x", "y"));
            }
        } catch (Exception ex) {
            log.error("Error in thread " + Thread.currentThread().getId(), ex);
            return null;
        }
        return null;
    }

    private static final Logger log = LoggerFactory.getLogger(StmtNamedWindowPriorityCallable.class);
}
