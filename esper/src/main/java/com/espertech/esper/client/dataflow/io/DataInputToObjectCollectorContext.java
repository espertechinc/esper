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
package com.espertech.esper.client.dataflow.io;

import com.espertech.esper.dataflow.interfaces.EPDataFlowEmitter;

import java.io.DataInput;

/**
 * Context for use with {@link DataInputToObjectCollector} carries data input and emitter.
 */
public class DataInputToObjectCollectorContext {
    private EPDataFlowEmitter emitter;
    private DataInput dataInput;

    /**
     * Returns the emitter.
     *
     * @return emitter
     */
    public EPDataFlowEmitter getEmitter() {
        return emitter;
    }

    /**
     * Sets the emitter
     *
     * @param emitter emitter
     */
    public void setEmitter(EPDataFlowEmitter emitter) {
        this.emitter = emitter;
    }

    /**
     * Returns the data input.
     *
     * @return data input
     */
    public DataInput getDataInput() {
        return dataInput;
    }

    /**
     * Sets the data input.
     *
     * @param dataInput data input
     */
    public void setDataInput(DataInput dataInput) {
        this.dataInput = dataInput;
    }
}
