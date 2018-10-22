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
package com.espertech.esper.common.internal.epl.dataflow.util;

import com.espertech.esper.common.client.dataflow.annotations.DataFlowContext;
import com.espertech.esper.common.client.dataflow.util.EPDataFlowSignalFinalMarker;
import com.espertech.esper.common.internal.epl.dataflow.interfaces.DataFlowOpCloseContext;
import com.espertech.esper.common.internal.epl.dataflow.interfaces.DataFlowOpOpenContext;
import com.espertech.esper.common.internal.epl.dataflow.interfaces.DataFlowSourceOperator;
import com.espertech.esper.common.internal.epl.dataflow.interfaces.EPDataFlowEmitter;

import java.util.concurrent.CountDownLatch;

public class DefaultSupportSourceOp implements DataFlowSourceOperator {
    public Object[] instructions;

    public DefaultSupportSourceOp() {
        this.instructions = new Object[0];
    }

    public DefaultSupportSourceOp(Object[] instructions) {
        this.instructions = instructions;
    }

    @DataFlowContext
    private EPDataFlowEmitter graphContext;

    private int currentCount = -1;

    public int getCurrentCount() {
        return currentCount;
    }

    public void next() throws InterruptedException {
        currentCount++;
        if (instructions.length <= currentCount) {
            graphContext.submitSignal(new EPDataFlowSignalFinalMarker() {
            });
            return;
        }

        Object next = instructions[currentCount];
        if (next instanceof CountDownLatch) {
            CountDownLatch latch = (CountDownLatch) next;
            latch.await();
        } else if (next instanceof Long) {
            long sleepTime = (Long) next;
            Thread.sleep(sleepTime);
        } else if (next instanceof RuntimeException) {
            RuntimeException ex = (RuntimeException) next;
            throw new RuntimeException("Support-graph-source generated exception: " + ex.getMessage(), ex);
        } else {
            graphContext.submit(next);
        }
    }

    public void open(DataFlowOpOpenContext openContext) {
    }

    public void close(DataFlowOpCloseContext openContext) {
    }
}
