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
import com.espertech.esper.client.SafeIterator;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.supportregression.bean.SupportMarketDataBean;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;

public class StmtNamedWindowIterateCallable implements Callable {
    private final EPServiceProvider engine;
    private final int numRepeats;
    private final String threadKey;
    private EPStatement statement;

    public StmtNamedWindowIterateCallable(String threadKey, EPServiceProvider engine, int numRepeats) {
        this.engine = engine;
        this.numRepeats = numRepeats;
        this.threadKey = threadKey;

        statement = engine.getEPAdministrator().createEPL("select theString, sum(longPrimitive) as sumLong from MyWindow group by theString");
    }

    public Object call() throws Exception {
        try {
            long total = 0;
            for (int loop = 0; loop < numRepeats; loop++) {
                // Insert event into named window
                sendMarketBean(threadKey, loop + 1);
                total += loop + 1;

                // iterate over private statement
                SafeIterator safeIter = statement.safeIterator();
                EventBean[] received = EPAssertionUtil.iteratorToArray(safeIter);
                safeIter.close();

                for (int i = 0; i < received.length; i++) {
                    if (received[i].get("theString").equals(threadKey)) {
                        long sum = (Long) received[i].get("sumLong");
                        Assert.assertEquals(total, sum);
                    }
                }
            }
        } catch (Exception ex) {
            log.error("Error in thread " + Thread.currentThread().getId(), ex);
            return false;
        }
        return true;
    }

    private void sendMarketBean(String symbol, long volume) {
        SupportMarketDataBean bean = new SupportMarketDataBean(symbol, 0, volume, "");
        engine.getEPRuntime().sendEvent(bean);
    }

    private static final Logger log = LoggerFactory.getLogger(StmtNamedWindowIterateCallable.class);
}
