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
package com.espertech.esper.supportregression.dataflow;

import com.espertech.esper.client.dataflow.EPDataFlowSignalFinalMarker;
import com.espertech.esper.dataflow.annotations.DataFlowContext;
import com.espertech.esper.dataflow.annotations.DataFlowOpProvideSignal;
import com.espertech.esper.dataflow.annotations.OutputType;
import com.espertech.esper.dataflow.annotations.OutputTypes;
import com.espertech.esper.dataflow.interfaces.*;

import java.util.Iterator;

@OutputTypes(value = {
        @OutputType(name = "line", typeName = "String")
})
@DataFlowOpProvideSignal
public class MyLineFeedSource implements DataFlowSourceOperator {

    @DataFlowContext
    private EPDataFlowEmitter dataFlowEmitter;

    private final Iterator<String> lines;

    public MyLineFeedSource(Iterator<String> lines) {
        this.lines = lines;
    }

    public DataFlowOpInitializeResult initialize(DataFlowOpInitializateContext context) throws Exception {
        return null;
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
