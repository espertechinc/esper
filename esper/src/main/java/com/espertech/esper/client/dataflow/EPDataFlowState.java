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

/**
 * Data flow instance states.
 */
public enum EPDataFlowState {
    /**
     * Start state: the state a data flow instance is in when it gets instantiated.
     */
    INSTANTIATED,

    /**
     * Running means the data flow instance is currently executing.
     */
    RUNNING,

    /**
     * Complete means the data flow instance completed.
     */
    COMPLETE,

    /**
     * Cancelled means the data flow instance was cancelled.
     */
    CANCELLED,
}
