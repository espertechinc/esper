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
 * Collector for use with the {@link com.espertech.esper.dataflow.ops.EPStatementSource} operator.
 */
public interface EPDataFlowIRStreamCollector {
    /**
     * Collect: use the context to transform statement output event(s) to data flow event(s).
     *
     * @param context contains event bean, emitter and related information
     */
    public void collect(EPDataFlowIRStreamCollectorContext context);
}
