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
package com.espertech.esper.client.dataflow;

import com.espertech.esper.client.EPException;

/**
 * Indicates cancellation of a data flow instance.
 */
public class EPDataFlowCancellationException extends EPException {

    private static final long serialVersionUID = -8736387615792206568L;
    private final String dataFlowName;

    /**
     * Ctor.
     *
     * @param message      cancel message
     * @param dataFlowName data flow name
     */
    public EPDataFlowCancellationException(String message, String dataFlowName) {
        super(message);
        this.dataFlowName = dataFlowName;
    }

    /**
     * Ctor.
     *
     * @param message      cancel message
     * @param cause        cause
     * @param dataFlowName data flow name
     */
    public EPDataFlowCancellationException(String message, Throwable cause, String dataFlowName) {
        super(message, cause);
        this.dataFlowName = dataFlowName;
    }

    /**
     * Ctor.
     *
     * @param cause        cause
     * @param dataFlowName data flow name
     */
    public EPDataFlowCancellationException(Throwable cause, String dataFlowName) {
        super(cause);
        this.dataFlowName = dataFlowName;
    }

    /**
     * Returns the data flow name.
     *
     * @return name
     */
    public String getDataFlowName() {
        return dataFlowName;
    }
}
