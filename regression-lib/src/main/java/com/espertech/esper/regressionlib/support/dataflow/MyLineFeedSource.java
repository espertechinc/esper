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

import java.util.Iterator;

public class MyLineFeedSource implements DataFlowSourceOperator {

    @DataFlowContext
    private EPDataFlowEmitter dataFlowEmitter;

    private final Iterator<String> lines;

    public MyLineFeedSource(Iterator<String> lines) {
        this.lines = lines;
    }

    public void open(DataFlowOpOpenContext openContext) {
    }

    public void next() {
        if (lines.hasNext()) {
            dataFlowEmitter.submit(new Object[]{lines.next()});
        } else {
            dataFlowEmitter.submitSignal(new EPDataFlowSignalFinalMarker() {
            });
        }
    }

    public void close(DataFlowOpCloseContext openContext) {
    }
}
