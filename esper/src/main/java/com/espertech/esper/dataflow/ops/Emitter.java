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
package com.espertech.esper.dataflow.ops;

import com.espertech.esper.client.dataflow.EPDataFlowSignal;
import com.espertech.esper.dataflow.annotations.DataFlowContext;
import com.espertech.esper.dataflow.annotations.DataFlowOpParameter;
import com.espertech.esper.dataflow.annotations.DataFlowOpProvideSignal;
import com.espertech.esper.dataflow.annotations.DataFlowOperator;
import com.espertech.esper.dataflow.interfaces.EPDataFlowEmitter;

@DataFlowOperator
@DataFlowOpProvideSignal
public class Emitter implements EPDataFlowEmitter {

    @DataFlowOpParameter
    private String name;

    @DataFlowContext
    private EPDataFlowEmitter dataFlowEmitter;

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
