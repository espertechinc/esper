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
package com.espertech.esper.epl.metric;

/**
 * Interface for producing a metric events.
 */
public interface MetricExec {
    /**
     * Execute the production of metric events.
     *
     * @param context provides services and scheduling
     */
    public void execute(MetricExecutionContext context);
}
