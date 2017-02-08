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
package com.espertech.esper.dataflow.core;

import com.espertech.esper.client.dataflow.EPDataFlowSignal;
import com.espertech.esper.dataflow.interfaces.EPDataFlowEmitter;
import com.espertech.esper.dataflow.util.DataFlowSignalManager;

public class EPDataFlowEmitterNStreamNTarget implements EPDataFlowEmitter {

    private final int operatorNum;
    private final DataFlowSignalManager signalManager;
    private final SubmitHandler[][] handlersPerStream;

    public EPDataFlowEmitterNStreamNTarget(int operatorNum, DataFlowSignalManager signalManager, SubmitHandler[][] handlersPerStream) {
        this.operatorNum = operatorNum;
        this.signalManager = signalManager;
        this.handlersPerStream = handlersPerStream;
    }

    public void submit(Object object) {
        throw new UnsupportedOperationException("Submit to a specific port is excepted");
    }

    public void submitSignal(EPDataFlowSignal signal) {
        signalManager.processSignal(operatorNum, signal);
        for (SubmitHandler[] handlerArr : handlersPerStream) {
            for (SubmitHandler handler : handlerArr) {
                handler.handleSignal(signal);
            }
        }
    }

    public void submitPort(int portNumber, Object object) {
        SubmitHandler[] targets = handlersPerStream[portNumber];
        for (SubmitHandler handler : targets) {
            handler.submitInternal(object);
        }
    }
}
