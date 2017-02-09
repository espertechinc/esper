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
 * Thrown to indicate a data flow execution exception.
 */
public class EPDataFlowExecutionException extends EPException {

    private static final long serialVersionUID = 5107965774689594350L;
    private final String dataFlowName;

    /**
     * Ctor.
     *
     * @param message      error message
     * @param dataFlowName data flow name
     */
    public EPDataFlowExecutionException(String message, String dataFlowName) {
        super(message);
        this.dataFlowName = dataFlowName;
    }

    /**
     * Ctor.
     *
     * @param message      error message
     * @param cause        cuase
     * @param dataFlowName data flow name
     */
    public EPDataFlowExecutionException(String message, Throwable cause, String dataFlowName) {
        super(message, cause);
        this.dataFlowName = dataFlowName;
    }

    /**
     * Ctor.
     *
     * @param cause        cuase
     * @param dataFlowName data flow name
     */
    public EPDataFlowExecutionException(Throwable cause, String dataFlowName) {
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
