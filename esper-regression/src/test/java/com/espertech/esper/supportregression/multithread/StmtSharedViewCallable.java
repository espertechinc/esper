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
import com.espertech.esper.supportregression.bean.SupportMarketDataBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;

public class StmtSharedViewCallable implements Callable {
    private final int numRepeats;
    private final EPServiceProvider engine;
    private final String[] symbols;

    public StmtSharedViewCallable(int numRepeats, EPServiceProvider engine, String[] symbols) {
        this.numRepeats = numRepeats;
        this.engine = engine;
        this.symbols = symbols;
    }

    public Object call() throws Exception {
        try {
            for (int loop = 0; loop < numRepeats; loop++) {
                for (String symbol : symbols) {
                    Object theEvent = makeEvent(symbol, loop);
                    engine.getEPRuntime().sendEvent(theEvent);
                }
            }
        } catch (Exception ex) {
            log.error("Error in thread " + Thread.currentThread().getId(), ex);
            return false;
        }
        return true;
    }

    private SupportMarketDataBean makeEvent(String symbol, double price) {
        return new SupportMarketDataBean(symbol, price, null, null);
    }

    private static final Logger log = LoggerFactory.getLogger(StmtSharedViewCallable.class);
}
