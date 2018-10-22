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
package com.espertech.esper.common.client.dataflow.core;

import com.espertech.esper.common.client.dataflow.util.EPDataFlowSignal;

/**
 * Emitter for use with data flow operators
 */
public interface EPDataFlowEmitterOperator {
    /**
     * Returns emitter name
     *
     * @return name
     */
    String getName();

    /**
     * Submit an underlying event
     *
     * @param underlying to process
     */
    void submit(Object underlying);

    /**
     * Submit a signal
     *
     * @param signal to process
     */
    void submitSignal(EPDataFlowSignal signal);

    /**
     * Submit an underlying event to a given port
     *
     * @param object     to process
     * @param portNumber port
     */
    void submitPort(int portNumber, Object object);
}
