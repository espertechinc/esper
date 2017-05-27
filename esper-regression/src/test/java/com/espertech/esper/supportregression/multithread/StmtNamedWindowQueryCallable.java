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

import com.espertech.esper.client.EPOnDemandQueryResult;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.core.service.EPRuntimeSPI;
import com.espertech.esper.supportregression.bean.SupportMarketDataBean;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;

public class StmtNamedWindowQueryCallable implements Callable {
    private final EPRuntimeSPI engine;
    private final int numRepeats;
    private final String threadKey;

    public StmtNamedWindowQueryCallable(String threadKey, EPServiceProvider engine, int numRepeats) {
        this.engine = (EPRuntimeSPI) engine.getEPRuntime();
        this.numRepeats = numRepeats;
        this.threadKey = threadKey;
    }

    public Object call() throws Exception {
        try {
            long total = 0;
            for (int loop = 0; loop < numRepeats; loop++) {
                // Insert event into named window
                sendMarketBean(threadKey, loop);
                total++;

                String selectQuery = "select * from MyWindow where theString='" + threadKey + "' and longPrimitive=" + loop;
                EPOnDemandQueryResult queryResult = engine.executeQuery(selectQuery);
                Assert.assertEquals(1, queryResult.getArray().length);
                Assert.assertEquals(threadKey, queryResult.getArray()[0].get("theString"));
                Assert.assertEquals((long) loop, queryResult.getArray()[0].get("longPrimitive"));
            }
        } catch (Exception ex) {
            log.error("Error in thread " + Thread.currentThread().getId(), ex);
            return false;
        }
        return true;
    }

    private void sendMarketBean(String symbol, long volume) {
        SupportMarketDataBean bean = new SupportMarketDataBean(symbol, 0, volume, "");
        engine.sendEvent(bean);
    }

    private static final Logger log = LoggerFactory.getLogger(StmtNamedWindowQueryCallable.class);
}
