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
package com.espertech.esper.regressionlib.support.dataflow;

import com.espertech.esper.common.client.dataflow.annotations.DataFlowContext;
import com.espertech.esper.common.client.dataflow.util.EPDataFlowSignalFinalMarker;
import com.espertech.esper.common.internal.epl.dataflow.interfaces.DataFlowOpCloseContext;
import com.espertech.esper.common.internal.epl.dataflow.interfaces.DataFlowOpOpenContext;
import com.espertech.esper.common.internal.epl.dataflow.interfaces.DataFlowSourceOperator;
import com.espertech.esper.common.internal.epl.dataflow.interfaces.EPDataFlowEmitter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Iterator;

public class MyObjectArrayGraphSource implements DataFlowSourceOperator {
    private static final Logger log = LoggerFactory.getLogger(MyObjectArrayGraphSource.class);

    private final Iterator<Object[]> iterator;

    @DataFlowContext
    private EPDataFlowEmitter graphContext;

    public MyObjectArrayGraphSource(Iterator<Object[]> iterator) {
        this.iterator = iterator;
    }

    public void next() {
        if (iterator.hasNext()) {
            Object[] next = iterator.next();
            if (log.isDebugEnabled()) {
                log.debug("submitting row " + Arrays.toString(next));
            }
            graphContext.submit(next);
        } else {
            if (log.isDebugEnabled()) {
                log.debug("submitting punctuation");
            }
            graphContext.submitSignal(new EPDataFlowSignalFinalMarker() {
            });
        }
    }

    public void open(DataFlowOpOpenContext openContext) {
    }

    public void close(DataFlowOpCloseContext openContext) {
    }
}
