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
import com.espertech.esper.core.service.EPRuntimeSPI;
import com.espertech.esper.regression.multithread.ExecMTStmtNamedWindowUpdate;
import com.espertech.esper.supportregression.bean.SupportBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;

public class StmtNamedWindowUpdateCallable implements Callable<StmtNamedWindowUpdateCallable.UpdateResult> {
    private final EPRuntimeSPI engine;
    private final int numRepeats;
    private final String threadName;
    private final List<UpdateItem> updates = new ArrayList<UpdateItem>();

    public StmtNamedWindowUpdateCallable(String threadName, EPServiceProvider engine, int numRepeats) {
        this.engine = (EPRuntimeSPI) engine.getEPRuntime();
        this.numRepeats = numRepeats;
        this.threadName = threadName;
    }

    public UpdateResult call() throws Exception {
        long start = System.currentTimeMillis();
        try {
            Random random = new Random();
            for (int loop = 0; loop < numRepeats; loop++) {
                String theString = Integer.toString(Math.abs(random.nextInt()) % ExecMTStmtNamedWindowUpdate.NUM_STRINGS);
                int intPrimitive = Math.abs(random.nextInt()) % ExecMTStmtNamedWindowUpdate.NUM_INTS;
                int doublePrimitive = Math.abs(random.nextInt()) % 10;
                sendEvent(theString, intPrimitive, doublePrimitive);
            }
        } catch (Exception ex) {
            log.error("Error in thread " + Thread.currentThread().getId(), ex);
            return null;
        }
        long end = System.currentTimeMillis();
        return new UpdateResult(end - start, updates);
    }

    private void sendEvent(String theString, int intPrimitive, double doublePrimitive) {
        SupportBean bean = new SupportBean(theString, intPrimitive);
        bean.setBoolPrimitive(false);
        bean.setDoublePrimitive(doublePrimitive);
        engine.sendEvent(bean);
        updates.add(new UpdateItem(theString, intPrimitive, doublePrimitive));
    }

    public static class UpdateResult {
        private long delta;
        private List<UpdateItem> updates;

        public UpdateResult(long delta, List<UpdateItem> updates) {
            this.delta = delta;
            this.updates = updates;
        }

        public long getDelta() {
            return delta;
        }

        public List<UpdateItem> getUpdates() {
            return updates;
        }
    }

    public static class UpdateItem {
        private String theString;
        private int intval;
        private double doublePrimitive;

        public UpdateItem(String theString, int intval, double doublePrimitive) {
            this.theString = theString;
            this.intval = intval;
            this.doublePrimitive = doublePrimitive;
        }

        public String getTheString() {
            return theString;
        }

        public int getIntval() {
            return intval;
        }

        public double getDoublePrimitive() {
            return doublePrimitive;
        }
    }

    private static final Logger log = LoggerFactory.getLogger(StmtNamedWindowUpdateCallable.class);
}
