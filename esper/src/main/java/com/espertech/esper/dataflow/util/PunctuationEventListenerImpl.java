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
package com.espertech.esper.dataflow.util;

import com.espertech.esper.client.dataflow.EPDataFlowSignal;
import com.espertech.esper.client.dataflow.EPDataFlowSignalFinalMarker;
import com.espertech.esper.dataflow.runnables.BaseRunnable;

public class PunctuationEventListenerImpl implements DataFlowSignalListener {
    private final OperatorMetadataDescriptor myOperator;

    private BaseRunnable runnable;

    public PunctuationEventListenerImpl(OperatorMetadataDescriptor myOperator) {
        this.myOperator = myOperator;
    }

    public void setRunnable(BaseRunnable runnable) {
        this.runnable = runnable;
    }

    public void processSignal(EPDataFlowSignal signal) {
        if (signal instanceof EPDataFlowSignalFinalMarker) {
            if (runnable != null) {
                runnable.shutdown();
            }
        }
    }
}
