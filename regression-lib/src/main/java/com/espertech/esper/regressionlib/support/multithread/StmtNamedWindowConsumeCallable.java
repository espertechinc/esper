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

import com.espertech.esper.regressionlib.support.bean.SupportMarketDataBean;
import com.espertech.esper.runtime.client.EPRuntime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

public class StmtNamedWindowConsumeCallable implements Callable {
    private final EPRuntime runtime;
    private final int numRepeats;
    private final String threadKey;

    public StmtNamedWindowConsumeCallable(String threadKey, EPRuntime runtime, int numRepeats) {
        this.runtime = runtime;
        this.numRepeats = numRepeats;
        this.threadKey = threadKey;
    }

    public Object call() throws Exception {
        List<String> eventKeys = new ArrayList<String>(numRepeats);
        try {
            for (int loop = 0; loop < numRepeats; loop++) {
                // Insert event into named window
                String theEvent = "E" + threadKey + "_" + loop;
                eventKeys.add(theEvent);
                sendMarketBean(theEvent, 0);
            }
        } catch (Exception ex) {
            log.error("Error in thread " + Thread.currentThread().getId(), ex);
            return null;
        }
        return eventKeys;
    }

    private void sendMarketBean(String symbol, long volume) {
        SupportMarketDataBean bean = new SupportMarketDataBean(symbol, 0, volume, "");
        runtime.getEventService().sendEventBean(bean, bean.getClass().getSimpleName());
    }

    private static final Logger log = LoggerFactory.getLogger(StmtNamedWindowConsumeCallable.class);
}
