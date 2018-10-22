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
package com.espertech.esper.runtime.internal.dataflow.op.emitter;

import com.espertech.esper.common.client.dataflow.annotations.DataFlowContext;
import com.espertech.esper.common.client.dataflow.core.EPDataFlowEmitterOperator;
import com.espertech.esper.common.client.dataflow.util.EPDataFlowSignal;
import com.espertech.esper.common.internal.epl.dataflow.interfaces.DataFlowOperator;
import com.espertech.esper.common.internal.epl.dataflow.interfaces.EPDataFlowEmitter;

public class EmitterOp implements EPDataFlowEmitter, DataFlowOperator, EPDataFlowEmitterOperator {

    @DataFlowContext
    private EPDataFlowEmitter dataFlowEmitter;

    private final String name;

    public EmitterOp(String name) {
        this.name = name;
    }

    public void submit(Object object) {
        dataFlowEmitter.submit(object);
    }

    public void submitSignal(EPDataFlowSignal signal) {
        dataFlowEmitter.submitSignal(signal);
    }

    public void submitPort(int portNumber, Object object) {
        dataFlowEmitter.submitPort(portNumber, object);
    }

    public String getName() {
        return name;
    }
}
