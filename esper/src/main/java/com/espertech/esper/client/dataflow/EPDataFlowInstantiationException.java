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
 * Indicates an exception instantiating a data flow.
 */
public class EPDataFlowInstantiationException extends EPException {

    private static final long serialVersionUID = -4807968754372920929L;

    /**
     * Ctor.
     *
     * @param message the message
     */
    public EPDataFlowInstantiationException(String message) {
        super(message);
    }

    /**
     * Ctor.
     *
     * @param message the message
     * @param cause   the inner exception
     */
    public EPDataFlowInstantiationException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Ctor.
     *
     * @param cause the inner exception
     */
    public EPDataFlowInstantiationException(Throwable cause) {
        super(cause);
    }
}
